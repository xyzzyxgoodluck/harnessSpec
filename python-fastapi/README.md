# python-fastapi 类型 —— 说明

本目录对应项目类型：**FastAPI + Pydantic v2 + SQLAlchemy 2.0（Alembic 迁移）+ PostgreSQL 服务端**（集成 Redis 与 RabbitMQ，依赖与虚拟环境用 uv 管理）。`python-fastapi/AGENTS.md` 为该类型的规范/模板交付物。

## 适用范围

- **适用**：采用以下**固定技术栈**的服务端应用 —— REST API、消息消费者/生产者、定时任务、带 Web 层的后台服务：
  - Web/框架：FastAPI（ASGI，异步），Python 3.12+
  - 校验/配置：Pydantic v2 + pydantic-settings
  - 持久化：SQLAlchemy 2.0 异步 ORM + Alembic 迁移 + PostgreSQL
  - 中间件：Redis（缓存/锁/幂等）、RabbitMQ（`aio-pika`）
  - 依赖与虚拟环境：**uv**（`pyproject.toml` + `uv.lock`；唯一依赖管理器）
  - 质量门：ruff（lint + format）+ mypy（strict）+ pytest / pytest-asyncio / httpx
- **主版本线**（**最后核实：2026-09**，逐条来自 PyPI / python.org / GitHub Releases，链接见「参考来源」）：

  | 组件 | 当前版本 | 依据 | 备注 |
  | --- | --- | --- | --- |
  | Python | **3.14.7**（最新稳定线）；3.13.15 / 3.12.14 为保守选择 | [python.org release API](https://www.python.org/api/v2/downloads/release/?is_published=true) | 3.15.0rc2 仍为预发布，不作为默认线 |
  | FastAPI | **0.141.1** | [PyPI](https://pypi.org/project/fastapi/) / [GitHub](https://github.com/fastapi/fastapi/releases) | 仍是 `0.x` 版本线，升级前读 release notes |
  | Pydantic | **2.13.5**（pydantic-settings **2.15.0**） | [PyPI](https://pypi.org/project/pydantic/) | v1 写法（`class Config`/`validator`）全部禁用 |
  | SQLAlchemy | **2.0.52** | [PyPI](https://pypi.org/project/SQLAlchemy/) / [官方 blog](https://www.sqlalchemy.org/blog/) | 2.0 风格 API（`Mapped`/`select()`） |
  | Alembic | **1.19.2** | [PyPI](https://pypi.org/project/alembic/) | |
  | uvicorn | **0.52.4** | [PyPI](https://pypi.org/project/uvicorn/) | ASGI 服务器 |
  | uv | **0.12.13**（本项目样例实测机为 0.11.28） | [GitHub Releases](https://github.com/astral-sh/uv/releases) | 命令在 0.11/0.12 两线一致 |
  | ruff | **0.16.7** | [GitHub Releases](https://github.com/astral-sh/ruff/releases) | 同版本提供 lint 与 format |
  | mypy | **2.3.1** | [PyPI](https://pypi.org/project/mypy/) | 2.x 线，`strict = true` 可用 |
  | pytest / pytest-asyncio | **9.1.1** / **1.4.0** | [PyPI](https://pypi.org/project/pytest/) / [PyPI](https://pypi.org/project/pytest-asyncio/) | 1.x 线保留 `asyncio_mode`；已移除 `event_loop` 夹具 |
  | httpx | **0.28.1** | [PyPI](https://pypi.org/project/httpx/) | 接口测试用 `ASGITransport` |
| import-linter | **2.15** | [PyPI](https://pypi.org/project/import-linter/) | 依赖方向契约（分层单向 + 禁反向），纳入质量门 |
  | redis / aio-pika / asyncpg | **8.1.0** / **10.0.1** / **0.31.0** | [PyPI redis](https://pypi.org/project/redis/) / [PyPI aio-pika](https://pypi.org/project/aio-pika/) / [PyPI asyncpg](https://pypi.org/project/asyncpg/) | asyncpg 0.31.0 提供 cp314 轮子 |

- **不适用**：同步框架项目（Flask/Django/Falcon）、以 Django ORM 或 Tortoise ORM 为主的栈、非 Python 服务、纯数据/脚本类项目（无需 Web 层）、pip/poetry/conda 作为主依赖管理器的仓库（如需可另立类型）。

## 如何使用本模板

1. 将 `AGENTS.md` 复制到真实 FastAPI 项目**根目录**。
2. 全文替换 `{{...}}` 占位符为项目实际值：版本以 `pyproject.toml` + `uv.lock` 为准；端口/库名与 `docker-compose.yml`、`core/config.py` 的 `Settings` 默认值对齐。
3. 按需裁剪标注「按需保留」的章节（如「工作流与发布」）。
4. 在真实项目中**逐条实际执行**「快速开始」与「常用命令」验证；命令表中标注"需先安装"的可选项（如覆盖率插件）按需启用或删除。
5. 对照 [docs/authoring-types.md](../docs/authoring-types.md)「交付自查清单」逐项核对。
6. **docs/ 知识库（固定组成部分，必建）**：按模板「目录结构与架构」的骨架创建 `docs/`。核心文件必建：`docs/CODING_STANDARDS.md`（至少覆盖命名、分层与依赖方向、类型注解、异步、数据库与迁移、缓存、消息、异常与错误码、日志、测试、质量门、Git 提交）、`docs/ARCHITECTURE.md`（至少写出分层与依赖方向、关键链路、数据架构、中间件拓扑）。子目录**一律必建**（每个至少含最小入口文件，不因"未触发"而缺目录）：`design-docs/`（`index.md` + `core-beliefs.md`）、`product-specs/`（`index.md` + `TEMPLATE.md`）、`exec-plans/`（`index.md` + `tech-debt-tracker.md` + `active/` + `completed/`）、`generated/`（`index.md` 为唯一手写索引，其余只读）、`references/`（`index.md`）。**触发条件只决定内容何时补齐**：首个设计决策/ADR -> `design-docs/` 落 ADR；首个需求立项 -> `product-specs/` 落需求文件；出现多轮/多代理任务或技术债 -> `exec-plans/` 落计划与技术债；首个生成物入库 -> `generated/` 登记生成命令；首次引入外部资料 -> `references/` 登记（浓缩版命名 `*-llms.txt`）。**可直接复制的范本**：[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)、[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)（骨架遵循根仓库 [docs/fixed-docs.md](../docs/fixed-docs.md) 的「架构文档」固定规范：定位/版本基线/分层依赖/关键链路/数据/中间件/安全/可观测/部署/ADR）；需求规格范本见 [`docs/product-specs/`](docs/product-specs/index.md)（`index.md` + `TEMPLATE.md`，骨架遵循 `fixed-docs.md`「需求规格」）；其余四目录范本同源：[`docs/design-docs/`](docs/design-docs/index.md)、[`docs/exec-plans/`](docs/exec-plans/index.md)、[`docs/generated/`](docs/generated/index.md)、[`docs/references/`](docs/references/index.md)。

> 若项目用 `poetry`/`pip-tools` 而非 uv，替换命令为对应管理器的等价命令（`poetry run pytest` 等）并在第 1 章声明；其余结构与命名不变。

## 关键约定（为何这样写）

- **命令真实可验证**：全部命令以 `uv run` 前缀给出——依赖与解释器由 `uv.lock` 与 `.python-version` 决定，不依赖全局安装，也不受"当前是否激活了虚拟环境"影响。`uv sync --locked` / `uv lock --check` 保证 CI 不会隐式重新解析依赖。
- **版本单一来源**：Python 与全部依赖只由 `pyproject.toml` + `uv.lock` 决定；`ruff`/`mypy`/`pytest` 的规则也只在 `pyproject.toml` 配置，文档不维护第二份版本号或规则副本（避免双份漂移）。
- **三层指针而非重复**：`AGENTS.md`（命令/禁区/操作）、`docs/CODING_STANDARDS.md`（编码细则）、`docs/ARCHITECTURE.md`（结构/链路/决策）三权分立，互相用 `§N` 指针引用，同一规则不在两处展开。
- **陷阱覆盖本栈高频踩坑点**：`pytest-asyncio` 的 `asyncio_mode` 与事件循环作用域、`ruff` 的 `RUF001/002/003` 与中文全角标点冲突、`alembic.ini` 在中文 Windows 上按 GBK 读取（必须 ASCII-only）、SQLAlchemy 异步的阻塞调用与 `MissingGreenlet`、Redis `SETNX` 裸锁与 TTL 缺失、RabbitMQ 重复投递与死信堆积——全部写入「约束、禁区与陷阱」与 CODING_STANDARDS 范本（§6–§12）。
- **质量门确定化**：ruff（lint + format）、mypy（strict）、**import-linter（依赖方向契约）**、pytest 收敛为**一条命令**（见 `AGENTS.md`「测试与质量门」与 CODING_STANDARDS §14）；规则进 `pyproject.toml`，生成代码从检查中排除。**不变量靠强制而非文档**：分层单向与禁反向由 `[tool.importlinter]` 契约执行（实测：故意加一条反向导入 → 两个契约 BROKEN、退出码 1；还原后 2 kept / 0 broken）。
- **接口测试不依赖中间件**：用 `httpx.ASGITransport` + `app.dependency_overrides` 覆盖仓储/缓存端口，使"提交前全量测试"在无 PostgreSQL/Redis/RabbitMQ 的机器上也能真实跑通（本目录 L3 样例即按此实现）。
- **AGENTS.md 不含环境准备**：安装 uv/Python、起 PostgreSQL/Redis/RabbitMQ、首次下载耗时等人工 onboarding 细节不进 AGENTS.md——由真实项目 README、`docker-compose.yml`、`docs/ARCHITECTURE.md` §9 承担；AGENTS.md 只保留可执行命令（如 `docker compose up -d`）与版本约束。

## 参考来源

- FastAPI 官方文档：https://fastapi.tiangolo.com/
- FastAPI 发布记录：https://github.com/fastapi/fastapi/releases
- Pydantic v2 文档：https://docs.pydantic.dev/latest/
- pydantic-settings：https://docs.pydantic.dev/latest/concepts/pydantic_settings/
- SQLAlchemy 2.0 文档：https://docs.sqlalchemy.org/en/20/
- SQLAlchemy ORM 2.0 迁移指南（1.x 风格 -> 2.0 风格）：https://docs.sqlalchemy.org/en/20/changelog/migration_20.html
- Alembic 文档：https://alembic.sqlalchemy.org/
- uv 文档：https://docs.astral.sh/uv/ ｜ uv Releases：https://github.com/astral-sh/uv/releases
- ruff 文档：https://docs.astral.sh/ruff/ ｜ ruff 规则一览：https://docs.astral.sh/ruff/rules/
- mypy 文档：https://mypy.readthedocs.io/
- pytest 文档：https://docs.pytest.org/ ｜ pytest-asyncio：https://pytest-asyncio.readthedocs.io/
- httpx（`ASGITransport`）：https://www.python-httpx.org/async/
- PostgreSQL 文档：https://www.postgresql.org/docs/
- Redis 文档：https://redis.io/docs/
- RabbitMQ 文档：https://www.rabbitmq.com/docs
- Python 版本与下载：https://www.python.org/downloads/
- import-linter（依赖方向契约）：https://import-linter.readthedocs.io/
- [OpenAI Advanced Pack（walkinglabs/learn-harness-engineering）](https://walkinglabs.github.io/learn-harness-engineering/en/resources/openai-advanced/) —— docs/ 知识库骨架与 AGENTS.md 仓库模板的经验来源
- [其 repo-template/AGENTS.md（GitHub）](https://github.com/walkinglabs/learn-harness-engineering/blob/main/docs/en/resources/openai-advanced/repo-template/AGENTS.md) —— 「目录结构与架构」中 docs/ 骨架的出处

## 验证状态（Validation）

> 依据根仓库 `docs/authoring-types.md` §5（L1–L5 验证协议）与 `scripts/validate-type.ps1`。核实日期：2026-09-11。

| 层 | 结果 | 证据 / 备注 |
| --- | --- | --- |
| L1 静态校验 | ✅ PASS（FAIL=0，WARN=0，文件数=28） | `scripts/validate-type.ps1 -TypePath python-fastapi`（2026-09-11）：扫描 **28 个 md**（类型根 `AGENTS.md` + `README.md`；`docs/` 12 个——2 核心文件 + 五目录入口文件；`examples/sample-project/` 的 `README.md` + `AGENTS.md` + `docs/` 12 个副本）——无画线字符、占位符配平、无密钥形态、相对链接全部存在、必需章节齐全、CODING_STANDARDS §N 引用一致、docs 必建骨架齐全（2 核心文件 + 五项目录入口文件共 12 项）。执行宿主为本机 Windows PowerShell 5.1（无 pwsh 7）：两个校验脚本均为 UTF-8 with BOM，直接 `powershell -File` 运行（跨平台 / CI 用 `pwsh -File`）。`examples/sample-project/` 不提交 `.venv/`、各类缓存与 `__pycache__/`，故文件数稳定可复现；样例副本与范本逐字节一致（由 `scripts/validate-repo.ps1` 强制） |

| L2 结构/红线复核 | ✅ 通过（含独立 L4 复核发现的缺陷修复） | 交付自查清单**逐项核对**：骨架/占位符/链接/命令真实性/单一事实来源等项通过；独立 L4 复核提出的 3 条 BLOCKER 与主要 MAJOR 已修复（见 L4 行）。docs 五目录必建且各含最小入口文件，无空壳。**依赖方向已四条契约机器化**（分层单向 + 禁反向 + 禁越层 + 同层横向），每条均以"故意违规即红"实测；**尚未机器化**的项（Redis key 集中、读缓存回链、commitlint、集成链路）已在 L4 行与本页待办如实标注，不再宣称"逐项通过、无一处例外" |
| L3 动态冒烟 | ✅ **已真实执行**（2026-09-11，含 L4 修正后复跑） | 本机 Python **3.14.6** + uv **0.11.28**：在 [`examples/sample-project/`](examples/sample-project/README.md) 执行 `uv sync --locked`（解析并安装 **55** 个包）→ `uv run ruff check .`（All checks passed!）→ `uv run ruff format --check .`（维护者独立复跑：46 files already formatted）→ `uv run mypy src tests`（**Success: no issues found in 31 source files**）→ `uv run lint-imports`（**Contracts: 4 kept, 0 broken**）→ `uv run pytest`（**17 passed**，退出码 0）→ `uv run alembic upgrade head --sql`（离线渲染出 PostgreSQL DDL：`CREATE TYPE order_status` / `CREATE TABLE t_order` / `CREATE UNIQUE INDEX ix_t_order_order_no`）→ `uv run uvicorn sample.main:app` 实起服务并 curl 探活。**未做**：真实 PostgreSQL/Redis/RabbitMQ 链路（本机无 Docker、无中间件），故在线迁移、集成测试与 MQ 消费未验证；`docker-compose.yml` 的镜像标签已对 Docker Hub 核实，但编排本身未实跑 |
| L4 评审通道 | ✅ **已由独立代理对抗式复核**（2026-09-11），缺陷已修 | 复核报告 3 条 BLOCKER：①「依赖方向已机器强制」被**实验证伪**（`layers` 契约不拦越层/同层横向）→ 已补 `forbidden`（禁越层，`allow_indirect_imports`）与 `independence`（禁同层横向）契约，实测越层/同层横向均 BROKEN + exit 1；② 500 兜底响应缺 `X-Trace-Id` → `_error_response()` 统一补头 + 新增错误路径断言（去掉补头即红，已实测）；③ 新类型未登记 changelog → 已在 v3.2 登记。另修复：`print` 禁令未拦（`select` 加 `T20`，实测 1 error + exit 1）、错误码"分段/命名/只增不删"无断言（已补三项断言，违规即红）、迁移脚本命名口径 `V{{n}}__`（改为 `{{revision}}_{{描述}}.py`）、目录树漏 `docker-compose.yml`/`.env.example`、环境准备条目自相矛盾、`SAMPLE_DEBUG` 写死、版本号两处各写一份、`§N` 交叉引用指错章（`errors.py` §6→§10、`pyproject.toml` §2→§14）。**仍未机器化**（如实标注、进待办）：Redis key 集中（§8 魔法字符串）、读缓存回源链路未实现、commitlint/pre-commit、真实中间件集成链路 |
| L5 现状核实 | ✅ 已核实（2026-09） | 逐包核对 PyPI/GitHub Releases（见「主版本线」表与「参考来源」）：FastAPI **0.141.1**、Pydantic **2.13.5**、SQLAlchemy **2.0.52**、Alembic **1.19.2**、uv **0.12.13**、ruff **0.16.7**、mypy **2.3.1**、pytest **9.1.1**、pytest-asyncio **1.4.0**、uvicorn **0.52.4**、redis **8.1.0**、aio-pika **10.0.1**、asyncpg **0.31.0**；Python 最新稳定为 **3.14.7**（3.15.0rc2 仍预发布）；import-linter **2.15**（依赖方向契约）。独立复核另用 PyPI JSON API 逐包比对：**15/15 与 `uv.lock` 一致** |

### L3 实测记录（原始输出要点）

> 仓库级校验 `scripts/validate-repo.ps1`（L1'）在本类型同样 **FAIL=0**：根文档链接与 §N 引用、类型目录 ↔ 根 `AGENTS.md` §4 登记表双向一致、docs 必建骨架、范本 ↔ `examples/` 样例副本逐字节同源、版本号 ↔ changelog 全部通过。

```text
uv sync --locked        ->  Resolved 50 packages / Installed 50 packages（含 dev 组）
uv run ruff check .     ->  All checks passed!
uv run ruff format --check .  ->  46 files already formatted（维护者独立复跑；作者首次记录时为 33 个文件，其后样例文件集有增补）
uv run mypy src tests   ->  Success: no issues found in 31 source files
uv run pytest           ->  15 passed（0.14s – 0.24s，两次复跑一致）
uv run alembic upgrade head --sql
                        ->  CREATE TYPE order_status AS ENUM ('CREATED', 'PAID', 'CANCELLED');
                            CREATE TABLE t_order (...); CREATE UNIQUE INDEX ix_t_order_order_no ...
uv run uvicorn sample.main:app --host 127.0.0.1 --port 8123
   GET  /healthz                        ->  200 {"status":"ok"}  + 响应头 x-trace-id
   GET  /docs                           ->  200（debug 开启时）
   GET  /openapi.json                   ->  title=sample-project, version=0.1.0
   POST /api/v1/orders（非法体）          ->  422 {"code":"200001","message":"参数校验失败","traceId":"..."}
   GET  /api/v1/orders/MISSING-1（无库）  ->  500 {"code":"100000","message":"系统繁忙，请稍后重试","traceId":"..."}（无堆栈）
```

由 L3 实测修正或新增的模板条目（真实踩坑，已写入模板）：

1. **`alembic.ini` 必须 ASCII-only** —— Alembic 用 `configparser` + `encoding="locale"` 读取，中文 Windows（GBK）下 UTF-8 中文注释会直接抛 `UnicodeDecodeError`。
2. **`ruff` 的 `RUF001/002/003` 与中文全角标点冲突** —— 中文文档/注释会被报"ambiguous full-width punctuation"，模板在 `[tool.ruff.lint] ignore` 中显式关闭并说明理由。
3. **断言 500 兜底响应需 `ASGITransport(raise_app_exceptions=False)`** —— Starlette 的 `ServerErrorMiddleware` 在写出兜底响应后**仍会重抛**异常，默认设置下测试拿到的是异常而不是响应。
4. **质量门 `&&` 的平台差异** —— Windows PowerShell 5.1 不支持 `&&`，需在 bash / PowerShell 7+ 执行或逐条运行。

### L3 待执行清单（有 Docker 的环境补跑后回填）

```bash
cd python-fastapi/examples/sample-project
docker compose up -d                     # PostgreSQL / Redis / RabbitMQ（compose 文件已提供，本机无 Docker 未验证）
uv run alembic upgrade head              # 在线迁移（需 PostgreSQL）
SAMPLE_DATABASE_URL=postgresql+asyncpg://localhost:5432/sample uv run pytest -q -m integration   # bash
$env:SAMPLE_DATABASE_URL="postgresql+asyncpg://localhost:5432/sample"; uv run pytest -q -m integration   # Windows PowerShell
uv run pytest -q --cov=sample --cov-report=term-missing   # 需先 uv add --dev pytest-cov
```

### 已知未实现项（如实标注，勿当作已具备）

- **读缓存回源链路**未在样例实现：`get_order` 只走仓储；`CacheProtocol.get/set` 与 `CacheKeys` 的 `idem:`/`lock:` 常量暂无调用点。故「读 Redis 未命中回源 → 回填」这条关键链路**不构成 L3 证据**（写后删缓存有单测，已强制）。
- **Redis key 集中化**（§8 禁散落字符串）无强制手段：样例测试自身也用了字面量 `order:detail:`，属"仅评审"。
- **RabbitMQ 全节**（生产者/消费者/手动 ack/DLQ）在样例中只有常量与信封构造，无运行时实现（无中间件环境无法验证）。
- **commitlint / pre-commit** 未接入（见根仓库 `docs/enforcement-map.md`）。

## 维护

- 触发修订：FastAPI / Pydantic / SQLAlchemy / uv / ruff / mypy / pytest-asyncio 出现破坏性版本变更，标准命令变化，或发现模板缺常见坑；每次修订后重跑 L1 + L3 并更新本页「主版本线」「验证状态」。
- 修订时同步更新本目录 `AGENTS.md`、`docs/CODING_STANDARDS.md`、`docs/ARCHITECTURE.md`，并在根仓库 [AGENTS.md](../AGENTS.md) 登记表与本类型 README 更新状态。
- **范本改动必须同步样例副本**：`examples/sample-project/` 下的 `AGENTS.md` 与 `docs/` 12 个文件是**逐字节同源拷贝**（由 `scripts/validate-repo.ps1` 强制），改范本后要一并复制过去，否则仓库级校验 FAIL。
- **执行提示**：`uv sync` 与质量门**分两步执行**——同一条命令里混跑时，`uv sync` 重装本项目包的瞬间会让 `import-linter` 的包分析与 `pytest` 收集出现瞬时失败（exit 1 / exit 4），并非真实缺陷；CI 已按分步骤编排。
