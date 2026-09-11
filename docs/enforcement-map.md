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

## 3. 仅靠人读（待补齐）
> 来源：对 springboot 类型的一次独立 L4 复核（`docs/authoring-types.md` §5）。**共 37 项**（原 36 项 + 本次新增的「质量门入口命令 AGENTS ↔ CI 一致」），按"最值得优先机器化"排序。表中 `§N` 默认以 **springboot** 的 `CODING_STANDARDS.md` 为准，`（py §N）` 给出 python-fastapi 的对应节号（不同才标）。

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
| P1 | 质量门**入口命令"AGENTS ↔ CI"两处逐字一致**（`fixed-docs.md` §6 规则 1）：模板中不得出现两份质量门命令 | **仅评审**（现无机器校验）——机器化路径：从类型 `AGENTS.md` 的质量门行提取命令、与 `.github/workflows/*` 中该类型 job 的命令比对（`validate-repo.ps1` 目前只校验链接与范本/样例同源） |
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

> 变更历史见 `changelog.md`。
