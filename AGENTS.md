# AiCodingSpec

> 状态：v3.0（精简地图版）｜变更历史见 [`docs/changelog.md`](docs/changelog.md)
>
> 一句话定位：本项目是**「各类项目 AGENTS.md 规范」的汇编项目**——让每种常见项目类型都有一份可直接复制、按项目微调的 `AGENTS.md`（写给 AI 编码代理看、人也应能读懂的"项目操作手册"），使真实项目"开箱即有合格 AGENTS.md"。
>
> **本文件刻意保持为地图（短 AGENTS + 渐进披露，原则见 [`docs/harness-principles.md`](docs/harness-principles.md)）**：正文按主题放在 `docs/`，按需展开；新增内容请放进对应文档，**不要把本文档重新养肥**。

## 1. 项目概览

- **使命**：为常见项目类型沉淀 `AGENTS.md` 规范/模板，并持续维护其质量与时效。
- **产出形态**：一个文件夹 = 一种项目类型；`<类型>/AGENTS.md` 即该类型的规范与模板，是**最终交付物**。例：`springboot/AGENTS.md`。
- **交付物三件套**（类型模板产出，真实项目落地时）：`AGENTS.md`（操作手册）+ `docs/CODING_STANDARDS.md`（编码细则）+ `docs/ARCHITECTURE.md`（架构权威），骨架见 `docs/fixed-docs.md`。
- **读者**：在本仓库里干活的人与 AI 编码代理（新增/修订类型规范）；下游为各真实项目的维护者与其编码代理。

## 2. 仓库地图（先读这个）

| 文件 | 是什么 | 何时读 |
| --- | --- | --- |
| `AGENTS.md`（本文件） | 起步地图：定位/布局/类型清单/硬性红线/入口 | 任何工作开始前 |
| [`docs/authoring-types.md`](docs/authoring-types.md) | 如何新增/修订类型：工作流、统一结构骨架、类型项目文件夹结构、交付自查清单、**L1–L5 验证协议** | 要写或改一个类型时 |
| [`docs/writing-standards.md`](docs/writing-standards.md) | 一切文档/AGENTS 的写作原则、命令书写规范、反例表 | 撰写任何正文前 |
| [`docs/fixed-docs.md`](docs/fixed-docs.md) | 四个固定组成部分：docs 布局 / Git 提交 / ARCHITECTURE / product-specs | 需要细则全文（类型模板只放摘要+链接） |
| [`docs/harness-principles.md`](docs/harness-principles.md) | Harness Engineering 工作方式准则（OpenAI/DSH 摘编） | 想了解本仓库为何这样组织 |
| [`docs/changelog.md`](docs/changelog.md) | 规范版本历史（v1.0–v3.0） | 查某条规则何时引入 |
| `springboot/` 及各类型目录 | 类型规范交付物（交付结构见 §3.1） | 复用/复制到真实项目 |

## 3. 目录约定与结构

```text
AiCodingSpec/
  AGENTS.md                     # 地图（本文件，保持精简）
  README.md                     # 给人/下游用户的入口介绍（对外，非代理指令）
  docs/                         # 本仓库的规范正文（见 §2 地图）
  scripts/validate-type.ps1     # 类型静态校验（L1）：红线/结构/链接/占位符检查
  springboot/                   # 一种项目类型 = 一个文件夹（小写 kebab-case）
    AGENTS.md                   # 该类型的规范/模板（交付物）
    README.md                   # 类型说明/适用范围/版本对照/验证状态（可选辅助物）
    docs/                       # 类型交付物范本：CODING_STANDARDS / ARCHITECTURE / product-specs
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
  docs/                       # ★ 交付物范本：真实项目 docs/ 的直接源头
    CODING_STANDARDS.md       # ★ 编码细则范本（fixed-docs §1「知识库布局」）
    ARCHITECTURE.md           # ★ 架构规范范本（fixed-docs §3 骨架）
    product-specs/            #   需求规格范本：index.md + TEMPLATE.md（fixed-docs §4）
  README.md                   #   类型说明：适用范围 / 版本对照 / 范本入口 / 验证状态
  examples/                       #   演练与辅助（非交付范本）；springboot 当前只放冒烟样例 sample-project/
```

- **AGENTS.md = 交付物本体**：复制到真实项目根 → 填 `{{占位符}}` → 裁剪可选章节 → 逐条验证命令。
- **docs/\* = 交付物范本**：复制到真实项目 `docs/` 后按团队裁剪；`CODING_STANDARDS.md`、`ARCHITECTURE.md` 必建，`product-specs/` 按触发器使用。
- **README.md**：给人看的类型说明，并登记验证状态（L1–L5 记录，见 `docs/authoring-types.md` §5）。
- **examples/**：只放演练/样例等**非交付**内容（当前 springboot 仅 `sample-project/`）；交付范本不得藏在这里。
- **发布条件**：`scripts/validate-type.ps1` 输出 FAIL=0 + §4 自查通过 + 在 §4 类型清单登记。

## 4. 类型清单（登记表）

| 类型文件夹 | 目标项目类型 | 状态 | 备注 |
| --- | --- | --- | --- |
| [`springboot/`](springboot/README.md) | Spring Boot + MyBatis-Plus + MySQL 服务端（集成 Redis/RabbitMQ；Java 17+，Boot 3.5.x 兼容成熟线） | v1.1 已发布 | 固定栈：MyBatis-Plus/MySQL/Redis/RabbitMQ + Swagger UI（springdoc）；Maven 主线；占位符模板式 |

> 每新增一种，在此登记一行。待办候选（按需求排期）：python-fastapi、node-express、nextjs、go-service、rust-cli、python-lib、monorepo 等。

## 5. 硬性红线（改任何文件都适用）

1. **命令必须真实可验证**：只写确认跑通的命令；不写"大概能跑"。CI/质量门失败不得靠跳过参数蒙混。
2. **单一事实来源**：版本号、端口、路径指向 pom/README/配置文件；同一规则不在两处重复维护。
3. **环境准备类内容不进 AGENTS.md**（装 JDK/Docker、手工起中间件、下载耗时）——归真实项目 README/部署文档。
4. **不泄密**：真实密钥/token/内网地址/账号永不出现；`@Schema` 示例值同样受限。
5. **确定性而非假设**：docs 必建项是固定要求，禁止"若有 docs/…"式条件句；触发器式表达（条件满足**必须**建立）。
6. **生成物与归档只读**：`generated/`、已归档 ADR/记录不得手改。
7. **超长即拆**：单文件超过约 250–400 行或主题独立，就拆到 `docs/` 或「examples/」——**本仓库自身即以此拆分**。
8. **结构描述用纯缩进**，不用 `├──/└──/│` 等制表符画线字符。
9. **改动要登记**：新增类型登记 §4；实质规范改动登记 [`docs/changelog.md`](docs/changelog.md)。

## 6. 我该从哪开始

- **新增一个类型** → 按 [`docs/authoring-types.md`](docs/authoring-types.md) 走：立项 → 起草（§2 骨架 + §3 项目文件夹结构）→ 质检（§4 自查清单）+ 验证（§5 L1–L5，先跑 `scripts/validate-type.ps1`）→ 登记 §4 → changelog。
- **修订现有类型**（如 springboot）→ 直接改 `<类型>/AGENTS.md` 与 `<类型>/docs/*`（范本），同步登记表/changelog；命令核实到官方现状。
- **撰写/评审任何文档** → 先过 [`docs/writing-standards.md`](docs/writing-standards.md) 的反例表与原则。
- **需要固定规范全文**（Git 提交/架构/需求规格/docs 布局）→ [`docs/fixed-docs.md`](docs/fixed-docs.md)。
- **理解本仓库组织哲学** → [`docs/harness-principles.md`](docs/harness-principles.md)。

## 7. 维护口径

- 评审：内容宁短勿水；任何与"当前主流脚手架/官方现状"不符的命令都是 bug，与坏代码同等对待。
- 版本：实质改动在 [`docs/changelog.md`](docs/changelog.md) 追加一行；根 AGENTS.md 只维护本"地图"内容。
