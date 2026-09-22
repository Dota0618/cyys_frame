#requires -Version 7.4
$ErrorActionPreference = 'Stop'
$sourceScript = Join-Path $PSScriptRoot 'deploy-dev.ps1'
$sandbox = Join-Path (Split-Path $PSScriptRoot -Parent) ('backend/logs/deploy-tests-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path "$sandbox/scripts" -Force | Out-Null
Copy-Item -LiteralPath $sourceScript -Destination "$sandbox/scripts/deploy-dev.ps1"
foreach ($file in @('toolchain.ps1', 'scripts/check-release-artifacts.ps1', 'scripts/check-deployment-http.ps1')) {
    Set-Content -LiteralPath (Join-Path $sandbox $file) -Value '# isolated test stub'
}
. "$sandbox/scripts/deploy-dev.ps1" -ToolchainScript "$sandbox/toolchain.ps1" -MySqlDump "$sandbox/dump.exe"
Set-Content -LiteralPath "$sandbox/dump.exe" -Value 'not executed'
$ToolchainScript = "$sandbox/toolchain.ps1"
$MySqlDump = "$sandbox/dump.exe"
function Assert([bool]$Condition, [string]$Message) { if (!$Condition) { throw $Message } }
function Invoke-DevCommand {
    param($Name, $Program, $Arguments, $Directory)
    $stages.Add($Name)
    $commands[$Name] = @($Arguments)
    if ($Name -eq $failStage) { throw "simulated $Name failure" }
    switch ($Name) {
        'docker-context' { 'npipe:////./pipe/dockerDesktopLinuxEngine' }
        'configuration' { $config | ConvertTo-Json -Depth 10 }
        'running-services' { "api`nweb" }
    }
}
function Backup-DevData {
    $stages.Add('backup')
    if ($failStage -eq 'backup') { throw 'simulated backup failure' }
}
function Wait-DevReady { $stages.Add('ready') }
$cases = @('check', 'build-only', 'remote-db', 'public-port', 'backend-package', 'backup', 'start-services', 'success', 'web-only', 'api-only', 'web-build', 'api-build', 'api-backup-failure', 'web-start-failure')
foreach ($case in $cases) {
    $framework = Join-Path $sandbox $case
    foreach ($directory in @('deploy', 'backend/.m2', 'backend/logs', 'admin-ui', 'files', 'secrets')) {
        New-Item -ItemType Directory -Path (Join-Path $framework $directory) -Force | Out-Null
    }
    Set-Content -LiteralPath "$framework/deploy/.env.dev" -Value @('CYYS_API_IMAGE=old-api', 'CYYS_WEB_IMAGE=old-web')
    Set-Content -LiteralPath "$framework/backend/.m2/settings.xml" -Value '<settings/>'
    Set-Content -LiteralPath "$framework/admin-ui/package-lock.json" -Value '{}'
    Set-Content -LiteralPath "$framework/secrets/password" -Value 'test-only'
    foreach ($file in @('compose.yml', 'runtime.yml', 'nginx.conf')) { Set-Content -LiteralPath "$framework/deploy/$file" -Value '# fixture' }
    $EnvFile = "$framework/deploy/.env.dev"
    $Target = if ($case -like 'web-*') { 'web' } elseif ($case -like 'api-*') { 'api' } else { 'all' }
    $Mode = switch ($case) { 'check' { 'Check' }; { $_ -in @('build-only', 'web-build', 'api-build') } { 'Build' }; default { 'Deploy' } }
    $stages = [Collections.Generic.List[string]]::new()
    $commands = @{}
    $failStage = $case
    if ($case -eq 'api-backup-failure') { $failStage = 'backup' }
    if ($case -eq 'web-start-failure') { $failStage = 'start-services' }
    $config = [pscustomobject]@{
        services = [pscustomobject]@{
            api = [pscustomobject]@{
                environment = [pscustomobject]@{ CYYS_ENV='development'; CYYS_DB_URL='jdbc:mysql://host.docker.internal:3306/cyys_frame'; CYYS_DB_USERNAME='test'; CYYS_REDIS_HOST='host.docker.internal' }
                build = [pscustomobject]@{ args = [pscustomobject]@{ JAVA_RUNTIME_IMAGE=('image@sha256:' + ('a' * 64)) } }
                volumes = @([pscustomobject]@{ target='/var/lib/cyys/files'; source="$framework/files" })
            }
            web = [pscustomobject]@{
                ports = @([pscustomobject]@{host_ip='127.0.0.1'; published='80'}, [pscustomobject]@{host_ip='127.0.0.1'; published='443'})
                build = [pscustomobject]@{ args = [pscustomobject]@{ NGINX_RUNTIME_IMAGE=('image@sha256:' + ('a' * 64)) } }
            }
        }
        secrets = [pscustomobject]@{
            db_password = [pscustomobject]@{ file="$framework/secrets/password" }
            redis_password = [pscustomobject]@{ file="$framework/secrets/password" }
            tls_certificate = [pscustomobject]@{ file="$framework/secrets/password" }
            tls_key = [pscustomobject]@{ file="$framework/secrets/password" }
        }
    }
    $MySqlDump = "$sandbox/dump.exe"
    if ($Target -eq 'web') {
        $MySqlDump = "$sandbox/missing-dump.exe"
        Remove-Item -LiteralPath "$framework/backend/.m2/settings.xml"
        $config.secrets.db_password.file = "$framework/missing-db-password"
        $config.secrets.redis_password.file = "$framework/missing-redis-password"
        $config.services.api.volumes[0].source = "$framework/missing-private-files"
    }
    if ($Target -eq 'api') { Remove-Item -LiteralPath "$framework/admin-ui/package-lock.json" }
    if ($case -eq 'remote-db') { $config.services.api.environment.CYYS_DB_URL = 'jdbc:mysql://production.example:3306/cyys_frame' }
    if ($case -eq 'public-port') { $config.services.web.ports[0].host_ip = '0.0.0.0' }
    $failed = $false
    $env:CYYS_DEPLOY_TEST_MARKER = 'must-be-restored'
    try { Invoke-DevDeployment } catch { $failed = $true; $failureMessage = $_.Exception.Message }
    $expectedFailure = $case -notin @('check', 'build-only', 'success', 'web-only', 'api-only', 'web-build', 'api-build')
    Assert ($failed -eq $expectedFailure) "$case : unexpected result $failureMessage"
    Assert ($env:CYYS_DEPLOY_TEST_MARKER -eq 'must-be-restored') "$case : environment not restored"
    if ($commands.ContainsKey('backend-package')) {
        Assert ($commands['backend-package'] -contains 'cyys-application' -and $commands['backend-package'] -notcontains 'cyys-business') 'Backend build did not use the application module'
    }
    if ($Target -eq 'web') {
        Assert (!$stages.Contains('backend-package') -and !$stages.Contains('backup') -and !$stages.Contains('stop-services')) 'Web deployment touched backend or database'
        Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'CYYS_API_IMAGE=old-api') 'Web deployment changed API image'
    }
    if ($Target -eq 'api') {
        Assert (!$stages.Contains('frontend-build') -and !$stages.Contains('frontend-typecheck') -and !$stages.Contains('frontend-dependencies') -and !$stages.Contains('nginx-check')) 'API deployment touched frontend build'
        Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'CYYS_WEB_IMAGE=old-web') 'API deployment changed Web image'
        foreach ($stage in @('stop-services', 'resume-previous-services')) {
            if ($commands.ContainsKey($stage)) { Assert ($commands[$stage][-1] -eq 'api' -and $commands[$stage] -notcontains 'web') 'API deployment touched Web container' }
        }
    }
    if ($Target -ne 'all' -and $commands.ContainsKey('start-services')) {
        Assert ($commands['start-services'][-1] -eq $Target -and $commands['start-services'] -contains '--no-deps') 'Single-service update could recreate dependencies'
        $other = if ($Target -eq 'web') { 'api' } else { 'web' }
        Assert ($commands['start-services'] -notcontains $other -and $commands['image-build'] -notcontains $other) 'Single-service update included another service'
    }
    $result = Get-ChildItem -Path "$framework/backend/logs/dev-*/result.json" | Get-Content -Raw | ConvertFrom-Json
    switch ($case) {
        'check' { Assert (($stages -join ',') -eq 'docker-context,configuration,docker-status') 'Check mutated deployment' }
        { $_ -in @('build-only', 'web-build', 'api-build') } { Assert (!$stages.Contains('stop-services') -and $result.status -eq 'BUILT') 'Build mode touched services' }
        { $_ -in @('remote-db', 'public-port', 'backend-package') } { Assert (!$stages.Contains('stop-services')) 'Old service stopped before successful build' }
        { $_ -in @('backup', 'api-backup-failure') } { Assert ($stages.Contains('resume-previous-services') -and !$stages.Contains('start-services')) 'Backup failure recovery incorrect' }
        'start-services' { Assert ($stages.Contains('backup') -and !$stages.Contains('resume-previous-services')) 'Unsafe automatic rollback' }
        'success' {
            Assert ($stages.IndexOf('backup') -lt $stages.IndexOf('start-services')) 'Started before backup'
            Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'cyys-framework-api:dev-') 'Image configuration not saved'
            Assert ($result.status -eq 'DEPLOYED') 'Success not recorded'
        }
        'web-only' { Assert ($result.status -eq 'DEPLOYED' -and (Get-Content -LiteralPath $EnvFile -Raw) -match 'CYYS_WEB_IMAGE=cyys-framework-web:dev-') 'Web update not recorded' }
        'api-only' { Assert ($result.status -eq 'DEPLOYED' -and $stages.Contains('backup')) 'API update skipped backup or failed' }
    }
    if ($expectedFailure) { Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'old-api') 'Failed deployment overwrote configuration' }
    Write-Host "PASS: $case"
}
Remove-Variable -Name Target
$stages.Clear()
$missingTargetRejected = $false
try { Invoke-DevDeployment } catch { $missingTargetRejected = $_.Exception.Message -match '请指定部署范围' }
Assert ($missingTargetRejected -and $stages.Count -eq 0) 'Missing scope triggered deployment'
Remove-Item Env:CYYS_DEPLOY_TEST_MARKER
Write-Host "$($cases.Count + 1) isolated deployment checks passed; no real services or databases were changed."
