param([switch]$Clean, [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix = 'verify', [string]$EnvFile)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../../scripts/use-local-toolchain.ps1') -Tools Maven
. (Join-Path $PSScriptRoot 'use-test-environment.ps1')
$framework = Split-Path $PSScriptRoot -Parent
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\backend'))
$arguments = @('-B','-s',(Join-Path $backend '.m2\settings.xml'))
if ($Clean) { $arguments += 'clean' }
$arguments += 'verify'
$outLog = Join-Path $backend "logs\${LogPrefix}_out.log"
$errLog = Join-Path $backend "logs\${LogPrefix}_err.log"
[IO.Directory]::CreateDirectory((Join-Path $backend 'logs')) | Out-Null
Invoke-WithDevelopmentTestEnvironment -ProjectRoot $framework -EnvFile $EnvFile -Action {
    $build = Start-Process -FilePath $env:MAVEN_EXE -ArgumentList $arguments -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $build.WaitForExit()
    Get-Content $outLog -Tail 45
    if ($build.ExitCode -ne 0) { throw "Maven verification failed: $($build.ExitCode)" }
}
