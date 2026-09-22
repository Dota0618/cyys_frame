param([ValidatePattern('^[a-zA-Z0-9_-]+$')][string]$RunName = ('local-recovery-' + (Get-Date -Format 'yyyyMMdd-HHmmss')))
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$module = Join-Path $framework 'backend/cyys-application'
$runtime = Join-Path $framework 'backend/cyys-business/target/docker-local'
$envFile = Join-Path $runtime '.env'
$compose = Join-Path $framework 'deploy/compose.yml'
$backup = Join-Path $runtime $RunName
$report = Join-Path $framework "backend/logs/$RunName.json"
if ((Test-Path $backup) -or (Test-Path $report)) { throw 'Choose a new run name; existing evidence is never overwritten.' }
if (!(Test-Path $envFile)) { throw 'Prepare the local Docker runtime inputs first.' }
$envLines = Get-Content $envFile
if ($envLines -notcontains 'CYYS_BIND_ADDRESS=127.0.0.1' -or $envLines -notcontains 'CYYS_ISOLATION_MODE=multi') { throw 'Only the prepared loopback multi-mode rehearsal is allowed.' }
[xml]$settings = Get-Content (Join-Path $module 'src/test/resources/application-test.xml')
foreach ($name in @('CYYS_TEST_DB_URL','CYYS_TEST_DB_USERNAME','CYYS_TEST_DB_PASSWORD')) {
    [Environment]::SetEnvironmentVariable($name, ($settings.properties.entry | Where-Object key -eq $name).InnerText, 'Process')
}
if ($env:CYYS_TEST_DB_URL -notmatch '^jdbc:mysql://(127\.0\.0\.1|localhost):3306/cyys_frame\?') { throw 'Only the existing local cyys_frame test database is allowed.' }
$mysql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe'
$dump = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe'
foreach ($path in @($mysql,$dump)) { if (!(Test-Path $path)) { throw "Missing local MySQL tool: $path" } }
[IO.Directory]::CreateDirectory($backup) | Out-Null
$clientConfig = Join-Path $backup 'client.cnf'
function OptionValue([string]$Value) { '"' + $Value.Replace('\','\\').Replace('"','\"').Replace("`r",'\r').Replace("`n",'\n') + '"' }
[IO.File]::WriteAllLines($clientConfig, [string[]]@('[client]','host=127.0.0.1','port=3306','default-character-set=utf8mb4',
    ('user='+ (OptionValue $env:CYYS_TEST_DB_USERNAME)), ('password='+ (OptionValue $env:CYYS_TEST_DB_PASSWORD))))
$password = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$env:CYYS_REHEARSAL_PASSWORD = $password
$classpath = (Join-Path $framework 'backend/.m2/repo/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar') + ';' +
    (Join-Path $framework 'backend/.m2/repo/cn/dev33/sa-token-core/1.46.0/sa-token-core-1.46.0.jar')
$fixture = Join-Path $module 'src/test/java/com/cyys/application/LocalDeploymentFixture.java'
$checks = [Collections.Generic.List[string]]::new()
function Fixture([string]$Action) {
    $output = & "$env:JAVA_HOME/bin/java.exe" -cp $classpath $fixture $Action
    if ($LASTEXITCODE -ne 0) { throw "Fixture $Action refused/failed; no automatic cleanup is attempted." }
    $output
}
function Compose([string[]]$Command, [string]$InputFile = $envFile) {
    & docker compose --env-file $InputFile -f $compose -p cyys-local-acceptance @Command
    if ($LASTEXITCODE -ne 0) { throw "Docker compose failed: $($Command -join ' ')" }
}
function Sql([string]$Query) {
    $result = & $mysql "--defaults-extra-file=$clientConfig" --batch --skip-column-names cyys_frame --execute=$Query
    if ($LASTEXITCODE -ne 0) { throw 'Local SQL operation failed' }
    $result
}
function Api([string]$Method, [string]$Path, [string]$Token = '', [string]$Scope = '', [object]$Body = $null, [int]$Status = 200) {
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($Method), 'https://localhost' + $Path)
    if ($Token) { $request.Headers.Add('Authorization', "Bearer $Token") }
    if ($Scope) { $request.Headers.Add('X-Cyys-Scope-Id', $Scope) }
    if ($Body -is [Net.Http.HttpContent]) { $request.Content = $Body }
    elseif ($null -ne $Body) { $request.Content = [Net.Http.StringContent]::new(($Body | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json') }
    try {
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        try {
            $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            if ([int]$response.StatusCode -ne $Status) { throw "HTTP $Method $Path expected $Status, got $([int]$response.StatusCode)" }
            if ($response.Content.Headers.ContentType.MediaType -eq 'application/json') { return ([Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json) }
            return ,$bytes
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}
function Login { (Api POST '/api/auth/login' -Body @{loginName='local-rehearsal'; password=$password}).data.token }
function Ready {
    $deadline = [DateTime]::UtcNow.AddSeconds(45)
    do {
        try { Api GET '/api/health' | Out-Null; return } catch { $failure = $_ }
        Start-Sleep -Seconds 1
    } while ([DateTime]::UtcNow -lt $deadline)
    throw $failure
}
function Snapshot([string]$Name) {
    $value = Fixture snapshot
    $value | Set-Content (Join-Path $backup "$Name.txt") -Encoding utf8
    $value
}
$handler = [Net.Http.HttpClientHandler]::new()
$handler.ServerCertificateCustomValidationCallback = [Net.Http.HttpClientHandler]::DangerousAcceptAnyServerCertificateValidator
$http = [Net.Http.HttpClient]::new($handler)
$http.Timeout = [TimeSpan]::FromSeconds(10)
$status = 'FAILED'
try {
    Compose @('stop','api')
    Fixture seed | Out-Null
    Compose @('up','-d','--no-build','api')
    Ready
    $token = Login
    foreach ($scope in @('rehearsal-a','rehearsal-b')) {
        $rows = (Api GET '/api/admin/users' $token $scope).data.records
        if ($rows.Count -ne 1) { throw 'Expected one owned member in each scope' }
    }
    $content = [Text.Encoding]::UTF8.GetBytes("本机恢复验收附件`n$RunName")
    $multipart = [Net.Http.MultipartFormDataContent]::new()
    $multipart.Add([Net.Http.ByteArrayContent]::new($content), 'file', 'recovery-proof.txt')
    $file = (Api POST '/api/admin/files' $token 'rehearsal-a' $multipart).data
    $download = Api GET "/api/admin/files/$($file.id)/content" $token 'rehearsal-a'
    if ([Convert]::ToHexString($download) -ne [Convert]::ToHexString($content)) { throw 'Uploaded bytes differ' }
    Api GET "/api/admin/files/$($file.id)/content" $token 'rehearsal-b' -Status 404 | Out-Null
    $checks.Add('multi-mode A/B member isolation and private attachment upload/download')
    $beforeMode = Snapshot 'before-mode'
    $singleFile = Join-Path $backup '.single.env'
    [IO.File]::WriteAllLines($singleFile, [string[]](($envLines | Where-Object {$_ -notmatch '^CYYS_SINGLE_SCOPE_ID='}) -replace '^CYYS_ISOLATION_MODE=multi$','CYYS_ISOLATION_MODE=single') + [string[]]@('CYYS_SINGLE_SCOPE_ID=rehearsal-a'))
    Compose @('up','-d','--no-build','--force-recreate','api') $singleFile
    Ready
    Api GET '/api/auth/me' $token -Status 401 | Out-Null
    $singleToken = Login
    Api GET '/api/admin/users' $singleToken 'rehearsal-a' | Out-Null
    Api GET '/api/admin/users' $singleToken 'rehearsal-b' -Status 403 | Out-Null
    $single = Snapshot 'single-mode'
    $ownership = '^(sys_scope|sys_org|sys_user_scope|sys_file_entry) '
    if (Compare-Object @($beforeMode | Where-Object {$_ -match $ownership}) @($single | Where-Object {$_ -match $ownership})) { throw 'Mode switch changed stored ownership' }
    Compose @('up','-d','--no-build','--force-recreate','api')
    Ready
    Api GET '/api/auth/me' $singleToken -Status 401 | Out-Null
    $token = Login
    Api GET '/api/admin/users' $token 'rehearsal-b' | Out-Null
    $multi = Snapshot 'multi-again'
    if (Compare-Object @($beforeMode | Where-Object {$_ -match $ownership}) @($multi | Where-Object {$_ -match $ownership})) { throw 'Return to multi changed stored ownership' }
    $checks.Add('multi -> single -> multi preserves A/B ownership and invalidates old sessions')
    Compose @('stop','api')
    $before = Snapshot 'before-backup'
    $configurationBackup = Join-Path $backup 'configuration'
    [IO.Directory]::CreateDirectory($configurationBackup) | Out-Null
    Copy-Item -LiteralPath $envFile -Destination (Join-Path $configurationBackup '.env')
    foreach ($name in @('runtime.yml','nginx.conf','compose.yml')) {
        Copy-Item -LiteralPath (Join-Path $framework "deploy/$name") -Destination $configurationBackup
    }
    Copy-Item -LiteralPath (Join-Path $runtime 'secrets') -Destination $configurationBackup -Recurse
    $images = foreach ($name in @('cyys-local-acceptance-api-1','cyys-local-acceptance-web-1')) {
        & docker inspect $name --format '{{.Name}} image={{.Image}}'
        if ($LASTEXITCODE -ne 0) { throw 'Cannot record matching image identity' }
    }
    $images | Set-Content (Join-Path $backup 'images.txt')
    & $dump "--defaults-extra-file=$clientConfig" --single-transaction --no-tablespaces --set-gtid-purged=OFF --skip-add-drop-table "--result-file=$backup/full.sql" cyys_frame
    if ($LASTEXITCODE -ne 0) { throw 'Full backup failed' }
    & $dump "--defaults-extra-file=$clientConfig" --single-transaction --no-tablespaces --set-gtid-purged=OFF --no-create-info --skip-add-locks --skip-disable-keys --skip-triggers --hex-blob --ignore-table=cyys_frame.flyway_schema_history "--result-file=$backup/data.sql" cyys_frame
    if ($LASTEXITCODE -ne 0) { throw 'Data backup failed' }
    $keys = @(Sql "select object_key from sys_file_entry where created_by='rehearsal-user'")
    if ($keys.Count -ne 1 -or $keys[0] -notmatch '^[a-zA-Z0-9]{1,32}$') { throw 'Unexpected owned object set' }
    $files = [IO.Path]::GetFullPath((Join-Path $runtime 'private-files'))
    $object = [IO.Path]::GetFullPath((Join-Path $files $keys[0]))
    if ([IO.Path]::GetDirectoryName($object) -ne $files) { throw 'Object path escaped local storage' }
    $objectBackup = Join-Path $backup 'file-object'
    Copy-Item -LiteralPath $object -Destination $objectBackup
    $fileHash = (Get-FileHash -LiteralPath $object -Algorithm SHA256).Hash
    if ((Get-FileHash -LiteralPath $objectBackup -Algorithm SHA256).Hash -ne $fileHash) { throw 'File backup mismatch' }
    Fixture clear | Out-Null
    Remove-Item -LiteralPath $object
    $empty = Snapshot 'cleared-owned-data'
    if (@($empty | Where-Object {$_ -notmatch '^flyway_schema_history ' -and $_ -notmatch ' rows=0 '}).Count) { throw 'Fixture clear did not leave empty business tables' }
    $restore = Start-Process -FilePath $mysql -ArgumentList @("--defaults-extra-file=$clientConfig",'cyys_frame') -WindowStyle Hidden -PassThru -RedirectStandardInput "$backup/data.sql" -RedirectStandardOutput "$backup/restore.log" -RedirectStandardError "$backup/restore-error.log"
    $restore.WaitForExit()
    if ($restore.ExitCode -ne 0) { throw 'Data restore failed; leave evidence and keep API stopped' }
    Copy-Item -LiteralPath $objectBackup -Destination $object
    $after = Snapshot 'after-restore'
    if (Compare-Object $before $after) { throw 'Restored database values differ from the stopped-write snapshot' }
    if ((Get-FileHash -LiteralPath $object -Algorithm SHA256).Hash -ne $fileHash) { throw 'Restored file hash differs' }
    $checks.Add('stopped-write backup and restore: all 26 table fingerprints and attachment SHA-256 identical')
    Compose @('up','-d','--no-build','api')
    Ready
    Api GET '/api/auth/me' $token -Status 401 | Out-Null
    $token = Login
    $restored = Api GET "/api/admin/files/$($file.id)/content" $token 'rehearsal-a'
    if ([Convert]::ToHexString($restored) -ne [Convert]::ToHexString($content)) { throw 'Restored download differs' }
    Api GET "/api/admin/files/$($file.id)/content" $token 'rehearsal-b' -Status 404 | Out-Null
    foreach ($scope in @('rehearsal-a','rehearsal-b')) { Api GET '/api/admin/users' $token $scope | Out-Null }
    $checks.Add('restored container login, A/B members and authorized file bytes verified; old session rejected')
    Compose @('stop','api')
    Fixture clear | Out-Null
    Remove-Item -LiteralPath $object
    Snapshot 'final-clean' | Out-Null
    $status = 'PASSED'
} finally {
    $http.Dispose(); $handler.Dispose()
    Remove-Item Env:CYYS_REHEARSAL_PASSWORD -ErrorAction SilentlyContinue
    [ordered]@{time=[DateTimeOffset]::Now.ToString('o'); status=$status; checks=@($checks.ToArray()); backup=$backup;
        limitation='Data restored into the unchanged V18 schema; not a production legacy import or empty-server provisioning test. Successful run leaves API stopped; on failure inspect state before retrying.'} |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $report -Encoding utf8
    Write-Output "Local rehearsal $status; report: $report"
}
