# 仓库级静态校验（L1'）——覆盖 validate-type.ps1（单类型 L1）之外的全仓库一致性
# 用法：
#   powershell -File scripts/validate-repo.ps1
#   powershell -File scripts/validate-repo.ps1 -Quiet
# 说明：
#   - 检查项：根文档（AGENTS.md/README.md/docs/*.md）画线字符与占位符配平、相对链接存在、
#     密钥形态启发、CODING_STANDARDS §N 引用一致；类型目录 ↔ 根 AGENTS §4 登记表双向一致；
#     每个类型的 docs 必建骨架（fixed-docs §1）与 examples 样例 docs 骨架；类型范本与样例副本同源；
#     根 AGENTS 版本号 ↔ changelog「（当前）」版本；陈旧引用写法（`AGENTS §N`，AGENTS.md 无编号章节）。
#   - 宿主：PowerShell 5.1+ 均可（本文件为 UTF-8 with BOM）；Windows 用 `powershell -File`，跨平台/CI 用 `pwsh -File`。
#   - 跨平台：脚本内路径一律用 '/' 分隔符（Windows 与 Linux 通用）。
#   - 退出码：0 = 无 FAIL；1 = 存在 FAIL（WARN 需人工确认，不计失败）。
param([switch]$Quiet)

$root = Split-Path $PSScriptRoot -Parent
$fail = 0; $warn = 0
$issues = [System.Collections.Generic.List[string]]::new()

function Add-Fail([string]$m) { $script:issues.Add("FAIL  $m"); $script:fail++ }
function Add-Warn([string]$m) { $script:issues.Add("WARN  $m"); $script:warn++ }

$glyphRe = '[├└┌┐┃┏┗│─►■]'
$secretRe = '(?i)(password|passwd|secret|token|api[_-]?key|access[_-]?key|secret[_-]?key)\s*[=:]\s*[A-Za-z0-9_\-]{8,}'
$linkRe = '\]\(([^)]+?)\)'
$requiredDocs = @(
    'CODING_STANDARDS.md',
    'ARCHITECTURE.md',
    'design-docs/index.md',
    'design-docs/core-beliefs.md',
    'product-specs/index.md',
    'product-specs/TEMPLATE.md',
    'exec-plans/index.md',
    'exec-plans/tech-debt-tracker.md',
    'exec-plans/active/index.md',
    'exec-plans/completed/index.md',
    'generated/index.md',
    'references/index.md'
)

# CODING_STANDARDS 的合法 §N（以 springboot 范本为权威；根文档引用的是同一套固定骨架）
$coding = Join-Path $root 'springboot/docs/CODING_STANDARDS.md'
$validSections = [System.Collections.Generic.HashSet[int]]::new()
if (Test-Path -LiteralPath $coding) {
    foreach ($l in ([System.IO.File]::ReadAllText($coding) -split "`r?`n")) {
        $m = [regex]::Match($l, '^##\s*(\d+)\.')
        if ($m.Success) { [void]$validSections.Add([int]$m.Groups[1].Value) }
    }
}

# 根 AGENTS.md 的真实编号章节（供 `AGENTS §N` 悬挂引用校验；支持 §3.1 形式）
$agentsSections = [System.Collections.Generic.HashSet[string]]::new()
foreach ($l in ([System.IO.File]::ReadAllText((Join-Path $root 'AGENTS.md')) -split "`r?`n")) {
    $m = [regex]::Match($l, '^#{2,3}\s+(\d+(?:\.\d+)?)[\.\s]')
    if ($m.Success) { [void]$agentsSections.Add($m.Groups[1].Value) }
}

# ---------- 1) 根级文档：画线字符 / 占位符配平 / 链接存在 / 密钥启发 / §N 引用 ----------
$rootDocs = [System.Collections.Generic.List[string]]::new()
$rootDocs.Add((Join-Path $root 'AGENTS.md'))
$rootDocs.Add((Join-Path $root 'README.md'))
if (Test-Path -LiteralPath (Join-Path $root 'docs')) {
    Get-ChildItem -LiteralPath (Join-Path $root 'docs') -Recurse -File -Filter *.md | ForEach-Object { $rootDocs.Add($_.FullName) }
}

foreach ($f in $rootDocs) {
    if (-not (Test-Path -LiteralPath $f)) { Add-Fail "[根文档] 缺失: $f"; continue }
    $rel = $f.Substring($root.Length).TrimStart('\', '/')
    $text = [System.IO.File]::ReadAllText($f)

    $g = [regex]::Matches($text, $glyphRe)
    if ($g.Count -gt 0) {
        # 豁免"禁止/反例"语境的元提及（writing-standards 反例表、AGENTS 红线 8 本身需要写出这些字符）
        $real = @()
        foreach ($gl in $g) {
            $ln = ($text.Substring(0, $gl.Index) -split "`r?`n")[-1]
            if ($ln -match '不用|禁用|禁止|反例|✗') { continue }
            $real += $gl.Value
        }
        if ($real.Count -gt 0) {
            $chars = ($real | Sort-Object -Unique) -join ''
            Add-Fail "[$rel] 含树形/画线字符: $chars —— 改用纯缩进（writing-standards §2.8）"
        }
    }

    $open = ([regex]::Matches($text, '\{\{')).Count
    $close = ([regex]::Matches($text, '\}\}')).Count
    if ($open -ne $close) { Add-Fail "[$rel] 占位符不配平: {{=$open  }}=$close" }

    foreach ($lm in [regex]::Matches($text, $linkRe)) {
        $t = $lm.Groups[1].Value
        if ($t -match '^(https?:|mailto:)') { continue }
        $pathOnly = (($t -split '#')[0]).Trim()
        if ($pathOnly -eq '' -or $pathOnly.StartsWith('#')) { continue }
        $cand = Join-Path (Split-Path $f -Parent) $pathOnly
        if (-not (Test-Path -LiteralPath $cand)) { Add-Fail "[$rel] 链接指向不存在的文件: $t" }
    }

    foreach ($sm in [regex]::Matches($text, $secretRe)) {
        Add-Warn "[$rel] 疑似密钥赋值（确认是占位/示例）: " + $sm.Value
    }

    # §N 类检查对 changelog 豁免：changelog 是历史记录，会引述当时的（可能已失效的）写法与章节号。
    # 与 validate-type.ps1 对 changelog 豁免链接检查同理。
    $isChangelog = ($f -eq (Join-Path $root 'docs/changelog.md'))

    if ($validSections.Count -gt 0 -and -not $isChangelog) {
        foreach ($ln in ($text -split "`r?`n")) {
            if (-not $ln.Contains('CODING_STANDARDS')) { continue }
            foreach ($rm in [regex]::Matches($ln, '§\s*(\d+)')) {
                $n = [int]$rm.Groups[1].Value
                if (-not $validSections.Contains($n)) { Add-Fail "[$rel] 引用了 CODING_STANDARDS 不存在的章节 §$n" }
            }
        }
    }

    # 悬挂引用：`AGENTS §N` 的 N 必须真实存在于根 AGENTS.md 的编号章节
    if (-not $isChangelog) {
        foreach ($ln in ($text -split "`r?`n")) {
            foreach ($am in [regex]::Matches($ln, 'AGENTS(\.md)?\s*§\s*(\d+(?:\.\d+)?)')) {
                $num = $am.Groups[2].Value
                if ($agentsSections.Count -gt 0 -and -not $agentsSections.Contains($num)) {
                    Add-Fail "[$rel] 悬挂引用：AGENTS 无 §$num（现有章节：$(($agentsSections | Sort-Object) -join '/')）—— 请指向 docs/CODING_STANDARDS.md §N 或真实存在的章节"
                }
            }
        }
    }
}

# ---------- 2) 类型目录 ↔ 根 AGENTS §4 登记表 ----------
$agentsText = [System.IO.File]::ReadAllText((Join-Path $root 'AGENTS.md'))
$typeDirs = @(Get-ChildItem -LiteralPath $root -Directory |
    Where-Object { $_.Name -notin @('docs', 'scripts', '.git') -and (Test-Path -LiteralPath (Join-Path $_.FullName 'AGENTS.md')) })

foreach ($d in $typeDirs) {
    if ($agentsText -notmatch [regex]::Escape($d.Name + '/')) {
        Add-Fail "[$($d.Name)] 类型目录未登记到根 AGENTS.md §4 类型清单"
    }
}
foreach ($m in [regex]::Matches($agentsText, '\[`([a-z0-9\-]+)/`\]\(')) {
    $name = $m.Groups[1].Value
    if (-not (Test-Path -LiteralPath (Join-Path $root $name))) { Add-Fail "[根 AGENTS §4] 登记的类型目录不存在: $name/" }
}

# ---------- 3) docs 必建骨架（类型交付物 + examples 样例） ----------
$docsRoots = [System.Collections.Generic.List[string]]::new()
foreach ($d in $typeDirs) { $docsRoots.Add((Join-Path $d.FullName 'docs')) }
foreach ($d in $typeDirs) {
    $ex = Join-Path $d.FullName 'examples'
    if (Test-Path -LiteralPath $ex) {
        Get-ChildItem -LiteralPath $ex -Directory | ForEach-Object {
            if (Test-Path -LiteralPath (Join-Path $_.FullName 'docs')) { $docsRoots.Add((Join-Path $_.FullName 'docs')) }
        }
    }
}
foreach ($dr in $docsRoots) {
    $rel = $dr.Substring($root.Length).TrimStart('\', '/')
    foreach ($req in $requiredDocs) {
        if (-not (Test-Path -LiteralPath (Join-Path $dr $req))) {
            Add-Fail "[$rel] 缺少 docs 必建文件: $req（fixed-docs §1：目录必建、内容按触发器）"
        }
    }
}

# ---------- 4) 范本 ↔ 样例副本同源 ----------
foreach ($d in $typeDirs) {
    $srcAgents = Join-Path $d.FullName 'AGENTS.md'
    $ex = Join-Path $d.FullName 'examples'
    if (-not (Test-Path -LiteralPath $ex)) { continue }
    Get-ChildItem -LiteralPath $ex -Directory | ForEach-Object {
        $sample = $_.FullName
        $sampleAgents = Join-Path $sample 'AGENTS.md'
        if (Test-Path -LiteralPath $sampleAgents) {
            $a = [System.IO.File]::ReadAllText($srcAgents)
            $b = [System.IO.File]::ReadAllText($sampleAgents)
            if ($a -ne $b) { Add-Fail "[$($d.Name)/examples/$($_.Name)] AGENTS.md 与类型范本不同源（应逐字节一致）" }
        }
        foreach ($req in $requiredDocs) {
            $s1 = Join-Path $d.FullName ('docs/' + $req)
            $s2 = Join-Path $sample ('docs/' + $req)
            if ((Test-Path -LiteralPath $s1) -and (Test-Path -LiteralPath $s2)) {
                if ([System.IO.File]::ReadAllText($s1) -ne [System.IO.File]::ReadAllText($s2)) {
                    Add-Fail "[$($d.Name)/examples/$($_.Name)] docs/$req 与类型范本不同源"
                }
            }
        }
    }
}

# ---------- 5) 版本号 ↔ changelog ----------
$vm = [regex]::Match($agentsText, '状态：(v\d+\.\d+)')
$changelog = Join-Path $root 'docs/changelog.md'
if ($vm.Success -and (Test-Path -LiteralPath $changelog)) {
    $ct = [System.IO.File]::ReadAllText($changelog)
    $cur = [regex]::Matches($ct, '##\s*(v\d+\.\d+)（当前）')
    if ($cur.Count -ne 1) {
        Add-Fail "[docs/changelog.md] 「（当前）」版本标记数量应为 1，实际 $($cur.Count)"
    } elseif ($cur[0].Groups[1].Value -ne $vm.Groups[1].Value) {
        Add-Fail "[版本] 根 AGENTS 状态 $($vm.Groups[1].Value) 与 changelog 当前版本 $($cur[0].Groups[1].Value) 不一致"
    }
} elseif ($vm.Success) {
    Add-Fail "[docs/changelog.md] 缺失"
}

# ---------- 输出 ----------
if (-not $Quiet) {
    Write-Host ''
    Write-Host '== 仓库级静态校验结果（L1''）=='
    if ($issues.Count -eq 0) {
        Write-Host '无问题：全部通过。' -ForegroundColor Green
    } else {
        $issues | ForEach-Object { Write-Host $_ }
    }
    Write-Host ''
}
Write-Host ("FAIL=" + $fail + "  WARN=" + $warn + "  检查的根文档数=" + $rootDocs.Count + "  类型目录数=" + $typeDirs.Count)
if ($fail -gt 0) { exit 1 }
exit 0
