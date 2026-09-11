# {{需求编号}}：{{需求名称}}（需求规格）

> 复制本模板为 `{{编号}}-{{简短名}}.md` 后填写；★=必填，○=按需求类型取舍。原则：**验收必须可测**（每条都能转 pytest 断言 / curl 期望）；**不写实现方案**（技术细节指向 ADR/ARCHITECTURE）。

## 0. 元信息 ★

| 字段 | 值 |
| --- | --- |
| 需求编号 | {{PRD-202609-001（唯一稳定）}} |
| 标题 | {{订单取消}} |
| 类型 | {{功能 \| 缺陷 \| 优化}} |
| 优先级 | {{P0（阻塞）/ P1 / P2}} |
| 状态 | {{草稿 / 评审中 / 已确认 / 开发中 / 已完成 / 已废弃}} |
| 提出方 / 负责人 | {{产品}} / {{研发}} |
| 关联 issue / 版本 | {{#编号}} / {{v1.x}} |

## 1. 背景与目标 ★

- 为什么做：{{用户/业务痛点，1–3 句}}。
- 目标（可度量）：{{如"取消率≤5% 的订单可自助取消；操作成功率 ≥ 99%"}}。
- **非目标**（明确不做，防蔓延）：{{如"不做批量取消；不做跨境订单取消"}}。

## 2. 范围 ★

**In（本需求做）**
- {{…}}

**Out（本需求不做）**
- {{…}}

## 3. 用户故事与验收标准 ★

> 每条验收标准必须可测：能直接写成 pytest 用例断言（`tests/test_{域}.py`）或 curl 期望（含预期 HTTP 与错误码）。

**用户故事**：作为 {{角色}}，我希望 {{能力}}，以便 {{收益}}。

| # | 验收标准（Given-When-Then / 检查项） | 对应测试 |
| --- | --- | --- |
| AC1 | 给定 {{状态}} 的订单，当 {{操作}} 时，应返回 HTTP {{200/204}}，订单状态变为 {{CANCELLED}} | `tests/test_{{order}}_service.py::{{test_cancel_order_success}}` |
| AC2 | 给定 {{已支付且超过可取消时限}} 的订单，当 {{操作}} 时，应返回业务错误 `ErrorCode.{{ORDER_CANNOT_CANCEL}}`（HTTP {{409}}），状态不变 | `tests/test_{{order}}_api.py::{{test_cancel_rejected_returns_409}}` |
| AC3 | {{边界：不存在的订单 -> HTTP 404 + `ORDER_NOT_FOUND` + 响应含 traceId}} | `tests/test_error_contract.py::{{…}}` |

## 4. 业务规则与状态机 ★

| 规则编号 | 规则 | 说明 |
| --- | --- | --- |
| R1 | {{仅待支付/待发货状态可取消}} | {{状态校验前置}} |
| R2 | {{取消需释放库存/原路退款（异步）}} | {{见 AC 与消息事件}} |

状态流转（受影响部分）：

```text
{{PAID -> CANCELLED}}（取消操作）
```

涉及枚举/字典：{{如新增 `OrderStatus.CANCELLED`（Python 枚举，不落库）；如需运营可配项登记到 `t_dict_item`（如渠道 `CHANNEL`），新增 type_code 先查重}}。

## 5. 接口契约 ◎

| 端点 | 方法 | 入参（Schema） | 出参 | 权限 |
| --- | --- | --- | --- | --- |
| `/api/v1/orders/{order_no}/cancel` | POST | `{{OrderCancelRequest}}` | `{{OrderResponse}}` | {{登录用户/订单属主}} |

- 校验规则：{{Pydantic 字段约束（`Field(min_length=...)`）与跨字段 `@model_validator`}}。
- 预期错误码（预登记到 `core/errors.py` 的 `ErrorCode`，见 CODING_STANDARDS §10）：{{`ORDER_NOT_FOUND`(404)、`ORDER_CANNOT_CANCEL`(409)}}。
- API 文档：接口上线即出现在 OpenAPI（`summary` + `responses` 中文描述，见 CODING_STANDARDS §5；生产环境不暴露 `/docs`）。

## 6. 数据影响 ◎

- 新增/变更表：{{新增/变更的表与字段，如"t_order 无结构变更"}}；迁移脚本：{{迁移脚本路径，如 migrations/versions/0001_xxx.py；只增不改}}。
- 字典项：{{无 / 新增 `t_dict_item` 记录（走字典维护入口，禁业务直写）}}。

## 7. 依赖与影响面 ◎

| 依赖/影响 | 说明 |
| --- | --- |
| 模块 | {{order：service/repository；涉及 MQ 则 + 消费者模块}} |
| MQ 事件 | {{事件名与交换机/路由键，拓扑登记见 ARCHITECTURE §6}} |
| 缓存 key | {{`order:detail:{order_no}` 取消后需删除}} |
| 外部系统 | {{支付/库存系统（如有）}} |
| ADR | {{是否新增决策（如退款异步一致性）-> design-docs/}} |

## 8. 测试计划 ○

- 单测：{{service：正常路径 / 每个业务异常分支断言 ErrorCode / 边界；repository 自定义查询}}。
- 接口测试：{{`httpx.ASGITransport` + `dependency_overrides` 覆盖端口；失败分支断言 code 与 traceId}}。
- curl 冒烟（记录到 PR 描述）：{{成功 + 409/404 失败分支各一条}}。
- 集成/性能/安全考虑：{{`-m integration` 覆盖 DB/Redis/RabbitMQ；涉及金额操作关注幂等与并发}}。

## 9. 评审与变更记录 ★

| 日期 | 版本 | 变更 | 评审人/结论 |
| --- | --- | --- | --- |
| {{2026-09-01}} | v0.1 | 初稿 | {{待评审}} |
| | | | |

> 已确认内容只增不改；变更追加本表并重新评审，同步更新 `index.md` 状态。
