# springboot 类型 —— 说明

本目录对应项目类型：**Spring Boot + MyBatis-Plus + MySQL 服务端**（集成 Redis 与 RabbitMQ）。`springboot/AGENTS.md` 为该类型的规范/模板交付物。

## 适用范围

- **适用**：采用以下**固定技术栈**的服务端应用 —— REST API、批处理任务、消息消费者/生产者、带 Web 层的后台服务：
  - Web/框架：Spring Boot（Java 17+，建议 LTS 21/25）
  - ORM：MyBatis-Plus（`mybatis-plus-spring-boot3-starter`；Boot 4 需先核对官方兼容）
  - 数据库：MySQL 8.x
  - 中间件：Redis（缓存/分布式锁）、RabbitMQ（消息/异步解耦）
  - API 文档：Swagger UI（springdoc-openapi，本地 `/swagger-ui.html`）
- **主版本线**（截至 2026 年中）：**Spring Boot 3.5.x 为 MyBatis-Plus 兼容成熟线**（推荐默认）；Spring Boot 4.1.x 已发布，但升级前必须对照 [MyBatis-Plus changelog](https://baomidou.com/resources/changlog/) 确认对应兼容 starter/版本，再在本模板填实。
- **不适用**：纯前端、非 Spring 的 Java 项目、Android；以 JPA/Hibernate、PostgreSQL 等其它数据栈为主的 Spring 项目（如需可另立类型）；Spring Cloud 全家桶微服务骨架（可另立 `spring-cloud-microservice` 类型）。

## 如何使用本模板

1. 将 `AGENTS.md` 复制到真实 Spring Boot 项目**根目录**。
2. 全文替换 `{{...}}` 占位符为项目实际值：版本以 `pom.xml` 为准（**MyBatis-Plus 版本在 `<properties>` 独立声明**）；中间件端口/库名与 `docker-compose.yml` 对齐。
3. 按需裁剪标注「按需保留或删除」的章节（如「工作流与发布」）。
4. 在真实项目中**逐条实际执行**「快速开始」与「常用命令」验证（Maven/Gradle 二选一后删去另一行）。
5. 对照 [docs/authoring-types.md](../docs/authoring-types.md)「交付自查清单」逐项核对。
6. **docs/ 知识库（固定组成部分，必建）**：按模板「目录结构与架构」的骨架创建 `docs/`。核心文件必建：`docs/CODING_STANDARDS.md`（至少覆盖：命名与包结构、MyBatis-Plus/SQL、Redis、RabbitMQ、事务与异常边界、日志规范、DTO 映射、测试与格式化规则，对应 Checkstyle / Spotless / spring-javaformat 配置）、`docs/ARCHITECTURE.md`（至少写出模块划分、关键链路与缓存/消息链路）。子目录按确定性触发器建立：存在设计决策/ADR → `design-docs/`；存在需求文档 → `product-specs/`；存在多轮/多代理任务或技术债 → `exec-plans/`（含 `tech-debt-tracker.md`）；存在自动生成物 → `generated/`（只读）；存在外部参考资料 → `references/`。**可直接复制的范本**（复制到真实项目 `docs/` 后按团队实际裁剪）：[`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md)、[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)（骨架遵循根仓库 [docs/fixed-docs.md](../docs/fixed-docs.md) 的「架构文档」固定规范：定位/版本基线/分层依赖/关键链路/数据/中间件/安全/可观测/部署/ADR）；需求规格范本见 [`docs/product-specs/`](docs/product-specs/)（`index.md` + `TEMPLATE.md`，骨架遵循 `fixed-docs.md`「需求规格」）。

> 若项目用 Gradle 而非 Maven，替换命令为备注列的 `./gradlew ...`，并在第 1 章声明；其余结构不变。

## 关键约定（为何这样写）

- **命令真实可验证**：`./mvnw`/`mvnw.cmd`（Maven Wrapper）是主流脚手架（start.spring.io）默认产物，不依赖全局安装。
- **版本单一来源**：Spring 系版本指向 `pom.xml` 的 `parent`（Boot BOM）；MyBatis-Plus 不在 Boot BOM，模板要求版本在 `<properties>` 显式声明并注释，避免版本漂移。
- **陷阱覆盖**：MyBatis-Plus 版本兼容与分页插件/逻辑删除/乐观锁、MySQL DDL 与慢 SQL、Redis 缓存一致性与分布式锁、RabbitMQ 幂等/死信/确认等是本栈最高频踩坑点，全部写入「约束、禁区与陷阱」与 CODING_STANDARDS 范本。
- **质量门确定化（Maven 插件固定集）**：Spotless（格式校验）、Checkstyle（Lint）、SpotBugs（缺陷分析）、Enforcer（版本/依赖规则）、JaCoCo（覆盖率门禁）——本地与 CI 统一收敛于 `./mvnw clean verify`；规则进 pom/XML 配置，生成代码从检查中排除；详见模板「测试与质量门」与 CODING_STANDARDS 范本 §13。
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
| L1 静态校验 | ✅ PASS（FAIL=0，WARN=0） | `pwsh -File scripts/validate-type.ps1 -TypePath springboot`；10 个 md 无画线字符、占位符配平、无密钥形态、链接全部存在、必需章节齐全、CODING_STANDARDS §N 引用一致 |
| L2 结构/红线复核 | ✅ 通过 | 交付自查清单全项通过；AGENTS「编码约定」指针（§3/§5/§6/§7/§8/§12/§13/§14）与 CODING_STANDARDS 实际章节一一对应；无环境准备内容；无"若有 docs/"假设句 |
| L3 动态冒烟 | ✅ **部分执行（无 Docker 切片，2026-09-09）** | 本机 JDK 17.0.12（D:\Java\jdk-17）+ Maven 3.9.16（D:\apache-maven-3.9.16）：在 [`examples/sample-project/`](examples/sample-project/) 执行 `mvn -DskipTests compile` → BUILD SUCCESS；`mvn -DskipTests package` → BUILD SUCCESS（可执行 jar `target/sample-0.0.1-SNAPSHOT.jar`，47MB）；**Boot 3.5.0 / mybatis-plus-spring-boot3-starter 3.5.17 / springdoc 2.8.13 坐标真实可解析并编译通过**。剩余 test/run/curl 需中间件（MySQL/Redis/RabbitMQ），待有 Docker 补跑 |
| L4 评审通道 | 🔶 建议补齐 | 已做 agent 自审（未发现阻断项）；发布前建议再由另一代理按 L1/L2 复核一次 |
| L5 现状核实 | ✅ 已核实（2026-09-09） | MyBatis-Plus 最新 **v3.5.17**（[baomidou](https://baomidou.com/resources/changlog/)）；springdoc-openapi **v3.0.1**（[release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.0.1)，Boot 4 用 3.x、Boot 3 用 2.x——样例用 2.8.13 编译通过）；Boot 4 + springdoc webflux-ui 存在已知问题（[#3196](https://github.com/springdoc/springdoc-openapi/issues/3196)，本类型为 WebMvc 不受影响） |

**L3 待执行清单**（有 JDK 17+ / Maven / Docker 的环境补跑后回填；合规样例已装配于 [`examples/sample-project/`](examples/sample-project/)）：

```bash
cd springboot/examples/sample-project
docker compose up -d                  # MySQL/Redis/RabbitMQ（带健康检查）
./mvnw test                           # 上下文测试（需中间件就绪）
./mvnw clean verify                   # 质量门（需先按 AGENTS §13 装配插件）
./mvnw spring-boot:run                # 另开终端：
curl http://localhost:8080/actuator/health
# Swagger UI: http://localhost:8080/swagger-ui.html
# 依 AGENTS.md 填 {{}}、裁剪、逐条验证后回填「验证状态」
```

## 维护

- 触发修订：Spring Boot / MyBatis-Plus 大版本升级、标准命令变化、中间件（Redis/RabbitMQ）行为变化、发现模板缺常见坑。
- 修订时同步更新上方「适用范围/主版本线」与本目录 `AGENTS.md`、`docs/CODING_STANDARDS.md`，并在根仓库 [AGENTS.md](../AGENTS.md) 登记表更新状态。
