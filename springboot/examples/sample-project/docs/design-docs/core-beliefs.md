# core-beliefs —— 核心信条与术语词典

> 本文件是**项目级共识**的落脚点：哪些选择不可协商、哪些自由；业务名词的中英对照。
> 由 `../CODING_STANDARDS.md` §1 引用（业务名词词典位置）。**只增不改**：信条变化追加新条目并标注取代关系。

## 1. 核心信条（不可协商）

| # | 信条 | 唯一实现处（不在两处各写一份） |
| --- | --- | --- |
| B1 | 错误可分类、错误码单一来源，对外只给 `message + traceId` | `common/ErrorCode`（`../CODING_STANDARDS.md` §6） |
| B2 | 依赖单向 `controller -> service -> mapper`，禁反向/越层/同层横向 | ArchUnit 架构测试（`../CODING_STANDARDS.md` §3） |
| B3 | 写库后**删缓存**，不"先更新缓存" | 缓存封装（`../CODING_STANDARDS.md` §7） |
| B4 | 消费幂等：重复投递不产生副作用 | 幂等键（业务唯一索引 / Redis `SETNX`+TTL，`../CODING_STANDARDS.md` §8） |
| B5 | 生成物与已归档记录只读，人工不手改 | 生成命令登记于 [`generated/index.md`](../generated/index.md) |
| B6 | 密钥、内网地址、真实账号不入代码/配置/日志/文档 | 环境变量注入（`../CODING_STANDARDS.md` §11） |
| B7 | **分层依赖固定为 `type -> config -> repo(mapper) -> service -> runtime -> ui(controller)` 单向**（依赖只由右向左；`ui` 最上、`type` 最底） | 映射表见 `../ARCHITECTURE.md` §3；现有 ArchUnit 契约覆盖 `controller->service->mapper` 与禁反向/横向，缺口（`config`/`type`/`runtime` 方向）登记为「仅评审」 |
| B8 | **规范先行**：先有规范与设计（`AGENTS.md`/`ARCHITECTURE`/需求规格/ADR），再有代码；改动先改规范再改实现 | 需求 → [`../product-specs/index.md`](../product-specs/index.md)；决策 → [`index.md`](index.md)；**对外版本变更 → 项目根级 `CHANGELOG.md`**（首个发布时建立） |
| B9 | **开闭原则（OCP）**：新增能力优先靠新增实现/策略/事件扩展，不靠增长 `if type == …` 分支 | 合规出口：`common/` 下沉、RabbitMQ 事件、显式编排 `XxxFlowService`（`../CODING_STANDARDS.md` §3） |
| B10 | **禁止过度设计**：抽象/配置项/扩展点必须由 ≥2 处真实使用或明确近期需求支撑 | 评审必问项（**仅评审**，见模板仓库 `docs/enforcement-map.md`（项目内可自建同名登记表）） |
| B11 | **RDBMS 表设计达到 3NF**：禁冗余可推导数据；反范式（缓存列/汇总表）必须记 ADR 并写明同步与一致性策略 | 迁移脚本 `db/V{{n}}__{{描述}}.sql` + `../ARCHITECTURE.md` §5 + ADR |
| B12 | **单一职责（SRP）**：一个模块/类/函数只对一个变化原因负责；出现两个变更原因即拆分 | `../CODING_STANDARDS.md` §3（分层职责与包结构）+ 评审；跨域协作三出口见 B9 |

> 以上 B7–B12 为**架构信条**（模板仓库固定规范 `docs/fixed-docs.md` §5 的 6 条，措辞以该文件为权威；**本表已内嵌全文，采用本模板的项目以本表为准**；本表给出本类型落地与强制者）。新增信条的顺序：先在本表登记 -> 落到唯一实现处 -> 登记强制者 -> 必要时记 ADR（[`index.md`](index.md)）。

## 2. 术语词典（业务名词中英对照）

| 中文 | 代码标识（英文） | 说明 |
| --- | --- | --- |
| {{订单}} | `Order`（表 `{{t_order}}`） | {{聚合根；状态用枚举不落库}} |
| {{订单状态}} | `OrderStatus`（Java 枚举） | {{稳定状态机不落字典表，见 ../CODING_STANDARDS.md §5}} |
| {{字典项}} | `DictItem`（表 `{{t_dict_item}}`） | {{运营可配枚举走字典表，`type_code`/`item_code` 用 `UPPER_SNAKE_CASE`}} |

> 规则：代码标识一律英文（`../CODING_STANDARDS.md` §1）；禁止拼音缩写与同义混用；新词先在本表登记再落代码。
