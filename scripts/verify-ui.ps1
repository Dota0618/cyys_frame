param(
    [switch]$SkipBuild,
    [ValidatePattern('^[a-z-]+\.spec\.ts$')][string[]]$Spec,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix = 'phase4'
)
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend = Join-Path $framework 'backend'
$frontend = Join-Path $framework 'admin-ui'
if (-not $SkipBuild) {
    foreach ($step in @('typecheck','build')) {
        $outLog = Join-Path $backend "logs\${LogPrefix}_ui_${step}_out.log"
        $errLog = Join-Path $backend "logs\${LogPrefix}_ui_${step}_err.log"
        $process = Start-Process -FilePath "$env:NODE_HOME\npm.cmd" -ArgumentList @('run',$step) -WorkingDirectory $frontend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
        $process.WaitForExit()
        Get-Content $outLog -Tail 8
        if ($process.ExitCode -ne 0) { throw "Frontend $step failed; see $errLog" }
    }
}
$outLog = Join-Path $backend "logs\${LogPrefix}_browser_maven_out.log"
$errLog = Join-Path $backend "logs\${LogPrefix}_browser_maven_err.log"
$arguments = @('-B','-s',"$backend\.m2\settings.xml",'-Dtest=BrowserAcceptance','-Dsurefire.failIfNoSpecifiedTests=false',"-Dcyys.browser.log-prefix=$LogPrefix")
if ($Spec) { $arguments += "-Dcyys.browser.spec=$($Spec -join ',')" }
$arguments += 'test'
$process = Start-Process -FilePath "$env:MAVEN_HOME\bin\mvn.cmd" -ArgumentList $arguments -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
$process.WaitForExit()
$browserLog = Join-Path $backend "logs\${LogPrefix}_browser_out.log"
if (Test-Path -LiteralPath $browserLog) { Get-Content -LiteralPath $browserLog -Tail 65 }
else { Get-Content -LiteralPath $outLog -Tail 20 }
if ($process.ExitCode -ne 0) { throw "Browser verification failed; see $outLog" }
