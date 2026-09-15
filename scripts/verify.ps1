param([switch]$Clean, [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix = 'verify')
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\backend'))
$arguments = @('-B','-s',(Join-Path $backend '.m2\settings.xml'))
if ($Clean) { $arguments += 'clean' }
$arguments += 'verify'
$outLog = Join-Path $backend "logs\${LogPrefix}_out.log"
$errLog = Join-Path $backend "logs\${LogPrefix}_err.log"
$build = Start-Process -FilePath "$env:MAVEN_HOME\bin\mvn.cmd" -ArgumentList $arguments -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
$build.WaitForExit()
Get-Content $outLog -Tail 45
if ($build.ExitCode -ne 0) { throw "Maven verification failed: $($build.ExitCode)" }
