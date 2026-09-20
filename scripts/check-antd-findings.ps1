param([Parameter(Mandatory)][string]$ReportPath)
$ErrorActionPreference = 'Stop'
$frontend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../admin-ui'))
$raw = Get-Content -LiteralPath $ReportPath -Raw
$start = $raw.IndexOf('{')
if ($start -lt 0) { throw 'Ant lint did not return a structured result.' }
$lint = $raw.Substring($start) | ConvertFrom-Json
if ($null -eq $lint.summary.total -or $lint.partial -ne $false -or @($lint.skippedFiles).Count -ne 0) {
    throw 'Ant lint is incomplete; all source files must be checked.'
}
if ($lint.summary.total -ne @($lint.issues).Count) { throw 'Ant lint report count is inconsistent.' }
if ($lint.summary.total -eq 0) { Write-Output 'Ant lint: no findings.'; return }
$baseline = Get-Content (Join-Path $PSScriptRoot 'antd-template-baseline.json') -Raw | ConvertFrom-Json
if ((Get-FileHash (Join-Path $frontend 'config/routes.ts') -Algorithm SHA256).Hash -ne $baseline.routesSha256) {
    throw 'Routes changed: review whether retained template warnings now affect enabled pages.'
}
$accepted = 0
foreach ($issue in $lint.issues) {
    $path = [IO.Path]::GetRelativePath($frontend, $issue.file).Replace('\','/')
    $file = @($baseline.files | Where-Object path -CEQ $path)
    if ($file.Count -ne 1 -or $issue.severity -ne 'warning') { throw "New Ant lint finding: $path : $($issue.line) $($issue.message)" }
    if ((Get-FileHash -LiteralPath (Join-Path $frontend $path) -Algorithm SHA256).Hash -ne $file[0].sha256) {
        throw "Reference page changed; re-evaluate its Ant warnings: $path"
    }
    $match = @($file[0].issues | Where-Object {
        $_.line -eq $issue.line -and $_.rule -ceq $issue.rule -and
        $_.severity -ceq $issue.severity -and $_.message -ceq $issue.message
    })
    if ($match.Count -ne 1) { throw "New Ant lint finding: $path : $($issue.line) $($issue.message)" }
    $accepted++
}
Write-Output "Ant lint: $accepted recorded warnings in unchanged, disabled template pages; no new findings."
