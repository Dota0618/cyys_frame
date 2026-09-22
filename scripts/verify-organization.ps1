param(
    [Parameter(Mandatory)][string]$TestDatabaseUrl,
    [ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$LogPrefix='organization'
)
$ErrorActionPreference='Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend=Join-Path $framework 'backend'
if($TestDatabaseUrl -notmatch '^jdbc:mysql://[^/]+/(?<schema>[a-zA-Z0-9_]+)(\?|$)' -or $Matches.schema -eq 'cyys_frame'){
    throw 'Specify a separate existing empty MySQL test schema, never the retained cyys_frame development database.'
}
# 凭据由调用环境提供；不要输出或写回配置。测试自身在 Flyway 前再次检查空库边界。
foreach($name in @('CYYS_TEST_DB_USERNAME','CYYS_TEST_DB_PASSWORD')){
    if([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable($name,'Process'))){throw "Missing environment input: $name"}
}
$env:CYYS_TEST_DB_URL=$TestDatabaseUrl
$env:SPRING_DATASOURCE_URL=$TestDatabaseUrl
$env:SPRING_DATASOURCE_USERNAME=$env:CYYS_TEST_DB_USERNAME
$env:SPRING_DATASOURCE_PASSWORD=$env:CYYS_TEST_DB_PASSWORD
$env:CYYS_ENVIRONMENT='organization-acceptance'
$tests='OrganizationModelIntegrationTest,UnifiedRoleIdentityTest,UnifiedRoleServiceTest,MemberDataPermissionServiceTest,UnitAdministrationServiceTest,ScopeManagementServiceTest,ArchitectureRulesTest'
$arguments=@('-B','-s',(Join-Path $backend '.m2/settings.xml'),'-pl','cyys-application','-am','package',"-Dtest=$tests",'-Dsurefire.failIfNoSpecifiedTests=false')
$outLog=Join-Path $backend "logs/${LogPrefix}_out.log"
$errLog=Join-Path $backend "logs/${LogPrefix}_err.log"
$build=Start-Process -FilePath "$env:MAVEN_HOME/bin/mvn.cmd" -ArgumentList $arguments -WorkingDirectory $backend -WindowStyle Hidden -PassThru -RedirectStandardOutput $outLog -RedirectStandardError $errLog
$build.WaitForExit()
Get-Content $outLog -Tail 35
if($build.ExitCode -ne 0){throw "Organization verification failed: $($build.ExitCode)"}
