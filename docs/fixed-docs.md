# 固定组成部分（Fixed Doc Standards）

> 类型模板/真实项目 `docs/` 中**必须**遵循的四个固定规范。类型 AGENTS.md 只保留"必须摘要 + 指向"，细则全文以本文件为唯一权威（避免双份漂移）。
> 写作细则见 `writing-standards.md`；如何在类型中产出范本见 `authoring-types.md`。

## 1. 知识库布局：docs/（固定组成部分）

> 分工：AGENTS.md 只写"怎么做"（命令/约定/禁区）；"是什么 / 为什么 / 长期计划"放稳定文档，AGENTS.md 用一行链接指向，不内联长文。
> **docs/ 是类型模板中的固定组成部分，不是假设选项**：`docs/` 目录及其核心文件（`CODING_STANDARDS.md`、`ARCHITECTURE.md`）**必建**；子目录遵循"确定性触发器"——条件一旦满足就必须建立并持续维护，**禁止**"若有…则可…"式条件句。核心文件不得为空壳：暂无内容时先写最小可用版（如 CODING_STANDARDS 至少覆盖命名、测试、格式命令三节），宁短勿空。

固定骨架（★=必建；其余为确定性触发器）：

```text
ARCHITECTURE.md                 # ★ 架构总览（默认放 docs/ 下，见下）
docs/
  CODING_STANDARDS.md         # ★ 编码规范（风格/测试规范/工具命令）——类型「编码约定」章节指向它
  ARCHITECTURE.md             # ★ 架构总览：模块划分、关键链路、部署形态
  design-docs/                # 触发器：产生首个设计决策/ADR 时建立；含 core-beliefs.md，用 index.md 索引
  product-specs/              # 触发器：产生需求/产品规格文档时建立（index.md + 按功能命名，见 §4）
  exec-plans/                 # 触发器：存在多轮/多代理任务或技术债时建立（active/、completed/、tech-debt-tracker.md）
  generated/                  # 触发器：首个自动生成物（db-schema.md 等）入库时建立；只读，标注生成命令与时间
  references/                 # 触发器：引入外部参考资料时建立；LLM 浓缩版命名 *-llms.txt
```

根级政策文件按需少量放置（建议 ≤5 个，避免碎片化）：`SECURITY.md`、`RELIABILITY.md`、`PLANS.md`、`QUALITY_SCORE.md`、`FRONTEND.md` 等。同样以明确规则触发，不预建空文件。

要点规则：

1. **分层不重复**：AGENTS.md 的命令与禁区是唯一操作权威；docs 写背景与规格，**不复制命令表**。
2. **generated/ 只读**：生成物可入库但人工不手改，顶部标注生成命令。
3. **exec-plans/ 服务多轮/多代理任务**：长周期改造用它接力进度与技术债，避免每次从头摸索。
4. **被引用的文档必须真实存在**：AGENTS.md「参考链接」只指向实际文件，新增即补、删除即清。
5. **确定性而非假设**：核心文件标必建、子目录给触发器，禁止"若项目建有 docs/…"式条件句。
6. **借鉴而非照搬**：骨架源自 [OpenAI Advanced Pack 仓库模板](https://github.com/walkinglabs/learn-harness-engineering/blob/main/docs/en/resources/openai-advanced/repo-template/AGENTS.md)，吸收"分层知识库 + 稳定文档名 + 生成物隔离 + agent 友好引用"四点；核心两层（`docs/` + 核心文件）不变。

## 2. Git 提交规范（每一份类型模板必须嵌入）

> 提交信息是 agent 与协作者可审计的最小单元，规则必须**确定、可机器校验**。类型模板须在「编码约定」中嵌入下方规范全文（scope 词表可按类型微调），不得用"遵循良好实践 / 规范提交"一类空话代替。

- **格式（必须）**：`<type>(<scope>): <subject>`，如 `feat(order): 增加取消订单接口`。
- **type（必填，小写）**：`feat` 新功能 / `fix` 缺陷修复 / `docs` 文档 / `style` 格式（不影响逻辑）/ `refactor` 重构 / `perf` 性能 / `test` 测试 / `build` 构建 / `ci` CI 变更 / `chore` 杂项 / `revert` 回滚。
- **scope（可选，小写）**：影响模块名（`order`、`auth`、`pom` 等），词表由各类型模板定义；跨模块大改可省略。
- **subject（必须）**：祈使句、简短（建议 ≤50 字符）、句末不加句号；语言（中文/英文）由项目选定后**全库统一并声明**，禁止中英混用。
- **body（需要时）**：写"为什么"（动机、取舍、影响），不复述 diff。
- **footer（需要时）**：破坏性变更写 `BREAKING CHANGE: <说明>`；关联 issue 写 `Closes #<编号>`。
- **规则（必须）**：
  1. 一个提交只含一个逻辑变更；无关改动（混入的 style/重构/格式）拆分或回退。
  2. 提交前必须通过质量门（lint/测试），禁止提交"应该能过"的代码。
  3. 禁止混入密钥、构建产物、生成代码（对应各类型禁区）。
  4. 不 amend / rebase 已推送的提交（维护者明确要求除外）。
  5. 能用工具校验的（格式、type 词表）写进 commitlint/husky 等配置，不在文档留第二份。

## 3. 架构文档：ARCHITECTURE.md（固定组成部分）

> 每个类型项目的 `docs/ARCHITECTURE.md` 是**架构权威文档**，与 AGENTS.md（操作手册）、CODING_STANDARDS.md（编码细则）三权分立、内容不重复。类型模板须把按本骨架填充的 ARCHITECTURE 范本放入类型目录 `docs/`（如 `springboot/docs/ARCHITECTURE.md`），并在类型 README 登记。

固定章节骨架（★=必建，子节可按类型增删，骨架不删）：

1. ★ **定位与范围**：一句话职责、形态、运行边界（对外接口、依赖的系统/服务）。
2. ★ **技术栈与版本基线**：组件/版本/版本来源（Boot BOM vs 独立声明）表格；**只写版本决策与升级约束，不写命令**。
3. ★ **分层与模块划分 + 依赖方向**：`controller → service → mapper(dao)` 单向图；模块职责表；禁反向/越层/同层横向。
4. ★ **关键链路**：同步（HTTP→…→存储）、缓存、消息、错误处理各至少一条文字链路图。
5. ★ **数据架构**：表命名与审计字段约定、DB 迁移纪律（只增不改、DB 先行）、索引与慢 SQL 基线、字典表设计。
6. ★ **中间件设计**：Redis key 布局登记表（缓存/lock/idem/rate/bloom + TTL + 序列化）；RabbitMQ 拓扑表（exchange/queue/routingKey/DLX/DLQ/消费者/幂等键）与消息信封。
7. ○ **安全**：认证/授权方案、权限矩阵、Swagger 生产关闭、密钥管理、脱敏。
8. ○ **可观测**：traceId 贯穿、日志级别、actuator/metrics 暴露面、告警项（系统错误、死信）。
9. ○ **Profile 与部署**：环境差异、发布顺序（DB 先行；缓存/MQ 兼容）。
10. ○ **技术决策记录（ADR）**：指向 `design-docs/` 的索引 + 必记决策清单（主键策略、缓存一致性、消息顺序/幂等等）。

规则：

1. **分工不重复**：ARCHITECTURE 不复制 AGENTS.md 命令表；命名/错误码/陷阱等细则只在 CODING_STANDARDS 一份，架构文档只写"结构、边界、链路、决策"。
2. 类型模板将本骨架填充为范本（`<类型>/docs/ARCHITECTURE.md`，如 `springboot/docs/ARCHITECTURE.md`），复制到真实项目后填 `{{占位符}}` 并按实际裁剪。
3. 架构变更（新增模块/链路/中间件、主键与一致性方案调整）**必须**同步本文件并记录 ADR 到 `design-docs/`。

## 4. 需求规格：product-specs/（固定组成部分，触发器驱动）

> 需求规格是"做什么 / 为什么 / 验收标准"的权威（`docs/product-specs/`）。**触发器**（见 §1）：首个需求/产品规格立项即建 `docs/product-specs/`（含 `index.md`）；一经建立，后续**每个需求一个文件**，维护到实现关闭。类型模板须提供需求规格范本（index + 单需求模板）放入类型目录 `docs/product-specs/` 并登记类型 README。

单需求文件固定骨架（★=必建，○=按需求类型取舍）：

0. ★ **元信息表**：需求编号 / 标题 / 类型（功能 | 缺陷 | 优化）/ 优先级（P0–P2）/ 状态 / 提出方 / 关联 issue
1. ★ **背景与目标**：为什么做；可度量目标；明确**非目标**（防止蔓延）
2. ★ **范围**：In / Out 清单（Out = 明确不做）
3. ★ **用户故事与验收标准**：`作为…我希望…以便…` + **可测验收**（Given-When-Then 或逐条检查项——每条都能直接转 JUnit 断言 / curl 期望）
4. ★ **业务规则与状态机**：规则编号 R1..；状态流转表；涉及的枚举/字典项
5. ◎ **接口契约**：端点 / 方法 / 出入参摘要 / 权限 / 预期 HTTP 与错误码（新增 `ErrorCode` 预登记）
6. ◎ **数据影响**：新增/变更表与字段、迁移脚本、字典项
7. ◎ **依赖与影响面**：模块、MQ 事件、缓存 key、外部系统、相关 ADR
8. ○ **测试计划**：单测 / 集成 / curl 冒烟要点，性能与安全考虑
9. ★ **评审与变更记录**：只增不改已确认内容；变更需重新评审

规则：

1. **编号与命名**：需求编号唯一稳定（如 `{{PRD}}-{{YYYYMM}}-{{序号}}`）；文件名 `{{编号}}-{{简短名}}.md`；`index.md` 维护状态流转（草稿 → 评审中 → 已确认 → 开发中 → 已完成 → 已废弃）。
2. **验收必须可测**：每条验收标准都能写成 JUnit 断言或 curl 期望；"大概能用"不通过。
3. **不写实现**：技术方案指向 `design-docs/` ADR 与 ARCHITECTURE；需求变化走 §9 变更记录，不靠口头/IM。
4. **落地闭环**：确认 → 拆 `exec-plans/active/` → 实现 + 测试 → 完成回写 `index.md` 状态。

> 历史与变更登记统一在根 `docs/changelog.md`。
