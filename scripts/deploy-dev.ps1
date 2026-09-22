#requires -Version 7.4
[CmdletBinding()]
param(
    [Parameter(Position = 0)][ValidateSet('web', 'api', 'all')][string]$Target,
    [ValidateSet('Deploy', 'Check', 'Build')][string]$Mode = 'Deploy',
    [string]$EnvFile,
    [string]$ToolchainScript,
    [string]$MySqlDump,
    [ValidatePattern('^[a-z0-9][a-z0-9_-]*$')][string]$Project = 'cyys-local-acceptance'
)
$ErrorActionPreference = 'Stop'
$framework = Split-Path $PSScriptRoot -Parent

function Invoke-DevCommand {
    param([string]$Name, [string]$Program, [string[]]$Arguments, [string]$Directory = $framework)
    Write-Host "[$Name]"
    # Start-Process 会拼接参数；逐项引用以支持含空格的路径。
    $quoted = foreach ($argument in $Arguments) {
        if ($argument -match '["\r\n]') { throw '命令参数不能包含引号或换行。' }
        '"' + $argument.TrimEnd('\') + '"'
    }
    $out = Join-Path $logDir "$Name.out.log"
    $err = Join-Path $logDir "$Name.err.log"
    $process = Start-Process -FilePath $Program -ArgumentList $quoted -WorkingDirectory $Directory -WindowStyle Hidden -PassThru -RedirectStandardOutput $out -RedirectStandardError $err
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "$Name 失败，查看 $out 和 $err" }
    Get-Content -LiteralPath $out -Raw
}

function Invoke-DevCompose {
    param([string]$Name, [string[]]$Arguments, [string]$Configuration = $EnvFile)
    Invoke-DevCommand $Name $docker (@('--context', 'desktop-linux', 'compose', '--env-file', $Configuration, '-f', $compose, '-p', $Project) + $Arguments)
}

function Assert-DevConfiguration {
    param($Configuration)
    $api = $Configuration.services.api
    $web = $Configuration.services.web
    if ($api.environment.CYYS_ENV -notmatch '(?i)(^|[-_])(dev|development|local)([-_]|$)') { throw '仅支持已有的 local/dev 开发环境命名空间。' }
    if ($Target -ne 'web') {
    if ($api.environment.CYYS_DB_URL -notmatch '^jdbc:mysql://(host\.docker\.internal|localhost|127\.0\.0\.1):(?<port>\d+)/(?<schema>[a-zA-Z0-9_]+)(\?|$)') { throw '开发部署仅连接本机 MySQL，URL 必须显式包含端口。' }
    if ($Matches.schema -in @('mysql', 'sys', 'information_schema', 'performance_schema')) { throw '不能使用 MySQL 系统库。' }
    if ($api.environment.CYYS_DB_URL -match '(?i)[?&](user|password)=') { throw '数据库凭据应使用独立配置，不能嵌入 URL。' }
    if ($api.environment.CYYS_REDIS_HOST -notin @('host.docker.internal', 'localhost', '127.0.0.1')) { throw '开发部署仅连接本机 Redis。' }
    }
    if ($api.ports -or @($web.ports).Count -ne 2 -or @($web.ports | Where-Object { $_.host_ip -ne '127.0.0.1' }).Count -gt 0 -or (@($web.ports.published | Sort-Object) -join ',') -ne '443,80') { throw '开发环境仅允许 Web 绑定 127.0.0.1 的 80/443 端口。' }
    foreach ($name in $services) {
        $service = $Configuration.services.$name
        $base = @($service.build.args.PSObject.Properties.Value)
        if ($base.Count -ne 1 -or $base[0] -notmatch '@sha256:[a-f0-9]{64}$') { throw '基础镜像必须使用已配置的 digest。' }
    }
    foreach ($name in $secretNames) {
        if (!(Test-Path -LiteralPath $Configuration.secrets.$name.file -PathType Leaf)) { throw '缺少部署凭据或证书文件，请按 deploy/development.md 完成首次配置。' }
    }
    if ($Target -ne 'web') {
    $storage = @($api.volumes | Where-Object target -eq '/var/lib/cyys/files')
    if ($storage.Count -ne 1 -or !(Test-Path -LiteralPath $storage[0].source -PathType Container)) { throw '现有私有文件目录不存在。' }
    if ((Get-Item -LiteralPath $storage[0].source).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw '私有文件根目录不能是链接。' }
    }
}

function Backup-DevData {
    param($Configuration, [string]$Destination)
    $api = $Configuration.services.api
    $null = $api.environment.CYYS_DB_URL -match '^jdbc:mysql://[^:]+:(?<port>\d+)/(?<schema>[a-zA-Z0-9_]+)'
    $port = $Matches.port
    $schema = $Matches.schema
    $password = [IO.File]::ReadAllText($Configuration.secrets.db_password.file)
    $clientFile = Join-Path $Destination 'mysql-client.cnf'
    function Quote-MySql([string]$Value) { '"' + $Value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '\r').Replace("`n", '\n') + '"' }
    try {
        [IO.File]::WriteAllLines($clientFile, @('[client]', 'host=127.0.0.1', "port=$port", 'default-character-set=utf8mb4', ('user=' + (Quote-MySql $api.environment.CYYS_DB_USERNAME)), ('password=' + (Quote-MySql $password))))
        $dumpFile = Join-Path $Destination 'database.sql'
        Invoke-DevCommand 'database-backup' $MySqlDump @("--defaults-extra-file=$clientFile", '--single-transaction', '--routines', '--triggers', '--events', '--hex-blob', '--no-tablespaces', '--set-gtid-purged=OFF', "--result-file=$dumpFile", $schema) | Out-Null
        if (!(Test-Path -LiteralPath $dumpFile) -or (Get-Item -LiteralPath $dumpFile).Length -eq 0) { throw '数据库备份为空，停止部署。' }
    } finally {
        if (Test-Path -LiteralPath $clientFile) { Remove-Item -LiteralPath $clientFile }
        $password = $null
    }
    $storage = ($api.volumes | Where-Object target -eq '/var/lib/cyys/files').source
    if (@(Get-ChildItem -LiteralPath $storage -Force -Recurse | Where-Object { $_.Attributes -band [IO.FileAttributes]::ReparsePoint }).Count -gt 0) { throw '私有文件目录含链接，停止自动备份。' }
    Copy-Item -LiteralPath $storage -Destination (Join-Path $Destination 'private-files') -Recurse -Force
}

function Backup-DevConfiguration {
    param($Configuration, [string]$Destination)
    Copy-Item -LiteralPath $EnvFile -Destination (Join-Path $Destination 'previous.env')
    $files = @('compose.yml')
    if ($Target -ne 'web') { $files += 'runtime.yml' }
    if ($Target -ne 'api') { $files += 'nginx.conf' }
    foreach ($name in $files) {
        Copy-Item -LiteralPath (Join-Path $framework "deploy/$name") -Destination $Destination
    }
    $secretsDir = New-Item -ItemType Directory -Path (Join-Path $Destination 'secrets')
    foreach ($name in $secretNames) {
        Copy-Item -LiteralPath $Configuration.secrets.$name.file -Destination (Join-Path $secretsDir.FullName $name)
    }
}

function Wait-DevReady {
    $path = if ($Target -eq 'web') { '/system/users' } else { '/api/health' }
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.ServerCertificateCustomValidationCallback = [Net.Http.HttpClientHandler]::DangerousAcceptAnyServerCertificateValidator
    $client = [Net.Http.HttpClient]::new($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(5)
    try {
        $ready = $false
        $deadline = [DateTime]::UtcNow.AddSeconds(120)
        do {
            try {
                $response = $client.GetAsync('https://localhost' + $path).GetAwaiter().GetResult()
                try { $ready = [int]$response.StatusCode -eq 200 } finally { $response.Dispose() }
            } catch { $ready = $false }
            if (!$ready) { Start-Sleep -Seconds 2 }
        } while (!$ready -and [DateTime]::UtcNow -lt $deadline)
        if (!$ready) { throw '服务未在 120 秒内就绪。' }
    } finally { $client.Dispose(); $handler.Dispose() }
}

function Invoke-DevDeployment {
    if (!$Target) { throw '请指定部署范围：./scripts/deploy-dev.ps1 web（前端）、api（后端）或 all（全部）。' }
    $services = if ($Target -eq 'all') { @('api', 'web') } else { @($Target) }
    $secretNames = @()
    if ($Target -ne 'web') { $secretNames += @('db_password', 'redis_password') }
    if ($Target -ne 'api') { $secretNames += @('tls_certificate', 'tls_key') }
    $run = 'dev-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '-' + [guid]::NewGuid().ToString('N').Substring(0, 6)
    $logDir = Join-Path $framework "backend/logs/$run"
    $compose = Join-Path $framework 'deploy/compose.yml'
    if (!$EnvFile) {
        $EnvFile = Join-Path $framework 'deploy/.env.dev'
        if (!(Test-Path -LiteralPath $EnvFile)) { $EnvFile = Join-Path $framework 'backend/cyys-business/target/docker-local/.env' }
    }
    $EnvFile = [IO.Path]::GetFullPath($EnvFile)
    if (!(Test-Path -LiteralPath $EnvFile -PathType Leaf)) { throw '缺少开发环境配置。请先按 deploy/development.md 配置 deploy/.env.dev。' }
    if (!$ToolchainScript) { $ToolchainScript = Join-Path (Split-Path $framework -Parent) 'scripts/use-local-toolchain.ps1' }
    if (!(Test-Path -LiteralPath $ToolchainScript -PathType Leaf)) { throw '缺少工具链脚本，请通过 -ToolchainScript 指定本机脚本。' }
    . $ToolchainScript
    $docker = (Get-Command docker.exe -ErrorAction Stop).Source
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
    $lock = [IO.File]::Open((Join-Path $framework 'backend/logs/dev-deployment.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    $savedEnvironment = @{}
    $stopped = $false
    $activationAttempted = $false
    $running = @()
    $backup = $null
    $report = [ordered]@{ status = 'FAILED'; target = $Target; mode = $Mode; startedAt = [DateTimeOffset]::Now.ToString('o'); backup = $null; url = 'https://localhost/'; error = $null }
    try {
        # Compose 的进程变量优先于 env 文件；日常部署只使用选定文件。
        foreach ($item in @(Get-ChildItem Env: | Where-Object Name -match '^(CYYS_|COMPOSE_)')) {
            $savedEnvironment[$item.Name] = $item.Value
            [Environment]::SetEnvironmentVariable($item.Name, $null, 'Process')
        }
        $endpoint = Invoke-DevCommand 'docker-context' $docker @('context', 'inspect', 'desktop-linux', '--format', '{{.Endpoints.docker.Host}}')
        if ($endpoint.Trim() -notmatch '^npipe://') { throw '开发部署仅支持本机 Docker Desktop Linux 引擎。' }
        $originalHash = (Get-FileHash -LiteralPath $EnvFile).Hash
        $configuration = (Invoke-DevCompose 'configuration' @('config', '--format', 'json')) | ConvertFrom-Json
        Assert-DevConfiguration $configuration
        if ($Target -ne 'web') {
        if (!$MySqlDump) {
            $MySqlDump = (Get-Command mysqldump.exe -ErrorAction SilentlyContinue).Source
            if (!$MySqlDump) {
                $candidates = @(Get-ChildItem -Path "$env:ProgramFiles/MySQL/*/bin/mysqldump.exe" -ErrorAction SilentlyContinue)
                if ($candidates.Count -eq 1) { $MySqlDump = $candidates[0].FullName }
            }
        }
        if (!$MySqlDump -or !(Test-Path -LiteralPath $MySqlDump -PathType Leaf)) { throw '缺少 mysqldump，请通过 -MySqlDump 指定；部署前需要备份现有数据库。' }
        $settings = Join-Path $framework 'backend/.m2/settings.xml'
        if (!(Test-Path -LiteralPath $settings)) { throw '缺少 backend/.m2/settings.xml，参见首次配置说明。' }
        }
        if ($Mode -eq 'Check') {
            Invoke-DevCompose 'docker-status' @('ps', '--format', 'json') | Out-Null
            $report.status = 'CHECKED'
            return
        }
        $backend = Join-Path $framework 'backend'
        $frontend = Join-Path $framework 'admin-ui'
        # 当前本机配置和附件可能位于 target 下，不能执行 clean。
        if ($Target -ne 'web') {
            Invoke-DevCommand 'backend-package' "$env:MAVEN_HOME/bin/mvn.cmd" @('-B', '-s', '.m2/settings.xml', '-pl', 'cyys-application', '-am', 'package', '-DskipTests') $backend | Out-Null
        }
        if ($Target -ne 'api') {
        $lockHash = (Get-FileHash -LiteralPath (Join-Path $frontend 'package-lock.json')).Hash
        $stamp = Join-Path $backend 'logs/dev-npm-lock.sha256'
        if (!(Test-Path -LiteralPath (Join-Path $frontend 'node_modules')) -or !(Test-Path -LiteralPath $stamp) -or (Get-Content -LiteralPath $stamp -Raw).Trim() -ne $lockHash) {
            Invoke-DevCommand 'frontend-dependencies' "$env:NODE_HOME/npm.cmd" @('ci', '--no-audit', '--no-fund') $frontend | Out-Null
            Set-Content -LiteralPath $stamp -Value $lockHash
        }
        Invoke-DevCommand 'frontend-typecheck' "$env:NODE_HOME/npm.cmd" @('run', 'typecheck') $frontend | Out-Null
        Invoke-DevCommand 'frontend-build' "$env:NODE_HOME/npm.cmd" @('run', 'build') $frontend | Out-Null
        }
        & (Join-Path $PSScriptRoot 'check-release-artifacts.ps1') -Target $Target
        if ($Mode -eq 'Build') { $report.status = 'BUILT'; return }

        $backup = Join-Path $framework "deploy/.local/$run"
        New-Item -ItemType Directory -Path $backup | Out-Null
        $report.backup = $backup
        $candidate = Join-Path $backup 'next.env'
        $imageKeys = @($services | ForEach-Object { 'CYYS_' + $_.ToUpperInvariant() + '_IMAGE' })
        $lines = @(Get-Content -LiteralPath $EnvFile | Where-Object { $_ -notmatch '^\s*(CYYS_(API|WEB)_IMAGE)\s*=' -or $Matches[1] -notin $imageKeys })
        $images = @($services | ForEach-Object { 'CYYS_' + $_.ToUpperInvariant() + "_IMAGE=cyys-framework-${_}:$run" })
        [IO.File]::WriteAllLines($candidate, $lines + $images)
        Invoke-DevCompose 'image-build' (@('build') + $services) $candidate | Out-Null
        if ($Target -ne 'api') { Invoke-DevCompose 'nginx-check' @('run', '--rm', '--no-deps', 'web', '-t') $candidate | Out-Null }
        if ((Get-FileHash -LiteralPath $EnvFile).Hash -ne $originalHash) { throw '部署期间配置发生变化，已停止更新。' }
        Backup-DevConfiguration $configuration $backup
        if ($Target -ne 'web') {
            $running = @((Invoke-DevCompose 'running-services' @('ps', '--services', '--status', 'running')) -split '\r?\n' | Where-Object { $_ -in $services })
            $stopped = $true
            Invoke-DevCompose 'stop-services' (@('stop') + $services) | Out-Null
            Backup-DevData $configuration $backup
        }
        $activationAttempted = $true
        Invoke-DevCompose 'start-services' (@('up', '-d', '--no-build', '--no-deps', '--force-recreate') + $services) $candidate | Out-Null
        Wait-DevReady
        & (Join-Path $PSScriptRoot 'check-deployment-http.ps1') -Target $Target -BaseUrl 'https://localhost/' -LocalCertificate -ReportPath (Join-Path $logDir 'http.json')
        if ((Get-FileHash -LiteralPath $EnvFile).Hash -ne $originalHash) { throw '服务已更新，但环境文件被其他进程修改；请根据 next.env 核对，未覆盖原文件。' }
        Copy-Item -LiteralPath $candidate -Destination $EnvFile -Force
        $report.status = 'DEPLOYED'
        Write-Host "开发环境已更新（$Target）：https://localhost/"
        if ($Target -ne 'web') { Write-Host 'API 已重启，需要重新登录。' }
    } catch {
        $report.error = $_.Exception.Message
        if ($stopped -and !$activationAttempted -and $running.Count -gt 0) {
            try { Invoke-DevCompose 'resume-previous-services' (@('start') + $running) | Out-Null }
            catch { Write-Warning '旧服务恢复失败，请查看 resume-previous-services 日志。' }
        }
        if ($activationAttempted) { Write-Warning "已尝试更新 $Target，未自动回滚。备份及新旧镜像配置：$backup" }
        throw
    } finally {
        foreach ($item in $savedEnvironment.GetEnumerator()) { [Environment]::SetEnvironmentVariable($item.Key, $item.Value, 'Process') }
        $report.finishedAt = [DateTimeOffset]::Now.ToString('o')
        try { $report | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $logDir 'result.json') -Encoding utf8 }
        finally { $lock.Dispose() }
        Write-Host "结果与日志：$logDir"
    }
}

if ($MyInvocation.InvocationName -ne '.') { Invoke-DevDeployment }
