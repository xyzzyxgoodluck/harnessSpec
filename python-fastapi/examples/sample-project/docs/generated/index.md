# generated —— 自动生成物（只读）

> **本目录内除本文件外一律只读**：内容由工具生成，人工不手改——手改会被下次生成覆盖，且合并必冲突。
> 固定组成部分：**目录必建**（含本索引）；生成物按触发器入库。
> 生成物入库时，其顶部必须标注 **生成命令 + 生成时间 + 输入来源**；本文件登记同一份信息，供人/代理核对。

## 触发器（条件满足即必须写入内容）

| 触发条件 | 必须写入 |
| --- | --- |
| 首个自动生成物需入库（如 OpenAPI 快照、数据模型代码、数据库结构文档、ER 图、客户端 SDK） | 生成物文件 + 本表登记一行（含生成命令与时间） |
| 生成命令或输入来源变化 | 更新本表对应行（生成物重生成，不改生成物本身） |

## 登记表

| 生成物 | 生成命令（真实可执行） | 输入来源（单一事实源） | 生成时间 | 备注 |
| --- | --- | --- | --- | --- |
| {{openapi.json}} | {{如 `uv run python -c "import json;from {{包名}}.main import app;print(json.dumps(app.openapi(),ensure_ascii=False))" > docs/generated/openapi.json`}} | {{src/{{包名}}/api/routers/（Pydantic schema + 路由定义）}} | {{2026-09-01}} | {{只读，勿手改}} |
| {{models_generated.py}} | {{如 `uv run datamodel-codegen --input docs/generated/openapi.json --output src/{{包名}}/schemas/generated`}} | {{docs/generated/openapi.json}} | {{2026-09-01}} | {{生成目录已从 lint/type 检查排除，见 CODING_STANDARDS §14}} |

## 规则（必须）

1. 生成命令必须真实可跑通并登记在此（单一事实源）；禁止"大概能生成"。
2. 生成物不得替代权威文档：决策在 [`../design-docs/index.md`](../design-docs/index.md)、编码细则在 [`../CODING_STANDARDS.md`](../CODING_STANDARDS.md)、结构与链路在 [`../ARCHITECTURE.md`](../ARCHITECTURE.md)。
3. 生成物与源码不一致时，以**重新生成**为准；不得手改生成物使其"看起来对"。
4. 生成物不含密钥、内网地址、真实个人数据（示例值同样受限）；OpenAPI 快照不得包含生产域名与凭据。
