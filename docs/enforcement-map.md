# 不变量与强制手段映射（Enforcement Map）

> 用途：Harness 原则第 4 条——**文档不治漂移，强制才治**。本文件把各类型 `docs/CODING_STANDARDS.md` 里每一条「必须」映射到**具体强制手段**（工具配置 / 架构测试 / 注册表测试 / 评审），并显式列出**目前仅靠人读**的规则与补齐路线。
> 分工不重复：规则正文与措辞级别只在各类型的 `CODING_STANDARDS.md`；本文件只做"**规则 → 强制者 → 落点**"的索引，不复制规则内容。
> 何时读：新增/修订一条「必须」规则时（同时登记强制者）；评审时核对"这条规则真的拦得住吗"。

## 1. 口径

1. 每条「必须」规则**必须**在本表有一行：要么有强制者（工具/测试），要么明确写「仅评审」并给出理由。
2. 「应该」级规则不强制，可不登记；一旦某条「应该」被当作既成事实引用（如"已用 ArchUnit 固化"），就必须登记。
3. 强制者分三类，优先级从高到低：
   - **工具**：Spotless / Checkstyle / SpotBugs / Enforcer / JaCoCo（配置在 `pom.xml` + `config/**`，随 `clean verify` 执行）；
   - **测试**：ArchUnit 架构测试、注册表/契约测试（随 `test` 执行）；
   - **评审**：只能人读，必须写清"为什么暂时无法机器化"。
4. 评判标准：**质量门失败即构建失败**。若规则只在日志里打印 WARN 而不拦构建，不算已强制（样例踩过：Checkstyle 默认 `violationSeverity=error` 会让 `severity=warning` 的规则形同虚设）。质量门的定义、单一入口与三条硬要求见 [`fixed-docs.md`](fixed-docs.md) §6——**本表登记"谁拦"，§6 规定"在哪拦、怎么算拦住了"**。

## 2. 已工具化（按类型）

### 2.1 springboot 类型

落点均以 `springboot/examples/sample-project/` 为准（该样例即"规则可执行"的证明）。

| 规则（CODING_STANDARDS） | 强制者 | 落点 |
| --- | --- | --- |
| §1 命名（类/方法/成员/参数/局部变量/包/类型参数） | Checkstyle | `config/checkstyle/checkstyle.xml`（Google 基底 + 团队规则，`violationSeverity=warning`） |
| §1 版式（google-java-format、import 排序、去无用 import） | Spotless | `pom.xml` spotless 插件（`googleJavaFormat`） |
| §3 依赖方向（部分：controller 不依赖 mapper / service.impl） | ArchUnit | `ArchitectureRulesTest` |
| §5 QueryWrapper 隔离（service 不得依赖 MP conditions 包） | ArchUnit | `ArchitectureRulesTest` |
| §5 禁 `SELECT *`（文本级） | Checkstyle | `config/checkstyle/checkstyle.xml`（`RegexpSingleline`） |
| §5 分页插件依赖（jsqlparser 模块显式引入） | Maven 解析 + Enforcer（依赖收敛） | `pom.xml` 依赖声明 |
| §6 错误码分段/命名/唯一/只增不删 | 注册表测试 | `ErrorCodeRegistryTest`（枚举分段、命名正则、同码查重） |
| §9 禁 `System.out/err`、禁 `printStackTrace`、禁 JUL/commons-logging | Checkstyle | `config/checkstyle/checkstyle.xml`（`RegexpSinglelineJava`/`IllegalImport`，TreeWalker 内） |
| §13 Java 版本、Maven 版本、依赖收敛、禁停维护依赖 | Enforcer | `pom.xml` enforcer 规则集 |
| §13 缺陷模式（空指针/资源/并发） | SpotBugs（`effort=Max`） | `pom.xml` + `config/spotbugs/excludeFilter.xml` |
| §13 覆盖率门禁 | JaCoCo `check` | `pom.xml`（LINE ≥ 80%，排除 entity/mapper/config/启动类） |
| §13 生成代码不纳入格式/静态检查 | Spotless + SpotBugs 排除配置 | `pom.xml` / `excludeFilter.xml` |

### 2.2 python-fastapi 类型

落点均以 `python-fastapi/examples/sample-project/` 为准（该样例即"规则可执行"的证明）。

| 规则（CODING_STANDARDS） | 强制者 | 落点 |
| --- | --- | --- |
| §1 命名 / §2 类型注解完整性 | ruff（`E,F,W,I,N,UP,B,C4,SIM,ASYNC,RUF`，显式关闭 `RUF001/002/003`）+ mypy `strict` | `pyproject.toml` |
| §3 分层职责 / §4 依赖方向（单向 + 禁反向 + **禁越层** + **禁同层横向**） | **import-linter** 四条契约（`layers` + 两条 `forbidden`〔其中禁越层用 `allow_indirect_imports`〕+ `independence`） | `[tool.importlinter]`；纳入质量门，四条各自以"故意违规即红"实测（越层/同层横向均 BROKEN + exit 1） |
| §5 出参结构（禁直接返回 ORM 实体、统一 `PageResult`） | `httpx.ASGITransport` 接口测试断言响应体 | `tests/test_orders_api.py` |
| §6 异步纪律（async 用法/阻塞调用） | ruff `ASYNC` 规则集（**部分**覆盖） | `pyproject.toml` |
| §8 **写库后删缓存** | service 单测断言"写后删 key 且缓存为空" | `tests/test_order_service.py::test_create_order_deletes_cache_after_write` |
| §10 错误码唯一 / 6 位分段 / 命名 / **只增不删** | 注册表测试（长度、纯数字、首位 ∈ 1..5、命名正则、`_FROZEN_CODES` 快照） | `tests/test_error_contract.py`（分段/命名/快照各自"违规即红"实测） |
| §10 不泄漏堆栈/SQL/表名 + §11 `traceId` **body 与响应头**贯穿（含错误路径） | 接口测试断言 500 响应体无 `Traceback`/异常类名/表名，且 **404/422/500 三路径**的 `X-Trace-Id` 与 body `traceId` 一致 | `test_error_contract.py`、`test_health.py` |
| §11 禁 `print`/`T20*`（日志统一走 logging） | ruff `T20`（`select` 内） | `pyproject.toml`；实测 `print` → 1 error + exit 1 |
| §13 测试纪律（标记登记、异步模式） | pytest `--strict-markers` + 登记 `markers`、`asyncio_mode = "auto"` | `pyproject.toml` |
| §15 提交信息规范 | **待补**：无 commitlint 类配置 | 见 §3 清单 |

### 2.3 架构信条（两个类型共同，正文见 `fixed-docs.md` §5）

> 6 条架构信条是固定组成部分，各类型已内嵌到 `docs/design-docs/core-beliefs.md` 并在 `ARCHITECTURE.md` §3 给出分层映射。下表登记**强制现状**与补齐路线。

| 信条 | 强制者（现状） | 缺口 / 补齐路线 |
| --- | --- | --- |
| ① 分层顺序 `type → config → repo/dao → service → runtime → ui` 单向 | **部分**：springboot = ArchUnit（`ui→service→repo` + 禁反向/横向）；python-fastapi = import-linter 四条契约（`ui→service→repo→type` + 禁反向/越层/同层横向） | `config`/`type`/`runtime` 三个方向的越界**未覆盖** → 待补：python 追加 `forbidden`（如 `core.config` 不得依赖 repositories/services；`services` 不得依赖 `api`）；springboot 追加 ArchUnit 包断言 |
| ② 规范先行（spec-first） | **仅评审**（流程约束） | 可部分机器化：CI 条件检查"代码变更是否同带 docs/需求/ADR 变更" |
| ③ 开闭原则（OCP） | **仅评审** | 可部分机器化：禁止"按类型增长的 `if/elif` 分支链"（自定义 lint/复杂度门限） |
| ④ 禁止过度设计 | **仅评审** | 难机器化；以评审清单执行（抽象是否有 ≥2 处真实使用） |
| ⑤ RDBMS 3NF | **仅评审**（schema 快照可辅助） | 待补：迁移脚本评审清单 + "反范式必须记 ADR 并在本文件登记"的检查 |
| ⑥ 单一职责（SRP） | **仅评审**（文件/方法长度门限可近似：Checkstyle 已启用） | 类职责判定仍需评审；可用"单类依赖数/方法数"阈值做粗筛 |

### 2.4 规范层（类型模板编写）—— 已工具化

> 覆盖"编写/修订一份类型交付物"这类不变量。此前它们写在 `docs/authoring-types.md` 与 `docs/fixed-docs.md` 里，却**一条都没登记到本表**（等于无人知悉强制者）。判据实现于 `scripts/validate-repo.ps1`（L1′）与 `scripts/validate-type.ps1`（L1）。

| 不变量 | 强制者 | 判据 / 范围 |
| --- | --- | --- |
| 根文档（`AGENTS.md`／`README.md`／`docs/*.md`）相对链接真实存在 | L1′ | FAIL；`changelog` 亦受检（仅 `§N` 类检查豁免） |
| 占位符 `{{ }}` 配平 | L1／L1′ | FAIL |
| 禁用树形/画线字符（`├`／`└`／`│` 等）：出现即 FAIL，正文一律用纯缩进 | L1／L1′ | 豁免「禁止/反例」语境所在行 |
| 类型目录 ↔ 根 `AGENTS.md` §4 登记表双向一致 | L1′ | FAIL |
| 类型范本 ↔ `examples/` 副本逐字节同源（`AGENTS.md` + docs 必建文件） | L1′ | FAIL。**这是设计**：样例须与范本同源，故样例保留占位符 |
| 根 `AGENTS.md` 版本号 ↔ `changelog` 唯一「（当前）」版本 | L1′ | FAIL |
| `AGENTS §N` 悬挂引用、`CODING_STANDARDS §N` 引用存在性 | L1′／L1 | FAIL |
| docs 必建骨架（两核心文件 + 五项目录最小入口文件） | L1（类型）／L1′（样例） | FAIL |
| 骨架第 1 章「项目概览」 | **L1（v3.3 新增）** | FAIL：`H1` 后至首个 `##` 前的概览块，或 `项目概览` 同名章；非空行 ≥3 且含"技术栈""形态"。**已按"故意违规即红"实测（2026-09-11）**：① 删至 2 行 → FAIL「非空行=2 < 3」；② 仅删「形态」→ FAIL「缺关键词『形态』」；两次均 exit=1，还原后复跑恢复 FAIL=0 |
| 骨架第 3、4、7、9 章标题存在 | L1 | FAIL，按标题前缀匹配 |
| 占位符契约 `<类型>/placeholders.json` ↔ `AGENTS.md` 双向一致 | **L1 检查 9（v3.5 新增）** | FAIL：未登记占位符、孤儿条目、`instance` 缺 regex 或有 default、`choice` 缺 options、`path` 未以路径形态出现。**已按"故意违规即红"实测（2026-09-11）**：临时插入 `{{未登记占位符}}` 与嵌套 `{{a{{b}}}}` → 3 条 FAIL、exit=1；还原后复跑 FAIL=0 |
| 占位符**不得嵌套**（双花括号内再出现双花括号） | **L1 检查 2b（v3.5 新增）** | FAIL：嵌套会让契约无法解析（历史实测 4 处：两个 `product-specs/TEMPLATE.md` 的迁移脚本行与 MQ 事件行、python `generated/index.md` 两行，已修） |
| 生成项目实例后，生成物结构仍合法 | `scripts/new-project.ps1` | 生成器内部调 `validate-type.ps1 -Mode Instance`，FAIL 即生成失败（实例无占位符契约，故跳过检查 9） |
| 密钥形态启发（含 `${VAR:默认非空}`） | L1／L1′ | **WARN**：需人工确认，**不算强制** |

**规范层仍靠人读的条目**（未实现判据前不得声称"已强制"）：

| 条目 | 现状 | 机器化路径 | 已知违反 |
| --- | --- | --- | --- |
| 骨架第 10 章「参考链接（默认不设）」（§4 自查 #15 同口径） | 仅评审 | L1 增判据：类型 `AGENTS.md` 出现 `## 参考链接` 即 FAIL | **`python-fastapi/AGENTS.md` 仍保留该节**（`springboot` 已删，两类型不对称） |
| §4 自查清单里的"语义类"条目（命令真实性、定位一致、常见坑覆盖、逐文件目录树是否完整、无模糊措辞、不含环境准备、文件规模） | 仅评审 | 词表/关键词扫描可覆盖部分（模糊措辞、环境准备词）；其余靠 L3/L4 | — |
| 质量门入口命令的"真身"（是否真能一条命令跑完） | 仅评审 | 见 §3 同名条目 | 见 §3 |

## 3. 仅靠人读（待补齐）
> 来源：对 springboot 类型的一次独立 L4 复核（`docs/authoring-types.md` §5）。**条目数 = 下表行数（当前 20 行）**：行内以「/」分隔的为同类规则聚合，故"规则条数"多于行数。**本计数不得在别处另写**——历史表述"共 37 项"无法从表格核对，已于 v3.3 删除。按"最值得优先机器化"排序。表中 `§N` 默认以 **springboot** 的 `CODING_STANDARDS.md` 为准，`（py §N）` 给出 python-fastapi 的对应节号（不同才标）。

| 优先 | 规则（§N 关键词） | 可行的机器化路径 |
| --- | --- | --- |
| P0 | §3 DAO 方法前缀词表（禁 `query/find/get/add/modify/remove` 开头） | Checkstyle 正则（Mapper 包内方法名）或 ArchUnit 方法名断言 |
| P0 | §3 依赖方向全覆盖：禁反向 / 禁越层 / **禁同层横向** / 无环 | ArchUnit：`slices().should().beFreeOfCycles()` + 反向/横向断言；（py）**已落地**：import-linter 三条 `forbidden`/`independence` 契约覆盖反向、越层、同层横向——剩"无环"仍待补 |
| P1 | §11 Redis key 集中（禁散落字符串）（py §8） | 常量类引用检查 + 评审（TTL 难静态判定）；（py）样例测试自身用了字面量，属"仅评审" |
| P0 | §5 禁业务代码直写字典表 | ArchUnit：service/controller 不得依赖 `DictMapper` 写路径 |
| P0 | §6 错误码只增不删 / 先查重 | 注册表测试（快照比对：删除或改语义即红） |
| P1 | §5 `@Transactional` 位置（Service 公有方法；禁 Controller/Mapper） | ArchUnit 注解断言 |
| P1 | §5 列表/分页必须有上限 | 契约测试或 Mapper 方法名/返回值断言（`IPage`/分页参数必填）；（py）端点测试断言 `page/size` 必填且有上限 |
| P1 | §1 禁同义混用 / 禁本地重复实现（口味：不重复造轮子）（py §1 同） | **仅评审**——目前无任何 lint 覆盖。机器化路径：Checkstyle `RegexpSingleline` / ruff 自定义规则禁"同义方法别名"；或用"共享实现引用"断言（业务包不得重新定义 Redis key 前缀/错误码/常量） |
| P1 | §11 配置按 Profile 拆分、本地默认 dev | 文件存在性 + 断言（仓库级校验脚本可选）；（py）断言 `Settings` 必填项在生产无默认值 |
| P1 | §11 密钥/口令以环境变量注入、禁入库（py §12） | `scripts/validate-type.ps1` 密钥启发式（**`.md` + `*.yml/*.yaml/*.properties/.env/.example/.ini/.toml`**，含 `${VAR:默认非空}` WARN）——已覆盖配置文件的"默认口令"形态；真实密钥库/Secrets 扫描仍需外部工具 |
| P1 | §14 提交信息规范（py §15；**两个类型均无 commitlint**） | commitlint + husky / pre-commit（fixed-docs §2 规则 5 要求工具化） |
| P1 | 质量门**入口命令"AGENTS ↔ CI"两处逐字一致**（`fixed-docs.md` §6 规则 1）：模板中不得出现两份质量门命令 | **仅评审**（现无机器校验）——机器化路径：从类型 `AGENTS.md` 的质量门行提取命令、与 `.github/workflows/*` 中该类型 job 的命令比对（`validate-repo.ps1` 目前只校验链接与范本/样例同源）  **已知违反（2026-09-11 复查）**：`python-fastapi` 的质量门只有未填占位符 `{{质量门入口命令}}`、样例无 `scripts/gate.py`、CI 按 5 条命令分步执行（`.github/workflows/validate.yml` 注释自认"本样例尚未提供入口脚本"）；`springboot` 合规（`./mvnw clean verify` ↔ CI `./mvnw -B clean verify`）。 |
| P1 | （py）读缓存回源链路（`CacheProtocol.get/set` 与 `idem:`/`lock:` 常量暂无调用点） | 在 service 补读缓存 + 回填，并用单测断言"未命中回源→回填"，使该条成为可执行证据 |
| P2 | §5 SQL 参数化、`${}` 白名单 | 静态扫描自定义规则（XML/注解 SQL 文本） |
| P2 | §7 Redis key 规范（域:对象:标识、TTL、常量类集中） | 常量类引用检查 + 代码评审（TTL 难静态判定） |
| P2 | §7 禁默认 JDK 序列化 | 配置存在性测试（断言 `RedisTemplate` 序列化器类型） |
| P2 | §8 消费手动 ack / 有限重试 / DLQ 绑定完整 / 幂等键 | 集成测试（需 Docker/Testcontainers） |
| P2 | §6 响应带 `traceId`、不泄漏堆栈 | `@WebMvcTest` 断言响应字段（错误分支） |
| P2 | §12 每个业务方法有测试且覆盖每个异常分支 | 覆盖率 + 变异测试（后者成本高，暂以评审为主） |
| — | §5 慢 SQL `EXPLAIN`/索引最左前缀、§10 并发模型、§12 curl 冒烟记录 等 | 依赖真实环境与人工判断，短期保持「仅评审」 |

## 4. 维护与校验

- **仓库自身的强制手段**：`scripts/validate-type.ps1`（类型级 L1）与 `scripts/validate-repo.ps1`（仓库级 L1'）是**规则可发现性**的机器兜底——画线字符/占位符配平/链接存在/`§N` 引用/登记表 ↔ 目录/docs 必建骨架/范本 ↔ 样例同源/版本 ↔ changelog。两者 FAIL=0 是发布条件（见根 `AGENTS.md` §3.1）。
- 新增「必须」规则时：先在本表登记强制者；**没有强制者且无法机器化的**，在 `CODING_STANDARDS.md` 对应条目显式标注「仅评审」。
- 类型发布前（`docs/authoring-types.md` §5 L2）核对本表：工具化条目必须能在样例里复现为"故意违反即红"。
- 本表随类型演进；某类型新增工具（如接入变异测试、commitlint）即回填本表。
- **条目计数口径**：本表条目数以表格行数为准；不得在别处另写计数（历史"共 37 项"因此删除）。
- **规范层登记处**：编写/修订类型模板这类不变量登记在 §2.4；其中「仅评审」项在实现判据前不得声称"已强制"。
- **本仓库自身的豁免**：本仓库作为"规范汇编"对六个固定组成部分的豁免范围与理由见 [`docs/decisions.md`](decisions.md) ADR-005；下游复制模板**以类型交付物为准**，不照抄本仓库根布局。

> 变更历史见 `changelog.md`。
