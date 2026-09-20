$ErrorActionPreference = 'Stop'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backend = Join-Path $framework 'backend'
$jar = Join-Path $backend 'cyys-business\target\cyys-business.jar'
$dist = Join-Path $framework 'admin-ui\dist'
if (!(Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Executable JAR is missing.' }
if (!(Test-Path -LiteralPath (Join-Path $dist 'index.html') -PathType Leaf)) { throw 'Built frontend index.html is missing.' }

$archive = [IO.Compression.ZipFile]::OpenRead($jar)
try {
    $entries = @($archive.Entries | ForEach-Object FullName)
    if ($entries -notcontains 'BOOT-INF/classes/com/cyys/application/CyysApplication.class' -or
        $entries -notcontains 'org/springframework/boot/loader/launch/JarLauncher.class') {
        throw 'The business JAR is not the expected executable application.'
    }
    $forbidden = @($entries | Where-Object {
        $_ -match '(?i)(^|/)(application-(test|local)\.[^/]+|dev-data)(/|$)' -or
        $_ -match '(^|/)(BrowserAcceptance|IsolationTestSupport|MySqlTestDatabaseGuard)\b' -or
        $_ -match '(?i)^BOOT-INF/lib/(h2-|shiro-|springfox-|tomcat-embed-jasper|jakarta.servlet.jsp|jstl)'
    })
    if ($forbidden) { throw 'The executable JAR contains excluded test, local configuration or legacy runtime entries.' }
    $expected = @(foreach ($directory in @('system','business')) {
        $source = Join-Path $framework "migrations\$directory"
        if (Test-Path -LiteralPath $source) {
            Get-ChildItem -LiteralPath $source -Filter 'V*.sql' -File | ForEach-Object { "BOOT-INF/classes/db/migration/$directory/$($_.Name)" }
        }
    })
    $actual = @($entries | Where-Object { $_ -like 'BOOT-INF/classes/db/migration/*.sql' })
    if (!$expected -or (Compare-Object ($expected | Sort-Object) ($actual | Sort-Object))) { throw 'Packaged migrations do not match the approved system/business scripts.' }
    foreach ($entry in $actual) {
        $source = Join-Path $framework ('migrations/' + $entry.Substring('BOOT-INF/classes/db/migration/'.Length))
        $stream = $archive.GetEntry($entry).Open()
        try { $hash = [Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)) }
        finally { $stream.Dispose() }
        if ($hash -ne (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash) { throw 'A packaged migration differs from its source.' }
    }
} finally { $archive.Dispose() }

foreach ($module in @('cyys-common','cyys-admin')) {
    $moduleJars = @(Get-ChildItem -LiteralPath (Join-Path $backend "$module\target") -Filter "$module-*.jar" -File | Where-Object { $_.Name -notmatch '-(sources|javadoc|tests)\.jar$' })
    if ($moduleJars.Count -ne 1) { throw "Expected one ordinary JAR for $module." }
    $library = [IO.Compression.ZipFile]::OpenRead($moduleJars[0].FullName)
    try {
        if (@($library.Entries | Where-Object { $_.FullName -like 'BOOT-INF/*' -or $_.FullName -like 'org/springframework/boot/loader/*' }).Count -gt 0) {
            throw "$module must remain an ordinary library JAR."
        }
    } finally { $library.Dispose() }
}
if (@(Get-ChildItem -LiteralPath $dist -Recurse -File | Where-Object { $_.Name -like '.env*' -or $_.Extension -in '.pem','.p12','.pfx','.key','.jks' }).Count -gt 0) {
    throw 'Frontend output contains an environment or credential file.'
}
Write-Output 'Release structure verified: one executable backend, ordinary libraries, exact migration resources and frontend entry.'
