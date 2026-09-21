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
$cases = @('check', 'build-only', 'remote-db', 'public-port', 'backend-package', 'backup', 'start-services', 'success')
foreach ($case in $cases) {
    $framework = Join-Path $sandbox $case
    foreach ($directory in @('deploy', 'backend/.m2', 'backend/logs', 'admin-ui', 'files', 'secrets')) {
        New-Item -ItemType Directory -Path (Join-Path $framework $directory) -Force | Out-Null
    }
    Set-Content -LiteralPath "$framework/deploy/.env.dev" -Value @('CYYS_API_IMAGE=old-api', 'CYYS_WEB_IMAGE=old-web')
    Set-Content -LiteralPath "$framework/backend/.m2/settings.xml" -Value '<settings/>'
    Set-Content -LiteralPath "$framework/admin-ui/package-lock.json" -Value '{}'
    Set-Content -LiteralPath "$framework/secrets/password" -Value 'test-only'
    $EnvFile = "$framework/deploy/.env.dev"
    $Mode = switch ($case) { 'check' { 'Check' }; 'build-only' { 'Build' }; default { 'Deploy' } }
    $stages = [Collections.Generic.List[string]]::new()
    $failStage = $case
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
        secrets = [pscustomobject]@{ db_password = [pscustomobject]@{ file="$framework/secrets/password" } }
    }
    if ($case -eq 'remote-db') { $config.services.api.environment.CYYS_DB_URL = 'jdbc:mysql://production.example:3306/cyys_frame' }
    if ($case -eq 'public-port') { $config.services.web.ports[0].host_ip = '0.0.0.0' }
    $failed = $false
    $env:CYYS_DEPLOY_TEST_MARKER = 'must-be-restored'
    try { Invoke-DevDeployment } catch { $failed = $true; $failureMessage = $_.Exception.Message }
    $expectedFailure = $case -notin @('check', 'build-only', 'success')
    Assert ($failed -eq $expectedFailure) "$case : unexpected result $failureMessage"
    Assert ($env:CYYS_DEPLOY_TEST_MARKER -eq 'must-be-restored') "$case : environment not restored"
    $result = Get-ChildItem -Path "$framework/backend/logs/dev-*/result.json" | Get-Content -Raw | ConvertFrom-Json
    switch ($case) {
        'check' { Assert (($stages -join ',') -eq 'docker-context,configuration,docker-status') 'Check mutated deployment' }
        'build-only' { Assert (!$stages.Contains('stop-services') -and $result.status -eq 'BUILT') 'Build mode touched services' }
        { $_ -in @('remote-db', 'public-port', 'backend-package') } { Assert (!$stages.Contains('stop-services')) 'Old service stopped before successful build' }
        'backup' { Assert ($stages.Contains('resume-previous-services') -and !$stages.Contains('start-services')) 'Backup failure recovery incorrect' }
        'start-services' { Assert ($stages.Contains('backup') -and !$stages.Contains('resume-previous-services')) 'Unsafe automatic rollback' }
        'success' {
            Assert ($stages.IndexOf('backup') -lt $stages.IndexOf('start-services')) 'Started before backup'
            Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'cyys-framework-api:dev-') 'Image configuration not saved'
            Assert ($result.status -eq 'DEPLOYED') 'Success not recorded'
        }
    }
    if ($expectedFailure) { Assert ((Get-Content -LiteralPath $EnvFile -Raw) -match 'old-api') 'Failed deployment overwrote configuration' }
    Write-Host "PASS: $case"
}
Remove-Item Env:CYYS_DEPLOY_TEST_MARKER
Write-Host "$($cases.Count) isolated deployment checks passed; no real services or databases were changed."
