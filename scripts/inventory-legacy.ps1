param()
$ErrorActionPreference = 'Stop'
. 'F:\git\frame\scripts\use-local-toolchain.ps1'
$frameworkRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$legacyRoot = 'F:\git\frame\cyys_monomer'
$designRoot = 'F:\git\frame\docs'
$outputRoot = Join-Path $frameworkRoot 'docs\migration'
$prefixes = @{
    'U/' = 'cyys-common/src/main/java/com/cyys/common/utils/'
    'R/' = 'cyys-common/src/main/java/com/cyys/common/redis/'
    'Q/' = 'cyys-common/src/main/java/com/cyys/common/ql/'
    'T/' = 'cyys-common/src/main/java/com/cyys/common/thirdInterface/'
    'C/' = 'cyys-core/src/main/java/com/cyys/modules/core/'
    'common/' = 'cyys-common/src/main/java/com/cyys/common/'
}
$sourceFiles = @(rg --files $legacyRoot -g '*.java' -g '*.jsp' -g '*.tld' -g '*.xml' -g '*.ftl' -g '!**/target/**' -g '!**/.git/**')
if ($LASTEXITCODE -ne 0) { throw 'Legacy source listing failed.' }
$documents = foreach ($sourceFile in $sourceFiles) {
    [pscustomobject]@{
        Path = [IO.Path]::GetRelativePath($legacyRoot, $sourceFile).Replace('\', '/')
        Lines = [IO.File]::ReadAllLines($sourceFile)
    }
}
$utilityRows = @(Get-Content -LiteralPath (Join-Path $designRoot '02_CYYS_源码与工具类改造建议.md') |
    Where-Object { $_ -match '^\| `(?:U/|R/|Q/|T/|C/|common/)[^`]+\.java` \|' })
if ($utilityRows.Count -ne 77) { throw "Expected D02's 77 utility entries, found $($utilityRows.Count)." }
$utilities = foreach ($utilityRow in $utilityRows) {
    $cells = $utilityRow.Split('|')
    $alias = $cells[1].Trim().Trim('`')
    $prefix = @($prefixes.Keys | Where-Object { $alias.StartsWith($_) })[0]
    $relative = $prefixes[$prefix] + $alias.Substring($prefix.Length)
    $source = Join-Path $legacyRoot $relative
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) { throw "Missing utility: $relative" }
    $symbol = [IO.Path]::GetFileNameWithoutExtension($relative)
    $pattern = '\b' + [regex]::Escape($symbol) + '\b'
    $callPattern = '\b' + [regex]::Escape($symbol) + '\s*\.\s*([A-Za-z_]\w*)\s*\('
    $references = foreach ($document in $documents) {
        if ($document.Path -eq $relative) { continue }
        for ($index = 0; $index -lt $document.Lines.Length; $index++) {
            $line = $document.Lines[$index]
            if ($line -match $pattern) {
                [pscustomobject]@{
                    path = $document.Path
                    line = $index + 1
                    methods = @([regex]::Matches($line, $callPattern) | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
                }
            }
        }
    }
    [pscustomobject]@{
        alias = $alias
        source = $relative
        sha256 = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash.ToLowerInvariant()
        designDisposition = $cells[3].Trim()
        destination = $cells[4].Trim()
        referenceFiles = @($references.path | Sort-Object -Unique).Count
        references = @($references)
    }
}
$legacyRevision = git -C $legacyRoot rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve legacy revision.' }
$targetRevision = git -C $frameworkRoot rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve target revision.' }
$report = [ordered]@{
    generatedAt = [DateTimeOffset]::Now.ToString('o')
    legacyRevision = $legacyRevision
    targetBaseRevision = $targetRevision
    searchedFiles = $sourceFiles.Count
    note = 'Static candidate references, including same-name classes and comments; inspect each caller before replacement. No source lines or credential values are copied. This is not an external-consumer or production-data inventory.'
    utilities = @($utilities)
}
$report | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $outputRoot 'legacy-utility-references.json') -Encoding utf8
$summary = [Collections.Generic.List[string]]::new()
$summary.Add('# D02 工具迁移核对')
$summary.Add('')
$summary.Add('由 `scripts/inventory-legacy.ps1` 从 D02 的 77 项清单和当前只读旧源码生成。逐项保留 D02 处置建议与目标归属；本表不是已完成迁移清单。')
$summary.Add('')
$summary.Add(('旧源码版本：`{0}`；目标基线：`{1}`；检索 Java/JSP/TLD/XML/FTL 共 {2} 个文件。' -f $legacyRevision, $targetRevision, $sourceFiles.Count))
$summary.Add('')
$summary.Add('引用文件数包含同名类与注释候选，不等于真实调用次数；完整文件、行号及静态方法名见 [引用明细](legacy-utility-references.json)。零命中不能证明反射、配置或外部项目没有消费者。迁移具体消费方时核对方法语义，完成相应回归后再更新实际进度。')
$summary.Add('')
$summary.Add('| D02 条目 | 当前候选引用文件数 | 原处置建议 | 目标及注意事项 |')
$summary.Add('|---|---:|---|---|')
foreach ($utility in $utilities) {
    $summary.Add(('| `{0}` | {1} | {2} | {3} |' -f $utility.alias, $utility.referenceFiles, $utility.designDisposition, $utility.destination))
}
$summary | Set-Content -LiteralPath (Join-Path $outputRoot 'legacy-utility-inventory.md') -Encoding utf8
Write-Output "Inventoried $($utilities.Count) utilities across $($sourceFiles.Count) source/config/template files."
