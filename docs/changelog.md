# 变更记录（Changelog）

> 本仓库规范/文档的版本历史统一登记于此（根 AGENTS.md 不再内嵌长历史）。登记口径：对规范/文档有实质影响的改动追加一行，格式 `vX.Y —— 说明（涉及文件）`。

## v3.0（当前）

- **文档拆分**：根 `AGENTS.md` 重写为精简"地图"版（项目定位/仓库地图/目录约定/类型清单/红线/入口）；正文按主题拆入本 `docs/`：
  - `writing-standards.md` —— 写作基本原则、命令与路径书写规范、常见反例（原 §6–§8）
  - `authoring-types.md` —— 新增/修订类型工作流、统一结构骨架、**类型项目文件夹结构**、交付自查清单（原 §3–§5 内容重组；不再内联整篇"通用骨架模板"，以文件夹结构 + fixed-docs 固定内容代替）
  - `fixed-docs.md` —— 四个固定组成部分：docs 布局 / Git 提交规范 / ARCHITECTURE 骨架 / product-specs 骨架（原 §5.1–§5.4）
  - `harness-principles.md` —— Harness Engineering 原则摘编（原 §11）
  - 本文件承接全部版本历史（v1.0–v2.8）
- **新增根 `README.md`**：面向人/下游用户的入口介绍（项目定位、快速使用、类型清单、仓库结构、贡献者入口），与根 AGENTS.md（代理地图）分工。
- **验证协议（L1–L5）**：新增 `scripts/validate-type.ps1`（L1 静态校验：画线字符/占位符/密钥启发/链接存在/必需章节），写入 `authoring-types.md` §5（L2 清单复核 / L3 样例项目冒烟 / L4 评审 / L5 现状核实+时间戳）；跑脚本修复了 springboot README 的 `../../` 链接深度与 product-specs 示例悬空链接。
- **样例项目**：springboot 类型新增 `examples/sample-project/`（Spring Initializr 真实生成的最小 Maven 项目），作为 L3 动态冒烟载体；springboot README 增加「验证状态」记录（L1 PASS / L3 待执行+步骤 / L5 已核实）。
- **布局修正（springboot 类型交付物上提）**：`docs/` 范本（CODING_STANDARDS / ARCHITECTURE / product-specs）从 `examples/docs/`（曾用名 `示例/docs/`）上提为 `springboot/docs/`——它们是类型交付物的一部分（真实项目 `docs/` 的直接源头）；`examples/` 仅保留冒烟样例 `sample-project/`。移动过程中源文件被误删，已在 `springboot/docs/` 按既定规范**完整重建**并全量修正引用与目录树描述。
- **根 AGENTS.md §3.1**：新增「类型交付物结构」固定描述（AGENTS.md 本体 + docs/ 范本 + README + examples 的★/按需划分与复制路径、发布条件）。
- **目录名英文化**：`springboot/示例/` 重命名为 `springboot/examples/`，全仓库 md 路径引用同步（中文行文用词如"示例值"保留）。
- **sample-project 按类型规范重做（完整栈合规样例）**：按 `springboot/AGENTS.md` 推导其应为"规则可套用的真实项目"——现已装配全栈（Boot 3.5.0 + MyBatis-Plus 3.5.17 + springdoc 2.8.13 + MySQL/Redis/RabbitMQ compose + application.yml）并复制 `AGENTS.md` 与 `docs/` 必建核心（CODING_STANDARDS/ARCHITECTURE）；触发器目录（product-specs 等）未触发不建。L1 对本样例 PASS；L3 编译/运行验证待 JDK+Maven+Docker 环境（版本坐标明确标注"基线，待编译核验升级"）。
- **L3 部分执行（2026-09-09，无 Docker 切片）**：系统 JDK 17.0.12（D:\Java\jdk-17）+ Maven 3.9.16（D:\apache-maven-3.9.16）就绪后，在 `examples/sample-project/` 实测 `mvn -DskipTests compile` 与 `mvn -DskipTests package` 均 **BUILD SUCCESS**（可执行 jar 47MB），证明 Boot 3.5.0 / mybatis-plus 3.5.17 / springdoc 2.8.13 坐标真实可解析编译；需中间件的 test/run/curl 切片待 Docker 补跑。

## v2.x（合并期）

- v2.8 —— §11 以 OpenAI harness-engineering（2026-02）为主参考改写，deepseek-harness 降为工程卫生补充；参考资料补 OpenAI/Codex 链接。
- v2.7 —— 新增 Harness 核心原则与最佳实践（deepseek-harness 根 AGENTS.md 摘编）。
- v2.6 —— 环境纪律：环境准备类内容不写入 AGENTS.md；版本约束指向单一来源。
- v2.5 —— 需求规格 product-specs/ 固定骨架。
- v2.4 —— 架构文档 ARCHITECTURE.md 固定骨架。
- v2.3 —— Git 提交规范定为固定组成部分。
- v2.2 —— docs/ 定为固定组成部分（核心文件必建、子目录触发器、禁"若有"句）。
- v2.1 —— 吸收 OpenAI Advanced Pack 经验：docs 知识库布局，编码规范独立成文。
- v2.0 —— 确立"一个文件夹 = 一种项目类型，内含该类型 AGENTS.md"的项目形态。

## v1.0

- 通用 AGENTS.md 写作规范初版（其内容已并入 writing-standards / fixed-docs / authoring-types）。
