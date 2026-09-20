param(
    [Parameter(Mandatory)][string]$PlanDirectory,
    [ValidateSet('apply','verify')][string]$Action = 'verify'
)
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$framework = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$planRoot = [IO.Path]::GetFullPath($PlanDirectory)
$targetRoot = [IO.Path]::GetFullPath((Join-Path $framework 'backend/cyys-business/target'))
if (!$planRoot.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Keep migration data inside the ignored business target directory.' }
$plan = Get-Content (Join-Path $planRoot 'plan.json') -Raw | ConvertFrom-Json
[xml]$settings = Get-Content (Join-Path $framework 'backend/cyys-business/src/test/resources/application-test.xml')
foreach ($name in @('CYYS_TEST_DB_URL','CYYS_TEST_DB_USERNAME','CYYS_TEST_DB_PASSWORD')) {
    [Environment]::SetEnvironmentVariable($name, ($settings.properties.entry | Where-Object key -eq $name).InnerText, 'Process')
}
if ($env:CYYS_TEST_DB_URL -notmatch '^jdbc:mysql://(127\.0\.0\.1|localhost):3306/cyys_frame\?') { throw 'Only the selected existing MySQL test database is allowed.' }
$data = Join-Path $planRoot 'data.tsv'
if ((Get-FileHash $data -Algorithm SHA256).Hash.ToLowerInvariant() -ne $plan.dataSha256) { throw 'Plan checksum mismatch' }
if ($Action -eq 'apply') {
    $backup = Join-Path $planRoot 'before-import.sql'
    if (Test-Path $backup) { throw 'Existing backup is preserved. Use verify to check an earlier import.' }
    $client = Join-Path $planRoot 'client.cnf'
    function OptionValue([string]$Value) { '"' + $Value.Replace('\','\\').Replace('"','\"').Replace("`r",'\r').Replace("`n",'\n') + '"' }
    try {
        [IO.File]::WriteAllLines($client,[string[]]@('[client]','host=127.0.0.1','port=3306','default-character-set=utf8mb4',
            ('user='+(OptionValue $env:CYYS_TEST_DB_USERNAME)),('password='+(OptionValue $env:CYYS_TEST_DB_PASSWORD))))
        & 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe' "--defaults-extra-file=$client" --single-transaction --no-tablespaces --set-gtid-purged=OFF "--result-file=$backup" cyys_frame
        if ($LASTEXITCODE -ne 0) { throw 'Backup failed; import not started.' }
    } finally { if(Test-Path -LiteralPath $client) { Remove-Item -LiteralPath $client } }
}
$driver = Join-Path $framework 'backend/.m2/repo/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar'
$output = & "$env:JAVA_HOME/bin/java.exe" -cp $driver (Join-Path $PSScriptRoot 'ImportLegacyBasicData.java') $Action $data $plan.dataSha256
if ($LASTEXITCODE -ne 0) { throw 'Migration failed/refused; see sanitized diagnostic. Backup and inputs retained.' }
$log = Join-Path $framework ('backend/logs/legacy-basic-' + $Action + '-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.log')
$output | Set-Content $log -Encoding utf8
$output
Write-Output "Evidence: $log"
