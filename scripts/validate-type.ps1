# 类型规范静态校验脚本（L1）
# 用法：
#   pwsh -File scripts/validate-type.ps1 -TypePath springboot
#   pwsh -File scripts/validate-type.ps1 -TypePath springboot -RequiredHeadings '## 快速开始','## 常用命令'
# 说明：
#   - L1 只做"机器可判"的静态检查；语义/命令真实性请配合 L2 自查清单、L3 样例项目冒烟、L4 评审（见 docs/authoring-types.md §5）。
#   - 检查项：树形画线字符 / 占位符配平 / 密钥形态启发 / 相对链接存在 / 根 AGENTS.md 必需章节 / 骨架第 1 章「项目概览」的概览块内容判据（authoring-types §2） / CODING_STANDARDS §N 引用 / docs 必建骨架（fixed-docs §1）。
#   - 宿主：PowerShell 5.1+ 均可（本文件为 UTF-8 with BOM）；Windows 用 `powershell -File`，跨平台/CI 用 `pwsh -File`。
#   - 跨平台：脚本内路径一律用 '/' 分隔符（Windows 与 Linux 通用）。
#   - 退出码：0 = 无 FAIL；1 = 存在 FAIL。WARN 需人工确认，不计失败。
param(
    [Parameter(Mandatory = $true)][string]$TypePath,
    [string[]]$RequiredHeadings = @('## 快速开始', '## 常用命令', '## 测试与质量门', '## 约束、禁区与陷阱')
)

$root = (Split-Path $PSScriptRoot -Parent)
$dir = (Resolve-Path -LiteralPath (Join-Path $root $TypePath) -ErrorAction Stop).Path
$mdFiles = Get-ChildItem -LiteralPath $dir -Recurse -File -Filter *.md |
    Where-Object { $_.FullName -notmatch '[\\/](target|\.venv|venv|node_modules|__pycache__|\.mvn|\.git|\.mypy_cache|\.ruff_cache|\.pytest_cache)[\\/]' }
$fail = 0; $warn = 0; $issues = [System.Collections.Generic.List[string]]::new()
$glyphRe = '[├└┌┐┃┏┗│─►■]'
$secretRe = '(?i)(password|passwd|secret|token|api[_-]?key|access[_-]?key|secret[_-]?key)\s*[=:]\s*[A-Za-z0-9_\-]{8,}'

foreach ($f in $mdFiles) {
    $rel = $f.FullName.Substring($dir.Length).TrimStart('\','/')
    if ([string]::IsNullOrWhiteSpace($rel)) { $rel = $f.Name }
    $text = [System.IO.File]::ReadAllText($f.FullName)

    # 1) 树形制表符/画线字符（豁免"禁止/反例"语境的元提及）
    $m = [regex]::Matches($text, $glyphRe)
    if ($m.Count -gt 0) {
        $real = @()
        foreach ($gl in $m) {
            $ln = ($text.Substring(0, $gl.Index) -split "`r?`n")[-1]
            if ($ln -match '不用|禁用|禁止|反例|✗') { continue }
            $real += $gl.Value
        }
        if ($real.Count -gt 0) {
            $chars = ($real | Sort-Object -Unique) -join ''
            $issues.Add("FAIL  [$rel] 含树形/画线字符: $chars —— 请改用纯缩进（writing-standards）")
            $fail++
        }
    }

    # 2) 占位符配平（模板允许 {{}}，但数量须成对）
    $open  = ([regex]::Matches($text, '\{\{')).Count
    $close = ([regex]::Matches($text, '\}\}')).Count
    if ($open -ne $close) {
        $issues.Add("FAIL  [$rel] 占位符不配平: {{=$open  }}=$close")
        $fail++
    }

    # 3) 密钥形态启发（WARN，需人工确认；${ENV} 形式不匹配）
    $sm = [regex]::Matches($text, $secretRe)
    if ($sm.Count -gt 0) {
        foreach ($s in $sm) {
            $issues.Add("WARN  [$rel] 疑似密钥赋值（请确认是占位/示例而非真实值）: " + $s.Value)
        }
        $warn += $sm.Count
    }

    # 3b) 密钥类环境变量带非空默认值（WARN，需人工确认）：${DB_PASSWORD:root} 会把开发口令固化成"默认可连"
    foreach ($dm in [regex]::Matches($text, '\$\{[A-Z0-9_]*(?:PASSWORD|PASSWD|SECRET|TOKEN|KEY)[A-Z0-9_]*:[^}]+\}')) {
        $issues.Add("WARN  [$rel] 密钥类环境变量带默认值（确认非真实口令/生产禁用）: " + $dm.Value)
        $warn++
    }

    # 4) 相对 .md 链接存在性（http(s)/mailto/纯锚点除外，向上 ../ 亦检查）
    if ($f.Name -ne 'changelog.md') { # changelog 含历史引述，宽松
        foreach ($lm in [regex]::Matches($text, '\]\(([^)]+?)\)')) {
            $t = $lm.Groups[1].Value
            if ($t -match '^(https?:|mailto:)') { continue }
            $pathOnly = (($t -split '#')[0]).Trim()
            if ($pathOnly -eq '' -or $pathOnly.StartsWith('#')) { continue }
            $cand = Join-Path $f.DirectoryName $pathOnly
            if (-not (Test-Path -LiteralPath $cand)) {
                $issues.Add("FAIL  [$rel] 链接指向不存在的文件: $t")
                $fail++
            }
        }
    }

    # 5) 类型根 AGENTS.md：必需章节（按标题前缀匹配）
    if ($f.Name -eq 'AGENTS.md' -and $f.DirectoryName -eq $dir) {
        $lines = $text -split "`r?`n"
        foreach ($h in $RequiredHeadings) {
            $hit = $lines | Where-Object { $_.TrimStart().StartsWith($h) }
            if (-not $hit) {
                $issues.Add("FAIL  [$rel] 缺少必需章节标题: $h")
                $fail++
            }
        }
    }
    # 5b) 骨架第 1 章「项目概览」：判据落在**内容**而非标题（authoring-types §2）
    #     两种呈现都接受：① H1 之后、首个 '##' 之前的概览块；② '项目概览' 同名（'##'/'###'）章。
    #     二者之一须满足：非空行 >= 3，且同时含"技术栈"与"形态"两个关键词。
    #     为什么不用必需标题判据：本章按惯例以 "H1 项目名 + 概览块" 呈现（无 H2），标题匹配天然看不见它。
    if ($f.Name -eq 'AGENTS.md' -and $f.DirectoryName -eq $dir) {
        $ls = $text -split "`r?`n"
        $h1 = -1; $h2 = -1
        for ($i = 0; $i -lt $ls.Count; $i++) {
            if ($h1 -lt 0 -and $ls[$i] -match '^#\s+\S') { $h1 = $i; continue }
            if ($h1 -ge 0 -and $ls[$i] -match '^##\s') { $h2 = $i; break }
        }
        $sec = -1; $secEnd = $ls.Count
        for ($i = 0; $i -lt $ls.Count; $i++) {
            if ($ls[$i] -match '^#{2,3}\s*(\d+\.\s*)?项目概览\s*$') { $sec = $i + 1; break }
        }
        $area = @()
        if ($sec -ge 0) {
            for ($i = $sec; $i -lt $ls.Count; $i++) {
                if ($ls[$i] -match '^#{1,3}\s') { $secEnd = $i; break }
            }
            if (($secEnd - 1) -ge $sec) { $area = $ls[$sec..($secEnd - 1)] }
        } elseif ($h1 -ge 0 -and $h2 -gt ($h1 + 1)) {
            $area = $ls[($h1 + 1)..($h2 - 1)]
        }
        $body = @($area | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        $joined = ($body -join "`n")
        if ($body.Count -lt 3) {
            $issues.Add("FAIL  [$rel] 骨架第 1 章「项目概览」缺失：H1 后的概览块（或「项目概览」同名章）非空行=$($body.Count) < 3 —— 见 docs/authoring-types.md §2 第 1 章")
            $fail++
        } else {
            foreach ($kw in @('技术栈', '形态')) {
                if ($joined -notmatch [regex]::Escape($kw)) {
                    $issues.Add("FAIL  [$rel] 骨架第 1 章「项目概览」缺关键词「$kw」—— 见 docs/authoring-types.md §2 第 1 章内容要点")
                    $fail++
                }
            }
        }
    }

}

# 6) AGENTS/README 等对 docs/CODING_STANDARDS.md 的 §N 引用存在性
$coding = Join-Path $dir 'docs/CODING_STANDARDS.md'
if (Test-Path -LiteralPath $coding) {
    $cLines = [System.IO.File]::ReadAllText($coding) -split "`r?`n"
    $valid = [System.Collections.Generic.HashSet[int]]::new()
    foreach ($cl in $cLines) {
        $hm = [regex]::Match($cl, '^##\s*(\d+)\.')
        if ($hm.Success) { [void]$valid.Add([int]$hm.Groups[1].Value) }
    }
    foreach ($f in $mdFiles) {
        $rel2 = $f.FullName.Substring($dir.Length).TrimStart('\','/')
        if ([string]::IsNullOrWhiteSpace($rel2)) { $rel2 = $f.Name }
        foreach ($ln in ([System.IO.File]::ReadAllText($f.FullName) -split "`r?`n")) {
            if (-not $ln.Contains('CODING_STANDARDS')) { continue }
            foreach ($rm in [regex]::Matches($ln, '§\s*(\d+)')) {
                $n = [int]$rm.Groups[1].Value
                if (-not $valid.Contains($n)) {
                    $issues.Add("FAIL  [$rel2] 引用了 CODING_STANDARDS 不存在的章节 §$n ：" + $ln.Trim())
                    $fail++
                }
            }
        }
    }
}

# 7) docs/ 必建骨架（fixed-docs §1）：两核心文件 + 五项目录及各自最小入口文件
#    目录必建（固定要求），触发条件只决定内容何时补齐；缺一即 FAIL。
$requiredDocs = @(
    'docs/CODING_STANDARDS.md',
    'docs/ARCHITECTURE.md',
    'docs/design-docs/index.md',
    'docs/design-docs/core-beliefs.md',
    'docs/product-specs/index.md',
    'docs/product-specs/TEMPLATE.md',
    'docs/exec-plans/index.md',
    'docs/exec-plans/tech-debt-tracker.md',
    'docs/exec-plans/active/index.md',
    'docs/exec-plans/completed/index.md',
    'docs/generated/index.md',
    'docs/references/index.md'
)
foreach ($rel in $requiredDocs) {
    if (-not (Test-Path -LiteralPath (Join-Path $dir $rel))) {
        $issues.Add("FAIL  [docs 骨架] 缺少必建文件: $rel —— fixed-docs §1 要求两核心文件 + 五项目录各含最小入口文件（目录必建、内容按触发器）")
        $fail++
    }
}

# 8) 配置文件（非 .md）的密钥形态与"带默认值的密钥类环境变量"（WARN）
#    为什么单列：密钥/默认口令最常出现在 yml/env/compose 里，而上面的扫描只覆盖 *.md。
$cfgExt = @('.yml', '.yaml', '.properties', '.env', '.example', '.ini', '.toml')
$cfgFiles = Get-ChildItem -LiteralPath $dir -Recurse -File |
    Where-Object { $cfgExt -contains $_.Extension.ToLower() } |
    Where-Object { $_.FullName -notmatch '[\\/]target[\\/]|[\\/]\.venv[\\/]|node_modules|__pycache__|[\\/]\.mvn[\\/]' }
foreach ($cf in $cfgFiles) {
    $crel = $cf.FullName.Substring($dir.Length).TrimStart('\', '/')
    $ctext = [System.IO.File]::ReadAllText($cf.FullName)
    foreach ($sm in [regex]::Matches($ctext, $secretRe)) {
        $issues.Add("WARN  [$crel] 疑似密钥赋值（确认是占位/示例而非真实值）: " + $sm.Value)
        $warn++
    }
    foreach ($dm in [regex]::Matches($ctext, '\$\{[A-Z0-9_]*(?:PASSWORD|PASSWD|SECRET|TOKEN|KEY)[A-Z0-9_]*:[^}]+\}')) {
        $issues.Add("WARN  [$crel] 密钥类环境变量带默认值（确认非真实口令/生产禁用）: " + $dm.Value)
        $warn++
    }
}

Write-Host ''
Write-Host "== 静态校验结果（$TypePath）=="
if ($issues.Count -eq 0) {
    Write-Host '无问题：全部通过。' -ForegroundColor Green
} else {
    $issues | ForEach-Object { Write-Host $_ }
}
Write-Host ''
Write-Host ("FAIL=" + $fail + "  WARN=" + $warn + "  文件数=" + $mdFiles.Count)
if ($fail -gt 0) { exit 1 }
exit 0
