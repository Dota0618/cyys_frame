param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend = Join-Path $framework 'backend'
$frontend = Join-Path $framework 'admin-ui'
if (-not $SkipBuild) {
    foreach ($step in @('typecheck','build')) {
        $outLog = Join-Path $backend "logs\phase4_ui_${step}_out.log"
        $errLog = Join-Path $backend "logs\phase4_ui_${step}_err.log"
        $process = Start-Process -FilePath "$env:NODE_HOME\npm.cmd" -ArgumentList @('run',$step) -WorkingDirectory $frontend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
        $process.WaitForExit()
        Get-Content $outLog -Tail 8
        if ($process.ExitCode -ne 0) { throw "Frontend $step failed; see $errLog" }
    }
}
$outLog = Join-Path $backend 'logs\phase4_browser_maven_out.log'
$errLog = Join-Path $backend 'logs\phase4_browser_maven_err.log'
$process = Start-Process -FilePath "$env:MAVEN_HOME\bin\mvn.cmd" -ArgumentList @('-B','-s',"$backend\.m2\settings.xml",'-Dtest=BrowserAcceptance','-Dsurefire.failIfNoSpecifiedTests=false','test') -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
$process.WaitForExit()
Get-Content (Join-Path $backend 'logs\phase4_browser_out.log') -Tail 65
if ($process.ExitCode -ne 0) { throw "Browser verification failed; see $outLog" }
