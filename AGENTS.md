# AiCodingSpec

> 状态：v3.3（精简地图版）｜变更历史见 [`docs/changelog.md`](docs/changelog.md)
>
> 一句话定位：本项目是**「各类项目 AGENTS.md 规范」的汇编项目**——让每种常见项目类型都有一份可直接复制、按项目微调的 `AGENTS.md`（写给 AI 编码代理看、人也应能读懂的"项目操作手册"），使真实项目"开箱即有合格 AGENTS.md"。
>
> **本文件刻意保持为地图（短 AGENTS + 渐进披露，原则见 [`docs/harness-principles.md`](docs/harness-principles.md)）**：正文按主题放在 `docs/`，按需展开；新增内容请放进对应文档，**不要把本文档重新养肥**。

## 1. 项目概览

- **使命**：为常见项目类型沉淀 `AGENTS.md` 规范/模板，并持续维护其质量与时效。
- **产出形态**：一个文件夹 = 一种项目类型；`<类型>/AGENTS.md` 即该类型的规范与模板，是**最终交付物**。例：`springboot/AGENTS.md`。
- **交付物三件套**（类型模板产出，真实项目落地时）：`AGENTS.md`（操作手册）+ `docs/CODING_STANDARDS.md`（编码细则）+ `docs/ARCHITECTURE.md`（架构权威）；另加 `docs/` 五项目录骨架（`design-docs/`、`product-specs/`、`exec-plans/`、`generated/`、`references/`，**目录必建、内容按触发器**），骨架见 `docs/fixed-docs.md`。
- **读者**：在本仓库里干活的人与 AI 编码代理（新增/修订类型规范）；下游为各真实项目的维护者与其编码代理。

## 2. 仓库地图（先读这个）

| 文件 | 是什么 | 何时读 |
| --- | --- | --- |
| `AGENTS.md`（本文件） | 起步地图：定位/布局/类型清单/硬性红线/入口 | 任何工作开始前 |
| [`docs/authoring-types.md`](docs/authoring-types.md) | 如何新增/修订类型：工作流、统一结构骨架、类型项目文件夹结构、交付自查清单、**L1–L5 验证协议** | 要写或改一个类型时 |
| [`docs/writing-standards.md`](docs/writing-standards.md) | 一切文档/AGENTS 的写作原则、命令书写规范、反例表 | 撰写任何正文前 |
| [`docs/fixed-docs.md`](docs/fixed-docs.md) | **六个固定组成部分**：docs 布局 / Git 提交 / ARCHITECTURE / product-specs / 架构信条（6 条） / **质量门控（单一入口 + 失败即停 + 整条真跑）**——后两项每个类型必须内嵌 | 需要细则全文（类型模板只放摘要+链接） |
| [`docs/harness-principles.md`](docs/harness-principles.md) | Harness Engineering 工作方式准则（OpenAI/DSH 摘编） | 想了解本仓库为何这样组织 |
| [`docs/enforcement-map.md`](docs/enforcement-map.md) | **不变量 → 强制手段映射**：哪些「必须」已被工具/测试强制、哪些仅靠人读 | 新增/修订「必须」规则、评审"这条规则拦得住吗" |
| [`docs/decisions.md`](docs/decisions.md) | 本仓库自身的架构决策记录（ADR） | 想知道某条仓库级约定为何这样定 |
| [`docs/changelog.md`](docs/changelog.md) | 规范版本历史（v1.0–v3.3） | 查某条规则何时引入 |
| `springboot/` 及各类型目录 | 类型规范交付物（交付结构见 §3.1） | 复用/复制到真实项目 |

## 3. 目录约定与结构

```text
AiCodingSpec/
  AGENTS.md                     # 地图（本文件，保持精简）
  README.md                     # 给人/下游用户的入口介绍（对外，非代理指令）
  docs/                         # 本仓库的规范正文（见 §2 地图）
  scripts/validate-type.ps1     # 类型静态校验（L1）：红线/结构/链接/占位符/docs 必建骨架检查
  scripts/validate-repo.ps1     # 仓库级静态校验（L1'）：根文档链接与 §N 引用/登记表↔目录/范本↔样例同源/版本↔changelog
  .github/workflows/validate.yml # CI：跑上述两层校验 + 各类型样例的质量门（与本地同一条命令）
  springboot/                   # 一种项目类型 = 一个文件夹（小写 kebab-case）
    AGENTS.md                   # 该类型的规范/模板（交付物）
    README.md                   # 类型说明/适用范围/版本对照/验证状态（可选辅助物）
    docs/                       # 类型交付物范本：CODING_STANDARDS / ARCHITECTURE + 五项目录（必建）
    examples/sample-project/        # L3 冒烟样例项目（start.spring.io 生成）
  ...                           # 每新增一种类型，登记到 §4
```

规则：

1. **每种项目类型一个文件夹**：文件夹名 = 类型惯用名的小写 kebab-case，可读、唯一。
2. **类型文件夹根必须含 `AGENTS.md`**：即"当某个真实项目属于该类型时，其根目录应放什么"；项目特有信息用 `{{占位符}}`，复制后填写并按需裁剪。
3. 类型文件夹内**可选**辅助物：`README.md`、`examples/`、`template/`。默认不建，确有必要才加。
4. 仓库根不得放属于某一具体类型的命令与约定；各类型规范互不干扰、各自自洽。

### 3.1 类型交付物结构（每个 `<类型>/` 的固定形态）

> 一个"项目类型"的完整交付 = 下列结构 + 内容（★=必含/必建，其余按需）。内容规范见 `docs/authoring-types.md` §2–§3 与 `docs/fixed-docs.md`；形态由 `scripts/validate-type.ps1`（L1）校验。

```text
{{类型名}}/                     # 例：springboot/（小写 kebab-case）
  AGENTS.md                   # ★ 交付物本体：该类型的规范/模板（操作手册）
  docs/                       # ★ 交付物范本：真实项目 docs/ 的直接源头（目录一律必建）
    CODING_STANDARDS.md       # ★ 编码细则范本（fixed-docs §1「知识库布局」）
    ARCHITECTURE.md           # ★ 架构规范范本（fixed-docs §3 骨架）
    design-docs/              # ★ 设计决策/ADR 范本：index.md + core-beliefs.md（内容按触发器）
    product-specs/            # ★ 需求规格范本：index.md + TEMPLATE.md（fixed-docs §4）
    exec-plans/               # ★ 执行计划与技术债范本：index.md + tech-debt-tracker.md + active/ + completed/
    generated/                # ★ 生成物登记范本：index.md（本目录唯一手写文件；其余只读）
    references/               # ★ 外部资料登记范本：index.md（LLM 浓缩版命名 *-llms.txt）
  README.md                   #   类型说明：适用范围 / 版本对照 / 范本入口 / 验证状态
  examples/                       #   演练与辅助（非交付范本）；springboot 当前只放冒烟样例 sample-project/
```

- **AGENTS.md = 交付物本体**：复制到真实项目根 → 填 `{{占位符}}` → 裁剪可选章节 → 逐条验证命令。
- **docs/\* = 交付物范本**：复制到真实项目 `docs/` 后按团队裁剪；`CODING_STANDARDS.md`、`ARCHITECTURE.md` 与五项目录（`design-docs/`、`product-specs/`、`exec-plans/`、`generated/`、`references/`）**一律必建**——每目录至少含一个最小入口文件（`index.md` 等），触发条件只决定内容何时补齐。
- **README.md**：给人看的类型说明，并登记验证状态（L1–L5 记录，见 `docs/authoring-types.md` §5）。
- **examples/**：只放演练/样例等**非交付**内容（当前 springboot 仅 `sample-project/`）；交付范本不得藏在这里。
- **发布条件**：`scripts/validate-type.ps1` 输出 FAIL=0 + §4 自查通过 + 在 §4 类型清单登记 + 质量门**整条真跑**与"故意违规即红"的证据已记入类型 `README.md`「验证状态」（口径见 `docs/fixed-docs.md` §6）。

## 4. 类型清单（登记表）

| 类型文件夹 | 目标项目类型 | 状态 | 备注 |
| --- | --- | --- | --- |
| [`springboot/`](springboot/README.md) | Spring Boot + MyBatis-Plus + MySQL 服务端（集成 Redis/RabbitMQ；Java 17+，Boot 3.5.x 兼容成熟线） | v1.1（L1/L2 通过；L3 部分；L4 复核中） | 固定栈：MyBatis-Plus/MySQL/Redis/RabbitMQ + Swagger UI（springdoc）；Maven 主线；占位符模板式；验证状态以 `springboot/README.md` 为准 |
| [`python-fastapi/`](python-fastapi/README.md) | FastAPI + Pydantic v2 + SQLAlchemy 2.0/PostgreSQL 服务端（集成 Redis/RabbitMQ；uv 管理依赖） | v1.0（L1–L5 通过；L4 复核的 3 条 BLOCKER 已修复并实证，待复审确认） | 固定栈：FastAPI/Pydantic v2/SQLAlchemy 2.0/Alembic/PostgreSQL + Redis/RabbitMQ；工具链 uv + ruff + mypy + import-linter + pytest；依赖方向四条契约（单向/禁反向/禁越层/禁同层横向）均"故意违规即红"；验证状态以 `python-fastapi/README.md` 为准 |

> 每新增一种，在此登记一行。待办候选（按需求排期）：node-express、nextjs、go-service、rust-cli、python-lib、monorepo 等。

## 5. 硬性红线（改任何文件都适用）

1. **命令必须真实可验证**：只写确认跑通的命令；不写"大概能跑"。质量门必须**整条真跑**（不是只跑 `compile`）；**只告警不拦构建＝假绿**，不得声称"已强制"；CI/质量门失败不得靠跳过参数蒙混。口径见 [`docs/fixed-docs.md`](docs/fixed-docs.md) §6。
2. **单一事实来源**：版本号、端口、路径指向 pom/README/配置文件；同一规则不在两处重复维护。
3. **环境准备类内容不进 AGENTS.md**（装 JDK/Docker、手工起中间件、下载耗时）——归真实项目 README/部署文档。
4. **不泄密**：真实密钥/token/内网地址/账号永不出现；`@Schema` 示例值同样受限。
5. **确定性而非假设**：docs 必建项是固定要求（两核心文件 + 五项目录，各含最小入口文件），禁止"若有 docs/…"式条件句，也禁止"未触发所以不建目录"；触发器只作用于**内容**（条件满足**必须**写入并维护）。
6. **生成物与归档只读**：`generated/`、已归档 ADR/记录不得手改。
7. **超长即拆**：单文件超过约 250–400 行或主题独立，就拆到 `docs/` 或「examples/」——**本仓库自身即以此拆分**。
8. **结构描述用纯缩进**，不用 `├──/└──/│` 等制表符画线字符。
9. **改动要登记**：新增类型登记 §4；实质规范改动登记 [`docs/changelog.md`](docs/changelog.md)。

## 6. 我该从哪开始

- **新增一个类型** → 按 [`docs/authoring-types.md`](docs/authoring-types.md) 走：立项 → 起草（§2 骨架 + §3 项目文件夹结构）→ 质检（§4 自查清单）+ 验证（§5 L1–L5，先跑 `scripts/validate-type.ps1`）→ 登记 §4 → changelog。CI（`.github/workflows/validate.yml`）会对 push/PR 跑同样两层校验与各类型样例质量门。
- **新增/修订一条「必须」规则** → 在 [`docs/enforcement-map.md`](docs/enforcement-map.md) 登记它的强制者（工具/测试），无法机器化的显式标注「仅评审」；假绿（只告警不拦构建）不算强制。
- **修订现有类型**（如 springboot）→ 直接改 `<类型>/AGENTS.md` 与 `<类型>/docs/*`（范本），同步登记表/changelog；命令核实到官方现状。
- **撰写/评审任何文档** → 先过 [`docs/writing-standards.md`](docs/writing-standards.md) 的反例表与原则。
- **需要固定规范全文**（docs 布局 / Git 提交 / 架构 / 需求规格 / 架构信条 / 质量门控）→ [`docs/fixed-docs.md`](docs/fixed-docs.md)。
- **理解本仓库组织哲学** → [`docs/harness-principles.md`](docs/harness-principles.md)。

## 7. 维护口径

- 评审：内容宁短勿水；任何与"当前主流脚手架/官方现状"不符的命令都是 bug，与坏代码同等对待。
- 版本：实质改动在 [`docs/changelog.md`](docs/changelog.md) 追加一行；根 AGENTS.md 只维护本"地图"内容。
- 规范层（本仓库自身）的**机器判据清单**与**已知违反**登记在 [`docs/enforcement-map.md`](docs/enforcement-map.md) §2.4 与 §3；本仓库作为"规范汇编"对自身固定组成部分的**豁免范围与理由**见 [`docs/decisions.md`](docs/decisions.md) ADR-005（下游不得照抄本仓库根布局）。
