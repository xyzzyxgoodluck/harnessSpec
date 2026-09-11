# sample-project —— springboot 类型合规样例（harness 规则验证载体）

> **角色**：验证 `springboot` 类型的 harness 规则能否在一个真实项目上成立。本目录按 `springboot/AGENTS.md` 的要求装配，是"规则可套用的最小真实项目"。
>
> **当前状态**：`./mvnw clean verify` 真实全绿（Checkstyle 0 违规 / SpotBugs 0 / Enforcer 4 规则 / JaCoCo 达标 / 85 个测试全通过）。

## 它为什么长这样（对照 springboot/AGENTS.md）

| AGENTS.md / CODING_STANDARDS 要求 | 本样例的落地 |
| --- | --- |
| 项目根含 `AGENTS.md`（操作手册） | `AGENTS.md`（类型规范模板本体，保留 `{{}}`——L3 演练"复制→填占位→验证"） |
| `docs/` 必建核心与五项目录 | 同源拷贝（12 个文件）：两核心文件 + 五目录最小入口；目录必建、实质内容按触发器 |
| 技术栈：Boot（3.5 兼容成熟线）/MyBatis-Plus/MySQL/Redis/RabbitMQ/Swagger | `pom.xml`：Boot **3.5.0** + `mybatis-plus-spring-boot3-starter` **3.5.17**（含 `mybatis-plus-jsqlparser`）+ springdoc **2.8.13** |
| 本地中间件 `docker compose up -d` | `docker-compose.yml`（MySQL 8.4 / Redis 7 / RabbitMQ 4-management，带健康检查；MySQL 挂载 `src/main/resources/db` 为 `/docker-entrypoint-initdb.d`） |
| 配置按 Profile、密钥走环境变量 | `application.yml`（公共）+ `application-dev.yml` + `application-prod.yml`；口令一律 `${DB_PASSWORD}` 等环境变量，**无默认值** |
| DB 结构以版本化脚本提交（§5） | `src/main/resources/db/V1__init.sql`（`t_order`/`t_dict_type`/`t_dict_item`，utf8mb4、审计字段、逻辑删除、乐观锁、`(type_code,item_code)` 唯一） |
| 事务在 Service（§5） | `OrderServiceImpl`/`DictServiceImpl` 写方法 `@Transactional`、只读方法 `@Transactional(readOnly = true)` |
| 分页必须有上限（§5） | MP `PaginationInnerInterceptor` + `IPage<Order>` 作 Mapper 自定义方法首参；统一 `PageResult<T>`；`page/size` 默认 1/10、上限 100 |
| 禁止 `SELECT *`（§5） | 所有 Mapper `@Select` 显式列名；Checkstyle `RegexpSingleline` 兜底 |
| 枚举状态机（§5） | `OrderStatus` 枚举 + `@EnumValue`（落库字符串 code），`Order.status` 为枚举类型 |
| 业务字典只读（§5） | `t_dict_type`/`t_dict_item` + `GET /dicts/{typeCode}`；启停走 `DictController → DictService`；ArchUnit 断言"仅 `DictServiceImpl` 可依赖字典 Mapper" |
| Redis（§7） | `RedisConfig`（`GenericJackson2JsonRedisSerializer`，禁 JDK 序列化）；写库后**事务提交后**删 `order:detail:{id}`；字典缓存 `dict:{typeCode}` 带 TTL 与抖动；幂等 `idem:order:paid:{messageId}` |
| RabbitMQ（§8） | `publisher-confirm: correlated`；`acknowledge-mode: manual`；`OrderMqListener` 手动 ack + 幂等 SETNX + 有限重试，耗尽 `basicNack(requeue=false)` 经 DLX 进 DLQ；DLQ 与 DLX 之间**有成对的 Binding**；消息体 `Jackson2JsonMessageConverter` |
| 错误响应带 traceId（§6/§9） | `Result.traceId` + `TraceIdFilter`（MDC 贯穿、支持 `X-Trace-Id` 透传）；`logging.pattern.level` 带 `%X{traceId}` |
| 错误码单源、分段（§6） | `common/ErrorCode`（1系统/2参数/3业务/4认证/5外部依赖）；`ErrorCodeRegistryTest` 断言分段/命名/查重/快照（只增不删） |
| 架构规则机器化（§3/§5） | `ArchitectureRulesTest`（ArchUnit）：无环、禁反向/越层、controller 不用条件构造器、字典表只经 DictService |
| 快速开始可执行、`./mvnw` wrapper | 已含 `mvnw`/`mvnw.cmd`（wrapper 3.3.4，`distributionType=only-script`）；`.\mvnw.cmd -v` 与 `.\mvnw.cmd -B clean verify` 均已实测通过 |

## 本地运行

```bash
# 1) 中间件（MySQL 8.4 / Redis 7 / RabbitMQ 4；MySQL 首次启动自动执行 db/V1__init.sql）
docker compose up -d

# 2) 环境变量（值取 docker compose 的本地配置；禁止入库，.env 已在 .gitignore）
#    需要：DB_USERNAME DB_PASSWORD RABBITMQ_USERNAME RABBITMQ_PASSWORD
#    Windows PowerShell 示例（把 <...> 换成实际值）：
$env:DB_USERNAME='<db-user>'; $env:DB_PASSWORD='<db-password>'
$env:RABBITMQ_USERNAME='<mq-user>'; $env:RABBITMQ_PASSWORD='<mq-password>'

# 3) 质量门与启动（Windows 用 mvnw.cmd）
./mvnw clean verify
./mvnw spring-boot:run
curl http://localhost:8080/actuator/health
# Swagger UI：http://localhost:8080/swagger-ui.html
```

## 验证状态（L1–L5，协议见根仓库 docs/authoring-types.md §5）

- **L1 静态**：`scripts/validate-type.ps1 -TypePath springboot/examples/sample-project` → ✅ PASS。
- **L2 结构复核**：上表逐项成立。
- **L3 动态冒烟（无 Docker 切片，2026-09-11）**：
  - `mvn -B clean verify` → **BUILD SUCCESS（exit 0）**：Tests run 85, Failures 0, Errors 0；
    Checkstyle `0 violations`；SpotBugs `BugInstance size is 0` / `Error size is 0`；
    Enforcer 4 条规则全部 passed；JaCoCo `All coverage checks have been met.`（LINE ≥ 0.80，实测 bundle 86.68%，门槛口径内约 0.98）。
  - `.\mvnw.cmd -v` → Apache Maven 3.9.16 / Java 17.0.12；`.\mvnw.cmd -B clean verify` → 见下节结论。
  - **未执行（需 Docker）**：`@SpringBootTest` 全上下文与 Testcontainers 集成测试（MySQL/Redis/RabbitMQ 真实链路）；本样例的自动化测试全部为切片/单测，**不依赖中间件**。
  - **待人工 curl 冒烟**（需 `docker compose up -d` 就绪）：

```bash
curl -i "http://localhost:8080/orders?page=1&size=10"                 # 200 + PageResult
curl -i "http://localhost:8080/orders/999999"                         # 404 + 310001（含 traceId）
curl -i -X POST "http://localhost:8080/orders" -H "Content-Type: application/json" \
     -d '{"orderNo":"NO001","amountFen":100}'                          # 200 + CREATED
curl -i -X POST "http://localhost:8080/orders" -H "Content-Type: application/json" -d '{}'  # 400 + 200001
curl -i "http://localhost:8080/dicts/CHANNEL"                         # 200 + 字典项
```

- **版本坐标**：Boot `3.5.0` 为基线且已编译通过；升级 3.5.x 补丁可在有网环境用 `mvn versions:display-dependency-update` 评估后回填。
- **L4/L5**：见 `springboot/README.md`「验证状态」。

## 维护

类型 `springboot/AGENTS.md`、`springboot/docs/*` 变更后，重跑"装配"（复制这两处到本目录）保持同源；不要在本目录另起一套内容。
