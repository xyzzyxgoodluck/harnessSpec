# {{项目名}}

{{一句话：这个项目做什么、不做什么。例："订单与库存的 REST API 服务。"}}

- 技术栈：Java {{17 | 21 | 25 LTS，以 pom 为准}} / Spring Boot {{以 pom parent 为准；Boot 3 用 `mybatis-plus-spring-boot3-starter`，升 Boot 4 前先核对 MyBatis-Plus 兼容}} / **MyBatis-Plus** / **MySQL** {{8.x}}
- 中间件：**Redis**（缓存/{{锁}}）、**RabbitMQ**（消息）；API 文档：**Swagger UI**（springdoc，`/swagger-ui.html`）；依赖管理：**Maven**（`./mvnw`，Windows 用 `mvnw.cmd`）
- 形态：{{REST API 服务 | 批处理 | 消息消费者 | …}}

## 环境与版本约束

- 版本单一来源：Java 取 `pom.xml` 的 `<java.version>`；Spring 系归 Boot BOM（`parent`）；非 BOM 组件（MyBatis-Plus、springdoc、jsqlparser 插件等）版本在 `<properties>` 集中声明并注释，禁散落。
- 依赖管理唯一 **Maven**，不混用 Gradle；质量工具链（Spotless/Checkstyle/SpotBugs/Enforcer/JaCoCo）版本锁在 `pom.xml`、规则文件在 `config/`，**文档不写版本号**（检查项口径见 `docs/CODING_STANDARDS.md` §13）。
- 环境准备（装 JDK/Docker、手工起中间件、首次下载耗时）归项目 `README.md` 与 `docs/ARCHITECTURE.md` §9，**不进本文件**。

## 快速开始

```bash
docker compose up -d                 # 启动 MySQL / Redis / RabbitMQ（以下均在项目根执行）
./mvnw spring-boot:run               # 启动应用（Swagger UI：/swagger-ui.html）
curl http://localhost:{{8080}}/actuator/health
./mvnw test                          # 全部测试（集成测试需本机 Docker）
```

## 常用命令

> 未注明时均在项目根执行；质量门与静态检查明细见 `docs/CODING_STANDARDS.md` §13。

| 目的 | 命令 | 备注 |
| --- | --- | --- |
| 启动开发服务 | `./mvnw spring-boot:run` | :{{端口}}；指定 Profile 加 `-Dspring-boot.run.profiles=dev` |
| 全部测试 / 单个测试 | `./mvnw test` ｜ `./mvnw test -Dtest={{XxxServiceTest}}` | 集成测试需 Docker；类不存在加 `-Dsurefire.failIfNoSpecifiedTests=false` |
| 编译 / 打包 / 运行 | `./mvnw -DskipTests compile` ｜ `clean package` | jar 在 `target/{{artifactId}}-{{version}}.jar`，`java -jar` 运行 |
| **质量门（CI 同命令）** | `./mvnw clean verify` | 格式+Checkstyle+SpotBugs+Enforcer+JaCoCo+全部测试；格式修复用 `./mvnw spotless:apply` |
| 添加依赖 | 编辑 `pom.xml` | MyBatis-Plus 版本在 `<properties>` 声明；分页须显式加 `mybatis-plus-jsqlparser` |
| 数据库结构变更 | 新增 `src/main/resources/db/V{{n}}__{{描述}}.sql` 随发布执行 | 一经应用不改；用 Flyway 的项目改记 `./mvnw flyway:migrate` |

## 目录结构与架构

> 目录树为**路径与文件的唯一描述**（职责随注释内联）；五项目录的用途与内容触发器见各 `index.md`，分层映射与依赖方向见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) §3。

```text
{{项目根}}/
  src/main/java/{{基础包}}/
    controller/          # HTTP 适配层（薄）：参数校验 → 调 Service → 返回 DTO
    service/             # 用例编排 / 事务边界 / 业务规则（XxxService + XxxServiceImpl）
    mapper/              # MyBatis-Plus Mapper（BaseMapper/自定义 SQL）；依赖终点，Wrapper 只在此包
    entity/              # 表映射实体（@TableName/@TableLogic/@Version）；纯载体，禁直接出参
    dto/                 # 入参 *Request/*Query、出参 *Response；禁与实体混用
    config/              # 分页插件 / Redis / RabbitMQ / 序列化 / 线程池装配
    common/              # BizException·ErrorCode / Result·PageResult / 常量 / 幂等工具
    listener/            # RabbitMQ 消费者（@RabbitListener）：仅消息适配（手动 ack + 幂等）
  # 字典功能（必建）：DictController/DictService/DictMapper/DictItem 按上列分层落点，不另开 dict/ 包
  src/main/resources/
    application-{profile}.yml        # 按 Profile 拆分（dev/test/prod）；密钥走环境变量
    mapper/*.xml                     # 复杂/多表 SQL（条件构造收口在 mapper 包）
    db/V{{n}}__{{描述}}.sql           # 版本化 DDL（只增不改，随发布执行）
  src/test/java/{{基础包}}/            # JUnit 5（与被测包一一对应：XxxServiceTest / XxxControllerTest）
  docker-compose.yml                   # 本地 MySQL/Redis/RabbitMQ（带健康检查）
  CHANGELOG.md                         # 对外版本变更史（Keep a Changelog）
  docs/                                # 知识库（★=必建；内容按触发器）
    CODING_STANDARDS.md                # ★ 编码规范：本文件全部"细则"的唯一权威
    ARCHITECTURE.md                    # ★ 架构总览：模块划分、关键链路、缓存/消息链路、ADR 索引
    design-docs/index.md               # ★ 设计决策/ADR 索引（一条决策一个文件）
    design-docs/core-beliefs.md        # ★ 架构信条（6 条）全文 + 栈内落地与强制者
    product-specs/index.md             # ★ 需求登记与状态流转
    product-specs/TEMPLATE.md          # ★ 单需求模板（含可测验收标准与变更记录）
    exec-plans/index.md                # ★ 计划索引
    exec-plans/tech-debt-tracker.md    # ★ 技术债台账（发现即登记）
    exec-plans/active/ completed/      # ★ 进行中计划 / 已归档（归档即冻结）
    generated/index.md                 # ★ 生成物登记（本目录唯一手写文件；其余只读）
    references/index.md                # ★ 外部资料登记（浓缩版命名 *-llms.txt）
  target/                              # 构建产物：禁止手改/提交
```

约定：实体 `@TableName` 显式映射表名、逻辑删除 `@TableLogic`、乐观锁 `@Version`、主键策略全库统一；`docs/` 核心文件与五项目录不得为空壳（每目录至少一个最小入口文件，用途与触发器见其 `index.md`）；依赖方向 `controller → service → mapper` 单向（防环，详见「编码约定」）。

关键链路：`HTTP→Controller→Service(事务)→Mapper→MySQL`；缓存读 Redis（key+TTL）未命中回源、**写库后删缓存**；消息 confirm 生产 → Queue → `@RabbitListener` 手动 ack+幂等 → DLQ；异常统一 `@RestControllerAdvice`。

> **固定组成部分落点（六个）**：① docs 布局 → 上方 `docs/`；② Git 提交 → [`docs/CODING_STANDARDS.md`](docs/CODING_STANDARDS.md) §14；③ 架构 → [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)；④ 需求 → [`docs/product-specs/index.md`](docs/product-specs/index.md)；⑤ 信条（6 条）→ [`docs/design-docs/core-beliefs.md`](docs/design-docs/core-beliefs.md) + `ARCHITECTURE.md` §3；⑥ 质量门 → 「测试与质量门」章 + `CODING_STANDARDS.md` §13。对外版本变更记 `CHANGELOG.md`，技术决策记 `docs/design-docs/`，需求验收记 `docs/product-specs/`——三者不互抄（细则见模板仓库 `docs/fixed-docs.md`）。

## 编码约定（只列硬约束；口味类细则见 `docs/CODING_STANDARDS.md` §3/§4/§7/§8/§12）

- **依赖方向**（§3；ArchUnit 强制）：只允许 `controller→service→mapper` 单向；禁反向、越层、**同层横向互依赖**（防调用环）；跨域协作走 `common/` 下沉、RabbitMQ 事件或显式编排 `XxxFlowService`。
- **QueryWrapper 隔离**（§5；ArchUnit 强制）：`QueryWrapper`/`LambdaQueryWrapper`/`UpdateWrapper` 只允许出现在 `mapper/` 包；`CrudRepository`（旧 `IService` 已不推荐）只用主键/实体级方法。
- **错误分类与错误码**（§6；注册表测试强制）：业务错误抛 `BizException(ErrorCode.xxx)`；400/422 与系统/外部 500/502/503 **不泄漏堆栈**（对外 `message+traceId`）；错误码单源枚举、分段 `1系统/2参数/3业务/4认证/5外部`、命名 `{域}_{对象}_{原因}`、只增不删先查重。
- **业务字典**（§5，必建）：可运营可枚举数据落 `{{t_dict_type}}`+`{{t_dict_item}}`（`UPPER_SNAKE_CASE`、`(type_code,item_code)` 唯一）；稳定状态机用枚举不落库；**禁业务代码直写字典表**。

## 测试与质量门

- 合并/提交前必须全绿（CI 与本地同一条命令）：`./mvnw clean verify`；集成测试需本机 Docker。
- 快查单项：`./mvnw spotless:apply checkstyle:check spotbugs:check`；新端点 curl 冒烟记录进 PR 描述（不替代自动化测试）。

## 工作流与发布 {{（按需保留）}}

- 分支 {{feature/* → main}}、SemVer、{{发布步骤}}；**DB 变更先于应用发布**；缓存/MQ 兼容变更考虑滚动发布。

## 约束、禁区与陷阱（红线）

- 禁止修改/提交：`target/`、生成代码、`.env`、IDE 文件（`.idea/`、`*.iml`）。
- 密钥不入代码/配置/日志；错误响应与日志不泄漏堆栈、表名、SQL；生产禁暴露 `/swagger-ui/**` 与 `/v3/api-docs/**`；测试禁连外网与本地手工服务。
- 新 `ErrorCode`、字典 `type_code` 先查重；DB 迁移脚本一经应用不改；生产 DDL 需评审。
- 版本与陷阱：Spring 系归 Boot BOM、MyBatis-Plus 版本在 `<properties>` 声明、分页必须显式引入 `mybatis-plus-jsqlparser`（漏加＝编译不过或静默失效）；其余坑（MP 分页/乐观锁/逻辑删除与唯一索引、MySQL 大表 DDL 与慢 SQL、Redis 一致性/序列化/锁、RabbitMQ 幂等/DLQ/confirm、Windows 用 `mvnw.cmd`、devtools 仅开发依赖）→ `docs/CODING_STANDARDS.md` §5–§8。
