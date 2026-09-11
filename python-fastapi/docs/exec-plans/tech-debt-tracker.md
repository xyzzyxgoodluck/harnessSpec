# 技术债台账（tech-debt-tracker）

> 发现即登记（不留在口头 / IM / 代码注释 / `TODO`）；闭环后标记状态，历史行保留。
> 每条债务必须**可定位、可判完成**：写清影响、何时必须还、改完用什么证明还了。

## 登记规则（必须）

1. 触发条件写"何时必须还"（如"列表数据量超过百万行前"），不写"以后有空"。
2. 验证方式必须可执行（命令 / 测试用例 / `EXPLAIN (ANALYZE, BUFFERS)` 结果 / 指标阈值）。
3. 状态只改不删：`未还 / 处理中 / 已还 / 不还（需写明理由与决策出处）`。
4. 来源于需求或决策的债务，给出编号互链：[`../product-specs/index.md`](../product-specs/index.md)、[`../design-docs/index.md`](../design-docs/index.md)。
5. 债务转化为实际改造时，在 [`active/index.md`](active/index.md) 建计划并在本表填「关联计划」列。

## 台账

| 编号 | 债务 | 影响 | 触发条件（何时必须还） | 验证方式 | 状态 | 关联（ADR / 需求 / 计划） |
| --- | --- | --- | --- | --- | --- | --- |
| TD-101 | 读缓存回源链路未实现：`get_order` 只走仓储，`CacheProtocol.get/set` 与 `CacheKeys` 的 `idem:`/`lock:` 常量无调用点 | "读 Redis 未命中回源 → 回填"这条关键链路没有 L3 证据（写后删缓存有单测，已强制） | 首个需要缓存加速的查询接口上线前 | 在 service 补读缓存 + 回填，并加单测断言"未命中→回源→回填" | 未还 | `../ARCHITECTURE.md` §4/§6 |
| TD-102 | Redis key 集中化（禁散落字符串）无强制手段；样例测试自身也用了字面量 `order:detail:` | 魔法字符串可能随接口增长扩散 | 出现第二个缓存 key 前缀前 | 测试改为引用 `core/cache.py` 常量，并补"禁字面量 key"的断言或 lint | 未还 | `../CODING_STANDARDS.md` §8 |
| TD-103 | RabbitMQ 相关规则（生产者 confirm / 手动 ack / 有限重试 / DLQ / 幂等）在样例中只有常量与信封构造，无运行时实现 | §9 的「必须」当前没有任何运行时证据 | 首次接入真实 broker 前 | 用真实 broker（或 Testcontainers）跑消费链路，覆盖重复投递与死信 | 未还 | `../CODING_STANDARDS.md` §9 |
| TD-104 | commitlint / pre-commit 未接入 | 提交规范（§15）仍完全靠评审，且 CI 无该步骤 | 团队规模扩大或发布流程自动化前 | 接入后本地钩子与 CI 对违规提交信息同时报错 | 未还 | 模板仓库 `docs/enforcement-map.md` |
| TD-105 | `-m integration` 标记已登记但没有任何集成用例 | 该标记是空集，容易被误读为"集成测试已覆盖" | 首个集成用例落地时 | `uv run pytest -m integration --collect-only` 至少选中一个用例 | 未还 | `pyproject.toml` 的 `markers` |
| TD-106 | 规范要求"质量门单一入口命令"（AGENTS「测试与质量门」的 `{{质量门入口命令}}`），但样例尚未提供入口脚本 | 该规范条目在样例上**未被验证**；CI 只能按相同顺序分步执行 | 样例下次装配/重验时 | 提供入口脚本（如 `scripts/gate.py`）并按序跑五项检查，本地与 CI 都调它且退出码 0 | 未还 | `AGENTS.md`「测试与质量门」 |
| TD-107 | **已定案的非平凡决策未记 ADR**：`allow_indirect_imports` 的取舍（越层契约只看直接导入）、Redis 缓存值必须为 `ArrayList`、`--strict-markers` 的引入 | `design-docs/index.md` 的触发器（"决策定案 → 必须写 ADR"）**已满足却无一条 ADR**；决策理由不可追溯 | 首次对外发布前（或样例下次装配时） | `docs/design-docs/` 下至少落 1 条 ADR（覆盖上列任一项）并在 `index.md` 登记 | 未还 | [`../design-docs/index.md`](../design-docs/index.md) 触发器表 |
| {{TD-001}} | {{订单列表查询未走索引}} | {{列表接口 P95 超基线}} | {{数据量超过百万行前}} | {{`EXPLAIN` 无全表扫描 + P95 达标}} | {{未还}} | {{ARCHITECTURE §5}} |
| {{TD-002}} | {{service 层缺少异常分支测试}} | {{回归风险高}} | {{下次改动该 service 前}} | {{`uv run pytest tests/test_order_service.py` 覆盖全部分支}} | {{未还}} | {{PRD-202609-001}} |

> `TD-1xx` 为**本类型样例在 L1–L5 验证中发现的真实债务**；`{{TD-xxx}}` 为范本占位行（格式示例），采用本模板的项目按实际替换。
