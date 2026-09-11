# 项目实例生成器（规范层）：从类型规范生成真实项目根的 AGENTS.md + docs/
# 契约：<类型>/placeholders.json（见 docs/authoring-types.md §7、docs/meta-spec.md §3）
# 用法：
#   powershell -NoProfile -File scripts/new-project.ps1 -TypePath springboot -OutDir D:\work\order-service -Answers answers.json
#   powershell -NoProfile -File scripts/new-project.ps1 -TypePath python-fastapi -OutDir D:\work\order-service   # 无答案则逐项提问
# 行为：
#   1) 校验答案（instance 必答 + 正则；choice 必须在 options 内；domain 用 default 并**记录已用默认**）
#   2) 复制交付物（AGENTS.md + docs/**，不含 README.md 与 examples/）
#   3) 按契约替换；conditional / example 类**不替换**（它们是写作示例与待决策项）
#   4) 产出待办：docs/exec-plans/active/<日期>-实例化待办.md + 在 docs/exec-plans/index.md 登记一行
#   5) 用 validate-type.ps1 -Mode Instance 复核生成物
# 退出码：0 = 生成成功且复核无 FAIL；1 = 失败（答案不全 / 契约缺失 / 输出目录非空 / 复核 FAIL）
param(
    [Parameter(Mandatory = $true)][string]$TypePath,
    [Parameter(Mandatory = $true)][string]$OutDir,
    [string]$Answers,
    [switch]$Force,
    [switch]$NonInteractive,
    [switch]$SkipValidation
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

function Fail([string]$msg) { Write-Host "FAIL  $msg" -ForegroundColor Red; exit 1 }

# ---------- 0) 载入契约 ----------
if ([System.IO.Path]::IsPathRooted($TypePath)) {
    $typeDir = (Resolve-Path -LiteralPath $TypePath -ErrorAction Stop).Path
} else {
    $typeDir = (Resolve-Path -LiteralPath (Join-Path $root $TypePath) -ErrorAction Stop).Path
}
$phPath = Join-Path $typeDir 'placeholders.json'
if (-not (Test-Path -LiteralPath $phPath)) {
    Fail "缺少占位符契约：$phPath（见 docs/authoring-types.md §7；范例 springboot/placeholders.json）"
}
$ph = [System.IO.File]::ReadAllText($phPath) | ConvertFrom-Json
$srcAgents = Join-Path $typeDir 'AGENTS.md'
$srcDocs = Join-Path $typeDir 'docs'
if (-not (Test-Path -LiteralPath $srcAgents)) { Fail "类型交付物缺 AGENTS.md：$srcAgents" }
if (-not (Test-Path -LiteralPath $srcDocs)) { Fail "类型交付物缺 docs/：$srcDocs" }

# ---------- 1) 输出目录 ----------
if (Test-Path -LiteralPath $OutDir) {
    $items = @(Get-ChildItem -LiteralPath $OutDir -Force)
    if ($items.Count -gt 0 -and -not $Force) {
        Fail "输出目录非空：$OutDir（换目录，或加 -Force 覆盖）"
    }
} else {
    New-Item -ItemType Directory -Path $OutDir -Force | Out-Null
}
$out = (Resolve-Path -LiteralPath $OutDir).Path

# ---------- 2) 答案与取值 ----------
$ans = @{}
if ($Answers) {
    if (-not (Test-Path -LiteralPath $Answers)) { Fail "答案文件不存在：$Answers" }
    $j = [System.IO.File]::ReadAllText($Answers) | ConvertFrom-Json
    foreach ($pr in $j.PSObject.Properties) { $ans[$pr.Name] = [string]$pr.Value }
}

$known = @{}
foreach ($p in $ph.placeholders) { $known[[string]$p.key] = $true }
foreach ($k in $ans.Keys) {
    if (-not $known.ContainsKey($k)) { Write-Host "WARN  答案中的键未在契约里登记，已忽略：$k" -ForegroundColor Yellow }
}

$values = @{}
$defaultsUsed = @()
$missing = @()
$bad = @()

foreach ($p in $ph.placeholders) {
    $k = [string]$p.key
    if ($p.category -eq 'conditional' -or $p.category -eq 'example') { continue }

    if ($p.category -eq 'domain') {
        $values[$k] = [string]$p.default
        $defaultsUsed += $k
        continue
    }

    $v = $null
    if ($ans.ContainsKey($k)) { $v = $ans[$k] }

    if ([string]::IsNullOrWhiteSpace($v)) {
        $canPrompt = (-not $NonInteractive) -and [Environment]::UserInteractive -and (-not [Console]::IsInputRedirected)
        if ($canPrompt) {
            for ($try = 0; $try -lt 3; $try++) {
                $hint = "  [$k] $($p.desc)"
                if ($p.options) { $hint += "`n         可选：" + ($p.options -join ' / ') }
                elseif ($p.example) { $hint += "（例：$($p.example)）" }
                Write-Host $hint
                $v = Read-Host "  > "
                if (-not [string]::IsNullOrWhiteSpace($v)) {
                    if ($p.category -eq 'choice' -and ($p.options -cnotcontains $v)) { Write-Host "  值不在可选项内，请重填" -ForegroundColor Yellow; $v = $null; continue }
                    if ($p.category -eq 'instance' -and $p.regex -and ($v -cnotmatch $p.regex)) { Write-Host "  不符合格式（$($p.regex)），请重填" -ForegroundColor Yellow; $v = $null; continue }
                    break
                }
            }
        }
    }

    if ([string]::IsNullOrWhiteSpace($v)) { $missing += $k; continue }
    if ($p.category -eq 'choice' -and ($p.options -cnotcontains $v)) { $bad += "$k = '$v'（须为：$($p.options -join ' / ')）"; continue }
    if ($p.category -eq 'instance' -and $p.regex -and ($v -cnotmatch $p.regex)) { $bad += "$k = '$v'（不符合 $($p.regex)，取值区分大小写）"; continue }
    $values[$k] = $v
}

if ($missing.Count -gt 0 -or $bad.Count -gt 0) {
    if ($bad.Count -gt 0) { Write-Host 'FAIL  以下答案不合法：' -ForegroundColor Red; $bad | ForEach-Object { Write-Host "  - $_" } }
    if ($missing.Count -gt 0) {
        Write-Host 'FAIL  缺少必答项（instance/choice 必须给出，禁止静默填充示例值）：' -ForegroundColor Red
        $missing | ForEach-Object { Write-Host "  - $_" }
        Write-Host ''
        Write-Host '  可用答案文件（-Answers）补齐，模板：'
        Write-Host '  {'
        $need = @($ph.placeholders | Where-Object { $missing -contains [string]$_.key })
        for ($i = 0; $i -lt $need.Count; $i++) {
            $p = $need[$i]
            $ex = if ($p.example) { $p.example } elseif ($p.options) { $p.options[0] } else { '' }
            $comma = if ($i -lt ($need.Count - 1)) { ',' } else { '' }
            Write-Host ("    `"{0}`": `"{1}`"{2}" -f $p.key, $ex, $comma)
        }
        Write-Host '  }'
    }
    exit 1
}

# ---------- 3) 复制交付物 ----------
Copy-Item -LiteralPath $srcAgents -Destination (Join-Path $out 'AGENTS.md') -Force
Copy-Item -LiteralPath $srcDocs -Destination $out -Recurse -Force

# ---------- 4) 替换（仅 instance / choice / domain） ----------
$repl = @{}
foreach ($k in $values.Keys) { $repl['{{' + $k + '}}'] = $values[$k] }
$mdFiles = @(Get-ChildItem -LiteralPath $out -Recurse -File -Filter *.md)
$changed = 0
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
foreach ($f in $mdFiles) {
    $t = [System.IO.File]::ReadAllText($f.FullName)
    $orig = $t
    foreach ($kv in $repl.GetEnumerator()) { $t = $t.Replace($kv.Key, $kv.Value) }
    if ($t -ne $orig) {
        [System.IO.File]::WriteAllText($f.FullName, $t, $utf8NoBom)
        $changed++
    }
}

# ---------- 5) 残留扫描 ----------
$catOf = @{}
foreach ($p in $ph.placeholders) { $catOf[[string]$p.key] = [string]$p.category }
$pending = @{}          # key -> @{cat; hits=@("file:line")}
$unknownAgents = @{}    # AGENTS.md 中未登记：模板与契约漂移，FAIL
$unknownDocs = @{}      # docs/** 中未登记：范本自带的写作示例，INFO（按出现次数计数）
foreach ($f in $mdFiles) {
    $rel = $f.FullName.Substring($out.Length).TrimStart('\', '/')
    $isAgents = ($rel -eq 'AGENTS.md')
    $lines = [System.IO.File]::ReadAllLines($f.FullName)
    for ($i = 0; $i -lt $lines.Count; $i++) {
        foreach ($m in [regex]::Matches($lines[$i], '\{\{([^}]*)\}\}')) {
            $k = $m.Groups[1].Value.Trim()
            if (-not $catOf.ContainsKey($k)) {
                if ($isAgents) {
                    if (-not $unknownAgents.ContainsKey($k)) { $unknownAgents[$k] = @() }
                    $unknownAgents[$k] += "$rel`:$($i + 1)"
                } else {
                    if (-not $unknownDocs.ContainsKey($k)) { $unknownDocs[$k] = 0 }
                    $unknownDocs[$k] = $unknownDocs[$k] + 1
                }
                continue
            }
            if (-not $pending.ContainsKey($k)) { $pending[$k] = @{ cat = $catOf[$k]; hits = @() } }
            if ($pending[$k].hits.Count -lt 4) { $pending[$k].hits += "$rel`:$($i + 1)" }
        }
    }
}

if ($unknownAgents.Count -gt 0) {
    Write-Host 'FAIL  AGENTS.md 中出现契约未登记的占位符（模板与 placeholders.json 已漂移）：' -ForegroundColor Red
    foreach ($k in $unknownAgents.Keys) { Write-Host "  - {{$k}}  @ " + (($unknownAgents[$k] | Select-Object -First 3) -join ', ') }
    exit 1
}
$docsUnknownCount = 0
foreach ($k in $unknownDocs.Keys) { $docsUnknownCount += $unknownDocs[$k] }

# ---------- 6) 待办文件 + 计划登记 ----------
$date = Get-Date -Format 'yyyyMMdd'
$stamp = Get-Date -Format 'yyyy-MM-dd'
$commit = ''
try { $commit = (& git -C $root rev-parse --short HEAD 2>$null) } catch { $commit = '' }
$todoName = "$date-实例化待办.md"
$todoRel = "docs/exec-plans/active/$todoName"
$todoPath = Join-Path $out $todoRel.Replace('/', '\')

$sb = New-Object System.Text.StringBuilder
$srcNote = ''
if ($commit) { $srcNote = "（模板修订点 $commit）" }
[void]$sb.AppendLine('# 实例化待办（生成器产出）')
[void]$sb.AppendLine('')
[void]$sb.AppendLine("> 由 ``scripts/new-project.ps1`` 于 $stamp 从类型 ``$($ph.type)`` 生成$srcNote。")
[void]$sb.AppendLine('> 本文件是**生成后必须逐项处理**的清单；处理完把本文件移入 `docs/exec-plans/completed/` 并从 `index.md` 更新状态。')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('## 1. 已按契约替换')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('| 占位符 | 取值 | 类别 |')
[void]$sb.AppendLine('| --- | --- | --- |')
foreach ($k in ($values.Keys | Sort-Object)) {
    $cat = $catOf[$k]
    $note = if ($defaultsUsed -contains $k) { "$cat（**未指定，已用契约默认值**）" } else { $cat }
    [void]$sb.AppendLine("| ``{{$k}}`` | $($values[$k]) | $note |")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('## 2. 待决策项（conditional：保留则删占位符填内容，不适用则删除整行/整章）')
[void]$sb.AppendLine('')
$cond = @($ph.placeholders | Where-Object { $_.category -eq 'conditional' })
if ($cond.Count -eq 0) { [void]$sb.AppendLine('（无）') }
foreach ($p in $cond) {
    $k = [string]$p.key
    $hits = ''
    if ($pending.ContainsKey($k)) { $hits = '　出现于 ' + (($pending[$k].hits) -join '、') }
    [void]$sb.AppendLine("- [ ] ``{{$k}}`` —— $($p.desc)${hits}")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine('## 3. 示例占位符（example：范本自带的写作示例，按项目实际替换或保留）')
[void]$sb.AppendLine('')
$ex = @($ph.placeholders | Where-Object { $_.category -eq 'example' })
foreach ($p in $ex) {
    $k = [string]$p.key
    $n = 0
    $files = @()
    foreach ($f in $mdFiles) {
        $rel = $f.FullName.Substring($out.Length).TrimStart('\', '/')
        $c = ([regex]::Matches([System.IO.File]::ReadAllText($f.FullName), '\{\{' + [regex]::Escape($k) + '\}\}')).Count
        if ($c -gt 0) { $n += $c; $files += "$rel($c)" }
    }
    [void]$sb.AppendLine("- ``{{$k}}`` × $n —— $($p.desc)　[$($files -join '、')]")
}
[void]$sb.AppendLine('')
[void]$sb.AppendLine("- **docs/ 范本自带示例占位符**：$($unknownDocs.Count) 种 / $docsUnknownCount 处（不在契约内，生成器不替换；按项目实际逐处替换，替换完在编辑器里搜索「两个连续左花括号」自查残留）")
[void]$sb.AppendLine('')
[void]$sb.AppendLine('## 4. 生成后必做')
[void]$sb.AppendLine('')
[void]$sb.AppendLine('1. 逐条处理上面的待决策与示例占位符；`docs/` 内各 `index.md` 的登记表按项目实际填行。')
[void]$sb.AppendLine('2. 「快速开始 / 常用命令」里的命令逐条在本项目真跑一遍（规范红线：不写未验证的命令）。')
[void]$sb.AppendLine('3. 落实质量门**单一入口命令**（python 类型需自建 `scripts/gate.py` 之类入口脚本，并让 CI 调同一条）。')
[void]$sb.AppendLine('4. 建 `CHANGELOG.md`（首个对外发布时）、按需补 `README.md` 与部署说明（环境准备类内容归 README，不进 AGENTS.md）。')
[void]$sb.AppendLine('5. 处理完删除本文件，或移入 `docs/exec-plans/completed/` 并回写 `docs/exec-plans/index.md`。')

[System.IO.File]::WriteAllText($todoPath, $sb.ToString(), $utf8NoBom)

# 在 docs/exec-plans/index.md 的计划清单表末追加一行
$idxPath = Join-Path $out 'docs/exec-plans/index.md'
if (Test-Path -LiteralPath $idxPath) {
    $lines = [System.IO.File]::ReadAllLines($idxPath)
    $last = -1
    for ($i = 0; $i -lt $lines.Count; $i++) { if ($lines[$i].StartsWith('|')) { $last = $i } }
    $row = "| 实例化待办（生成器产出） | new-project.ps1 | $stamp | 待办 | 无 | — | ``$todoName`` |"
    $new = @()
    if ($last -ge 0) { $new = $lines[0..$last] + $row + $lines[($last + 1)..($lines.Count - 1)] } else { $new = $lines + $row }
    [System.IO.File]::WriteAllLines($idxPath, $new, $utf8NoBom)
}

# ---------- 7) 汇总 + 复核 ----------
Write-Host ''
Write-Host "== 生成完成：$out =="
Write-Host "  来源类型：$($ph.type)（$typeDir）"
Write-Host "  替换：$($values.Count) 项（含 domain 默认值 $($defaultsUsed.Count) 项）；变更文件 $changed / $($mdFiles.Count)"
Write-Host "  待办：$todoRel"
$condLeft = @($pending.Keys | Where-Object { $pending[$_].cat -eq 'conditional' }).Count
$exLeft = @($pending.Keys | Where-Object { $pending[$_].cat -eq 'example' }).Count
Write-Host "  残留占位符：conditional $condLeft 种 / example $exLeft 种（均为预期，见待办清单）"
Write-Host "  docs/ 范本示例占位符：$($unknownDocs.Count) 种 / $docsUnknownCount 处（不在契约内，按项目实际替换）"

if (-not $SkipValidation) {
    $validator = Join-Path $PSScriptRoot 'validate-type.ps1'
    $hostExe = if ($PSVersionTable.PSVersion.Major -ge 6) { 'pwsh' } else { 'powershell' }
    Write-Host ''
    Write-Host "== 复核（validate-type.ps1 -Mode Instance）=="
    $outText = & $hostExe -NoProfile -File $validator -TypePath $out -Mode Instance 2>&1 | Out-String
    Write-Host $outText
    if ($LASTEXITCODE -ne 0) { Write-Host 'FAIL  生成物复核未通过（见上方 FAIL 项）' -ForegroundColor Red; exit 1 }
}

exit 0
