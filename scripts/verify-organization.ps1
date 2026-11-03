param(
    [string]$TestDatabaseUrl,
    [string]$EnvFile,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix='organization'
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot '../../scripts/use-local-toolchain.ps1') -Tools Maven
. (Join-Path $PSScriptRoot 'use-test-environment.ps1')
$framework=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend=Join-Path $framework 'backend'
$tests='OrganizationModelIntegrationTest,UnifiedRoleIdentityTest,UnifiedRoleServiceTest,MemberDataPermissionServiceTest,UnitAdministrationServiceTest,ScopeManagementServiceTest,ArchitectureRulesTest'
$arguments=@('-B','-s',(Join-Path $backend '.m2/settings.xml'),'-pl','cyys-application','-am','package',"-Dtest=$tests",'-Dsurefire.failIfNoSpecifiedTests=false')
$outLog=Join-Path $backend "logs/${LogPrefix}_out.log"
$errLog=Join-Path $backend "logs/${LogPrefix}_err.log"
[IO.Directory]::CreateDirectory((Join-Path $backend 'logs')) | Out-Null
Invoke-WithDevelopmentTestEnvironment -ProjectRoot $framework -EnvFile $EnvFile -DatabaseUrl $TestDatabaseUrl -Action {
    $build=Start-Process -FilePath $env:MAVEN_EXE -ArgumentList $arguments -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
    $build.WaitForExit()
    Get-Content $outLog -Tail 35
    if($build.ExitCode -ne 0){throw "Organization verification failed: $($build.ExitCode)"}
}
