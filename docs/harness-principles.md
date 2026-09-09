# Harness Engineering 核心原则与最佳实践（摘编）

> 主参考：[OpenAI — Harness engineering: using Codex in an agent-first world](https://openai.com/index/harness-engineering/)（2026-02，中文页：https://openai.com/zh-Hans-CN/index/harness-engineering/）与官方 [Codex AGENTS.md 指南](https://developers.openai.com/codex/guides/agents-md/)；工程卫生补充自 [deepseek-ai/deepseek-harness 根 AGENTS.md](https://github.com/deepseek-ai/deepseek-harness/blob/master/AGENTS.md)。
>
> 一句话：**Harness engineering = 把 agent 周围的软件"房间"改造成可读、可执行、可检查的工件**——把内隐的人为判断搬进仓库，而不是靠 prompt 让 agent 猜。本文摘编并映射到本仓库（AiCodingSpec）；OpenAI/DSH 的具体实现细节不搬运，与更具体的规范文件冲突时以具体文件为准。

1. **短 AGENTS.md 当"桌面便笺"，docs/ 当"文件夹系统"**（渐进披露 progressive disclosure）。→ OpenAI 实测：单个大 AGENTS.md 会上下文压力、指导稀释、腐化、难验证。根 AGENTS 只给地图与不可妥协边界，深层真相放 docs/，用到再展开。落点：根 `AGENTS.md` 保持精简地图态（本仓库 v3.0 拆分即此）；类型模板瘦身同理。
2. **仓库知识是系统真相；对 agent 不可见的知识等于不存在**（repository knowledge as system of record）。→ agent 无法问人；决策要写进仓库，规则要可发现、尽量可执行。落点：版本号/端口指向单一来源（pom/README）、被引用文档必须真实存在。
3. **先索引后展开（渐进披露）**。→ agent 先见名称/路径/职责，需要时才读全文：根 AGENTS 小索引 → 类型 docs（CODING_STANDARDS/ARCHITECTURE/product-specs/exec-plans）按需展开——本仓库 docs 分层的目的即此。
4. **不变量优于文档：文档不治漂移，强制才治**（invariants, not micromanagement）。→ 不变量分三层：架构（依赖方向/包边界/层归属）、可靠性（结构化日志/边界解析/幂等）、口味（命名/文件规模/错误消息风格）。写成 lint 与结构测试进 `verify` 门禁，且**失败消息要教 agent 怎么改**。落点：ArchUnit/Spotless/Checkstyle/JaCoCo 门禁、DAO 前缀/依赖方向用检查固化。
5. **定义"哪些选择自由、哪些不可协商"**（speed without boundaries is decay）。→ 只锁不可协商项（禁反向/越层/横向依赖、QueryWrapper 隔离、密钥红线、脚本只增不改…），不微观管理实现细节。
6. **让 agent"看得到"运行与证据**。→ 快速开始可执行、curl 自测证据进 PR/MR、traceId 贯穿、健康检查可探测——测试、日志、错误消息、lint、评审都是反馈通道。
7. **评审是反馈通道，不是走形式**。→ agent 自审 → agent 互审（按严重度+证据分类）→ 作者修复或**书面反驳**（允许 push back），循环到 rubric 满足或记录真实 blocker。落点：PR/MR 必须带验收标准与验证证据。
8. **循环与清理防熵**。→ 长任务循环迭代；清理/归档防止积累：exec-plans/active→completed、tech-debt-tracker、ADR 归档即冻结。
9. **口味靠共享样例 + lint 反重复**。→ agent 会模仿仓库里的坏样例：给出唯一共享实现（common/、ErrorCode、字典、常量类）并让 lint/门禁拒绝本地重复与自造同义写法。
10. **测试描述行为；记录决策；失败响亮；精确措辞；收尾卫生**（deepseek-harness 工程卫生）。→ 行为变化连同测试/文档同变更并说明原因；非平凡变更同变更记录 ADR；配置缺失/引用悬空/查重冲突不得静默；用具体术语不用玄词；文件单换行收尾、无调试残留、无"若有"式假设句。

适用对象：本仓库内的人与 AI 编码代理。以上为工作方式准则；涉及工具使用边界时以 Harness 运行时规则为准。
