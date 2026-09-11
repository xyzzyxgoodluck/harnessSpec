# {{项目名}}

{{一句话：这个项目做什么、不做什么。例："订单与库存的 REST API 服务。"}}

- 技术栈：Python {{3.12 | 3.13 | 3.14，以 pyproject.toml 的 requires-python 为准}} / **FastAPI**（ASGI，异步） / **Pydantic v2**（含 pydantic-settings） / **SQLAlchemy 2.0**（异步 ORM）+ **Alembic**（迁移） / **PostgreSQL**
- 中间件：**Redis**（缓存/{{锁}}/幂等）、**RabbitMQ**（`aio-pika`，消息）；API 文档：**OpenAPI**（FastAPI 内置，`/docs`、`/openapi.json`，仅非生产开启）
- 依赖与虚拟环境：**uv**（`pyproject.toml` + `uv.lock`；禁 pip / poetry / conda 混用）；形态：{{REST API 服务 | 消息消费者 | 定时任务 | …}}

## 环境与版本约束

- 解释器最低版本以 `pyproject.toml` 的 `requires-python` 为单一来源，实际版本由 `.python-version` 决定；依赖版本以 `uv.lock` 为单一来源。
- 工具链在 `[dependency-groups] dev` 锁定：**ruff**（lint + format）、**mypy**（`strict = true`）、**pytest** + **pytest-asyncio**、**httpx**（接口测试用 `ASGITransport`）、**import-linter**（依赖方向契约）；规则配置全部写在 `pyproject.toml`。
- 环境准备（安装 uv/Python、中间件的人工安装与排障、首次下载耗时）归项目 `README.md` 与 `docs/ARCHITECTURE.md` §9；本文件只在「快速开始」保留一条**本地编排入口** `docker compose up -d`（编排定义见 `docker-compose.yml`）。
- 密钥红线：密钥/token/口令只经环境变量注入，禁止进代码、进 `.env`（`.env` 不入库）、进日志与文档；示例值同样受限。

## 快速开始

```bash
# 在项目根执行：
uv sync --locked                     # 按 uv.lock 创建 .venv 并安装依赖（含 dev 组）
docker compose up -d                 # 启动 PostgreSQL / Redis / RabbitMQ
uv run alembic upgrade head          # 应用数据库迁移（DB 先行）
uv run uvicorn {{包名}}.main:app --reload --port {{8000}}   # 启动开发服务
curl http://localhost:{{8000}}/healthz
# OpenAPI 文档（非生产，{{调试开关}} 为真时开放）：http://localhost:{{8000}}/docs
uv run pytest -q                     # 全部测试
```

## 常用命令

> 未注明时均在项目根执行；一律用 `uv run` 前缀，保证走 `uv.lock` 锁定的依赖与解释器。静态检查与质量门明细见 `docs/CODING_STANDARDS.md` §14。

| 目的 | 命令 | 备注 |
| --- | --- | --- |
| 同步依赖 | `uv sync --locked` | CI 必须带 `--locked`，防止锁定文件漂移 |
| 校验锁定文件 | `uv lock --check` | `pyproject.toml` 与 `uv.lock` 不一致即失败 |
| 添加运行时依赖 | `uv add {{httpx}}` | 同时更新 `pyproject.toml` 与 `uv.lock` |
| 添加开发依赖 | `uv add --dev {{pytest-cov}}` | 落入 `[dependency-groups] dev` |
| 启动开发服务 | `uv run uvicorn {{包名}}.main:app --reload --port {{8000}}` | :{{端口}}；生产去掉 `--reload`，用 `--workers {{N}}` |
| 全部测试 | `uv run pytest -q` | 单元 + 接口测试，不依赖真实中间件 |
| 单个测试 | `uv run pytest tests/{{test_orders_api.py}}::{{test_create_order_returns_201}}` | 精确定位失败用例 |
| 覆盖率 | `uv run pytest -q --cov={{包名}} --cov-report=term-missing` | 需先 `uv add --dev pytest-cov` |
| lint | `uv run ruff check .` | 本地修复加 `--fix` |
| 格式校验 / 修复 | `uv run ruff format --check .` / `uv run ruff format .` | 修复后必须复跑校验 |
| 类型检查 | `uv run mypy src tests` | `strict = true`，配置在 `pyproject.toml` |
| 依赖方向契约 | `uv run lint-imports` | 契约定义在 `pyproject.toml` 的 `[tool.importlinter]`（分层单向 + 禁反向 + 禁越层 + 同层横向）；违规即失败 |
| **质量门（单一入口）** | `{{质量门入口命令}}` | **约定**：把 ruff check / ruff format --check / mypy / lint-imports / pytest 封装成**一条命令**（顺序与失败即停写进项目自己的入口脚本，推荐形如 `uv run python scripts/gate.py`），入口命令填在本行；CI 调同一条命令。检查项口径见 `docs/CODING_STANDARDS.md` §14 |
| 生成迁移脚本 | `uv run alembic revision --autogenerate -m "{{描述}}"` | 生成后**必须人工审阅**再提交 |
| 应用迁移 | `uv run alembic upgrade head` | DB 先行；回滚 `uv run alembic downgrade -1` |
| 离线预览迁移 DDL | `uv run alembic upgrade head --sql` | 不连数据库，评审用 |
| 生成代码 | {{代码生成命令}}（如 `uv run datamodel-codegen {{参数}}`） | 生成物**勿手改**，登记 `docs/generated/index.md` |

## 目录结构与架构

> 项目结构以**下方目录树为唯一描述**（职责随注释内联）；命名与依赖方向见「编码约定」。

```text
{{项目根}}/
  pyproject.toml          # 依赖声明 + ruff/mypy/pytest 配置的唯一来源
  uv.lock                 # 锁定文件：必须提交；禁止手改（改依赖用 uv add/uv lock）
  .python-version         # 解释器版本；uv 据此选取运行时
  docker-compose.yml      # 本地 PostgreSQL/Redis/RabbitMQ 编排（快速开始入口；生产由部署平台提供）
  .env.example            # 本地配置样例（真实 .env 不入库）
  alembic.ini             # 迁移配置：只放非连接项，连接串由 migrations/env.py 从 Settings 读
  migrations/
    env.py                # 迁移运行环境（异步引擎 + 离线模式）
    versions/             # 迁移脚本 {{revision}}_{{描述}}.py（如 0001_create_order.py；只增不改）
  src/{{包名}}/
    main.py               # 应用工厂 create_app() + 模块级 app（uvicorn 入口）
    api/
      deps.py             # 组装根：engine/session/cache/service 的唯一装配点
      routers/            # 路由层（薄）：schema 校验 → 调 service → 返回 schema
    services/             # 业务规则与用例编排；事务边界的发起方；唯一抛 BizError 的层
    repositories/         # SQLAlchemy 2.0 数据访问；select()/SQL 只允许出现在此层
    models/               # 表映射（DeclarativeBase/AuditMixin），纯载体，禁直接作出参
    schemas/              # Pydantic v2 入参（*Request/*Query）与出参（*Response）
    core/                 # config（Settings）/ errors（ErrorCode）/ logging（traceId）/ cache（key）/ mq（拓扑）
    db/                   # 引擎、会话工厂、Base
  tests/                  # pytest：conftest.py 夹具 + fakes.py 内存假实现
  scripts/                # {{运维与生成脚本；质量门单一入口放在此处，如 gate.py}}
  CHANGELOG.md            # 对外版本变更史（Keep a Changelog；首个对外发布时建立，与 docs/design-docs 分工：变更 vs 决策）
  docs/                   # 知识库：固定组成部分（★=必建目录；括号内为内容触发器）
    CODING_STANDARDS.md   # ★ 编码规范（本文件全部"细则"的唯一权威）
    ARCHITECTURE.md       # ★ 架构总览（分层、关键链路、中间件拓扑、ADR 索引）
    design-docs/          # ★ 设计决策/ADR：index.md + core-beliefs.md（触发：首个决策/ADR 定案）
    product-specs/        # ★ 需求规格：index.md + TEMPLATE.md（触发：首个需求立项）
    exec-plans/           # ★ 执行计划与技术债：index.md + tech-debt-tracker.md + active/ + completed/（触发：多轮任务或技术债）
    generated/            # ★ 自动生成物：index.md 为唯一手写索引，其余只读（触发：首个生成物入库）
    references/           # ★ 外部资料登记：index.md（触发：首次引入外部资料）；浓缩版命名 *-llms.txt
  .venv/                  # uv 创建的虚拟环境：禁止提交
```

约定：模型用 `Mapped[...]` + `mapped_column(...)` 显式标注；主键与审计字段全库统一；`docs/` 核心文件与五项目录不得为空壳（每目录至少一个最小入口文件）；依赖方向 `routers → services → repositories → models` 单向（防环，详见「编码约定」）。

> **六个固定组成部分的落点**（细则见本类型维护仓库的 `docs/fixed-docs.md`；采用本模板的项目以本文件与 `docs/` 为准）：① docs 布局 → 上方目录树 + `docs/`；② Git 提交规范 → [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) §15；③ 架构骨架 → [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)；④ 需求规格 → [`docs/product-specs/index.md`](docs/product-specs/index.md)；⑤ 架构信条（6 条）→ [`docs/design-docs/core-beliefs.md`](docs/design-docs/core-beliefs.md) + `docs/ARCHITECTURE.md` §3；⑥ **质量门控** → 本文件「测试与质量门」（**单一入口命令**、CI 跑同一条）+ [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) §14 的检查项口径。**对外版本变更记根级 `CHANGELOG.md`，技术决策记 `docs/design-docs/`，需求与验收记 `docs/product-specs/`——三者不互抄。**

关键链路：同步 `HTTP → Router → Service（业务规则/事务）→ Repository（SQLAlchemy 2.0）→ PostgreSQL`；缓存：读 Redis（key + TTL）未命中回源，**写库后删缓存**；消息：`aio-pika` 生产（publisher confirm）→ Exchange → 队列 → 消费者**手动 ack + 幂等**→ 失败有限重试 → DLQ。异常统一由注册在 `core/errors.py` 的处理器转 `{code, message, traceId}`。

## 编码约定（必须摘要；细则以 `docs/CODING_STANDARDS.md` 为准）

- **命名**（§1）：模块/函数/变量 `snake_case`，类 `PascalCase`，常量 `UPPER_SNAKE_CASE`；包名全小写无下划线；测试文件 `test_{{模块}}.py`、测试函数 `test_{{行为}}`。禁拼音缩写与同义混用。
- **类型注解**（§2）：所有函数（含 `__init__`、夹具、测试）必须写全参数与返回注解；新代码一律通过 `mypy --strict`，禁止 `# type: ignore` 无原因标注。
- **分层职责**（§3）：路由薄——不做业务判断、不碰 session、不拼 SQL；service 承载业务规则与事务；repository 只做数据访问（方法前缀 `select`/`insert`/`update`/`delete` + 宾语）。
- **依赖方向**（§4）：只允许 `routers → services → repositories → models` 单向；禁反向、越层、**同层横向互依赖**（防调用环）；跨域协作靠 `core/` 下沉、RabbitMQ 事件或显式编排服务。**由 `import-linter` 四条契约强制**（分层单向 `layers` + 禁反向 + 禁越层 + 同层横向 `independence`；`uv run lint-imports` 已纳入质量门），每条均以"故意违规即红"验证过；新增同级模块时必须同步加入 independence 契约。
- **接口与 Schema**（§5）：入参/出参一律 Pydantic 模型（`extra="forbid"`），**禁止**直接返回 ORM 实体；分页统一 `PageResult[T]`；端点必须写 `summary` 与错误响应模型。
- **异步纪律**（§6）：路由/服务/仓储全链路 `async def`；**禁止**在异步路径调用阻塞 IO（`requests`、`time.sleep`、同步驱动、同步 `Session`），需阻塞时走 `anyio.to_thread.run_sync`。
- **数据库与迁移**（§7）：会话与事务由 `api/deps.py` 提供（一请求一会话一事务），service 不手工 `commit()`；结构变更只走 Alembic 迁移，**已应用脚本不得改写**；`select()` 显式列，禁无上限全表查询。
- **Redis**（§8）：key 布局 `{{域}}:{{对象}}:{{标识}}`（`lock:`/`idem:` 前缀 + TTL），常量集中在 `core/cache.py`；**写库后删缓存**；禁裸 `SETNX` 手写锁。
- **RabbitMQ**（§9）：交换机/路由键/队列/死信全部登记在 `core/mq.py`，禁魔法字符串；消费手动 ack + **幂等**（业务唯一索引或 Redis 幂等键）；消息体 JSON，含 `messageId`/`traceId`。
- **异常与错误码**（§10）：业务失败抛 `core/errors.py` 的 `BizError(ErrorCode.xxx, ...)`；错误码单源枚举、6 位分段（`1xxxxx` 系统 / `2xxxxx` 参数 / `3xxxxx` 业务 / `4xxxxx` 认证授权 / `5xxxxx` 外部依赖）、命名 `{域}_{对象}_{原因}`、只增不删；对外只给 `code/message/traceId`，**不泄漏堆栈、SQL、表名**。
- **日志**（§11）：统一 `logging` + 参数化占位符（禁字符串拼接）、`log.error(..., exc_info=...)`；`traceId` 由中间件写入 ContextVar 并进日志格式与错误响应；禁打印密钥与个人信息。
- **配置与密钥**（§12）：统一 `pydantic-settings` 的 `Settings`（前缀 + `.env`），字段带类型与默认值；密钥一律环境变量注入；生产关闭 `/docs`、`/openapi.json`。
- **测试要求**（§13）：每个 service 公有方法必须有对应测试（正常路径 + 每个 `ErrorCode` 分支 + 边界）；路由用 `httpx.ASGITransport` + `dependency_overrides` 覆盖端口，**测试不连真实 PostgreSQL/Redis/RabbitMQ、不访问外网**。
- **Git 提交**（§15）：`<type>(<scope>): <subject>`；type：`feat/fix/docs/style/refactor/perf/test/build/ci/chore/revert`；subject 语言全库统一；一个提交一个逻辑变更；提交前过质量门。

## 测试与质量门

- 合并/提交前必须全绿（CI 与本地**同一条命令**）：`{{质量门入口命令}}` —— 该项把五项检查（ruff check / ruff format --check / mypy / lint-imports / pytest）按序封装、**失败即停**；单项检查见「常用命令」，检查项口径见 `docs/CODING_STANDARDS.md` §14。
- CI 另跑 `uv sync --locked`：锁定文件未同步即失败。
- 新端点完成后用 `curl` 真实调用成功与失败分支，把命令与期望响应记录进 PR 描述；curl 冒烟**不替代**自动化测试。
- 涉及 PostgreSQL/Redis/RabbitMQ 的集成测试放在独立标记下（如 `-m integration`），本地无中间件时默认不跑。

## 工作流与发布 {{（按需保留）}}

- 分支 {{feature/* → main}}、版本 {{SemVer}}；**数据库迁移先于应用发布**；涉及缓存 key 或消息契约的变更按兼容方式演进（先双写/双读，再清理）。

## 约束、禁区与陷阱（红线；完整陷阱清单见 `docs/CODING_STANDARDS.md` §6–§14）

- 禁止提交/修改（括号内为原因）：`.venv/`、`__pycache__/`、`.mypy_cache/`、`.ruff_cache/`、`.pytest_cache/`（全部由工具重建，入库只产生无意义 diff 与合并冲突）；`.env`（含本地凭据，只提交 `.env.example`）；`uv.lock` 的手工编辑（改依赖用 `uv add` / `uv remove` / `uv lock`，手改会与 `pyproject.toml` 失配并被 `uv sync --locked` 直接拒绝）。
- 生成物与已归档内容只读（括号内为原因）：`migrations/versions/` 中已应用的脚本（改写会让各环境迁移状态与校验和不一致，只能追加新脚本）；`docs/generated/` 下除 `index.md` 以外的文件（下一次生成即覆盖）；`docs/design-docs/` 中已定案 ADR（结论变化时追加新 ADR 并注明取代关系）。
- 密钥、内网地址、真实账号不入代码/配置/日志/文档；测试禁止连生产库与生产中间件。
- 错误码与 Redis key 前缀先查重再新增；消息拓扑变更必须同步 `docs/ARCHITECTURE.md` §6 并记 ADR。
- 版本纪律：Python 与全部依赖只由 `pyproject.toml` + `uv.lock` 决定，禁止在文档或脚本里另写版本号；`mypy`/`ruff` 规则只改 `pyproject.toml`，不在文档维护第二份。
- 陷阱细则（`pytest-asyncio` 的 `asyncio_mode`/事件循环作用域、`ruff` 与中文全角标点的 `RUF001/002/003` 取舍、`alembic.ini` 必须 ASCII-only（Windows 按 GBK 读）、SQLAlchemy 2.0 阻塞调用与 `greenlet`、Redis `decode_responses` 与 `setnx` 锁续期、RabbitMQ 重复投递与死信堆积、Windows 下 `&&` 不可用于 PowerShell 5.1）→ 见 `docs/CODING_STANDARDS.md` §6–§14。

## 参考链接

- 架构权威：[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ｜ 编码细则：[docs/CODING_STANDARDS.md](docs/CODING_STANDARDS.md)
- 知识库入口：设计决策 [docs/design-docs/index.md](docs/design-docs/index.md) ｜ 需求规格 [docs/product-specs/index.md](docs/product-specs/index.md) ｜ 执行计划与技术债 [docs/exec-plans/index.md](docs/exec-plans/index.md) ｜ 生成物 [docs/generated/index.md](docs/generated/index.md) ｜ 外部资料 [docs/references/index.md](docs/references/index.md)
- FastAPI：https://fastapi.tiangolo.com/ ｜ Pydantic v2：https://docs.pydantic.dev/latest/ ｜ SQLAlchemy 2.0：https://docs.sqlalchemy.org/en/20/ ｜ Alembic：https://alembic.sqlalchemy.org/
- uv：https://docs.astral.sh/uv/ ｜ ruff：https://docs.astral.sh/ruff/ ｜ mypy：https://mypy.readthedocs.io/ ｜ pytest：https://docs.pytest.org/
- 本类型适用范围与版本对照：见本目录 `README.md`
