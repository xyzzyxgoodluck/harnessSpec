# {{项目名}} 架构规范（ARCHITECTURE）

> 本文件是 **springboot 类型交付物的一部分**（可复制范本）：复制到真实项目 `docs/ARCHITECTURE.md` 后填 `{{占位符}}`。它是**架构权威文档**，与 AGENTS.md（操作手册）、CODING_STANDARDS.md（编码细则）三权分立、内容不重复：**只写结构、边界、链路与决策，不复制命令表**。架构变更必须同步本文件并记 ADR（见 §10）。

---

## 1. 定位与范围 ★

- 一句话职责：{{如"订单与库存管理的 REST API 服务"}}；不做什么：{{不在本服务内实现的内容}}。
- 形态：{{单体 Web 服务（REST API）| 批处理 | 消息消费者 | …}}；部署单位：{{一个可执行 jar / 多实例}}。
- 运行边界：对外提供 HTTP API（{{/api/**}}）；依赖外部系统/服务：{{列出并登记}}；不直接对外暴露内部数据表与中间件。

## 2. 技术栈与版本基线 ★

| 组件 | 版本 | 版本来源 | 升级/选型约束 |
| --- | --- | --- | --- |
| Java | {{17|21|25 LTS}} | `.sdkmanrc`/pom | 全库统一 LTS |
| Spring Boot | {{以 pom parent 为准}} | Boot BOM | MyBatis-Plus/springdoc 兼容性需对照官方 changelog |
| MyBatis-Plus | {{3.5.x 等}} | pom `<properties>` 独立声明 | 不在 Boot BOM；升 Boot 大版本先核兼容 starter；分页等 jsqlparser 插件**默认不携带**，需另引依赖（见下行） |
| MyBatis-Plus jsqlparser 模块 | {{与 MyBatis-Plus 同号，如 3.5.17}} | pom `<properties>`（与 MP 同号声明） | MP 3.5.9 起从主包拆为 `com.baomidou:mybatis-plus-jsqlparser`；分页 `PaginationInnerInterceptor` 依赖它，漏加 = 编译不过或分页静默失效（细则见 CODING_STANDARDS §5） |
| MySQL | {{8.x}} | docker-compose / 云 RDS | 字符集 utf8mb4 |
| Redis | {{7.x|8.x}} | docker-compose | 序列化方案统一（见 §6） |
| RabbitMQ | {{4.x}} | docker-compose | 拓扑登记见 §6 |
| springdoc（Swagger UI） | {{2.x|3.x}} | pom `<properties>` | Boot 3→2.x、Boot 4→3.x |

基线原则：Spring 系版本只由 Boot BOM 提供；非 BOM 组件（MyBatis-Plus/springdoc 等）版本集中在 pom `<properties>` 显式声明并注释；本表不写命令（命令见 AGENTS.md）。

## 3. 分层与模块划分 + 依赖方向 ★

依赖只允许**单向向下**（防调用环）：`Controller → Service → Mapper(DAO) → MySQL/中间件`。

**固定分层顺序（架构信条 B7，见 `design-docs/core-beliefs.md`）**：`type → config → repo(mapper) → service → runtime → ui(controller)`——依赖只允许由右向左（`ui` 最上、`type` 最底）。本类型映射：

| 规范层 | 本类型落地 |
| --- | --- |
| `ui` | `controller/`（HTTP 适配，薄） |
| `runtime` | 应用装配与运行时：`SampleApplication` + Spring 容器；消息适配 `listener/` |
| `service` | `service/`（+ `impl/`，事务边界与业务规则） |
| `repo or dao` | `mapper/`（`BaseMapper`/自定义 SQL；条件构造器只在此层） |
| `config` | `config/`（分页插件/Redis/RabbitMQ/序列化/线程池） |
| `type` | `entity/` + `dto/`（表映射与出入参契约类型） |
| **共享基础设施**（`common/`：`ErrorCode`/`Result`/`PageResult`/`CacheKeys`/`TraceId`/`OrderStatus` 等跨域常量与工具） | **不参与六层单向约束**：可被任意层依赖，自身**不得依赖业务层**（已由 ArchUnit `commonMustNotDependOnBusinessLayers` 强制） |

**已批准的判定例外**（不算越层）：`entity/` 可依赖 `common/`（如状态常量 `OrderStatus`）；`config/` 内的常量类（`MqDestinations` 等）属该层内部引用。

> 强制现状：`ui → service → repo`（含禁反向/同层横向）与"`common/` 不得依赖业务层"已由 ArchUnit 固化；**六层中 `config`/`type`/`runtime` 方向的越界仍属「仅评审」**（缺口与补齐路线见模板仓库 `docs/enforcement-map.md`（项目内可自建同名登记表））。

```text
controller/ -> service/ -> mapper/(dao) -> MySQL
  service 层及以下可使用中间件：Redis / RabbitMQ（Controller 不得直接持有）
  禁止：反向依赖 / 越层依赖 / 同层横向互依赖（防调用环，ArchUnit 固化）
```

模块划分（**分层**，与 `AGENTS.md` 目录树同源；feature 优先为可选变体，需三处同步）：

| 模块/包 | 职责 | 备注 |
| --- | --- | --- |
| `controller/` | HTTP 适配：参数校验、调 Service、返回 DTO | 薄；禁业务判断与 Mapper 调用 |
| `service/`（+ `impl/`） | 用例编排、事务边界、业务规则 | 接口与实现分离 |
| `mapper/` | MyBatis-Plus Mapper（`BaseMapper`/自定义 SQL） | 依赖终点；条件构造器只在此包 |
| `entity/` `dto/` | 表映射实体 / 入出参对象 | 实体禁直接出参 |
| `config/` | 分页插件/Redis/RabbitMQ/序列化/线程池 | 声明集中，禁魔法串 |
| `common/` | `BizException`/`ErrorCode`/`Result`/`PageResult`/常量/幂等工具 | 可被所有模块依赖，不反向依赖业务 |
| `listener/` | RabbitMQ 消费者（`@RabbitListener`） | 仅做消息适配；幂等与重试集中 |
| 字典功能 | 基础元数据字典（只读查询 + `GET /dicts/{typeCode}`） | 基础设施；按分层落点（`DictController`/`DictService`/`DictMapper`/`DictItem`），业务只读引用，禁写 |

跨域协作合规出口（按优先级）：① 公共能力下沉 `common/`；② RabbitMQ 事件解耦；③ 显式编排服务 `XxxFlowService`（唯一允许依赖多个域 Service 的上层组件）。违规 = 评审打回（ArchUnit 固化）。

## 4. 关键链路 ★

- **同步读/写**：`HTTP → Controller（参数校验/出参 DTO）→ Service（事务边界/业务规则/错误码）→ Mapper（BaseMapper/自定义 SQL）→ MySQL`；异常统一 `@RestControllerAdvice` 转 `Result{code,message,traceId}`。
- **缓存链路**：读 `GET` 优先 Redis（key+TTL）→ 未命中回源 DB → 回填（防击穿互斥回填）；**写：先更 DB 再删缓存**；缓存一致性/失效策略见 §6 与 ADR。
- **消息链路**：Producer（publisher-confirm）→ Exchange → Queue → `@RabbitListener`（手动 ack + 幂等）→ 失败有限重试 → DLQ + 告警。
- **错误处理链路**：业务错误 `BizException(ErrorCode)`→`WARN`+业务码；系统/外部错误兜底→`ERROR`+告警，对外仅 `message+traceId`，堆栈只进日志。

## 5. 数据架构 ★

- **表与命名**：表名 `{{带 t_ 前缀或不带，全库一致}}`、字段 `snake_case` ↔ Java `camelCase`（MP 自动映射或 `@TableField`）；审计字段固定 `create_time`/`update_time`、逻辑删除 `deleted`、版本 `version`；主键策略 {{雪花 | 自增}}（决策理由记 ADR）。
- **字典表（必建）**：`{{t_dict_type}}`（type_code 唯一）+ `{{t_dict_item}}`（`(type_code, item_code)` 唯一）；稳定状态机用枚举不落库（详见 CODING_STANDARDS §5）。
- **迁移纪律**：结构变更以 `db/V{{n}}__{{描述}}.sql` 版本化脚本提交；**已应用脚本不得改写**（Flyway 校验和/发布纪律），只追加；发布顺序 **DB 先行**。
- **索引与慢 SQL 基线**：where/排序列按需建索引（最左前缀）；复杂查询经 `EXPLAIN` 核对执行计划；慢 SQL 治理另见 exec-plans/tech-debt-tracker.md。

## 6. 中间件设计 ★

### Redis：key 布局登记表（全库唯一事实源）

| 用途 | key 模式 | TTL | 说明 |
| --- | --- | --- | --- |
| 缓存 | `{{域}}:{{对象}}:{{id}}`（如 `order:detail:12345`） | {{如 10 分钟 + 随机抖动}} | 读回源回填，写库后删 |
| 分布式锁 | `lock:{{域}}:{{操作}}:{{id}}` | {{看门狗}} | Redisson，禁裸 SETNX |
| 幂等 | `idem:{{域}}:{{操作}}:{{id}}` | {{24h}} | 随 MQ 消费/写接口使用 |
| 字典 | `dict:{{typeCode}}` | {{如 30 分钟}} | 字典项列表缓存；字典变更后**删缓存**（细则见 CODING_STANDARDS §5） |
| {{rate:/bloom:（按需）}} | … | … | 限流/布隆 |

序列化全库统一：{{GenericJackson2JsonRedisSerializer | String+显式序列化}}，禁用 JDK 序列化。key 前缀常量类：`common/` 的 `OrderCacheKeys` 等。

### RabbitMQ：拓扑表（全库唯一事实源，声明集中 config/）

| 交换机 | 类型 | RoutingKey | 队列 | 消费者 | 幂等键 | DLX/DLQ |
| --- | --- | --- | --- | --- | --- | --- |
| `{{svc}}.{{域}}.exchange` | topic | `{{域}}.{{事件}}`（如 `order.paid`） | `{{svc}}.{{域}}.{{事件}}.queue`（多消费者加 `.消费方`） | `{{Xxx}}MqListener` | {{消息内 idemKey / 业务唯一索引}} | DLX `{{svc}}.{{域}}.dlx` → DLQ `…queue.dlq` |

消息信封统一顶层字段：`messageId(UUID)/traceId/type/timestamp/data`；JSON 序列化。拓扑登记常量类 `MqDestinations`。本表条目增删 = 架构变更（走 §10 ADR）。

## 7. 安全 ○

- 认证/授权方案：{{JWT/会话/OAuth/网关统一认证…}}；接口权限矩阵（角色 ↔ 端点）登记位置：{{docs/references/ 或权限模块文档}}。
- 生产环境：禁用 Swagger/OpenAPI（`springdoc.api-docs.enabled=false` 或网关白名单）；actuator 只暴露白名单端点。
- 密钥管理：一律环境变量/Secrets 注入，禁止入库入日志；个人敏感数据脱敏规则见 CODING_STANDARDS §9。

## 8. 可观测 ○

- `traceId`：入口过滤器/MDC 贯穿 HTTP、MQ 消费与日志；错误响应带 `traceId`（见 §4）。
- 健康检查：`/actuator/health`（含 DB/Redis/RabbitMQ 探针，按需定制）。
- metrics 与告警项：系统错误率、`ERROR` 日志量、MQ 死信深度、Redis/DB 连接池水位；告警接收方：{{值班渠道}}。

## 9. Profile 与部署 ○

| Profile | 用途 | 差异要点 |
| --- | --- | --- |
| dev | 本地开发 | 连接 docker compose 中间件；Swagger 开 |
| test | 集成测试/联调 | {{同 dev，测试库}} |
| prod | 生产 | 连接云中间件/Secrets；Swagger 关 |

发布顺序：**DB 迁移先行 → 新版本应用**；涉及缓存/MQ 的兼容变更考虑滚动发布（先兼容旧消费者/旧缓存 key 再切换）。CI/本地命令见 AGENTS.md。

## 10. 技术决策记录（ADR）○

- 索引：`docs/design-docs/index.md`；ADR 列表：`docs/design-docs/`（命名 `{{YYYYMMDD}}-{{简述}}.md`）。
- 必记决策（一经确定必须留痕）：主键策略、缓存一致性方案（写后删/延迟双删/版本号）、消息幂等键与顺序性取舍、逻辑删除与唯一索引冲突处理、是否引入 Flyway、MyBatis-Plus 通用 CRUD 抽象（`CrudRepository`，`IService` 官方已不推荐）是否启用等。
- 新增决策流程：方案 → 权衡 → 记 ADR → 同步本文件相关小节 → 评审。

---

> 范本结束。采用步骤：填入 `{{占位符}}` → 按实际裁剪 ○ 章节 → 与 `AGENTS.md`/`CODING_STANDARDS.md` 核对一致性 → 变更时同步并记 ADR。
