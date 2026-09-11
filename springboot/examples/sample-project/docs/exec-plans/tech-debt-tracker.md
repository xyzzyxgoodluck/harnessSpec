# 技术债台账（tech-debt-tracker）

> 发现即登记（不留在口头 / IM / 代码注释）；闭环后标记状态，历史行保留。
> 每条债务必须**可定位、可判完成**：写清影响、何时必须还、改完用什么证明还了。

## 登记规则（必须）

1. 触发条件写"何时必须还"（如"列表量超过百万行前"），不写"以后有空"。
2. 验证方式必须可执行（命令 / 测试 / `EXPLAIN` 结果 / 指标阈值）。
3. 状态只改不删：`未还 / 处理中 / 已还 / 不还（需写明理由与决策出处）`。
4. 来源于需求或决策的债务，给出编号互链：[`../product-specs/index.md`](../product-specs/index.md)、[`../design-docs/index.md`](../design-docs/index.md)。
5. 债务转化为实际改造时，在 [`active/index.md`](active/index.md) 建计划并在本表填「关联计划」列。

## 台账

| 编号 | 债务 | 影响 | 触发条件（何时必须还） | 验证方式 | 状态 | 关联（ADR / 需求 / 计划） |
| --- | --- | --- | --- | --- | --- | --- |
| TD-101 | 无 Docker：集成链路未验证（`@SpringBootTest` 全上下文 / Testcontainers / `db/V1__init.sql` 在真实 MySQL 执行 / `docker compose up -d` 后 curl 冒烟） | 枚举读写（`MybatisEnumTypeHandler` + `@EnumValue`）、`<script>` 动态分页 SQL、死信落 DLQ 三处只有单测与静态推断，无真机证据 | 首次在有 Docker 的环境交付前 | `docker compose up -d` → `./mvnw test` → 起服务后 curl 成功与失败分支，记录原始输出 | 未还 | AGENTS「测试与质量门」；`examples/sample-project/README.md`「L3 待执行」 |
| TD-102 | `/v3/api-docs` 未端到端校验（`@Tag`/`@Operation`/`@Schema` 已补，但未真实拉取） | OpenAPI 文档质量无证据，接口契约可能与注解不一致 | 首次起应用联调前 | `curl http://localhost:{{8080}}/v3/api-docs` 检查路径/模型/错误响应是否齐全 | 未还 | `../CODING_STANDARDS.md` §4 |
| TD-103 | `DictCache` 写入须为 `ArrayList`（Jackson 对 JDK 不可变集合不写类型信息）——该约束只由单测注释兜住 | 将来新增第二种缓存值类型时可能读回失败且无通用校验 | 新增第二种缓存值类型前 | 在 `RedisConfigTest` 扩展序列化往返断言，或加通用"缓存值类型白名单"测试 | 未还 | `../CODING_STANDARDS.md` §7 |
| TD-104 | `SummaryJavadoc.period` 设为全角「。」（团队中文 Javadoc 约定，非放宽规则） | 与 Google 默认 ASCII `.` 不同，升级 Checkstyle 大版本时可能出现误判 | 升级 Checkstyle 大版本时 | 复核规则配置与 27 处中文 Javadoc 首句 | 不还（团队本地化决策，配置注释已写明理由） | `config/checkstyle/checkstyle.xml` 注释 |
| TD-105 | 样例的架构规则（ArchUnit 9 条）除字典两条外**未带 `because(...)` 修法提示** | 失败消息只说"违反了哪条规则"，没告诉 agent **怎么改**（harness 原则 4） | 样例下次装配/重验时 | 逐条补 `because("…见 CODING_STANDARDS §3 / core-beliefs B2")`，故意造违规后消息中应含修法指向 | 未还 | `../CODING_STANDARDS.md` §3/§13 |
| TD-106 | **已定案的非平凡决策未记 ADR**：`CrudRepository` 选型、Redis 值类型必须 `ArrayList`、DAO 前缀用"正则 + 路径抑制过滤器"而非 XPath、`SummaryJavadoc.period` 中文本地化 | `design-docs/index.md` 的触发器（"决策定案 → 必须写 ADR"）**已满足却无一条 ADR**；决策理由不可追溯，下游按通用建议实现会与样例不一致 | 首次对外发布前（或样例下次装配时） | `docs/design-docs/` 下至少落 1 条 ADR（覆盖上列任一项）并在 `index.md` 登记；评审时以"是否留痕"判定 | 未还 | [`../design-docs/index.md`](../design-docs/index.md) 触发器表 |
| {{TD-001}} | {{订单列表查询未走索引}} | {{列表接口 P95 超基线}} | {{数据量超过百万行前}} | {{`EXPLAIN` 无全表扫描 + P95 达标}} | {{未还}} | {{ARCHITECTURE §5}} |
| {{TD-002}} | {{债务描述}} | {{…}} | {{…}} | {{…}} | {{未还}} | {{…}} |

> `TD-1xx` 为**本类型样例在 L1–L5 验证中发现的真实债务**；`{{TD-xxx}}` 为范本占位行（格式示例），采用本模板的项目按实际替换。
