# references —— 外部参考资料登记

> 引入外部资料（官方文档、行业标准、第三方规范、竞品调研）时登记在此，避免"凭记忆引用"。
> 固定组成部分：**目录必建**（含本索引）；资料按触发器入库。
> 大篇幅资料一律**浓缩入库**：命名 `{{主题}}-llms.txt`（LLM 浓缩版：保留结论、版本与来源链接，不整篇搬运）。

## 触发器（条件满足即必须写入内容）

| 触发条件 | 必须写入 |
| --- | --- |
| 首次引入外部资料（版本基线核对、标准依据、选型对比） | 本表登记一行 +（内容较长时）浓缩版 `{{主题}}-llms.txt` |
| 资料版本更新或链接失效 | 更新本表对应行；失效链接不得静默保留 |

## 登记表

| 资料 | 类型 | 来源链接 | 版本 / 日期 | 本项目用途 | 浓缩版文件 |
| --- | --- | --- | --- | --- | --- |
| {{FastAPI 官方文档}} | {{官方文档}} | https://fastapi.tiangolo.com/ | {{0.141.x}} | {{路由/依赖注入/异常处理基线}} | `{{fastapi-llms.txt}}` |
| {{Pydantic v2 迁移与校验文档}} | {{官方文档}} | https://docs.pydantic.dev/latest/ | {{2.13.x}} | {{Schema 校验与 v2 写法约束}} | `{{pydantic-llms.txt}}` |
| {{SQLAlchemy 2.0 ORM 文档}} | {{官方文档}} | https://docs.sqlalchemy.org/en/20/ | {{2.0.x}} | {{`Mapped`/`select()` 2.0 风格与异步用法}} | `{{sqlalchemy-llms.txt}}` |
| {{uv 官方文档}} | {{官方文档}} | https://docs.astral.sh/uv/ | {{0.12.x}} | {{依赖管理、锁文件与 CI 用法}} | `{{uv-llms.txt}}` |
| {{ruff 规则参考}} | {{官方文档}} | https://docs.astral.sh/ruff/rules/ | {{0.16.x}} | {{lint 规则取舍（含 RUF001/002/003 关闭理由）}} | `{{ruff-llms.txt}}` |

## 规则（必须）

1. 只登记**实际被引用**的资料；引用它的地方（AGENTS.md / ARCHITECTURE / 需求规格）给出可定位指向。
2. 外部资料不覆盖本项目规范：冲突时以 [`../CODING_STANDARDS.md`](../CODING_STANDARDS.md)、[`../ARCHITECTURE.md`](../ARCHITECTURE.md)、`AGENTS.md` 为准，并记 ADR 说明取舍。
3. 不登记需授权的内容与含 token 的链接；密钥、内网地址、真实账号一律不得出现。
4. 浓缩版保持"结论 + 版本 + 来源链接"三要素，禁止整篇复制外部文本。
