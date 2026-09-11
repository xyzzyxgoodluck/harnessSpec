# springboot 类型 —— 说明

本目录对应项目类型：**Spring Boot + MyBatis-Plus + MySQL 服务端**（集成 Redis 与 RabbitMQ）。`springboot/AGENTS.md` 为该类型的规范/模板交付物。

## 适用范围

- **适用**：采用以下**固定技术栈**的服务端应用 —— REST API、批处理任务、消息消费者/生产者、带 Web 层的后台服务：
  - Web/框架：Spring Boot（Java 17+；LTS 线 17/21/25，项目实际版本以 `pom.xml` 为准）
  - ORM：MyBatis-Plus（`mybatis-plus-spring-boot3-starter`；Boot 4 需先核对官方兼容）
  - 数据库：MySQL 8.x
  - 中间件：Redis（缓存/分布式锁）、RabbitMQ（消息/异步解耦）
  - API 文档：Swagger UI（springdoc-openapi，本地 `/swagger-ui.html`）
- **主版本线**（**最后核实：2026-09**，依据 Maven Central 元数据）：
  - **Spring Boot 3.5.x = MyBatis-Plus 兼容成熟线**（推荐默认）；3.5 线最新补丁 **3.5.16**（样例当前锁 3.5.0，升级待办：需重跑 `clean verify`）。
  - Spring Boot **4.1.1** 已发布（4.2.0-M1 为里程碑版，不用于生产）；升级前必须对照 [MyBatis-Plus changelog](https://baomidou.com/resources/changlog/) 确认对应兼容 starter/版本，再在本模板填实。
  - MyBatis-Plus **3.5.17** 为当前最新（`mybatis-plus-spring-boot3-starter`）；分页等 jsqlparser 插件需另引 `mybatis-plus-jsqlparser`（同号，见 `docs/CODING_STANDARDS.md` §5）。
  - springdoc：Boot 3 线用 **2.x**（最新 2.9.1）、Boot 4 线用 **3.x**（最新 3.1.1）。
- **不适用**：纯前端、非 Spring 的 Java 项目、Android；以 JPA/Hibernate、PostgreSQL 等其它数据栈为主的 Spring 项目（如需可另立类型）；Spring Cloud 全家桶微服务骨架（可另立 `spring-cloud-microservice` 类型）。

## 如何使用本模板

1. 将 `AGENTS.md` 复制到真实 Spring Boot 项目**根目录**。
2. 全文替换 `{{...}}` 占位符为项目实际值：版本以 `pom.xml` 为准（**MyBatis-Plus 版本在 `<properties>` 独立声明**）；中间件端口/库名与 `docker-compose.yml` 对齐。
3. 按需裁剪标注「按需保留或删除」的章节（如「工作流与发布」）。
4. 在真实项目中**逐条实际执行**「快速开始」与「常用命令」验证（Maven/Gradle 二选一后删去另一行）。
5. 对照 [docs/authoring-types.md](../docs/authoring-types.md)「交付自查清单」逐项核对。
6. **docs/ 知识库（固定组成部分，必建）**：按模板「目录结构与架构」的骨架创建 `docs/`。核心文件必建：`docs/CODING_STANDARDS.md`（至少覆盖：命名与包结构、MyBatis-Plus/SQL、Redis、RabbitMQ、事务与异常边界、日志规范、DTO 映射、测试与格式化规则，对应 Checkstyle / Spotless / spring-javaformat 配置）、`docs/ARCHITECTURE.md`（至少写出模块划分、关键链路与缓存/消息链路）。子目录**一律必建**（每个至少含最小入口文件，不因"未触发"而缺目录）：`design-docs/`（`index.md` + `core-beliefs.md`）、`product-specs/`（`index.md` + `TEMPLATE.md`）、`exec-plans/`（`index.md` + `tech-debt-tracker.md` + `active/` + `completed/`）、`generated/`（`index.md` 为唯一手写索引，其余只读）、`references/`（`index.md`）。**触发条件只决定内容何时补齐**：首个设计决策/ADR → `design-docs/` 落 ADR；首个需求立项 → `product-specs/` 落需求文件；出现多轮/多代理任务或技术债 → `exec-plans/` 落计划与技术债；首个生成物入库 → `generated/` 登记生成命令；首次引入外部资料 → `references/` 登记（浓缩版命名 `*-llms.txt`）。**可直接复制的范本**（复制到真实项目 `docs/` 后按团队实际裁剪）：[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)、[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)（骨架遵循根仓库 [docs/fixed-docs.md](../docs/fixed-docs.md) 的「架构文档」固定规范：定位/版本基线/分层依赖/关键链路/数据/中间件/安全/可观测/部署/ADR）；需求规格范本见 [`docs/product-specs/`](docs/product-specs/)（`index.md` + `TEMPLATE.md`，骨架遵循 `fixed-docs.md`「需求规格」）；其余四目录范本同源：[`docs/design-docs/`](docs/design-docs/index.md)、[`docs/exec-plans/`](docs/exec-plans/index.md)、[`docs/generated/`](docs/generated/index.md)、[`docs/references/`](docs/references/index.md)。

> 若项目用 Gradle 而非 Maven，替换命令为备注列的 `./gradlew ...`，并在第 1 章声明；其余结构不变。

## 关键约定（为何这样写）

- **命令真实可验证**：`./mvnw`/`mvnw.cmd`（Maven Wrapper）是主流脚手架（start.spring.io）默认产物，不依赖全局安装。
- **版本单一来源**：Spring 系版本指向 `pom.xml` 的 `parent`（Boot BOM）；MyBatis-Plus 不在 Boot BOM，模板要求版本在 `<properties>` 显式声明并注释，避免版本漂移。
- **陷阱覆盖**：MyBatis-Plus 版本兼容与分页插件/逻辑删除/乐观锁、MySQL DDL 与慢 SQL、Redis 缓存一致性与分布式锁、RabbitMQ 幂等/死信/确认等是本栈最高频踩坑点，全部写入「约束、禁区与陷阱」与 CODING_STANDARDS 范本。
- **质量门确定化（Maven 插件固定集）**：Spotless（格式校验）、Checkstyle（Lint，团队规则文件 `config/checkstyle/checkstyle.xml` = Google 基底 + 禁 `SELECT *`/`System.out`/`printStackTrace`/JUL&commons-logging）、SpotBugs（`effort=Max` + `config/spotbugs/excludeFilter.xml`）、Enforcer（Java/Maven 版本、依赖收敛、`bannedDependencies`）、JaCoCo（`report` + **`check`：LINE ≥ 80%**，排除 entity/mapper/config/启动类）——本地与 CI 统一收敛于 `./mvnw clean verify`。**关键点**：Checkstyle 的 checker 内 `severity=warning` 必须配合 pom 的 `<violationSeverity>warning</violationSeverity>`，否则团队规则只打印不拦构建（"0 violations"是假绿）。规则进 pom/XML 配置、生成代码从检查中排除；每条「必须」规则的强制手段登记在根仓库 `docs/enforcement-map.md`；详见模板「测试与质量门」与 CODING_STANDARDS 范本 §13。
- **AGENTS.md 不含环境准备**：JDK/Docker 安装、手工起中间件、首次下载耗时等人工 onboarding 细节不进 AGENTS.md——由真实项目 README、`docker-compose.yml`、`docs/ARCHITECTURE.md` §9（Profile 与部署）承担；AGENTS.md 只保留可执行命令（如 `docker compose up -d`）与版本约束。

## 参考来源

- Spring Boot 官方文档：https://docs.spring.io/spring-boot/
- MyBatis-Plus 官方文档：https://baomidou.com/
- MyBatis-Plus 更新日志（版本/兼容核对）：https://baomidou.com/resources/changlog/
- MySQL 官方文档：https://dev.mysql.com/doc/
- Redis 官方文档：https://redis.io/docs/
- RabbitMQ 官方文档：https://www.rabbitmq.com/docs
- springdoc-openapi（Swagger UI 集成与配置项）：https://springdoc.org/
- Checkstyle：https://checkstyle.org/
- SpotBugs：https://spotbugs.github.io/
- Spotless：https://github.com/diffplug/spotless
- JaCoCo：https://www.jacoco.org/jacoco/
- 脚手架（项目生成）：https://start.spring.io/
- [Spring Boot 版本与 EOL（HeroDevs 追踪）](https://www.herodevs.com/blog-posts/spring-boot-versions-eol-dates-and-latest-releases-april-2026)
- [OpenAI Advanced Pack（walkinglabs/learn-harness-engineering）](https://walkinglabs.github.io/learn-harness-engineering/en/resources/openai-advanced/) —— docs/ 知识库骨架与 AGENTS.md 仓库模板的经验来源
- [其 repo-template/AGENTS.md（GitHub）](https://github.com/walkinglabs/learn-harness-engineering/blob/main/docs/en/resources/openai-advanced/repo-template/AGENTS.md) —— 「目录结构与架构」中 docs/ 骨架的出处

## 验证状态（Validation）

> 依据根仓库 `docs/authoring-types.md` §5（L1–L5 验证协议）与 `scripts/validate-type.ps1`。

| 层 | 结果 | 证据 / 备注 |
| --- | --- | --- |
| L1 静态校验 | ✅ PASS（两层均 FAIL=0，WARN=0） | ① 类型级：`scripts/validate-type.ps1 -TypePath springboot` → **28 个 md**（含 `examples/sample-project/`），无画线字符、占位符配平、无密钥形态、链接全部存在、必需章节齐全、CODING_STANDARDS §N 引用一致、**docs 必建骨架 12 项齐全**；② 仓库级：`scripts/validate-repo.ps1` → 根文档链接/§N 引用、**类型目录 ↔ §4 登记表双向一致、类型范本 ↔ 样例副本逐字节同源**、版本号 ↔ changelog 全部通过。宿主 Windows PowerShell 5.1（本机无 `pwsh` 7）：两个脚本均为 UTF-8 with BOM，直接 `powershell -File` 运行（见 [docs/decisions.md](../docs/decisions.md) ADR-003） |
| L2 结构/红线复核 | ✅ 通过（含独立 L4 复核发现的冲突修复） | 交付自查清单全项通过；docs 五目录必建且各含最小入口文件（无空壳、无"未触发所以不建"）；**结构口径已统一为分层**（原 AGENTS 目录树 vs CODING_STANDARDS「feature 优先」vs ARCHITECTURE 三方打架已消除）；`dict/` 包"必建"与分层落点的矛盾已修正；ARCHITECTURE Redis key 表补齐 `dict:{typeCode}`；条件句（`若用 Flyway`/`（若配置）`）已改为确定性表述；「参考链接」改为真实可点击链接并补五目录入口；无环境准备内容；无真实密钥/内网地址/账号 |
| L3 动态冒烟 | ✅ **部分执行（无 Docker 切片，2026-09-09）** | 本机 JDK 17.0.12（D:\Java\jdk-17）+ Maven 3.9.16（D:\apache-maven-3.9.16）：在 [`examples/sample-project/`](examples/sample-project/) 执行 `mvn -DskipTests compile` → BUILD SUCCESS；`mvn -DskipTests package` → BUILD SUCCESS（可执行 jar `target/sample-0.0.1-SNAPSHOT.jar`，47MB）；**Boot 3.5.0 / mybatis-plus-spring-boot3-starter 3.5.17 / springdoc 2.8.13 坐标真实可解析并编译通过**。剩余 test/run/curl 需中间件（MySQL/Redis/RabbitMQ），待有 Docker 补跑 |
| L4 评审通道 | 🔶 建议补齐 | 已做 agent 自审（未发现阻断项）；发布前建议再由另一代理按 L1/L2 复核一次 |
| L5 现状核实 | ✅ 已核实（**2026-09-11**，Maven Central 元数据 + 官方 changelog） | Spring Boot 3.5 线最新补丁 **3.5.16**（样例锁 3.5.0，升级为待办）；4.x 最新 **4.1.1**（4.2.0-M1 为里程碑版）；MyBatis-Plus **3.5.17** 仍为最新（[baomidou changelog](https://baomidou.com/resources/changlog/)），jsqlparser 插件需同号显式引入；springdoc **2.x 最新 2.9.1**（Boot 3 线）、3.x 最新 3.1.1（Boot 4 线）；Boot 4 + springdoc webflux-ui 存在已知问题（[#3196](https://github.com/springdoc/springdoc-openapi/issues/3196)，本类型为 WebMvc 不受影响） |

**L3 待执行清单**（有 JDK 17+ / Maven / Docker 的环境补跑后回填；合规样例已装配于 [`examples/sample-project/`](examples/sample-project/)）：

```bash
cd springboot/examples/sample-project
docker compose up -d                  # MySQL/Redis/RabbitMQ（带健康检查）
./mvnw test                           # 上下文测试（需中间件就绪）
./mvnw clean verify                   # 质量门（插件固定集见 docs/CODING_STANDARDS.md §13）
./mvnw spring-boot:run                # 另开终端：
curl http://localhost:8080/actuator/health
# Swagger UI: http://localhost:8080/swagger-ui.html
# 依 AGENTS.md 填 {{}}、裁剪、逐条验证后回填「验证状态」
```

## 维护

- 触发修订：Spring Boot / MyBatis-Plus 大版本升级、标准命令变化、中间件（Redis/RabbitMQ）行为变化、发现模板缺常见坑。
- 修订时同步更新上方「适用范围/主版本线」与本目录 `AGENTS.md`、`docs/CODING_STANDARDS.md`，并在根仓库 [AGENTS.md](../AGENTS.md) 登记表更新状态。
