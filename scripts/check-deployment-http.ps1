param(
    [ValidateSet('web', 'api', 'all')][string]$Target = 'all',
    [Parameter(Mandatory)][uri]$BaseUrl,
    [Parameter(Mandatory)][string]$ReportPath,
    [switch]$LocalCertificate,
    [switch]$ExpectUnavailable
)
$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -ne 'https' -or $BaseUrl.AbsolutePath -ne '/') { throw 'Use the HTTPS site root.' }
if ($LocalCertificate -and -not $BaseUrl.IsLoopback) { throw 'Test certificates are allowed only on loopback.' }
$handler = [Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
if ($LocalCertificate) { $handler.ServerCertificateCustomValidationCallback = [Net.Http.HttpClientHandler]::DangerousAcceptAnyServerCertificateValidator }
$client = [Net.Http.HttpClient]::new($handler)
$client.Timeout = [TimeSpan]::FromSeconds(15)
$results = [Collections.Generic.List[object]]::new()
function Check([string]$Path, [int]$Status, [string]$Type, [string]$Method = 'GET', [byte[]]$Body = $null) {
    $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($Method), [uri]::new($BaseUrl, $Path))
    if ($null -ne $Body) { $request.Content = [Net.Http.ByteArrayContent]::new($Body) }
    try {
        $response = $client.SendAsync($request).GetAwaiter().GetResult()
        try {
            $bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
            $actualType = $response.Content.Headers.ContentType.MediaType
            $entry = [ordered]@{path=$Path; status=[int]$response.StatusCode; type=$actualType; bytes=$bytes.Length; passed=$false}
            $results.Add($entry)
            if ($entry.status -ne $Status -or $actualType -ne $Type) { throw "Unexpected response for $Path : $($entry.status) $actualType" }
            if ($Type -eq 'application/json') { [Text.Encoding]::UTF8.GetString($bytes) | ConvertFrom-Json | Out-Null }
            $entry.passed = $true
        } finally { $response.Dispose() }
    } finally { $request.Dispose() }
}
try {
    if ($ExpectUnavailable) { Check '/api/health' 503 'application/json' }
    else {
        if ($Target -ne 'web') {
        Check '/api/health' 200 'application/json'
        Check '/api' 404 'application/json'
        Check '/api/auth/me' 401 'application/json'
        }
        if ($Target -ne 'api') {
        Check '/system/users' 200 'text/html'
        Check '/missing-acceptance.js' 404 'text/html'
        Check '/v3/api-docs' 404 'text/html'
        Check '/.env' 404 'text/html'
        Check '/api/admin/files' 413 'application/json' 'POST' ([byte[]]::new(22MB))
        $redirect = [uri]::new('http://' + $BaseUrl.Host + '/system/users')
        $response = $client.GetAsync($redirect).GetAwaiter().GetResult()
        try {
            $expected = [uri]::new($BaseUrl, '/system/users').AbsoluteUri
            $passed = [int]$response.StatusCode -eq 308 -and $response.Headers.Location.AbsoluteUri -eq $expected
            $results.Add([ordered]@{path=$redirect.AbsoluteUri; status=[int]$response.StatusCode; location=$response.Headers.Location.AbsoluteUri; passed=$passed})
            if (-not $passed) { throw 'HTTP did not redirect to the expected HTTPS URL.' }
        } finally { $response.Dispose() }
        }
    }
} finally {
    [ordered]@{time=[DateTimeOffset]::Now.ToString('o'); target=$Target; baseUrl=$BaseUrl.AbsoluteUri; checks=@($results.ToArray())} |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $ReportPath -Encoding utf8
    $client.Dispose(); $handler.Dispose()
}
Write-Output "$($results.Count) deployment HTTP checks passed; report: $ReportPath"
