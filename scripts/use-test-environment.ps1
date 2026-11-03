function Invoke-WithDevelopmentTestEnvironment {
    param(
        [Parameter(Mandatory)][string]$ProjectRoot,
        [string]$EnvFile,
        [string]$DatabaseUrl,
        [Parameter(Mandatory)][scriptblock]$Action
    )
    $values = @{}
    if (!$EnvFile) {
        $EnvFile = @('deploy/.env.dev', 'backend/cyys-business/target/docker-local/.env') |
            ForEach-Object { Join-Path $ProjectRoot $_ } |
            Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
    }
    if ($EnvFile) {
        if (![IO.Path]::IsPathRooted($EnvFile)) { $EnvFile = Join-Path $ProjectRoot $EnvFile }
        foreach ($line in Get-Content -LiteralPath $EnvFile) {
            if ($line -match '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
                $value = $Matches[2].Trim()
                if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                    $value = $value.Substring(1, $value.Length - 2)
                }
                $values[$Matches[1]] = $value
            }
        }
    }
    function Read-TestSetting([string]$TestName, [string]$Name) {
        foreach ($key in @($TestName, $Name)) {
            $value = [Environment]::GetEnvironmentVariable($key, 'Process')
            if ($null -ne $value) { return $value }
        }
        if ($values.ContainsKey($Name)) { return $values[$Name] }
    }
    function Read-TestPassword([string]$TestName, [string]$Name) {
        $value = Read-TestSetting $TestName $Name
        if ($null -ne $value) { return $value }
        $file = Read-TestSetting "${TestName}_FILE" "${Name}_FILE"
        if (!$file) { throw "缺少 $TestName 或 ${Name}_FILE，请提供现有开发连接配置。" }
        if (![IO.Path]::IsPathRooted($file)) { $file = Join-Path (Join-Path $ProjectRoot 'deploy') $file }
        return [IO.File]::ReadAllText($file).TrimEnd([char]13, [char]10)
    }
    if (!$DatabaseUrl) { $DatabaseUrl = Read-TestSetting 'CYYS_TEST_DB_URL' 'CYYS_DB_URL' }
    if ($DatabaseUrl -notmatch '^jdbc:mysql://(?:host\.docker\.internal|localhost|127\.0\.0\.1):\d+/(?<schema>[A-Za-z0-9_]+)(?:\?.*)?$' -or
        $Matches.schema -in @('mysql', 'sys', 'information_schema', 'performance_schema')) {
        throw '验证仅连接明确指定的本机开发测试库，URL 必须包含端口和数据库名。'
    }
    if ($DatabaseUrl -match '(?i)[?&](user|password)=') { throw '数据库连接地址不能包含凭据。' }
    $DatabaseUrl = $DatabaseUrl -replace '^jdbc:mysql://host\.docker\.internal:', 'jdbc:mysql://127.0.0.1:'
    $username = Read-TestSetting 'CYYS_TEST_DB_USERNAME' 'CYYS_DB_USERNAME'
    if (!$username) { throw '缺少开发测试库用户名。' }
    $password = Read-TestPassword 'CYYS_TEST_DB_PASSWORD' 'CYYS_DB_PASSWORD'
    $redisPassword = Read-TestPassword 'CYYS_TEST_REDIS_PASSWORD' 'CYYS_REDIS_PASSWORD'
    $redisHost = Read-TestSetting 'CYYS_TEST_REDIS_HOST' 'CYYS_REDIS_HOST'
    if (!$redisHost) { $redisHost = '127.0.0.1' }
    if ($redisHost -notin @('host.docker.internal', 'localhost', '127.0.0.1')) { throw '验证仅连接本机 Redis。' }
    if ($redisHost -eq 'host.docker.internal') { $redisHost = '127.0.0.1' }
    $redisPort = Read-TestSetting 'CYYS_TEST_REDIS_PORT' 'CYYS_REDIS_PORT'
    if (!$redisPort) { $redisPort = '6379' }
    $redisDatabase = Read-TestSetting 'CYYS_TEST_REDIS_DATABASE' 'CYYS_REDIS_DATABASE'
    if (!$redisDatabase) { $redisDatabase = '0' }
    $variables = @{
        CYYS_TEST_DB_URL = $DatabaseUrl
        CYYS_TEST_DB_USERNAME = $username
        CYYS_TEST_DB_PASSWORD = $password
        SPRING_DATASOURCE_URL = $DatabaseUrl
        SPRING_DATASOURCE_USERNAME = $username
        SPRING_DATASOURCE_PASSWORD = $password
        CYYS_TEST_REDIS_PASSWORD = $redisPassword
        SPRING_DATA_REDIS_HOST = $redisHost
        SPRING_DATA_REDIS_PORT = $redisPort
        SPRING_DATA_REDIS_PASSWORD = $redisPassword
        SPRING_DATA_REDIS_DATABASE = $redisDatabase
        CYYS_ENVIRONMENT = 'framework-verification'
    }
    $previous = @{}
    try {
        foreach ($key in $variables.Keys) {
            $previous[$key] = [Environment]::GetEnvironmentVariable($key, 'Process')
            [Environment]::SetEnvironmentVariable($key, $variables[$key], 'Process')
        }
        & $Action
    } finally {
        foreach ($key in $previous.Keys) { [Environment]::SetEnvironmentVariable($key, $previous[$key], 'Process') }
        $variables.Clear()
    }
}
