# sample-project —— python-fastapi 类型的 L3 冒烟样例

本目录是 [`python-fastapi/AGENTS.md`](../../AGENTS.md) 的**动态验证用样例**（非交付范本）：结构与「目录结构与架构」一致的最小可跑项目，用于**真实执行**「快速开始 / 常用命令 / 测试与质量门」中的命令。

## 目录

```text
sample-project/
  pyproject.toml          # 依赖 + ruff/mypy/pytest 配置（质量门规则的单一来源）
  uv.lock                 # uv 锁定文件（提交入库）
  .python-version         # uv 自动选取的解释器版本
  alembic.ini             # 迁移配置（连接串在 migrations/env.py 从 Settings 读取；本文件保持 ASCII-only）
  docker-compose.yml      # 本地 PostgreSQL/Redis/RabbitMQ（带健康检查；仅本地，未在本机验证）
  migrations/
    env.py                # 离线模式可跑：alembic upgrade head --sql
    script.py.mako        # alembic revision 模板
    versions/0001_create_order.py
  src/sample/
    main.py               # 应用工厂（uvicorn 入口 sample.main:app）
    api/                  # 适配层：deps.py（组装根）+ routers/
    services/             # 业务规则与事务外编排
    repositories/         # SQLAlchemy 2.0 数据访问（Protocol + 真实实现）
    models/               # 表映射（DeclarativeBase + AuditMixin）
    schemas/              # Pydantic v2 入参/出参
    core/                 # config / errors / logging / cache / mq
    db/                   # 引擎、会话工厂、Base
  tests/                  # pytest + pytest-asyncio + httpx ASGITransport
```

## 已验证命令

在 `python-fastapi/examples/sample-project/` 下执行（真实输出见 [`../../README.md`](../../README.md)「验证状态」）：

```bash
uv sync --locked                     # 按 uv.lock 安装（含 dev 依赖组）
uv run ruff check .                  # lint
uv run ruff format --check .         # 格式校验
uv run mypy src tests                # 类型检查（strict）
uv run pytest -q                     # 单元 + 接口测试，不连 PostgreSQL/Redis/RabbitMQ
uv run alembic upgrade head --sql    # 离线渲染迁移 DDL，无需数据库连接
uv run uvicorn sample.main:app --reload --port 8000   # SAMPLE_DEBUG=true 时 /docs 可用
```

## 说明

- 测试通过 `app.dependency_overrides` 覆盖仓储与缓存端口，**不需要** PostgreSQL/Redis/RabbitMQ 即可全绿。
- `docker-compose.yml` 镜像标签取自 Docker Hub 现有 tag（`postgres:18-alpine`、`redis:8-alpine`、`rabbitmq:4-management`），但**本机无 Docker，未经实跑验证**；带中间件的链路（在线迁移、集成测试、MQ 消费）同样待补跑。
- Redis 适配器（`core/cache.py`）与 RabbitMQ 拓扑（`core/mq.py`）已就位但未被本样例的测试覆盖：本机无中间件环境，故不写入未验证的结论。
- 本目录的 `AGENTS.md` 与 `docs/` 是类型范本的**逐字节副本**（与 [`../../AGENTS.md`](../../AGENTS.md)、[`../../docs/CODING_STANDARDS.md`](../../docs/CODING_STANDARDS.md) 同源），用于演练"复制范本 -> 填占位 -> 逐条验证"的落地路径；**规范正文只在类型目录维护一份**，两者由仓库校验脚本 `scripts/validate-repo.ps1` 强制同源。
