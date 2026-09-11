# {{项目名}} 架构规范（ARCHITECTURE）

> 本文件是 **python-fastapi 类型交付物的一部分**（可复制范本）：复制到真实项目 `docs/ARCHITECTURE.md` 后填 `{{占位符}}`。它是**架构权威文档**，与 AGENTS.md（操作手册）、CODING_STANDARDS.md（编码细则）三权分立、内容不重复：**只写结构、边界、链路与决策，不复制命令表**。架构变更必须同步本文件并记 ADR（见 §10）。

---

## 1. 定位与范围 ★

- 一句话职责：{{如"订单与库存管理的 REST API 服务"}}；不做什么：{{不在本服务内实现的内容}}。
- 形态：{{单体 ASGI 服务（REST API）| 消息消费者 | 定时任务 | …}}；部署单位：{{一个容器镜像 / 多副本 + 前置网关}}。
- 运行边界：对外提供 HTTP API（{{/api/v1/**}}）与消息消费（{{队列清单见 §6}}）；依赖外部系统：{{数据库、缓存、消息、第三方服务逐项登记}}；不直接对外暴露数据库与中间件。

## 2. 技术栈与版本基线 ★

| 组件 | 版本约束 | 版本来源 | 升级/选型约束 |
| --- | --- | --- | --- |
| Python | `{{>=3.12,<3.15}}`（与 `requires-python` 一致） | `.python-version` + `pyproject.toml` 的 `requires-python` | 全库统一；仅用 uv 管理解释器 |
| FastAPI | `{{>=0.141,<0.142}}` | `pyproject.toml`（`uv.lock` 锁定） | 仍为 `0.x` 语义：升级前看 release notes，破坏性变更走兼容分支 |
| Starlette | {{传递依赖，不单独声明}} | `uv.lock` | 由 FastAPI 依赖解析决定 |
| Uvicorn | `{{>=0.52,<0.53}}` | `pyproject.toml` | ASGI 服务器；生产用 `--workers`，开发 `--reload` |
| Pydantic v2 + pydantic-settings | `{{>=2.13,<3}}` | `uv.lock` | v2 语法（`model_config`/`field_validator`）；禁止混用 v1 写法 |
| SQLAlchemy | `{{>=2.0.52,<2.1}}` | `uv.lock` | 必须 2.0 风格（`Mapped`/`select()`），禁 1.x `session.query` |
| Alembic | `{{>=1.19,<2}}` | `uv.lock` | 迁移脚本与模型 metadata 同源 |
| PostgreSQL | {{16 \| 17 \| 18}} | 部署环境/云 RDS | 时间列 `timestamptz`，字符集与排序规则建库时固定 |
| Redis | {{7.x \| 8.x}} | 部署环境 | key 布局见 §6；序列化统一 |
| RabbitMQ | {{4.x}} | 部署环境 | 拓扑见 §6；`aio-pika` 客户端 |
| 质量工具链 | ruff / mypy / pytest / pytest-asyncio / httpx / **import-linter** | `[dependency-groups] dev` | 规则只写在 `pyproject.toml` |

基线原则：**版本只由 `pyproject.toml` + `uv.lock` 决定**（本表只记录**约束形式**与决策，不承载版本号真相；具体版本一律查 `uv.lock`）；升级依赖一律用 `uv add`/`uv lock --upgrade-package` 并同提交 `uv.lock`。本表不写命令（命令见 `AGENTS.md`）。

## 3. 分层与模块划分 + 依赖方向 ★

依赖只允许**单向向下**（防调用环）：`api/routers → services → repositories → models`；`core/` 与 `db/` 为共享基础设施层。

**固定分层顺序（架构信条 B9，见 `design-docs/core-beliefs.md`）**：`type → config → repo/dao → service → runtime → ui`——依赖只允许由右向左（`ui` 最上、`type` 最底）。本类型映射：

| 规范层 | 本类型落地 |
| --- | --- |
| `ui` | `api/routers/`（路由，薄：schema 校验 → 调 service） |
| `runtime` | `main.py`（应用工厂）+ `api/deps.py`（装配根）——**装配根是唯一允许引用 `ui` 的组件**（已批准例外，见下） |
| `service` | `services/`（业务规则与用例编排；唯一抛 `BizError` 的层） |
| `repo or dao` | `repositories/`（SQLAlchemy 2.0 数据访问层） |
| `config` | `core/config.py`（`Settings`） |
| `type` | `models/`（表映射）+ `schemas/`（出入参契约） |
| **共享基础设施**（`db/`、`core/errors.py`、`core/logging.py`、`core/cache.py`、`core/mq.py`） | **不参与六层单向约束**：可被任意层依赖，自身**不得依赖业务层**（`core → services/api` 已由既有 forbidden 契约强制；`core → repositories` 待补，见模板仓库 `docs/enforcement-map.md`（项目内可自建同名登记表）） |

**已批准的判定例外**（与本文件 §3 判定表一致，不算越层）：`models/` 可依赖 `db/`（仅取 `Base`/`AuditMixin` 等类型工具）；`services/` 可依赖 `core/cache.py`（缓存端口与 key 模板）；`main.py`（`runtime`）可引用 `ui`（装配根职责）。

> 强制现状：`ui → service → repo → type` 与禁反向/越层/同层横向已由 import-linter 四条契约固化；**六层中 `config`/`runtime` 方向与共享基础设施的越界仍属「仅评审」**（缺口与补齐路线见模板仓库 `docs/enforcement-map.md`（项目内可自建同名登记表））。

```text
api/routers/  ->  services/  ->  repositories/  ->  models/  ->  PostgreSQL
      |                |                |
      +----------------+----------------+--> core/（config/errors/logging/cache/mq）
      +-------------------------------------> db/（engine/session factory/Base）
  禁止：反向依赖 / 越层依赖 / 同层横向互依赖（import-linter 契约固化）
  中间件客户端（AsyncSession、Redis、RabbitMQ）：仅 services 层及以下可用
```

模块划分（技术分层 + 按域分文件）：

| 模块/包 | 职责 | 备注 |
| --- | --- | --- |
| `api/` | `deps.py` 组装根 + `routers/` 路由；只做 HTTP 适配 | 薄；不碰 session、不拼 SQL |
| `services/` | 业务规则、用例编排、事务发起、抛 `BizError` | 域内自治，域间不横向依赖 |
| `repositories/` | `Protocol` 端口 + SQLAlchemy 2.0 实现；`select()`/SQL 唯一所在 | 依赖终点，不依赖 service |
| `models/` | 表映射与枚举（`DeclarativeBase` 子类） | 纯载体，禁直接作出参 |
| `schemas/` | Pydantic v2 入参/出参与分页结构 | 与 models 严格分离 |
| `core/` | `config`（Settings）/`errors`（ErrorCode、处理器）/`logging`（traceId）/`cache`（key）/`mq`（拓扑） | 可被任意层依赖，不反向依赖业务 |
| `db/` | 引擎、会话工厂、`Base` | 无业务语义 |
| `migrations/` | Alembic `env.py` + `versions/` | 只增不改 |

跨域协作合规出口（按优先级）：① 公共能力下沉 `core/`；② RabbitMQ 事件解耦；③ 显式编排服务 `{{域}}FlowService`（唯一允许依赖多个领域 service 的上层组件）。违规 = 评审打回。

依赖方向判定表（评审与 import-linter 共用同一口径）：

| 起点 | 允许依赖 | 明确禁止 |
| --- | --- | --- |
| `api/routers/` | `services/`、`schemas/`、`core/`、`api/deps.py` | `repositories/`、`models/`、`db/`、`AsyncSession` |
| `services/` | `repositories/`、`models/`、`schemas/`、`core/`、同域内私有模块 | 其它领域的 `services/`、`api/` |
| `repositories/` | `models/`、`db/`、`core/` | `services/`、`api/`、`schemas/`、`core/errors.py` 的 `BizError` |
| `models/` | `db/`、`core/`（仅类型工具） | 其余全部 |
| `core/`、`db/` | 仅标准库与第三方库 | 任何业务模块 |

## 4. 关键链路 ★

- **同步读/写**：`HTTP → Router（schema 校验、鉴权依赖）→ Service（业务规则、错误分类）→ Repository（select()/ORM）→ PostgreSQL`；事务由 `api/deps.py` 的会话依赖包裹一个请求；异常统一由 `core/errors.py` 注册的处理器转 `{code, message, traceId}`。
- **缓存链路**：读 `GET` 优先 Redis（key + TTL）→ 未命中回源数据库 → 回填（击穿时互斥回填）；**写：先更数据库再删缓存**；缓存 key 与 TTL 登记见 §6，一致性方案见 ADR。
- **消息链路**：Producer（publisher confirm）→ Exchange → Queue → 消费者（手动 ack + 幂等 + 有限重试）→ 失败进 DLQ 并告警。
- **错误处理链路**：业务错误 `BizError(ErrorCode)` → `WARNING` + 业务码 + 对应 HTTP 状态；参数错误由 `RequestValidationError` 处理器统一转 `2xxxxx`；系统/外部错误兜底 `ERROR` + 告警，对外只给 `message + traceId`（堆栈只进日志）。
- **启动链路**：`create_app()` → 加载 `Settings`（校验失败即启动失败）→ 配置日志 → 注册中间件（traceId）→ 注册异常处理器 → 注册路由；连接池与客户端惰性创建，启动期不强制建连（健康检查与就绪检查区分见 §8）。

单请求生命周期（从进入应用到写出响应）：

1. 中间件生成/透传 `X-Trace-Id`，写入 `ContextVar` 与 `request.state`。
2. 路由匹配 → 依赖解析：`get_session`（打开会话与事务）→ `get_order_repository`（注入仓储）→ `get_cache`（注入缓存端口）→ `get_order_service`（组装 service）。
3. 入参由 Pydantic schema 校验（失败即抛 `RequestValidationError`，由处理器转 `2xxxxx`）。
4. service 执行业务规则；需要时读缓存、调仓储、写库后删缓存。
5. 返回值按 `*Response` 序列化；依赖退出时提交事务（异常则回滚）。
6. 异常路径：`BizError` → 业务码与对应 HTTP 状态；其余异常 → 兜底 `1xxxxx` 且只记日志。
7. 响应头回带 `X-Trace-Id`，与日志、下游消息信封同源。

## 5. 数据架构 ★

- **表与命名**：表名 `{{带 t_ 前缀或不带，全库一致}}`、字段 `snake_case` ↔ 属性 `snake_case`；审计字段固定 `created_at`/`updated_at`（`server_default=func.now()`，`timestamptz`）；软删除如需则全库统一（`deleted_at`）并说明查询过滤策略；主键策略 {{自增 BIGSERIAL | 雪花 | UUID}}（决策理由记 ADR）。
- **字典表（可选，按触发器建）**：需要运营维护的可枚举数据落 `{{t_dict_type}}` + `{{t_dict_item}}`（`(type_code, item_code)` 唯一，`UPPER_SNAKE_CASE`）；**稳定状态机用 Python 枚举不落库**（细节见 CODING_STANDARDS §3）。
- **迁移纪律**：结构变更以 `migrations/versions/{{n}}__{{描述}}.py` 版本化提交；**已应用脚本不得改写**，只允许追加；发布顺序 **DB 迁移先行**；迁移命令（生成/应用/回滚/离线预览）见 `AGENTS.md`「常用命令」，本文件只规定纪律。
- **索引与慢查询基线**：where/order by 列按需建索引（最左前缀）；列表与聚合查询经 `EXPLAIN (ANALYZE, BUFFERS)` 核对；慢查询治理登记到 `exec-plans/tech-debt-tracker.md`。
- **连接与事务**：engine 进程内单例、连接池参数按部署副本数与数据库上限设定；事务粒度 = 一个请求；长事务与事务内远程调用一律禁止（见 CODING_STANDARDS §7）。

字段与约束约定（全库统一，评审按此核对）：

| 项 | 约定 | 说明 |
| --- | --- | --- |
| 主键 | `id`，类型 {{BIGINT 自增 / BIGINT 雪花 / UUID}} | 策略一经选定不得混用；决策记 ADR |
| 审计字段 | `created_at`、`updated_at`（`timestamptz`，`server_default=func.now()`） | 由数据库默认值填充，应用不手工赋值 |
| 软删除 | {{`deleted_at`（可空时间戳）或不做软删除}} | 若做软删除，唯一索引需带条件（部分唯一索引），否则删除后无法重建同键数据 |
| 时间 | 一律 UTC 存储，展示层转换 | 禁止混用本地时间与 `timestamp without time zone` |
| 金额 | 以最小货币单位整数存储（分） | 禁用浮点 |
| 枚举列 | {{PostgreSQL 原生 enum / varchar + 应用层校验}} | 与 Python `StrEnum` 值逐一对应；新增枚举值需迁移 |

结构变更工作流（必须按序）：

1. 改 `models/`（`Mapped[...]` + `mapped_column(...)`）。
2. 生成迁移脚本草稿（命令见 `AGENTS.md`「常用命令」）。
3. **人工审阅**草稿：补索引/约束、修正枚举与数据回填、补 `downgrade()`。
4. 离线渲染 DDL 作为评审材料（不连数据库，命令见 `AGENTS.md`「常用命令」）。
5. 迁移脚本与模型改动**同一提交**；发布时先迁移后发应用。

## 6. 中间件设计 ★

### Redis：key 布局登记表（全库唯一事实源）

| 用途 | key 模式 | TTL | 说明 |
| --- | --- | --- | --- |
| 缓存 | `{{域}}:{{对象}}:{{标识}}`（如 `order:detail:{order_no}`） | {{10 分钟 + 随机抖动}} | 读回源回填；写库后删 |
| 幂等 | `idem:{{域}}:{{操作}}:{{标识}}` | {{24 小时}} | 写接口与消息消费共用 |
| 分布式锁 | `lock:{{域}}:{{操作}}:{{标识}}` | {{按业务超时 + 续期}} | 用库自带 lock 实现，禁裸 SETNX |
| {{限流 / 布隆（按需）}} | `rate:{{域}}:{{维度}}:{{标识}}` | {{1 分钟}} | 按需实现并登记 |

序列化全库统一：{{JSON 字符串（`decode_responses=True`）}}；key 模板与取键函数集中在 `core/cache.py` 的 `CacheKeys`。**本表条目增删属架构变更**。

### RabbitMQ：拓扑表（全库唯一事实源，声明集中在 `core/mq.py`）

| 交换机 | 类型 | RoutingKey | 队列 | 消费者 | 幂等键 | DLX / DLQ |
| --- | --- | --- | --- | --- | --- | --- |
| `{{svc}}.{{域}}.exchange` | topic | `{{域}}.{{事件}}`（如 `order.created`） | `{{svc}}.{{域}}.{{事件}}.queue`（多消费者加 `.消费方`） | `{{域}}_consumer.py` | {{业务唯一索引 / Redis `idem:` 键}} | DLX `{{svc}}.{{域}}.dlx` -> DLQ `...queue.dlq` |

消息信封统一顶层字段：`messageId`(hex) / `traceId` / `type` / `timestamp`(ISO8601 UTC) / `data`；JSON 序列化。消费端 prefetch 与重试次数在 `core/mq.py` 常量中声明。

## 7. 安全 ○

- 认证/授权方案：{{JWT / 会话 / 网关统一认证}}；依赖注入实现鉴权（`Depends`），权限矩阵（角色 ↔ 端点）登记位置：{{docs/references/ 或权限模块文档}}。
- 生产环境：`/docs`、`/redoc`、`/openapi.json` 关闭（由 `Settings` 开关控制，默认关闭）；错误响应不含堆栈、表名、SQL；CORS 只允许白名单来源。
- 密钥管理：一律环境变量/Secrets 注入，禁止入库入日志；`.env` 不入库；示例值与文档同样受限。
- 输入与依赖安全：请求体大小与字段长度设上限；上线前跑依赖漏洞扫描（{{工具与频率}}）；个人敏感数据脱敏规则见 CODING_STANDARDS §11。

上线前安全自查（逐项确认，未确认不得发布）：

| # | 检查项 | 判据 |
| --- | --- | --- |
| S1 | 交互式文档与 OpenAPI | 生产 `/docs`、`/redoc`、`/openapi.json` 均为 404 |
| S2 | 错误响应 | 不含堆栈、SQL、表名、内部路径；含 `traceId` |
| S3 | 认证与授权 | 每个端点显式声明所需权限依赖；无"默认放行"路由 |
| S4 | CORS | 只允许白名单来源，不使用通配符叠加凭据 |
| S5 | 密钥 | 全部来自环境变量/Secret；仓库与镜像中无凭据 |
| S6 | 依赖 | 依赖漏洞扫描无高危未处置项 |
| S7 | 日志 | 无密钥与个人敏感字段输出（抽样检查） |

## 8. 可观测 ○

- `traceId`：中间件生成/透传（`X-Trace-Id`），写入 `ContextVar`、注入日志格式、随响应头与错误体回带、随消息信封跨进程传递。
- 健康检查：`/healthz`（存活，不探测下游）与 `{{/readyz}}`（就绪，探测数据库/缓存/消息，按需实现）。
- metrics 与告警项：请求量与 P95 延迟、系统错误率（`1xxxxx`）、外部依赖错误率（`5xxxxx`）、消息积压与死信深度、数据库连接池水位；告警接收方：{{值班渠道}}。
- 日志级别与采样策略：{{生产 INFO + 慢请求采样}}；日志脱敏见 CODING_STANDARDS §11。

## 9. 环境、配置与部署 ○

| 环境 | 用途 | 差异要点 |
| --- | --- | --- |
| local | 本地开发 | 连接 compose 起的 PostgreSQL/Redis/RabbitMQ；`/docs` 开；`.env` 本地文件 |
| test | 集成测试/联调 | {{同上，独立测试库}}；集成测试标记 `-m integration` |
| prod | 生产 | 云数据库与中间件、Secret 注入；`/docs` 关；多副本 + 网关 |

环境准备（安装 uv/Python、起中间件、首次下载耗时）写在本项目 `README.md`，不写进 `AGENTS.md`。

发布顺序：**DB 迁移先行 → 新版本应用**；涉及缓存 key 或消息契约的变更按兼容方式演进（先双写/双读或新增 key/队列，稳定后再清理旧路径）；破坏性迁移需先备份并准备回滚脚本。

发布检查清单（按序执行，任一步失败即停止并回滚）：

1. 质量门全绿（命令见 `AGENTS.md`「测试与质量门」），且 CI 以锁定校验模式安装依赖通过。
2. 数据库迁移已在预发环境演练：升级与回滚脚本均可执行（命令见 `AGENTS.md`「常用命令」）。
3. 生产按 **DB 先行** 顺序执行迁移，并确认版本表（`alembic_version`）已推进到目标 revision。
4. 发布应用新版本，观察就绪探针与错误率（`1xxxxx`/`5xxxxx`）。
5. 涉及消息契约时确认新队列已声明、旧消费者仍在消费；稳定后再下线旧路径。
6. 记录发布结果（版本、迁移 revision、观察窗口）到 `exec-plans/` 对应计划。

## 10. 技术决策记录（ADR）○

- 索引：`docs/design-docs/index.md`；ADR 命名 `{{YYYYMMDD}}-{{简述}}.md`。
- 必记决策（一经确定必须留痕）：主键策略、软删除与唯一索引冲突处理、缓存一致性方案（写后删/延迟双删/版本号）、消息幂等键与顺序性取舍、事务边界粒度、同步还是异步（`async def` 与线程池的边界）、依赖管理与锁文件策略、是否引入 import-linter 或 Testcontainers。
- 新增决策流程：方案 → 权衡 → 记 ADR → 同步本文件相关小节 → 评审。

---

> 范本结束。采用步骤：填入 `{{占位符}}` → 按实际裁剪 ○ 章节 → 与 `AGENTS.md`/`CODING_STANDARDS.md` 核对一致性 → 变更时同步并记 ADR。
