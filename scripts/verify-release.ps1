param(
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix = ('release-' + (Get-Date -Format 'yyyyMMdd-HHmmss')),
    [switch]$InstallDependencies
)
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend = Join-Path $framework 'backend'
$frontend = Join-Path $framework 'admin-ui'
$logs = Join-Path $backend 'logs'
[IO.Directory]::CreateDirectory($logs) | Out-Null
$reportPath = Join-Path $logs "${LogPrefix}_release.json"
if (Test-Path -LiteralPath $reportPath) { throw 'This log prefix already has a release record; choose a new prefix.' }

# 文件句柄跨进程/会话互斥；退出即释放，磁盘上的空文件不是运行中的证明。
try {
    $verificationLock = [IO.File]::Open((Join-Path $logs 'release-verification.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
} catch [IO.IOException] { throw 'Another release verification is using this workspace. Do not run database suites in parallel.' }

$report = [ordered]@{
    startedAt = [DateTimeOffset]::Now.ToString('o')
    finishedAt = $null
    commit = $null
    status = 'FAILED'
    steps = [Collections.Generic.List[object]]::new()
    artifacts = @()
}

function Invoke-Stage([string]$Name, [scriptblock]$Action) {
    $step = [ordered]@{name=$Name; startedAt=[DateTimeOffset]::Now.ToString('o'); finishedAt=$null; status='FAILED'}
    $report.steps.Add($step)
    try { & $Action; $step.status = 'PASSED' }
    finally { $step.finishedAt = [DateTimeOffset]::Now.ToString('o') }
}

function Invoke-Frontend([string]$Name, [string[]]$Arguments) {
    $outLog = Join-Path $logs "${LogPrefix}_${Name}_out.log"
    $errLog = Join-Path $logs "${LogPrefix}_${Name}_err.log"
    $process = Start-Process -FilePath "$env:NODE_HOME\npm.cmd" -ArgumentList $Arguments -WorkingDirectory $frontend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "$Name failed; see $outLog and $errLog" }
    if ($Name -eq 'antd') {
        & (Join-Path $PSScriptRoot 'check-antd-findings.ps1') -ReportPath $outLog
    }
}

Push-Location $framework
try {
    Invoke-Stage 'source-and-toolchain' {
        $report.commit = (& git rev-parse HEAD | Out-String).Trim()
        if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve the source commit.' }
        $dirty = & git status --porcelain
        if ($LASTEXITCODE -ne 0 -or $dirty) { throw 'Commit source changes before release verification.' }
        if (!(Test-Path -LiteralPath (Join-Path $backend '.m2\settings.xml'))) { throw 'The required Maven settings file is missing.' }
        if (!(Test-Path -LiteralPath (Join-Path $frontend 'package-lock.json'))) { throw 'The frontend lockfile is missing.' }
    }
    Invoke-Stage 'frontend-dependencies' {
        if ($InstallDependencies -or !(Test-Path -LiteralPath (Join-Path $frontend 'node_modules'))) {
            Invoke-Frontend 'npm-ci' @('ci','--no-audit','--no-fund')
        }
    }
    Invoke-Stage 'backend-verify' { & (Join-Path $PSScriptRoot 'verify.ps1') -Clean -LogPrefix "${LogPrefix}_backend" }
    Invoke-Stage 'frontend-typecheck' { Invoke-Frontend 'typecheck' @('run','typecheck') }
    Invoke-Stage 'frontend-antd' { Invoke-Frontend 'antd' @('run','antd','--','lint','src','--format','json') }
    Invoke-Stage 'frontend-build' { Invoke-Frontend 'build' @('run','build') }
    # BrowserAcceptance 在独立测试启动中拥有/清理自己的 MySQL 数据；与前一阶段不重叠。
    Invoke-Stage 'real-backend-browser' { & (Join-Path $PSScriptRoot 'verify-ui.ps1') -SkipBuild -LogPrefix "${LogPrefix}_ui" }
    Invoke-Stage 'artifact-structure' {
        & (Join-Path $PSScriptRoot 'check-release-artifacts.ps1')
        $jar = Join-Path $backend 'cyys-application\target\cyys-application.jar'
        $dist = Join-Path $frontend 'dist'
        $files = @((Get-Item -LiteralPath $jar)) + @(Get-ChildItem -LiteralPath $dist -Recurse -File)
        $report.artifacts = @($files | Sort-Object FullName | ForEach-Object {
            [ordered]@{path=[IO.Path]::GetRelativePath($framework, $_.FullName); sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash; bytes=$_.Length}
        })
    }
    Invoke-Stage 'source-unchanged' {
        $commit = (& git rev-parse HEAD | Out-String).Trim()
        if ($LASTEXITCODE -ne 0 -or $commit -ne $report.commit) { throw 'The source commit changed during verification.' }
        $dirty = & git status --porcelain
        if ($LASTEXITCODE -ne 0 -or $dirty) { throw 'Source files changed during verification.' }
    }
    $report.status = 'PASSED'
} finally {
    Pop-Location
    $report.finishedAt = [DateTimeOffset]::Now.ToString('o')
    try { $report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $reportPath -Encoding utf8 }
    finally { $verificationLock.Dispose() }
    Write-Output "Release verification record: $reportPath ($($report.status))"
}
