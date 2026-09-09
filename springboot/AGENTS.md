# {{项目名}}

{{一句话：这个项目做什么、不做什么。例："订单与库存的 REST API 服务。"}}

- 技术栈：Java {{17 | 21 | 25 LTS，以 pom 实际为准}} / Spring Boot {{以 pom parent 为准；Boot 3 用 `mybatis-plus-spring-boot3-starter`，升 Boot 4 前先核对 MyBatis-Plus 兼容}} / **MyBatis-Plus**（版本独立声明，不在 Boot BOM）/ **MySQL** {{8.x}}
- 中间件：**Redis**（缓存/{{锁}}）、**RabbitMQ**（消息）；API 可视化：**Swagger UI**（springdoc-openapi，`/swagger-ui.html`、`/v3/api-docs`）
- 依赖管理：Maven（`./mvnw`，Windows 用 `mvnw.cmd`）；形态：{{REST API 服务 | 批处理 | 消息消费者 | …}}

## 快速开始

```bash
# 在项目根执行：
docker compose up -d                 # 启动 MySQL / Redis / RabbitMQ
./mvnw spring-boot:run               # 启动应用
curl http://localhost:{{8080}}/actuator/health
# Swagger UI：http://localhost:{{8080}}/swagger-ui.html
./mvnw test                          # 全部测试（集成测试需本机 Docker）
```

## 常用命令

> 未注明时均在项目根执行。质量门与静态检查明细见 `docs/CODING_STANDARDS.md` §13。

| 目的 | 命令 | 备注 |
| --- | --- | --- |
| 启动开发服务 | `./mvnw spring-boot:run` | :{{端口}}；热重载需 devtools（可选） |
| 指定 Profile | `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | Gradle：`./gradlew bootRun --args='--spring.profiles.active=dev'` |
| 全部测试 | `./mvnw test` | 集成测试需 Docker |
| 单个测试 | `./mvnw test -Dtest={{XxxServiceTest}}` | 类不存在加 `-Dsurefire.failIfNoSpecifiedTests=false` |
| 编译/打包 | `./mvnw -DskipTests compile` / `clean package` | jar 在 `target/{{artifactId}}-{{version}}.jar` |
| 运行产物 | `java -jar target/{{artifactId}}-{{version}}.jar` | |
| 格式修复 | `./mvnw spotless:apply` | 提交前执行 |
| **质量门（CI 同命令）** | `./mvnw clean verify` | 绑定格式+Checkstyle+SpotBugs+Enforcer+JaCoCo+全部测试 |
| 添加依赖 | 编辑 `pom.xml` | MyBatis-Plus 版本在 `<properties>` 显式声明（见禁区） |
| 数据库结构变更 | 新增 `src/main/resources/db/V{{n}}__{{描述}}.sql` 随发布执行 | 若用 Flyway：`./mvnw flyway:migrate`；脚本一经应用不改 |
| 生成代码 | {{MyBatis-Plus Generator 运行命令}}（若配置） | 生成物**勿手改** |

## 目录结构与架构

> 项目结构以**下方目录树为唯一描述**（职责随注释内联）；命名与依赖方向见「编码约定」。

```text
{{项目根}}/
  src/main/java/{{基础包}}/
    controller/          # HTTP 适配层：参数校验、调 Service、返回 DTO（薄）
    service/             # 用例编排 / 事务边界 / 业务规则
    mapper/              # MyBatis-Plus Mapper（BaseMapper/自定义 SQL）；依赖终点，Wrapper 只在此包
    entity/              # 表映射实体（@TableName 等），纯载体，禁直接出参
    dto/                 # 入参（*Request/*Query）与出参（*Response），禁与实体混用
    config/              # 分页插件/Redis/RabbitMQ/序列化/线程池配置
    common/              # BizException/ErrorCode、Result/PageResult、常量、幂等工具
    dict/                # 元数据字典（必建）：DictType/DictItem + DictMapper（只读）+ DictController
  src/main/resources/
    application-{profile}.yml  # 按 Profile 拆分；密钥走环境变量
    mapper/*.xml        # 复杂/多表 SQL（条件构造收口在 mapper 包）
    db/                 # 版本化 SQL 脚本 V{{n}}__{{描述}}.sql（只增不改）
  src/test/java/        # JUnit 5 测试（与被测包一一对应）
  docker-compose.yml    # 本地 MySQL/Redis/RabbitMQ（带健康检查）
  docs/                 # 知识库：固定组成部分（★=必建；子目录按确定性触发器建立）
    CODING_STANDARDS.md # ★ 编码规范（本文件全部"细则"的唯一权威）
    ARCHITECTURE.md     # ★ 架构总览（模块划分、关键链路、缓存/消息链路、ADR 索引）
    design-docs/        # 触发器：首个设计决策/ADR 时建（core-beliefs.md、index.md）
    product-specs/      # 触发器：有需求/规格时建（index.md + 每需求一文件）
    exec-plans/         # 触发器：多轮/多代理任务或技术债（active/、completed/、tech-debt-tracker.md）
    generated/          # 触发器：首个自动生成物入库时建；只读，标注生成命令
    references/         # 触发器：引入外部资料时建；LLM 浓缩版命名 *-llms.txt
  target/               # 构建产物：禁止手改/提交
```

约定：实体 `@TableName` 显式映射表名、逻辑删除 `@TableLogic`、乐观锁 `@Version`、主键策略全库统一；`docs/` 核心文件（CODING_STANDARDS.md、ARCHITECTURE.md）不得为空壳；依赖方向 `controller → service → mapper` 单向（防环，详见「编码约定」）。

关键链路：同步 `HTTP→Controller→Service(事务)→Mapper→MySQL`；缓存：读 Redis（key+TTL）未命中回源、**写库后删缓存**；消息：RabbitMQ confirm 生产 → Queue → `@RabbitListener` 手动 ack+幂等 → DLQ。异常统一 `@RestControllerAdvice`。

## 编码约定（必须摘要；细则以 `docs/CODING_STANDARDS.md` 为准）

- **命名与分层**（§3）：Controller `XxxController`；Service `XxxService`+`XxxServiceImpl`；Mapper `XxxMapper`；实体 `Xxx`（表 `t_xxx`）；入参 `XxxCreateRequest`/`XxxUpdateRequest`/`XxxPageQuery`；出参 `XxxResponse`。禁拼音缩写、DTO 禁一物两用。
- **DAO 前缀**（§3）：Mapper 方法名 `select`/`count`/`insert`/`update`/`delete` + 宾语；禁 `query`/`find`/`get` 开头（归 Service 层）。
- **依赖方向**（§3）：只允许 `controller→service→mapper` 单向；禁反向、越层、**同层横向互依赖**（防调用环）；跨域协作：`common/` 下沉 / RabbitMQ 事件 / 显式编排 `XxxFlowService`。
- **QueryWrapper 隔离**（§5）：`QueryWrapper`/`LambdaQueryWrapper`/`UpdateWrapper` 只允许出现在 `mapper/` 包；`IService` 体系只用主键/实体级方法，不调用 wrapper 重载。
- **中间件命名**（§7/§8）：Redis key `{{域}}:{{对象}}:{{id}}`（`lock:`/`idem:` 前缀+TTL）；RabbitMQ `{{svc}}.{{域}}.exchange` / `{{域}}.{{事件}}` / `...{{事件}}.queue` + `.dlx`/`.dlq`——全部常量类登记，禁魔法字符串。
- **API 文档**（§4）：Swagger UI + `@Tag`/`@Operation`/`@Schema`（中文描述+示例）；生产禁用或鉴权；示例禁敏感值。
- **错误分类与错误码**（§6）：业务错误抛 `BizException(ErrorCode.xxx)`；参数 400/422、系统/外部兜底 500/502/503 **不泄漏堆栈**（对外 `message+traceId`）；`common/ErrorCode` 单源枚举，分段 `1系统/2参数/3业务/4认证/5外部`，命名 `{域}_{对象}_{原因}`，只增不删、先查重。
- **Git 提交**（§14）：`<type>(<scope>): <subject>`；type：`feat/fix/docs/style/refactor/perf/test/build/ci/chore/revert`；subject 语言全库统一；一个提交一个逻辑变更；提交前过质量门。
- **测试与自测**（§12）：业务方法必须有 JUnit（正常+每个异常分支断言 `ErrorCode`+边界+写库结果），无测试不得提交；Controller 端点完成后 **curl 真实验证**成功与失败分支并记录 PR。
- **业务字典**（§5，必建）：可运营可枚举数据落 `{{t_dict_type}}`+`{{t_dict_item}}`（`UPPER_SNAKE_CASE`、`(type_code,item_code)` 唯一）；稳定状态机用枚举不落库；只读 `GET /dicts/{typeCode}` 或共享 `DictMapper`；**禁业务代码直写字典表**。
- **测试约定**（§12）：Service mock Mapper；Controller `@WebMvcTest`+`@MockitoBean`；集成用 Testcontainers（`@ServiceConnection`）；Lombok/注入风格全库统一（构造器注入优先）。

## 测试与质量门

- 合并/提交前必须全绿：`./mvnw clean verify`（CI 与本地同一条命令；集成测试需本机 Docker）。
- 快速单项：`./mvnw spotless:apply checkstyle:check spotbugs:check`。
- 新端点 curl 冒烟记录进 PR 描述（不替代自动化测试）。

## 工作流与发布 {{（按需保留）}}

- 分支 {{feature/* → main}}、SemVer、{{发布步骤}}；**DB 变更先于应用发布**；缓存/MQ 兼容变更考虑滚动发布。

## 约束、禁区与陷阱（红线；完整陷阱清单见 CODING_STANDARDS §5–§8）

- 禁止修改/提交：`target/`、生成代码、`.env`、IDE 文件（`.idea/`、`*.iml`）。
- 密钥不入代码/配置/日志；测试禁连外网与本地手工服务；生产 DDL 需评审。
- 错误响应/日志不泄漏堆栈、表名、SQL；生产禁暴露 `/swagger-ui/**` 与 `/v3/api-docs/**`。
- 新 `ErrorCode`、字典 `type_code` 先查重；DB 迁移脚本一经应用不改。
- 版本：Spring 系归 Boot BOM；MyBatis-Plus 不在 BOM（pom `<properties>` 显式声明并注释）；`mysql-connector-j` 勿手改版本。
- 陷阱细则（MP 分页/乐观锁/逻辑删除与唯一索引、MySQL 大表 DDL/慢 SQL、Redis 缓存一致性/JDK 序列化/Redisson 锁、RabbitMQ 幂等/DLQ/confirm、Windows 用 `mvnw.cmd`、devtools 仅开发依赖）→ 见 `docs/CODING_STANDARDS.md` §5–§8。

## 参考链接

- {{架构：docs/ARCHITECTURE.md}} ｜ {{编码规范：docs/CODING_STANDARDS.md}}
- Spring Boot：https://docs.spring.io/spring-boot/ ｜ MyBatis-Plus：https://baomidou.com/ ｜ springdoc：https://springdoc.org/ ｜ 脚手架：https://start.spring.io/
- 本类型适用范围与版本对照：见本目录 `README.md`
