# 类型规范静态校验脚本（L1）
# 用法：
#   pwsh -File scripts/validate-type.ps1 -TypePath springboot
#   pwsh -File scripts/validate-type.ps1 -TypePath springboot -RequiredHeadings '## 快速开始','## 常用命令'
# 说明：
#   - L1 只做"机器可判"的静态检查；语义/命令真实性请配合 L2 自查清单、L3 样例项目冒烟、L4 评审（见 docs/authoring-types.md §5）。
#   - 退出码：0 = 无 FAIL；1 = 存在 FAIL。WARN 需人工确认，不计失败。
param(
    [Parameter(Mandatory = $true)][string]$TypePath,
    [string[]]$RequiredHeadings = @('## 快速开始', '## 常用命令', '## 测试与质量门', '## 约束、禁区与陷阱', '## 参考链接')
)

$root = (Split-Path $PSScriptRoot -Parent)
$dir = (Resolve-Path -LiteralPath (Join-Path $root $TypePath) -ErrorAction Stop).Path
$mdFiles = Get-ChildItem -LiteralPath $dir -Recurse -File -Filter *.md
$fail = 0; $warn = 0; $issues = [System.Collections.Generic.List[string]]::new()
$glyphRe = '[├└┌┐┃┏┗│─►■]'
$secretRe = '(?i)(password|passwd|secret|token|api[_-]?key|access[_-]?key|secret[_-]?key)\s*[=:]\s*[A-Za-z0-9_\-]{8,}'

foreach ($f in $mdFiles) {
    $rel = $f.FullName.Substring($dir.Length).TrimStart('\','/')
    if ([string]::IsNullOrWhiteSpace($rel)) { $rel = $f.Name }
    $text = [System.IO.File]::ReadAllText($f.FullName)

    # 1) 树形制表符/画线字符
    $m = [regex]::Matches($text, $glyphRe)
    if ($m.Count -gt 0) {
        $chars = ($m | ForEach-Object { $_.Value } | Sort-Object -Unique) -join ''
        $issues.Add("FAIL  [$rel] 含树形/画线字符: $chars —— 请改用纯缩进（writing-standards）")
        $fail++
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
}

# 6) AGENTS/README 等对 docs/CODING_STANDARDS.md 的 §N 引用存在性
$coding = Join-Path $dir 'docs\CODING_STANDARDS.md'
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
