# 仓库决策记录（ADR）

> 用途：本仓库**自身**的架构/约定决策留痕（不是类型模板的 `docs/design-docs/`，那里是给真实项目的）。
> 格式：编号 / 日期 / 状态 / 背景 / 决策 / 取舍 / 触发重审条件。状态取值：提议中 / 已接受 / 已取代 / 已废弃。
> 规则：决策一经确认**只增不改**——结论变化追加新 ADR 并标注取代关系；实质决策同步登记 `changelog.md`。

## ADR-001：多类型间 docs 骨架采用"同形不同文"，暂不引入共享单一源

- 日期：2026-09-11｜状态：已接受
- 背景：`docs/fixed-docs.md` §1 要求每个类型的 `docs/` 必建五项目录（`design-docs/`、`product-specs/`、`exec-plans/`、`generated/`、`references/`）与两个核心文件。第二个类型（`python-fastapi`）出现后，同一问题浮现：这些骨架文件在不同类型间**结构相同、示例内容不同**（如 `design-docs/index.md` 的 ADR 命名约定一致，但举例是各自栈的）。
- 决策：**骨架同形、内容随栈定制**。文件集合与章节骨架由 `scripts/validate-type.ps1`（L1）强制统一；不建 `_shared/` 之类的跨类型单一源。
- 取舍：不引入共享层 ⇒ 无需为"栈无关片段"设计模板引擎或生成器，改动直观；代价是结构漂移风险需要靠 L1 与 `scripts/validate-repo.ps1`（同源/骨架检查）兜住。
- 触发重审：出现**第 3 个类型**，或同一段文本在 ≥3 处逐字重复时，改为评估 `_shared/docs-skeleton/` + 同步脚本。

## ADR-002：静态校验分两层——类型级 L1 与仓库级 L1'

- 日期：2026-09-11｜状态：已接受
- 背景：`scripts/validate-type.ps1` 只扫描 `-TypePath` 指定的目录。根 `AGENTS.md`/`README.md`/`docs/*.md` 的链接与 `§N` 引用、类型范本与样例副本的同源一致、类型登记表与目录的双向一致、版本号与 `changelog` 的一致——这些**当时没有任何机器兜底**（一次实际改动中，根文档被修改却零校验通过）。
- 决策：新增 `scripts/validate-repo.ps1`（L1'）覆盖上述仓库级不变量；与 L1 并存，两者都要求 FAIL=0 才能发布/提交。
- 取舍：两个脚本有少量重复逻辑（画线字符、占位符配平、密钥启发），换来职责清晰、可各自独立运行；重复逻辑若继续增长则抽公共函数。
- 触发重审：出现第 3 个脚本或校验耗时明显影响提交节奏时，合并为带 `-Scope Repo|Type` 的单脚本。

> 补充（2026-09-11）：两层校验已接入 CI——`.github/workflows/validate.yml` 在 push / PR 上跑 `validate-repo.ps1` 与逐类型的 `validate-type.ps1`，并把两个样例的质量门（`uv run ruff/mypy/pytest`、`./mvnw -B clean verify`）作为独立 job。脚本内路径统一用 `/` 分隔符，故 Windows 与 Linux runner 通用。

## ADR-003：PowerShell 脚本采用 UTF-8 with BOM

- 日期：2026-09-11｜状态：已接受
- 背景：两个校验脚本都含中文输出。Windows PowerShell 5.1 对**无 BOM** 的 UTF-8 脚本按 ANSI 解析，会在中文串处报解析错误（实测：`Missing ')' in method call`），导致脚本在只装了 5.1 的机器上完全不可用；而仓库文档过去只写 `pwsh -File ...`（PowerShell 7）。
- 决策：`scripts/*.ps1` 统一存为 **UTF-8 with BOM**，命令写法同时给出两种宿主（`pwsh -File` 与 `powershell -File`）。
- 取舍：BOM 会让某些工具（如部分 diff 视图）显示一个不可见字符；换来跨宿主可用。用 `edit` 工具修改脚本后需复核 BOM 是否仍在。
- 触发重审：仓库改为强制 PowerShell 7 环境（如 CI 镜像固定）时，可去 BOM 并只保留 `pwsh` 写法。
> 补充（2026-09-11，实测）：本仓库文档与脚本为 **LF 行尾、UTF-8**。在 Windows PowerShell 5.1 下用 `Get-Content` 读取这些文件会**误报行数并乱码中文**（实测同一文件 `Get-Content` 报 81 行、`.NET` 报 136 行）。凡"文件规模是否超限""是否含某中文串"的测量，必须用 `[System.IO.File]::ReadAllLines/ReadAllText`（UTF-8 感知）或 `read` 类工具，不能用 5.1 的 `Get-Content` 结论。校验脚本本身已用 `[System.IO.File]::ReadAllText`，不受影响。

> 补充 2（2026-09-11，实测）：**Windows 多盘符环境**下 surefire fork 可能偶发崩溃——`target/surefire-reports/*.dumpstream` 会写 `'other' has different root`，伴随个别测试类报"class path resource … cannot be opened because it does not exist"。原因是 Maven 本地仓库（如 `E:\mavenrepo`）与 JVM 临时目录（C 盘）不同盘符，surefire booter 的"清单 JAR + 绝对路径"classpath 被 JDK 17 的 URLClassPath 拒绝。判定口径：**以每个测试类的 `surefire-reports/*.txt` 为准**，并**重跑** `clean verify`（本机实测重跑即绿）；频繁复发时可用 `-DargLine="-Djdk.net.URLClassPath.disableClassPathURLCheck=true"` 规避（属环境适配，**不写进样例 pom 或正式命令**）。Linux CI 单一根路径，不受影响。

> 补充 3（2026-09-11，实测）：本条第 30 行的风险**已实际发生一次**——用 `edit` 工具改过 `scripts/validate-type.ps1` 后 BOM 丢失（首字节由 `239,187,191` 变为 `35,32,231`），PowerShell 5.1 随即按 ANSI 解析并在中文串处报 `Unexpected token '\'`、`Missing closing ')'`，脚本完全不可用（同一时刻 `validate-repo.ps1` 未改动、BOM 仍在，对照成立）。**改完任一 `scripts/*.ps1` 必须核验首字节**：`([System.IO.File]::ReadAllBytes('scripts/validate-type.ps1')[0..2] -join ',')` 应输出 `239,187,191`；不符则补写 `EF BB BF` 后再跑校验。

## ADR-004：类型的"强制力声明"必须与实际契约一致，且 L4 必须在冻结修订点上评审

- 日期：2026-09-11｜状态：已接受
- 背景：`python-fastapi` 类型首次 L4 对抗复核（独立代理）以实验方式证伪了两类**假绿**：① 文档写"依赖方向（禁反向、**越层**、**同层横向**）由 `import-linter` 契约强制"，而 `layers` 契约的语义只保证"上层可引用下层"——实测在路由里加越层导入、加同层横向导入，契约仍报 `2 kept, 0 broken`；② `docs/enforcement-map.md` 把"错误码 6 位分段/命名/只增不删"与"`traceId` 贯穿"登记为已被测试强制，而测试实际只断言了长度、纯数字与 body 字段，**错误路径的响应头缺 `X-Trace-Id` 无任何测试覆盖**（真实缺陷）。同一轮复核还两次因交付物被并发修改而需要"修订点更新"。
- 决策：
  1. **强制力声明必须可被实验证伪**：凡文档声称"由 X 强制/已机器化"的条目，L4 评审方须亲手制造一次违规确认它真的会红；声明强于实际视为 **BLOCKER**，修法**优先补强制**（本次补了禁越层、同层横向两条契约与错误路径断言），其次才是收窄措辞。
  2. **L4 期间冻结交付物**：进入 L4 前固定修订点（commit/tag 或等价的文件哈希集合），评审中不得并发改动；否则结论随时失效。
  3. 该要求已写入 `docs/authoring-types.md` §5 的 L4 条目与 §4 自查清单。
- 取舍：L4 成本上升（需造违规、需冻结窗口），换来"文档里的强制力 = 真实的强制力"——这正是本仓库对下游的核心承诺。
- 触发重审：当引入自动化变异测试（mutation testing）可批量验证"规则真的拦得住"时，可放宽"人手造违规"的要求。
