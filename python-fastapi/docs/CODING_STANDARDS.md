# Python / FastAPI（Pydantic v2 + SQLAlchemy 2.0 + Alembic）编码规范（示例范本）

> 本文件是 **python-fastapi 类型交付物的一部分**（可复制范本）：团队采用时将其放入真实项目 `docs/CODING_STANDARDS.md`，按自身情况**裁剪**后执行，并同步更新该真实项目 `AGENTS.md`「编码约定」章节的指向。
>
> 措辞级别：**必须**=强制（违反即 CI/评审不通过）；**应该**=默认做法，例外需注释说明；**可以**=可选。
> 规则冲突时以本文件细则为准；工具能自动执行的规则（格式、import 顺序、类型、commit 格式）一律写进工具配置，不人工争论。

---

## 1. 通用与命名

- **必须**：模块、包、函数、变量、参数 `snake_case`（`order_service.py`、`create_order`、`order_no`）；类与类型别名 `PascalCase`；模块级常量 `UPPER_SNAKE_CASE`。
- **必须**：包名全小写、无下划线（`order/` 而非 `order_module/`）；测试文件 `test_{{模块}}.py`，测试函数 `test_{{被测行为}}_{{预期结果}}`（`test_create_order_returns_201`）。
- **应该**：布尔量用 `is_`/`has_`/`can_` 前缀；集合用复数或类型名，禁用 `data`/`list`/`tmp` 这类无信息量命名。
- **必须**：不写 `from __future__ import annotations`（目标版本 ≥3.12 且全库统一）；类型别名与泛型统一用 PEP 695 语法——泛型模型写成 `class PageResult[T]` 并继承 `BaseModel`，类型别名写成 `type OrderNo = str`。
- **禁止**：拼音缩写、无意义缩写、同义混用（`query`/`find`/`get` 只保留一个）；单文件超过 400 行或职责超过一个主题即拆分模块。
- **必须**：代码标识一律英文；业务名词的中英对照登记在 `docs/design-docs/core-beliefs.md`，新词先登记再落代码。

## 2. 类型注解与静态检查

- **必须**：所有函数（含 `__init__`、私有方法、pytest 夹具、测试函数）写全参数与返回类型注解；`mypy` 在 `strict = true` 下运行，覆盖 `src/` 与 `tests/`。
- **必须**：容器用内置泛型（`list[str]`、`dict[str, int]`），联合用 `X | None`，可调用用 `collections.abc.Callable`；禁止 `typing.List`/`Optional` 这类已过时写法（`ruff` 的 `UP` 规则集覆盖）。
- **应该**：需要"结构化鸭子类型"时用 `typing.Protocol` 定义端口（如仓储、缓存），测试用内存实现替换，而不是继承真实类或打补丁。
- **必须**：`# type: ignore` 必须带具体错误码与原因（`# type: ignore[arg-type]  # 三方库缺失类型`）；禁止裸 `# type: ignore`。
- **禁止**：`Any` 出现在公共函数签名（`**kwargs: Any` 仅限装饰器与框架适配层）、在业务代码用 `cast()` 掩盖真实类型问题。
- **应该**：Pydantic 插件（`plugins = ["pydantic.mypy"]`）与 SQLAlchemy 内联类型让 ORM 构造与模型字段可类型检查；第三方库缺类型时用 `[[tool.mypy.overrides]]` 的 `ignore_missing_imports` 精确豁免，禁止全局关闭。

## 3. 分层职责与包结构

```text
src/{{包名}}/
  main.py                 # 应用工厂：中间件、异常处理器、路由注册
  api/
    deps.py               # 组装根：engine/session/cache/service 的唯一装配点
    routers/{{域}}.py      # HTTP 适配：schema 校验 -> 调 service -> 返回 schema
  services/{{域}}_service.py   # 业务规则、用例编排、事务发起、抛 BizError
  repositories/{{域}}_repository.py  # 数据访问：select()/SQL、端口 Protocol + 实现
  models/{{域}}.py        # 表映射：DeclarativeBase 子类 + Mapped[] 标注
  schemas/{{域}}.py       # Pydantic v2：*Request/*Query/*Response
  core/                   # config / errors / logging / cache / mq（跨域基础设施）
  db/                     # 引擎、会话工厂、Base
```

- **必须**：路由**薄**——只做"取依赖 → 调 service → 返回 schema"，不含业务判断、不打开会话、不拼 SQL、不发消息。
- **必须**：service 是业务规则的唯一所在，也是唯一抛 `BizError` 的层；事务边界由 `api/deps.py` 提供（一个请求一个会话一个事务），service **禁止**手工 `commit()`/`rollback()`。
- **必须**：repository 是数据访问的唯一所在，条件拼装（`select().where(...)`）与裸 SQL 只允许出现在此层；repository 不调用 service、不发消息、不删缓存。
- **必须**：`models/` 只做表映射，**不含业务方法**，**禁止**直接作为接口出参（用 `schemas/`）。
- **必须**：`schemas/` 与 `models/` 禁止混用：入参/出参模型不继承 ORM 基类，ORM 实体不写 Pydantic 校验字段。

### 命名对照表（固定词典，全库统一）

| 组件 | 命名 | 说明 |
| --- | --- | --- |
| 路由模块 | `api/routers/orders.py`，`router = APIRouter(prefix="/api/v1/orders", tags=["order"])` | 路径资源用复数，版本前缀集中在路由 |
| 路由函数 | `create_order` / `get_order` / `list_orders` / `update_order` / `cancel_order` | 复用 service 的用例动词 |
| 服务类 | `OrderService`（模块 `services/order_service.py`） | 无状态；依赖经构造函数注入 |
| 仓储 | `OrderRepository` + `OrderRepositoryProtocol` | 方法名 `select_*`/`insert`/`update_*`/`delete_*` |
| 表映射 | `Order`（`__tablename__ = "t_order"`） | 显式表名；`Mapped[...]` + `mapped_column(...)` |
| 入参模型 | `OrderCreateRequest` / `OrderUpdateRequest` / `OrderPageQuery` | Pydantic v2，`extra="forbid"` |
| 出参模型 | `OrderResponse`（列表项 `OrderItemResponse`） | 出参不暴露实体；分页统一 `PageResult[T]` |
| 错误码 | `ErrorCode` 枚举（`core/errors.py`） | 见 §10；禁散落数字/字符串 |
| 缓存 key | `CacheKeys` + 取键函数（`core/cache.py`） | 见 §8；禁魔法字符串 |
| 消息拓扑 | `MqTopology`（`core/mq.py`） | 见 §9；交换机/路由键/队列/死信集中登记 |
| 配置 | `Settings`（`core/config.py`） | `pydantic-settings`，环境变量前缀全库统一 |

方法动词词典（必须）：读 `get`（单条）/`list`（多条，带上限）/`page`（分页）/`count`；写 `create`/`update`/`delete`/`enable`/`disable`/`cancel` 等业务动词 + 宾语。仓储层方法**必须**以 SQL 动词 `select`/`insert`/`update`/`delete` 开头，两层词表分层隔离、禁止混用。

## 4. 依赖方向约束（必须，防调用环）

- 依赖只允许**单向向下**：`routers → services → repositories → models`；`core/` 与 `db/` 可被任意层依赖，但它们**不得**反向依赖业务模块。
- **禁止反向依赖**：repository 不得导入 service，service 不得导入 router，`core/` 不得导入具体业务模块。
- **禁止越层依赖**：router 不得直接使用 `AsyncSession`/repository 类型；repository 不得抛 `BizError`（业务语义归 service）。
- **禁止同层横向互依赖**：`OrderService ↔ UserService`、`OrderRepository ↔ UserRepository`、`order_router ↔ user_router` 一律禁止——横向依赖是调用环的主要来源，评审必打回。
- 跨域协作只允许以下三种出口（按优先级）：
  1. **下沉**：多个 service 复用的无状态能力下沉到 `core/`（工具、时间、ID 生成、分页换算）；
  2. **事件解耦**：跨域最终一致发 RabbitMQ 事件（见 §9）；
  3. **显式编排**：确需同步编排多个领域时新增独立编排服务 `{{域}}FlowService`（唯一允许依赖多个领域 service 的上层组件），领域 service 禁止反向依赖它。
- **必须**：用 `import-linter` **四条契约**把上述方向固化进质量门，做到"依赖方向违规即失败"：① `layers` 分层单向；② `forbidden` 禁反向（repositories/models/core 不得依赖 services/api）；③ `forbidden` + `allow_indirect_imports` 禁越层（`api/routers` 不得直接依赖 repositories/models/db；经装配根 `deps` 的传递依赖属正常路径）；④ `independence` 禁同层横向。契约与代码同步演进——**新增同级模块时必须同步加入 independence 契约**（该契约只能枚举）。
- **应该**：让门禁失败消息自带**修法指向**（harness 原则：让 agent 看得到怎么改）——`import-linter` 会打印具体违规导入链（谁 import 了谁、第几行），新增检查时保持同样可读性；不得只留"规则不满足"这类无指向消息。

## 5. API 与 Pydantic Schema

- **必须**：入参/出参一律 Pydantic v2 模型；入参默认 `model_config = ConfigDict(extra="forbid")`，避免静默吞掉多余字段。
- **必须**：字段校验写在 schema（`Field(min_length=..., pattern=..., gt=...)`），业务规则不写在校验器里；跨字段校验用 `@model_validator(mode="after")`。
- **必须**：出参不直接返回 ORM 实体；确实要从实体构造时用 `model_config = ConfigDict(from_attributes=True)` + `model_validate(entity)`。
- **必须**：分页返回统一结构（`PageResult[T]`：`items/total/page/page_size`），列表端点必须有 `page`/`page_size` 上限。
- **必须**：路由必须写 `summary`（中文一句话），并通过 `responses` 参数为每个失败分支声明状态码与 `ErrorResponse` 模型；OpenAPI 文档由代码生成，**禁止**维护与代码漂移的独立 API 文档。
- **必须**：交互式文档（`/docs`、`/openapi.json`）只在非生产开启——由 `Settings` 的开关决定 `docs_url`/`openapi_url`，默认关闭；生产由网关白名单控制。
- **禁止**：`Field` 的 `examples`/`description` 出现真实密钥、token、身份证/手机号等敏感信息（示例值同样受限）；禁止把 `Request`/`Response` 原生对象透传进 service。

## 6. 异步与并发

- **必须**：路由、service、repository 全链路 `async def`；数据库用 `AsyncSession` + `create_async_engine`。
- **必须**：异步路径内**禁止**阻塞调用——`requests`、`time.sleep`、同步 DB 驱动、同步 `Session`、大文件同步读写；确需阻塞时用 `await anyio.to_thread.run_sync({{fn}})` 并注释原因。
- **必须**：CPU 密集或 >100ms 的同步任务不得占用事件循环；长耗时任务改用后台任务（`BackgroundTasks`）或投递 RabbitMQ 由消费者处理。
- **必须**：engine 与 Redis/RabbitMQ 客户端在进程内复用（模块级惰性单例，见 `api/deps.py` 与 `core/cache.py`），**禁止**每请求新建 engine 或连接。
- **必须**：事务边界与请求边界一致（一个请求一个会话一个事务）；事务内**禁止**做远程调用（HTTP/消息发送/大耗时 IO）。
- **禁止**：异步代码里用可变全局状态；跨协程共享可变对象时必须说明并发模型（`asyncio.Lock`/`ContextVar`）。
- **应该**：`greenlet` 由 SQLAlchemy 异步扩展自动依赖，禁止手工操作；出现 `MissingGreenlet` 一律视为"在非 await 上下文访问了惰性属性"，用 `selectinload`/显式查询修正，不用 `expire_on_commit=True` 掩盖。

## 7. 数据库与迁移（SQLAlchemy 2.0 + Alembic + PostgreSQL）

### 会话与事务

- **必须**：会话与事务由 `api/deps.py` 的依赖提供（`async with session.begin(): yield session`）；service/repository **禁止**手工 `commit()`。
- **必须**：只读查询可以显式标注（如 `session.begin()` 之外的只读事务或数据库只读账号），写操作必须在事务内。
- **禁止**：在事务内做远程调用、发消息、删缓存之外的中间件写操作；"事务提交后发消息/删缓存"用明确的提交后回调或依赖退出钩子。

### ORM 使用（SQLAlchemy 2.0 风格）

- **必须**：模型用 `Mapped[...]` + `mapped_column(...)` 显式声明类型与约束（`nullable`、`unique`、`index`、`server_default`）；表名显式 `__tablename__`。
- **必须**：查询用 `select()` 2.0 风格 + `await session.execute(stmt)`；**禁止**遗留的 `session.query(...)` 与 1.x 风格写法。
- **必须**：显式列或显式实体，**禁止**无条件 `select(Model)` 后把整行大对象塞进内存的做法用于列表端点；列表/分页必须有 `limit`。
- **必须**：关系加载策略显式声明（`lazy="raise"` 或 `selectinload`/`joinedload`），避免 N+1 与异步惰性加载报错。
- **应该**：审计字段用 Mixin（`created_at`/`updated_at`，`server_default=func.now()`）统一；主键策略（自增/雪花）全库统一并在 `docs/ARCHITECTURE.md` §5 声明。
- **禁止**：业务代码拼接 SQL 字符串；f-string 拼 SQL 一律禁止（参数化绑定或 `text()` + 绑定参数）。

### Alembic 迁移纪律

- **必须**：所有结构变更以 Alembic 迁移脚本提交（迁移脚本的生成/应用/离线预览命令见 `AGENTS.md`「常用命令」），生成后**人工审阅**（autogenerate 不识别重命名、枚举变更、数据迁移），审阅通过才提交。
- **必须**：**已应用到任何环境的迁移脚本不得改写**（校验和与发布纪律），只允许追加新脚本；回滚用 `downgrade`，不靠改历史。
- **必须**：迁移脚本必须写可运行的 `downgrade()`；不能回滚时在脚本注释中写明原因与人工回滚步骤。
- **应该**：`alembic upgrade head --sql` 离线渲染 DDL 供评审（不连数据库）；生产 DDL 评估锁与耗时，大表变更走低峰窗口。
- **禁止**：应用启动时自动建表/自动迁移（`Base.metadata.create_all`）用于生产；它只允许出现在测试夹具中。

### PostgreSQL

- **必须**：字符集与排序规则在建库时确定并登记；时间列一律 `timestamptz`（`DateTime(timezone=True)`），业务时间用 UTC 存储。
- **必须**：where/order by 涉及的列按需建索引，联合索引遵守最左前缀；慢查询用 `EXPLAIN (ANALYZE, BUFFERS)` 核对执行计划。
- **禁止**：事务中做超大范围 `SELECT ... FOR UPDATE`；批量写单批过大（按批拆分）；在应用层做分页/聚合（交 SQL）。

## 8. Redis 使用规范

- **必须**：key 布局统一 `{{域}}:{{对象}}:{{标识}}`（全小写、冒号分隔），按用途加类别前缀：缓存 `order:detail:{order_no}`、幂等 `idem:order:create:{request_id}`、锁 `lock:order:create:{order_no}`。
- **必须**：**所有 key 必须带 TTL**（无 TTL 只允许显式声明的配置类数据，并在 `docs/ARCHITECTURE.md` §6 登记）；key 模板集中在 `core/cache.py` 的 `CacheKeys` + 取键函数，禁止散落字符串。
- **必须**：写库后**删缓存**，不"先更新缓存"；强一致场景（延迟双删/版本号）必须先记 ADR 再实现。
- **必须**：客户端统一 `decode_responses=True`（或统一 bytes + 显式解码），序列化方案全库一致；值用 JSON 字符串，禁止存放 pickle。
- **必须**：分布式锁用成熟实现（`redis-py` 的 lock 或 Redisson 等价物）并设置超时与续期；**禁止**手写 `SETNX` + `EXPIRE` 裸锁。
- **应该**：雪崩（TTL 随机抖动）、击穿（互斥回填）、穿透（空值短 TTL 或布隆过滤器）防护按需实现并统一封装，不散落各处手写。
- **禁止**：缓存密钥/token/个人信息；把 Redis 当主存储；在事务内执行关键缓存写/删（回滚语义混乱）。

## 9. RabbitMQ 使用规范

- **必须**：生产端开启 publisher confirm（`aio-pika` 的 confirm 模式），确认失败记录并补偿/告警。
- **必须**：消费端**手动 ack**（`message.ack()` 在业务成功后调用）+ **有限重试**（如 3 次）；重试耗尽投递死信队列并告警；**禁止**无限重试刷爆日志、禁止异常未 ack（表现为消息堆积）。
- **必须**：消费处理**幂等**——at-least-once 下重复投递是常态；用业务唯一索引或 Redis 幂等键（`idem:` 前缀 + TTL）去重，幂等键由消息内容派生。
- **必须**：消息体统一 JSON 序列化；信封固定顶层字段 `messageId`/`traceId`/`type`/`timestamp`/`data`（见 `core/mq.py`）。
- **必须**：交换机/路由键/队列/死信命名集中登记在 `core/mq.py` 的 `MqTopology`，拓扑表登记在 `docs/ARCHITECTURE.md` §6；推荐命名（点分小写）：
  - 业务交换机 `{{svc}}.{{域}}.exchange`（topic）；RoutingKey `{{域}}.{{事件}}`（如 `order.created`）；
  - 业务队列 `{{svc}}.{{域}}.{{事件}}.queue`（多消费者再加 `.` + 消费方名）；死信 `{{svc}}.{{域}}.dlx` + `...queue.dlq`。
- **必须**：消费者声明 `prefetch_count`（按处理能力设定），避免单消费者抢占全部消息。
- **禁止**：在消费者里写跨域大事务 + 远程调用组合；消费者之间共享可变状态；把消息拓扑变更当成普通代码改动（必须同步架构文档并记 ADR）。

## 10. 异常与错误码

> 目标：错误可分类、编码可机器消费、命名可读——杜绝"500 一把梭 / 数字码乱飞 / 堆栈直出"。

- **必须**：业务失败抛 `BizError(ErrorCode.xxx, 参数...)`（定义在 `core/errors.py`）；**禁止**返回 `{"success": false}` 这类 200 包失败、禁止在路由里 `if/else` 拼错误响应。
- **禁止**：`except` 后吞异常（空 `except` 或只 `log` 不上抛不转译）；捕获后必须至少做记录、转译、上抛三者之一；禁止裸 `except:`。
- **必须**：错误码单源枚举 `ErrorCode`，成员含错误码值；**禁止**业务代码散落数字/字符串 code 与 message（对应"禁魔法字符串"）。
- **必须**：编码分段（6 位数字，首位=大类）：`1xxxxx` 系统错误 / `2xxxxx` 参数与校验 / `3xxxxx` 业务错误 / `4xxxxx` 认证授权 / `5xxxxx` 外部依赖。
- **必须**：命名 `UPPER_SNAKE_CASE`、模式 `{域}_{对象/动作}_{原因}`：`ORDER_NOT_FOUND`、`ORDER_NO_DUPLICATED`、`PARAM_INVALID`、`UNAUTHORIZED`、`SYSTEM_ERROR`、`DEPENDENCY_UNAVAILABLE`；**禁止** `E001`/`FAIL` 这类无信息量码名。
- **必须**：消息模板与 HTTP 状态与错误码一一对应（同一字典维护 `_MESSAGE` 与 `_HTTP_STATUS`），带业务上下文时用 `BizError(ErrorCode.ORDER_NOT_FOUND, order_no)` 参数化填充，不在调用处拼字符串。
- **必须**：错误响应结构固定 `{"code", "message", "traceId"}`，不含 `data`、不含堆栈；系统错误对外统一文案 + `traceId`，堆栈只进日志。
- **必须**：新增错误码前**先查重**（同语义同码；不同域不同段）；**只增不删**（语义变化标废弃，避免客户端与日志历史歧义）。
- **必须**：日志级别与错误类别匹配：业务错误与参数错误 `WARNING`/`INFO`，系统错误与外部依赖错误 `ERROR` + 告警；**禁止**同一个 code 表达两种以上含义、用异常做正常流程控制。

## 11. 日志与可观测

- **必须**：统一标准库 `logging`（禁用 `print` 输出业务日志）；日志格式与处理器在 `core/logging.py` 统一配置。
- **必须**：参数化占位符（`logger.info("order_created order_no=%s", order_no)`）；**禁止** f-string/`+` 拼接日志内容（含敏感值拼接）。
- **必须**：`logger.error`/`exception` 必须带 `exc_info=exc`（或 `exc_info=True`），否则丢失堆栈。
- **必须**：`traceId` 由中间件生成/透传（请求头 `X-Trace-Id`），写入 `ContextVar`、注入日志格式、并在错误响应中回带；日志与响应同源，便于对账。
- **必须**：健康检查端点（`/healthz`）不依赖下游；就绪检查（如需）单独提供并暴露关键依赖状态；探针路径登记在 `docs/ARCHITECTURE.md` §8。
- **应该**：错误日志含足够定位上下文（业务主键、`messageId`、`traceId`）；关键业务节点 `INFO`，可恢复问题 `WARN`，需要人介入 `ERROR`；生产默认 `INFO`。
- **禁止**：打印密钥/token/口令、完整身份证/手机号/银行卡等个人敏感信息；第三方报文只打 `traceId` 与状态摘要。

## 12. 配置与密钥

- **必须**：配置统一用 `pydantic-settings` 的 `Settings`（`core/config.py`），带环境变量前缀与 `.env` 支持；进程内单例（`@lru_cache`）避免请求期反复解析。
- **必须**：`.env` 不入库（只提交 `.env.example`，只含本地默认值与占位）；生产用部署平台的 Secret/配置中心注入。
- **必须**：密钥、口令、DSN 一律环境变量注入；DSN 默认值只能是不含凭据的本地地址；**禁止**真实密钥出现在代码、配置样例、日志与文档中。
- **必须**：生产关闭 `/docs`、`/redoc`、`/openapi.json`；调试开关（如 `debug`）默认 `False`，只能在非生产为真。
- **应该**：成组配置用嵌套模型 + `@field_validator` 校验（端口范围、必填项、URL 形态），配置错误在启动期即失败（fail fast），不留到运行期。
- **禁止**：用不同环境的配置文件互相拷贝造成的"环境泄漏"；在 `Settings` 之外散落 `os.environ` 读取。

## 13. 测试规范

- **必须**：新增逻辑带测试，测试与被测代码同提交；**无对应测试的业务方法不得合并**。
- **必须**（service 单测）：每个公有业务方法覆盖 ① 正常路径（断言返回值与副作用）② **每个**业务异常分支（断言 `ErrorCode` 与 HTTP 状态）③ 边界/空值/重复输入 ④ 写库等副作用的实际结果。
- **必须**（路由测试）：用 `httpx.ASGITransport` + `AsyncClient` 直连应用，通过 `app.dependency_overrides` 覆盖仓储/缓存等端口；**禁止**在测试中依赖真实 PostgreSQL/Redis/RabbitMQ 或本地手工服务。
- **必须**（异步）：`pytest-asyncio` 配置在 `pyproject.toml`（`asyncio_mode = "auto"` 与 `asyncio_default_fixture_loop_scope`），异步测试函数与异步夹具都无需逐个加标记；`asyncio_mode = "strict"` 时用 `@pytest.mark.asyncio`。
- **必须**（错误契约）：测试断言失败响应含正确 `code` 与 `traceId`、**不含堆栈/表名/SQL**；`ASGITransport(raise_app_exceptions=False)` 用于断言兜底的 500 响应本身。
- **必须**：测试不访问外网、不依赖执行顺序、不共享可变全局状态；每个测试可单独运行（单用例运行命令见 `AGENTS.md`「常用命令」）。
- **应该**：集成测试（真实 PostgreSQL/Redis/RabbitMQ，用 Testcontainers 或 docker compose）用标记隔离（`-m integration`），默认不跑；MQ 消费测试覆盖"重复投递同一条消息不产生副作用"。
- **应该**：测试数据用工厂/构建器生成并语义化（`an_order().with_status(PAID)`）；覆盖率只作参考，禁止为数字硬编无断言测试。

## 14. 提交前质量门

质量门由 **uv + 五个工具** 构成，本地与 CI 行为一致；工具与规则版本锁定在 `pyproject.toml` / `uv.lock`（**不在文档写版本号**）。**具体命令只在 `AGENTS.md`「测试与质量门」与「常用命令」维护一份**，本节只规定"检查什么、规则配在哪"。

| 检查项（工具） | 检查内容 | 配置要点 |
| --- | --- | --- |
| Lint（`ruff check`） | 错误、未用导入、import 顺序、命名、现代语法 | 规则集在 `[tool.ruff.lint] select`；中文团队关闭 `RUF001/002/003`（全角标点歧义） |
| 格式（`ruff format`） | 格式（Black 兼容风格） | `line-length`、`quote-style` 统一；修复后必须复跑校验 |
| 类型（`mypy`） | 类型正确性 | `strict = true`；`plugins = ["pydantic.mypy"]`；第三方缺类型用 overrides 精确豁免 |
| 依赖方向（`lint-imports`） | 分层单向 + 禁反向 + 禁越层 + 禁同层横向（§4 的机器强制） | 契约在 `[tool.importlinter]`（`layers` + 两条 `forbidden` + `independence`）；违规即门禁失败 |
| 测试（`pytest`） | 单元 + 接口测试 | `asyncio_mode = "auto"`；`--strict-markers` + 登记 `markers`；集成测试用标记隔离 |

- **必须**：上述五项检查由**单一入口命令**按序执行、**失败即停**（顺序与退出码收敛在项目自己的入口脚本里；入口命令只在 `AGENTS.md` 维护一份，本节不复制）——既保证"本地与 CI 同一条命令"，又满足"一个动作一条命令"。
- **例外（显式、有理由）**：CLI 入口脚本（如质量门入口）允许用 `print` 向终端输出结果；业务代码仍由 `ruff` 的 `T20` 拦 `print`，日志一律走 `logging`（§11）。该例外必须写在 `pyproject.toml` 的 `per-file-ignores` 里，不靠评审记忆。

**CI 与本地必须执行同一条质量门命令**：命令与执行顺序以 `AGENTS.md`「测试与质量门」为唯一来源，本节不复制。

规则：

- **必须**：任一工具失败 = 禁止提交/合并；不得用 `--fix`/`--exit-zero`/`--no-verify` 之类参数绕过（本地先修复再提交）。
- **必须**：`uv.lock` 与 `pyproject.toml` 必须同步提交；CI 必须以锁定校验模式安装依赖（不做隐式重新解析），命令见 `AGENTS.md`「常用命令」。
- **必须**：生成代码（如 `datamodel-codegen` 产物）从 lint/type 检查中排除（`ruff` 的 `extend-exclude` + `mypy` 的 `exclude`），避免"生成 → 改格式 → 再生成"循环。
- **应该**：把上述**五项检查**逐条接进 CI（模板仓库已接入，见 `.github/workflows/validate.yml`；项目内按同一组命令编排）；`pre-commit` 钩子为**待接入**项，规则只维护一份配置。

## 15. Git 提交规范

- **格式（必须）**：`<type>(<scope>): <subject>`，示例：
  - `feat(order): 增加取消订单接口`
  - `fix(auth): 修复 token 刷新竞态`
  - `test(order): 补充重复创建订单分支`
  - `build(deps): 升级 sqlalchemy 到 {{版本}}`（版本真相在 `pyproject.toml`/`uv.lock`，文档不写死）
- **type（必填，小写）**：`feat` 新功能 / `fix` 缺陷修复 / `docs` 文档 / `style` 格式（不影响逻辑）/ `refactor` 重构 / `perf` 性能 / `test` 测试 / `build` 构建与依赖 / `ci` CI 变更 / `chore` 杂项 / `revert` 回滚。
- **scope（可选，小写）**：影响模块（`order`、`auth`、`core`、`migrations`、`deps`）；跨模块大改可省略。
- **subject（必须）**：祈使句、≤50 字符、句末不加句号；语言（中文/英文）全库统一并在 `README`/`AGENTS.md` 声明，禁止中英混用。
- **body（需要时）**：写"为什么"（动机、取舍、影响），不复述 diff。
- **footer（需要时）**：破坏性变更 `BREAKING CHANGE: <说明>`；关联 issue `Closes #<编号>`。
- **必须遵守**：
  1. 一个提交只含一个逻辑变更；无关改动（格式、重构、依赖升级）拆分。
  2. 提交前通过 §14 质量门；禁止提交含密钥、`.venv/`、缓存目录、生成代码。
  3. 迁移脚本与 `docs/ARCHITECTURE.md` 的同步改动放在同一提交。
  4. 不 amend / rebase 已推送的提交（维护者明确要求除外）。
- **工具化（待接入）**：格式与 type 词表拟用 commitlint + pre-commit 钩子强制——**本类型当前尚无该配置**（现状与待办见模板仓库 `docs/enforcement-map.md`（项目内可自建同名登记表））；规则一律进配置，本文件不维护第二份。

## 16. 规则修订

- 本文件随团队实践演进：修改需在评审中说明理由，重大变更记录到 `docs/design-docs/`（ADR 或 `core-beliefs.md`），并同步真实项目 `AGENTS.md`「编码约定」章节。
- 工具能表达的规则（格式、import 顺序、类型严格度、commit 格式）一律写进 `pyproject.toml` 与钩子配置，**不在本文件重复**，避免双份漂移。

---

> 范本结束。采用步骤：裁剪与本团队不符的条目 → 放入 `docs/CODING_STANDARDS.md` → 在 `AGENTS.md`「编码约定」确认指向 → 把「提交前质量门」命令与真实 CI 对齐。
