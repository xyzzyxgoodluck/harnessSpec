# AiCodingSpec

> 各类项目 **AGENTS.md 规范与模板**的汇编仓库：让真实项目"开箱即有合格 AGENTS.md"。
>
> AGENTS.md 是什么：写给 AI 编码代理、人也应能读懂的**项目操作手册**——告诉代理"用什么命令、改哪里、不能碰哪里、按什么约定提交"，减少试错与破坏性假设。主流编码代理（Claude Code、Cline、Copilot 等）进入仓库时会读取这类文件。

## 这个仓库做什么

- 一个文件夹 = 一种**项目类型**（如 `springboot/`），内含该类型的 `AGENTS.md` 规范/模板，以及可复制范本（编码规范、架构文档、需求规格）。
- 产出可直接复制到真实项目根目录、按项目微调的 AGENTS.md，并配套稳定知识库骨架（`docs/`）。

## 怎么使用（How to Use）

### 用法一：项目开发者 —— 把规范套到你的真实项目

> 环境安装类内容（装 JDK/Docker 等）由你项目自己的 README/部署文档负责，模板不含这些。

以 springboot 类型为例（Linux/macOS；Windows 用 `copy`/`xcopy` 或资源管理器）：

```bash
# 1. 复制类型 AGENTS.md 到你的项目根
cp springboot/AGENTS.md  /path/to/your-project/AGENTS.md

# 2. （推荐）复制 docs 范本作为知识库起点：编码规范 / 架构 / 需求规格
cp -r springboot/docs/  /path/to/your-project/docs/
```

然后逐项处理：

1. **填占位符**：把 `{{...}}` 全部替换为项目实际值（项目名、技术栈版本、包名、端口、模块等）。检查还有哪些没填：
   ```bash
   grep -n '{{' AGENTS.md
   ```
   替换后再跑一次应为空（有意保留的除外）。
2. **裁剪**：删除标注"{{按需保留或删除}}" / "○" 的章节（如「工作流与发布」）；不适用本项目的条目一并删掉。
3. **建 docs/ 固定文件**：确保存在 `docs/CODING_STANDARDS.md`、`docs/ARCHITECTURE.md`（从复制来的范本裁剪）；`design-docs/`、`product-specs/`、`exec-plans/`、`generated/`、`references/` 按模板里写的**触发器**建立（条件满足才建，不为建而建）。
4. **逐条验证命令**：在真实项目里把「快速开始」「常用命令」逐条执行一遍（模板要求命令真实可跑，跑不通的要改正后再提交）。
5. **对照自查**：按 [docs/authoring-types.md](docs/authoring-types.md) 的「交付自查清单」逐项核对。

### 用法二：让 AI 编码代理用起来

- 把 `AGENTS.md` 放在项目根后，主流编码代理（Claude Code、Cline、Copilot 等）进入仓库会自动向上查找并读取它。
- 若工具默认读 `CLAUDE.md`，可在其中 `@import AGENTS.md`，不要维护两份。
- 多模块/大目录可放**局部 AGENTS.md**（该目录专属规则，子级优先），根级保持简短地图。
- 不确定时直接要求代理："先读 AGENTS.md 再动手。"

### 用法三：本仓库贡献者 —— 新增/修订类型

- 先读根 [AGENTS.md](AGENTS.md)（地图）→ [docs/authoring-types.md](docs/authoring-types.md)（流程/结构/自查/**验证协议**）。
- 每个类型 = 一个文件夹（如 `springboot/`）：含 `AGENTS.md`（规范/模板）+ 可选 `README.md`、`examples/` 范本。
- 发布/修订前跑静态校验：`pwsh -File scripts/validate-type.ps1 -TypePath springboot`（要求 FAIL=0），并按 `authoring-types.md` §5 完成 L1–L5 验证。
- 改完登记到根 AGENTS.md 类型清单与 [docs/changelog.md](docs/changelog.md)。



## 已收录类型

| 类型文件夹 | 目标项目类型 | 说明 |
| --- | --- | --- |
| [springboot/](springboot/README.md) | Spring Boot + MyBatis-Plus + MySQL 服务端（集成 Redis / RabbitMQ，Swagger UI 文档） | Maven 主线；含examples/范本（CODING_STANDARDS、ARCHITECTURE、product-specs） |

> 规划中：python-fastapi、node-express、nextjs、go-service、rust-cli、python-lib、monorepo 等（按需排期）。

## 仓库结构

```text
AiCodingSpec/
  AGENTS.md                     # 给代理的地图：定位/仓库地图/类型清单/硬性红线/入口
  README.md                     # 本文件：给人/下游用户的入口介绍
  docs/                         # 本仓库自身的规范正文（贡献者阅读）
    authoring-types.md          # 如何新增/修订类型：结构骨架 + 项目文件夹结构 + 自查清单 + L1–L5 验证协议
    writing-standards.md        # 一切文档的写作原则 / 命令书写 / 反例表
    fixed-docs.md               # 固定组成部分：docs 布局 / Git 提交 / 架构文档 / 需求规格
    harness-principles.md       # 工作方式准则（OpenAI/DeepSeek harness 经验摘编）
    changelog.md                # 版本历史
  scripts/validate-type.ps1     # 类型静态校验脚本（L1）
  springboot/                   # 示例：一种项目类型
    AGENTS.md                   # 该类型规范/模板（复制到真实项目）
    README.md                   # 类型适用范围与使用方法、验证状态
    docs/                       # 类型交付物范本：CODING_STANDARDS / ARCHITECTURE / product-specs
    examples/sample-project/        # 冒烟样例项目（start.spring.io 生成，用于 L3 验证）
```

## 贡献者入口

新增或修订一个项目类型、撰写任何规范文档前，请先读根 [AGENTS.md](AGENTS.md)（地图）与 [docs/authoring-types.md](docs/authoring-types.md)（流程与自查）；写作细则见 [docs/writing-standards.md](docs/writing-standards.md)。

## 相关资源

- [AGENTS.md 官方规范站点](https://agents.md)
- [OpenAI — Harness engineering: using Codex in an agent-first world](https://openai.com/index/harness-engineering/)
- [OpenAI Codex — AGENTS.md 指南](https://developers.openai.com/codex/guides/agents-md/)
- [deepseek-ai/deepseek-harness（AGENTS.md 工程实践出处）](https://github.com/deepseek-ai/deepseek-harness)
- [Claude Code Best Practices（Anthropic）](https://www.anthropic.com/engineering/claude-code-best-practices)
