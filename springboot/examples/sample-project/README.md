# sample-project —— springboot 类型合规样例（harness 规则验证载体）

> **角色**：验证 `springboot` 类型的 harness 规则能否在一个真实项目上成立。本目录按 `springboot/AGENTS.md` 的要求装配，是"规则可套用的最小真实项目"。

## 它为什么长这样（对照 springboot/AGENTS.md）

| AGENTS.md 要求 | 本样例的落地 |
| --- | --- |
| 项目根含 `AGENTS.md`（操作手册） | `AGENTS.md`（类型规范模板本体，保留 `{{}}`——L3 演练"复制→填占位→验证"） |
| `docs/` 必建核心：`CODING_STANDARDS.md`、`ARCHITECTURE.md` | `docs/CODING_STANDARDS.md`、`docs/ARCHITECTURE.md`（同源拷贝） |
| 触发器子目录（design-docs/product-specs/exec-plans/generated/references） | 未触发 → 不建（不为建而建） |
| 技术栈：Boot（3.5 兼容成熟线）/MyBatis-Plus/MySQL/Redis/RabbitMQ/Swagger | `pom.xml`：Boot **3.5.0** + `mybatis-plus-spring-boot3-starter` **3.5.17** + springdoc **2.8.13** + `mysql-connector-j`（BOM 管） |
| 本地中间件 `docker compose up -d` | `docker-compose.yml`（MySQL 8.4 / Redis 7 / RabbitMQ 4-management，带健康检查） |
| 配置按 Profile、密钥走环境变量 | `application.yml`（`${DB_PASSWORD}` 等） |
| 快速开始可执行、`./mvnw` wrapper | 已含 `mvnw`/`mvnw.cmd`（Initializr 生成） |

## 验证状态（L1–L5，协议见根仓库 docs/authoring-types.md §5）

- **L1 静态**：`pwsh scripts/validate-type.ps1 -TypePath springboot/examples/sample-project` → ✅ PASS（FAIL=0）。
- **L2 结构复核**：上表逐项成立。
- **L3 动态冒烟（✅ 部分执行 2026-09-09，无 Docker 切片）**：
  - `mvn -DskipTests compile` → **BUILD SUCCESS**（JDK 17.0.12 + Maven 3.9.16）
  - `mvn -DskipTests package` → **BUILD SUCCESS**，可执行 jar `target/sample-0.0.1-SNAPSHOT.jar`（47MB）
  - ⇒ Boot 3.5.0 / mybatis-plus-spring-boot3-starter 3.5.17 / springdoc 2.8.13 **坐标真实可解析、编译通过**。
  - **剩余待补（需中间件 → Docker）**：

```bash
docker compose up -d          # MySQL/Redis/RabbitMQ
./mvnw test                   # @SpringBootTest 上下文测试（需中间件就绪）
./mvnw spring-boot:run        # 另开终端：
curl http://localhost:8080/actuator/health
# Swagger UI: http://localhost:8080/swagger-ui.html
```

- **版本坐标**：Boot `3.5.0` 为基线且已编译通过；升级到最新 3.5.x 补丁可在有网环境用 `mvn versions:display-dependency-updates` 评估后回填。
- **L4/L5**：见 `springboot/README.md`「验证状态」。

## 维护

类型 `springboot/AGENTS.md`、`springboot/docs/*` 变更后，重跑"装配"（复制这两处到本目录）保持同源；不要在本目录另起一套内容。
