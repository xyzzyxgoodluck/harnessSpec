# design-docs —— 设计决策与 ADR 索引

> 本目录存放**设计决策/ADR**（"为什么这样定"）。固定组成部分：**目录必建**（含本文件与 `core-beliefs.md`）；ADR 文件按触发器写入。
> 分工不重复：`AGENTS.md` = 操作手册（命令/禁区）、`../CODING_STANDARDS.md` = 编码细则、`../ARCHITECTURE.md` = 结构/链路权威、本目录 = 决策与取舍。

## 内容约定

- `core-beliefs.md`：项目级核心信条 + 业务名词中英对照词典（`../CODING_STANDARDS.md` §1 指向此处）。
- `{{YYYYMMDD}}-{{简述}}.md`：单条 ADR，日期在前、小写 kebab-case，如 `{{20260901-order-id-strategy.md}}`。
- ADR 一经确认即**冻结**（只增不改）：结论变化不改旧文，追加新 ADR 并注明取代关系。

## 触发器（条件满足即必须写入内容）

| 触发条件 | 必须写入 |
| --- | --- |
| 主键策略、软删除与唯一索引冲突、缓存一致性方案、消息幂等/顺序、事务边界粒度、是否引入 import-linter/Testcontainers 等决策定案 | 一条 ADR + 同步 `../ARCHITECTURE.md` §10 索引 |
| 新增/调整模块、关键链路、中间件拓扑（Redis key、RabbitMQ 交换机/队列） | 一条 ADR + 同步 `../ARCHITECTURE.md` 对应小节 |
| 出现业务名词中英对照、跨团队通用术语 | 追加到 `core-beliefs.md` 词典 |

## ADR 清单

| 编号 | 标题 | 状态 | 日期 | 关联（ARCHITECTURE 小节 / 需求 / issue） | 文件 |
| --- | --- | --- | --- | --- | --- |
| {{ADR-001}} | {{订单主键采用数据库自增}} | {{已接受}} | {{2026-09-01}} | {{ARCHITECTURE §5 / PRD-202609-001}} | `{{20260901-order-id-strategy.md}}` |

> 状态取值：提议中 / 已接受 / 已取代 / 已废弃。新增：复制命名约定建文件 -> 填「背景 / 方案对比 / 决策 / 影响」-> 在本表登记一行。
