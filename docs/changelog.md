# 变更记录（Changelog）

> 本仓库规范/文档的版本历史统一登记于此（根 AGENTS.md 不再内嵌长历史）。登记口径：对规范/文档有实质影响的改动追加一行，格式 `vX.Y —— 说明（涉及文件）`。

## v3.2（当前）

**规范与校验机制（仓库层）**

- **新增仓库级校验 L1'**：`scripts/validate-repo.ps1` 覆盖根文档（链接/`§N` 引用/画线字符/占位符/密钥启发）、**类型目录 ↔ 根 `AGENTS.md` §4 登记表双向一致**、**类型范本 ↔ `examples/` 副本逐字节同源**、根 AGENTS 版本号 ↔ 本 changelog「（当前）」、`AGENTS §N` 悬挂引用。实测抓到并修掉：根 README 占位符不配平、`springboot/README.md` 的 `AGENTS §13` 悬挂引用、范本与样例不同源。
- **两个校验脚本跨平台化 + 宿主兼容**：路径统一 `/`；排除 `target/.venv/venv/node_modules/__pycache__/.mvn/.git/*_cache`（修掉"`.venv` 内依赖包 README 被计入文件数"导致 28→42 漂移的缺陷）；**UTF-8 with BOM**，Windows PowerShell 5.1 可直接 `powershell -File` 运行（见 `docs/decisions.md` ADR-003）。
- **密钥扫描扩展到配置文件**：`.md` 之外新增 `*.yml/*.yaml/*.properties/.env/.example/.ini/.toml`，并新增"**密钥类环境变量带非空默认值**"WARN（正是 `${DB_PASSWORD:root}` 这类形态）。
- **仓库自身接入 CI**：`.github/workflows/validate.yml` 在 push/PR 跑两层静态校验，并把两个样例的质量门（python：`uv sync --locked` + ruff + mypy + import-linter + pytest；springboot：`./mvnw -B clean verify`）作为独立 job；新增 `.gitattributes`（`* text=auto eol=lf`）保证跨平台行尾一致，避免"同源校验"被 CRLF 误伤。
- **新增 `docs/enforcement-map.md`（不变量 → 强制手段映射）**：按类型登记每条「必须」的强制者（工具/测试）与「仅靠人读」清单及机器化路线；明确"只告警不拦构建 = 假绿，不算强制"。springboot 侧来源为一次独立 L4 复核（8 类有工具 / 36 项仅人读），python-fastapi 侧为落地该类型时的实现盘点。
- **新增 `docs/decisions.md`（本仓库 ADR）**：ADR-001 多类型 docs 骨架"同形不同文"（暂不引入共享单一源，触发重审条件：出现第 3 个类型或同文重复 ≥3 处）；ADR-002 校验分两层（类型级 L1 + 仓库级 L1'，已接 CI）；ADR-003 脚本 UTF-8 with BOM；另附两条实测补充（PowerShell 5.1 `Get-Content` 对 LF 文件误报行数/乱码；Windows 多盘符下 surefire fork 偶发 `'other' has different root` 的判定口径）。
- **把"L3 怎么算通过"写进规范**：`docs/authoring-types.md` §5 L3 增两条硬要求——质量门必须**整条真跑**且**故意违规即红**（两次输出都要记入 README）；环境不足时先跑"无中间件切片"并**如实标注未覆盖范围**，禁止 `-Dskip.*` 或"应该能跑"。§4 自查清单同步新增两项（每条「必须」在 enforcement-map 有强制者或标注「仅评审」；质量门整条真跑）。`docs/writing-standards.md` 反例表新增两行（只告警不拦构建＝假绿；只在 `compile` 上验证过的门禁命令）。
- **下游迁移指引**：根 `README.md` 新增「用法一之二：已有项目从旧版模板升级」，含补建五项目录的逐条命令与"合并而非覆盖"纪律。
- **根 `AGENTS.md`**：新增 `docs/enforcement-map.md` 与 `docs/decisions.md` 入口、`scripts/validate-repo.ps1` 与 CI 行；§6 增加"新增/修订一条「必须」规则"入口；状态升至 v3.2。

**架构信条（新增固定组成部分，两个类型共同）**

- **`docs/fixed-docs.md` 新增 §5「架构信条（6 条）」**，由"四个固定组成部分"变为**五个**：① 分层依赖固定为 `type → config → repo/dao → service → runtime → ui` 单向（`ui` 最上、`type` 最底，类型必须给出目录映射与缺口）；② 规范先行（spec-first，禁代码先行）；③ 开闭原则（靠新增实现/事件扩展，不靠增长 `if type == …` 分支）；④ 禁止过度设计（抽象需 ≥2 处真实使用或明确近期需求）；⑤ RDBMS 表设计达到 3NF（反范式必须记 ADR 并写明同步与一致性）；⑥ 单一职责（一个变化原因）。
- **两个类型已内嵌**：`springboot/docs/design-docs/core-beliefs.md`（B7–B12）与 `python-fastapi/docs/design-docs/core-beliefs.md`（B9–B14）逐条给出**栈内落地 + 强制者**（类型是独立交付物，必须自带全文而非指针）；两者 `docs/ARCHITECTURE.md` §3 增**分层映射表**（规范层 ↔ 本类型目录/包）与"已强制 / 仍属仅评审"的现状说明。
- **`docs/enforcement-map.md` 增 §2.3**：登记 6 条信条的强制现状——① 为**部分强制**（两类型现有契约仅覆盖 `ui→service→repo(→type)` 与禁反向/横向，`config`/`type`/`runtime` 方向待补契约），②–⑥ 目前为**仅评审**，并各自给出可机器化路径。
- 根 `AGENTS.md` 与 `docs/authoring-types.md` 的"四个固定组成部分"表述同步改为**五个**（`docs/changelog.md` 中 v3.0 的历史描述保持不变）。

**质量门控（新增固定组成部分，两个类型共同）**

- **`docs/fixed-docs.md` 新增 §6「质量门控（固定组成部分，每个类型必须内嵌）」**，固定组成部分由**五个**变为**六个**：把"质量门 = 提交/合并前必须全绿、且本地与 CI 跑**同一条命令的单一入口**"写成规范，含——①**三条硬要求**（单一入口 `{{质量门入口命令}}`，禁 `&&` 拼接的检查清单；失败即停、**只告警不拦构建＝假绿**且不得声称"已强制"；发布前**整条真跑** + **故意违规即红**，两次输出记入类型 README「验证状态」）；②**最小覆盖范围**（格式 / lint / 类型检查 / 架构与依赖方向不变量 / 单元测试 /（如适用）覆盖率与构建；集成测试可置于标记下默认不跑，但必须**如实标注未覆盖范围**）；③**落点分工表**（AGENTS「测试与质量门」= 唯一一份入口命令；`CODING_STANDARDS.md` 质量门节 = 检查项与规则位置；CI = 调同一条命令；`enforcement-map.md` = 强制者登记，**不重复命令正文**）与三条维护规则。
- 同步口径：根 `AGENTS.md`（地图行 → **六个**；红线 1 增"整条真跑 / 假绿 / 不得跳过参数蒙混"；§3.1 发布条件增"质量门整条真跑与'故意违规即红'的证据已记入类型 `README.md`"；§6 入口括注）、`docs/authoring-types.md`（开头固定部分清单 → 六个、§2 第 7 章要点改为"单一入口命令 + §6 硬要求"、§4 自查清单"已落实固定内容"补质量门控、§5 L3 硬要求指向 §6）、`docs/enforcement-map.md` §1.4（明确分工：**本表登记"谁拦"，§6 规定"在哪拦、怎么算拦住了"**）、`docs/writing-standards.md` 反例表（假绿行指向 §6）、根 `README.md` 仓库结构、两个类型 `AGENTS.md` 的「固定组成部分的落点」块（⑤ 之后新增 ⑥ **质量门控** → 本文件「测试与质量门」+ `CODING_STANDARDS.md` 质量门节；样例副本同步）。
- **未新增类型正文**：两个类型的 `CODING_STANDARDS.md` 质量门节已满足 §6——springboot = 一条 `./mvnw clean verify`（CI 同命令）+「任一插件失败＝构建失败、不得 `-Dskip.*` 绕过」+ 假绿警告；python-fastapi = 五项检查由**单一入口**按序执行、失败即停、入口命令只在 AGENTS 维护一份。故本次只补"落点枚举"，不重复内容。
- 理由：质量门此前在 `authoring-types` §4/§5、`writing-standards` 反例表与 `enforcement-map` §1.4 各自表述（同一要求四处口径），与"单一事实来源"冲突；升为固定组成部分后，"在哪拦、怎么算拦住了"有了唯一权威。

**对外变更记录口径（新增）与下游可读性修正**

- **根级 `CHANGELOG.md` 纳入固定口径**：`docs/fixed-docs.md` §1 的根级文件清单新增 `CHANGELOG.md`（**首个对外发布时建立**，不计入"建议 ≤5 个"上限），并给出**三者分工表**：`CHANGELOG.md` = 对外版本变更史（Keep a Changelog 语义）、`docs/design-docs/` = 对内技术决策（ADR/core-beliefs）、`docs/product-specs/` = 需求与验收——**禁止同一变更三处各写一份**。
- **两个类型的 `AGENTS.md`**：目录树补 `CHANGELOG.md` 行；新增一节**「五个固定组成部分的落点」**（① docs 布局 ② Git 提交规范 ③ 架构骨架 ④ 需求规格 ⑤ 架构信条，各自落在哪个文件；**指针式、不复制细则**，避免与根仓库 `fixed-docs.md` 形成双份权威）。
- **下游可读性**：会被复制到真实项目的 `docs/**` 中"指向模板仓库"的表述统一改为"模板仓库 `…`（项目内可自建同名登记表）"；`core-beliefs.md` 的信条表补注"本表已内嵌全文，采用本模板的项目以本表为准"。类型 `README.md` 等**维护者向**文本仍保留"根仓库"措辞（它们不随模板复制）。

**按 harness-principles / writing-standards 的对照审计（两类型）**

- 审计结论：两类型整体符合；发现 3 条需处置，**全部在规范层修掉**（不改样例）：
  1. `python-fastapi/AGENTS.md` 的质量门曾是 5 段 `&&`（违反 `writing-standards` §2.2"一个动作一条命令，串联 2–3 个"）→ 改为**单一入口约定**：AGENTS 只写 `{{质量门入口命令}}`（把五项检查按序封装成一条命令、失败即停；推荐形如 `uv run python scripts/gate.py`），`CODING_STANDARDS` §14 同步规定"五项检查由单一入口按序执行"与"CLI 入口允许 `print` 的**显式例外**（须写在 `per-file-ignores`）"。
  2. harness 原则 8（发现即登记）→ 两个类型的 `docs/exec-plans/tech-debt-tracker.md` 由**占位行**补齐为真实台账：springboot `TD-101..TD-105`（无 Docker 集成链路未验证、`/v3/api-docs` 未端到端、`DictCache` 类型约束、`SummaryJavadoc` 本地化、架构规则缺 `because`），python-fastapi `TD-101..TD-106`（读缓存回链未实现、Redis key 集中无强制、MQ 无运行时实现、commitlint 未接入、`-m integration` 空集、**质量门单入口未在样例落地**）。
  3. harness 原则 4（失败消息要教怎么改）→ 写成规范要求：springboot `CODING_STANDARDS` §3「架构规则必须带 `because(...)` 说明修法」、python-fastapi §4「门禁失败消息必须自带修法指向」；样例侧的落实缺口登记为上表 `TD-105`（springboot）。
- 审计中确认的合规项（不再登记）：AGENTS 规模（116 / 142 行，限 100–250）、单文件规模（CODING 304 / 244，限 ≤400）、文件单换行收尾、无 TODO/FIXME/调试残留、无"将来会支持"式愿望、目录树纯缩进、占位符与密钥红线、环境准备内容不进 AGENTS、命令真实可跑（`mvn clean verify` 85 测试绿 / python 五项门禁绿）。

**`springboot/docs/` 知识库对照 harness-principles 的取证核查（8/10 完全符合 → 修 3 处）**

- **① 原则 2（不可见的知识等于不存在）**：两条**已验证的实现坑**原本只写在样例里、规范 `docs/` 零记录——`MatchXpath` 在 Checkstyle 9.3 本栈不可用（挂 Checker 报 not allowed、挂 TreeWalker 时 JAXP 对 `DetailAST` 抛 `Operation is not supported`），以及由此改用 **Checker 级 `SuppressionSingleFilter` 按文件路径限包**。已写入 `springboot/docs/CODING_STANDARDS.md` §13 新增的「**已验证的实现坑**」两条（另含中文 Javadoc 必须把 `SummaryJavadoc.period` 设为「。」的本地化要点），使下一个照规范装配规则的人不必重踩。
- **② 原则 9（口味靠 lint 反重复）**：「禁同义写法 / 禁本地重复实现」两个类型都无任何工具覆盖，且 `enforcement-map.md` **零登记**（违反本仓库"未强制必须标仅评审"的纪律）→ §3 增一行「**仅评审** + 机器化路径（自定义 lint 或"共享实现引用"断言）」。
- **③ 原则 10（非平凡变更同变更记 ADR）**：`design-docs/index.md` 的触发器（"决策定案 → 必须写 ADR"）**已满足却无一条 ADR**（`CrudRepository` 选型、Redis 值类型必须 `ArrayList`、DAO 前缀实现方式、`SummaryJavadoc.period` 本地化）→ 按用户选择 **(b)** 登记为技术债：springboot `TD-106`、python-fastapi `TD-107`。
- 核查中确认合规的 8 条：AGENTS 只作地图 + docs 承载细节（原则 1）、三处 index 索引与 AGENTS 树给名称/路径/职责（原则 3）、不变量已机器化且登记强制者（原则 4）、`core-beliefs` 不可协商 + 必须/应该/可以三级（原则 5）、快速开始/curl/health/traceId/验证状态（原则 6）、curl 冒烟进 PR + L4 协议（原则 7）、台账已真实使用 + 归档规则（原则 8）、措辞精确/无残留/单换行收尾（原则 10）。另记录：依赖方向的"强制现状"在 `ARCHITECTURE §3`、`core-beliefs`、`enforcement-map` 三处并存属**受众不同**（下游自包含 vs 维护者登记），不视为双份漂移。
- 本次全部为**规范层**改动，未改任何样例（sample 仅作规范的检验载体）。
- **补齐 §2 骨架缺失章**：`springboot/AGENTS.md` 原缺 ◎ 章「环境与版本约束」（版本约束散落在第 1 章 bullet 与「约束、禁区与陷阱」里）→ 新增该章 5 条：语言/运行时最低版本以 `pom.xml` 的 `<java.version>` 为单一来源、依赖管理唯一（Maven，不混用 Gradle）、版本来源分层（Spring 系归 Boot BOM，非 BOM 组件在 `<properties>` 显式声明且禁散落）、质量工具链版本在 pom 锁定且文档不写版本号、环境准备类内容归项目 `README.md`/`ARCHITECTURE §9`。同时与 `python-fastapi/AGENTS.md`（本就有该章）对齐，消除两类型骨架不对称。
- **「编码约定」章口径收紧（`springboot/AGENTS.md`）**：原 11 条摘要中，**命名与分层、DAO 方法前缀、中间件命名、API 文档注解、测试与自测、测试约定**属口味类（无可拦构建的强制者）→ 移出，改为章首一行指针指向 `docs/CODING_STANDARDS.md` §3/§4/§7/§8/§12；保留的 5 条均**就地标注强制者**（依赖方向/QueryWrapper → ArchUnit，错误码 → 注册表测试）或属既定红线（业务字典必建、Git 提交属固定组成部分）。章规模 13 → 9 行，`AGENTS.md` 124 → 120 行。同步：章首写明"本章只留有强制者或红线的条目"的判据；「目录结构与架构」的命名指引改指 `CODING_STANDARDS §3`；`docs/authoring-types.md` §2 第 6 章内容要点改为"**只列硬约束 + 口味类只留指针（推荐就地标强制者）**"、规模提示 ≤25 → ≤15 行，§4 自查清单新增一项（不得摘要口味类细则）。理由：摘要与细则两处维护必然漂移，而 `enforcement-map.md` §3 已登记"摘要 ↔ 细则一致性"只能靠评审。**python-fastapi 未改**：按同一判据复核，其章内条目均已有强制者（ruff/mypy/import-linter/注册表测试）或同属可靠性、安全红线，无需删条。
- **`docs/fixed-docs.md` §2 落点表述修正**：原文要求"类型模板须在「编码约定」中嵌入提交规范全文"，与本文件开头"类型 AGENTS.md 只保留**必须摘要 + 指向**"及上方新口径自相矛盾（现实是全文在 `CODING_STANDARDS.md`「Git 提交规范」节）→ 改为"全文嵌入类型 `docs/CODING_STANDARDS.md` 的「Git 提交规范」节，`AGENTS.md`「编码约定」保留一行摘要 + 指向该节"，并指名 `springboot/AGENTS.md` 为范例。
- **`springboot/AGENTS.md` 地图化瘦身（120 → 95 行，−21%）**：**删维护者旁注**（Gradle 备选命令——与"依赖管理唯一 Maven"自相矛盾、"未启用代码生成则删除本行"、单列一行"运行产物"）；**目录树由 28 行穷举改为顶层职责树**（逐文件与内容触发器指向各 `docs/*/index.md`，分层映射指向 `docs/ARCHITECTURE.md` §3）；**常用命令表 12 → 6 行**（合并 启动/指定 Profile、全部/单个测试、编译/打包/运行）；环境与版本约束 5 → 3 条；编码约定 5 → 4 条（Git 提交并入下方"固定组成部分落点"块）；约束禁区 6 → 4 条；参考链接 5 → 2 行。形态与 `harness-principles.md` 原则 1/3（短 AGENTS + docs 当文件夹系统）一致。
- **规范口径同步（否则瘦身后的模板会与规范冲突）**：`docs/writing-standards.md` §1 规模口径拆成两类——**类型模板版（地图式）目标 ≤120 行、典型 80–140 行**（复制到项目根的地图，只放不可协商项与入口），**真实项目版典型 100–250 行、上限约 300 行**（填完占位符并并入项目特有内容后）；`docs/authoring-types.md` §2 第 5 章改为"**顶层职责树**（不逐文件穷举——细节指向 `docs/*/index.md` 与 `ARCHITECTURE.md` §3）"、规模提示 ≤30 → ≤20 行，§3 要点与 §4 自查清单同口径。
- **未丢知识**：移出 AGENTS 的内容在类型 `docs/` 内均有唯一权威（命名 / 中间件命名 / API 注解 / 测试约定 → `CODING_STANDARDS.md` §3/§4/§7/§8/§12；逐文件与触发条件 → 各 `docs/*/index.md`；分层与依赖方向 → `ARCHITECTURE.md` §3）；`examples/sample-project/AGENTS.md` 副本逐字节同步。
- **按用户口径再细化（同日修订，取代上条中的两处）**：① **「目录结构与架构」恢复为逐文件目录树**（路径 + 文件 + 内联职责：8 个 Java 包逐个成行、`resources` 下三个路径、`docs/` 的 11 个必建文件逐个列出并标 ★）——上一版的"顶层职责树"过于笼统，结构描述仍需到文件级；② **删除「参考链接」小节**（原 2 行）——docs 链接已在目录树与"固定组成部分落点"块就地给出，官方文档链接移交 `springboot/docs/references/index.md` 登记表（新增 springdoc-openai 一行；Spring Boot / MyBatis-Plus 两行原有），脚手架链接保留在类型 `README.md`。同步：`docs/authoring-types.md` §2 第 5 章改回"**逐文件目录树**"、规模 ≤20 → ≤35 行，第 10 章改为"**参考链接（默认不设）**"，§3 要点与 §4 两条自查项（逐文件树 / 默认不设链接小节）同口径；`docs/fixed-docs.md` §1 规则 4 补"AGENTS 默认不设参考链接小节"；`scripts/validate-type.ps1` 的必需章节去掉 `## 参考链接`（原与 `authoring-types` 把该章列为 ○ 可选自相矛盾）。`springboot/AGENTS.md` 最终 **102 行**（原始 120 行；目录树 31 行）。

**springboot 类型（v1.1）**

- **模板 L5 修正（官方现状）**：`IService`/`ServiceImpl` 指引改为官方现行的 **`CrudRepository`**（官方自 MP 3.5.9 起不再建议 IService），并补"**分页等 jsqlparser 插件默认不携带、必须显式引入 `mybatis-plus-jsqlparser`**"红线；版本线刷新为 Boot **3.5.16**（3.5 线最新）/ 4.1.1（4.2.0-M1 为里程碑）/ MP 3.5.17 / springdoc 2.x 最新 2.9.1。
- **单一事实来源与确定性修正**：结构口径三方打架（AGENTS 目录树 vs CODING_STANDARDS「feature 优先」vs ARCHITECTURE）统一为**分层**（feature 优先降为"需三处同步"的可选变体）；`dict/` 包"必建"与分层落点矛盾改为"字典功能必建、按分层落点"；ARCHITECTURE 的 Redis key 表补齐 `dict:{typeCode}`；清掉条件句（`若用 Flyway`/`（若配置）`）；`AGENTS.md`「参考链接」由占位符改为**真实可点击链接**并补五目录入口。
- **样例质量门修复（此前 `mvn test/verify` 从未跑通）**：`spotbugs-maven-plugin:4.8.6` 中央仓库不存在 → **4.10.4.1**；MP 3.5.9+ 拆包导致 `IService`/`PaginationInnerInterceptor` 缺失 → 显式引入 `mybatis-plus-jsqlparser` 并改用 `CrudRepository`；`CrudRepository.baseMapper` 单测显式装配。补齐缺失门禁：团队 Checkstyle 规则文件（Google 基底 + 禁 `SELECT *`/`System.out`/`printStackTrace`/JUL&commons-logging）、SpotBugs `effort=Max` + excludeFilter、**JaCoCo `check`（LINE ≥ 80%）**、Enforcer `requireMavenVersion` + `bannedDependencies`；并让规则**真的拦构建**（`violationSeverity=warning`，修掉"0 violations 假绿"）。原始输出摘要见 `springboot/README.md`「验证状态」L3 行。
- **样例按类型规范补齐**：版本化 DDL `db/V1__init.sql` + compose 初始化；Profile 拆分与去默认口令；`@Transactional` 边界；分页 `PageResult` 与上限；状态机改真枚举；主键策略统一；`RedisConfig`（JSON 序列化，禁 JDK 序列化）+ 写后删缓存 + 字典缓存；RabbitMQ DLQ 绑定 + 手动 ack + 幂等；`Result.traceId` 贯穿；错误码补 `5xxxxx` 段。
- **可机器化的不变量固化**：DAO 方法前缀词表（Checkstyle 正则）、依赖无环/禁反向/禁同层横向（ArchUnit）、禁业务代码直写字典表、错误码注册表测试（唯一/6 位分段/命名），每条均以"**故意违规即红**"验证。

**python-fastapi 类型（v1.0，新增）**

- 按 `authoring-types.md` §2 骨架与 §3 结构新增完整类型交付物：`AGENTS.md` + `README.md` + `docs/`（CODING_STANDARDS / ARCHITECTURE + 五项目录 12 个文件）+ `examples/sample-project/`（L3 样例：FastAPI + Pydantic v2 + SQLAlchemy 2.0/Alembic + PostgreSQL + Redis + RabbitMQ，uv 管理依赖）。
- **L3 已真实执行**（无 Docker 切片）：`uv sync --locked` → ruff（check/format）→ mypy（strict，31 文件）→ pytest（15 passed）→ `alembic upgrade head --sql`（离线 DDL）→ `uvicorn` 起服务 + curl 探活；如实标注未覆盖的中间件链路。
- **依赖方向机器化**：新增 `import-linter` 契约（`layers` 单向 + `forbidden` 禁反向）并纳入质量门，反向用例实测 BROKEN + 退出码 1；pytest 开 `--strict-markers` 并登记 `integration` 标记。
- **L5 已核实**（2026-09，逐包 PyPI/GitHub Releases，与 `uv.lock` 逐项吻合）；**L4 独立复核已执行**（另一代理对抗式复核，2026-09-11）：报告 3 条 BLOCKER——①「依赖方向已机器强制」被实验证伪（`layers` 契约不拦越层/同层横向）→ 补 `forbidden`（禁越层，含 `allow_indirect_imports`）与 `independence`（禁同层横向）两条契约；② 500 兜底响应缺 `X-Trace-Id` → `_error_response()` 统一补头并加错误路径断言；③ changelog 未登记 → 即本段。另修 `print` 禁令未拦（ruff 加 `T20`）、错误码分段/命名/只增不删无断言（补三组断言）、迁移命名口径、目录树缺项、环境准备条目自相矛盾、`SAMPLE_DEBUG` 写死、版本号两处各写一份、`§N` 错章引用。**每条修复均以"故意违规即红"实证**（越层/同层横向/print/去响应头/非法错误码各自实测失败后还原）；`docs/enforcement-map.md` §2.2 的强制力声明同步校正为与实测一致。

## v3.1

- **docs/ 五项目录改为「一律必建」（目录必建 + 内容触发器）**：`design-docs/`、`product-specs/`、`exec-plans/`、`generated/`、`references/` 不再"条件满足才建"——目录与最小入口文件必建，触发条件只决定**内容**何时补齐（首个 ADR / 首个需求 / 多轮任务或技术债 / 首个生成物 / 首份外部资料）。同步改写口径：`docs/fixed-docs.md` §1（骨架图 + 规则 5/6/7）、`docs/authoring-types.md` §3（类型与真实项目两棵树 + 要点）、§4 自查清单（新增一项）、§5 L1 说明、根 `AGENTS.md` §3/§3.1/红线 5、根 `README.md` 使用步骤第 3 条、`springboot/AGENTS.md` 与 `springboot/examples/sample-project/AGENTS.md` 目录树 + 约定、`springboot/README.md` 第 6 条、`examples/sample-project/README.md` 对照表。
- **新增范本 12 个文件**：`springboot/docs/` 下 `design-docs/{index.md,core-beliefs.md}`、`exec-plans/{index.md,tech-debt-tracker.md,active/index.md,completed/index.md}`、`generated/index.md`、`references/index.md`（各含用途、内容触发器、命名约定与占位表格）；并同源拷贝到 `examples/sample-project/docs/`（含此前缺失的 `product-specs/`），使交付物与真实项目骨架一致。
- **L1 校验加强**：`scripts/validate-type.ps1` 新增第 7 项——docs 必建骨架存在性（2 个核心文件 + 10 个目录入口文件，共 12 项），缺一即 FAIL；脚本头注明宿主需 PowerShell 7+（本文件 UTF-8 无 BOM，Windows PowerShell 5.1 按 ANSI 解析会报解析错误）。
- **验证（2026-09-11）**：`-TypePath springboot` → FAIL=0、WARN=0、28 个 md；`-TypePath springboot/examples/sample-project` → FAIL=0、WARN=0、14 个 md；反向用例（临时骨架故意缺 `references/index.md` 与 `exec-plans/tech-debt-tracker.md`）→ 2 条 FAIL、退出码 1。springboot README「验证状态」L1/L2 行与样例 README L1 行同步回填。

## v3.0

- **文档拆分**：根 `AGENTS.md` 重写为精简"地图"版（项目定位/仓库地图/目录约定/类型清单/红线/入口）；正文按主题拆入本 `docs/`：
  - `writing-standards.md` —— 写作基本原则、命令与路径书写规范、常见反例（原 §6–§8）
  - `authoring-types.md` —— 新增/修订类型工作流、统一结构骨架、**类型项目文件夹结构**、交付自查清单（原 §3–§5 内容重组；不再内联整篇"通用骨架模板"，以文件夹结构 + fixed-docs 固定内容代替）
  - `fixed-docs.md` —— 四个固定组成部分：docs 布局 / Git 提交规范 / ARCHITECTURE 骨架 / product-specs 骨架（原 §5.1–§5.4）
  - `harness-principles.md` —— Harness Engineering 原则摘编（原 §11）
  - 本文件承接全部版本历史（v1.0–v2.8）
- **新增根 `README.md`**：面向人/下游用户的入口介绍（项目定位、快速使用、类型清单、仓库结构、贡献者入口），与根 AGENTS.md（代理地图）分工。
- **验证协议（L1–L5）**：新增 `scripts/validate-type.ps1`（L1 静态校验：画线字符/占位符/密钥启发/链接存在/必需章节），写入 `authoring-types.md` §5（L2 清单复核 / L3 样例项目冒烟 / L4 评审 / L5 现状核实+时间戳）；跑脚本修复了 springboot README 的 `../../` 链接深度与 product-specs 示例悬空链接。
- **样例项目**：springboot 类型新增 `examples/sample-project/`（Spring Initializr 真实生成的最小 Maven 项目），作为 L3 动态冒烟载体；springboot README 增加「验证状态」记录（L1 PASS / L3 待执行+步骤 / L5 已核实）。
- **布局修正（springboot 类型交付物上提）**：`docs/` 范本（CODING_STANDARDS / ARCHITECTURE / product-specs）从 `examples/docs/`（曾用名 `示例/docs/`）上提为 `springboot/docs/`——它们是类型交付物的一部分（真实项目 `docs/` 的直接源头）；`examples/` 仅保留冒烟样例 `sample-project/`。移动过程中源文件被误删，已在 `springboot/docs/` 按既定规范**完整重建**并全量修正引用与目录树描述。
- **根 AGENTS.md §3.1**：新增「类型交付物结构」固定描述（AGENTS.md 本体 + docs/ 范本 + README + examples 的★/按需划分与复制路径、发布条件）。
- **目录名英文化**：`springboot/示例/` 重命名为 `springboot/examples/`，全仓库 md 路径引用同步（中文行文用词如"示例值"保留）。
- **sample-project 按类型规范重做（完整栈合规样例）**：按 `springboot/AGENTS.md` 推导其应为"规则可套用的真实项目"——现已装配全栈（Boot 3.5.0 + MyBatis-Plus 3.5.17 + springdoc 2.8.13 + MySQL/Redis/RabbitMQ compose + application.yml）并复制 `AGENTS.md` 与 `docs/` 必建核心（CODING_STANDARDS/ARCHITECTURE）；触发器目录（product-specs 等）未触发不建。L1 对本样例 PASS；L3 编译/运行验证待 JDK+Maven+Docker 环境（版本坐标明确标注"基线，待编译核验升级"）。
- **L3 部分执行（2026-09-09，无 Docker 切片）**：系统 JDK 17.0.12（D:\Java\jdk-17）+ Maven 3.9.16（D:\apache-maven-3.9.16）就绪后，在 `examples/sample-project/` 实测 `mvn -DskipTests compile` 与 `mvn -DskipTests package` 均 **BUILD SUCCESS**（可执行 jar 47MB），证明 Boot 3.5.0 / mybatis-plus 3.5.17 / springdoc 2.8.13 坐标真实可解析编译；需中间件的 test/run/curl 切片待 Docker 补跑。

## v2.x（合并期）

- v2.8 —— §11 以 OpenAI harness-engineering（2026-02）为主参考改写，deepseek-harness 降为工程卫生补充；参考资料补 OpenAI/Codex 链接。
- v2.7 —— 新增 Harness 核心原则与最佳实践（deepseek-harness 根 AGENTS.md 摘编）。
- v2.6 —— 环境纪律：环境准备类内容不写入 AGENTS.md；版本约束指向单一来源。
- v2.5 —— 需求规格 product-specs/ 固定骨架。
- v2.4 —— 架构文档 ARCHITECTURE.md 固定骨架。
- v2.3 —— Git 提交规范定为固定组成部分。
- v2.2 —— docs/ 定为固定组成部分（核心文件必建、子目录触发器、禁"若有"句）。
- v2.1 —— 吸收 OpenAI Advanced Pack 经验：docs 知识库布局，编码规范独立成文。
- v2.0 —— 确立"一个文件夹 = 一种项目类型，内含该类型 AGENTS.md"的项目形态。

## v1.0

- 通用 AGENTS.md 写作规范初版（其内容已并入 writing-standards / fixed-docs / authoring-types）。
