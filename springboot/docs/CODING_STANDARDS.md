# Java / Spring Boot（MyBatis-Plus + MySQL）编码规范（示例范本）

> 本文件是 **springboot 类型交付物的一部分**（可复制范本）：团队采用时将其放入真实项目 `docs/CODING_STANDARDS.md`，按自身情况**裁剪**后执行，并同步更新该真实项目 `AGENTS.md`「编码约定」章节的指向。
>
> 措辞级别：**必须**=强制（违反即 CI/评审不通过）；**应该**=默认做法，例外需注释说明；**可以**=可选。
> 规则冲突时，以本文件细则为准；工具能自动执行的（格式/静态检查）交给工具，不人工争论。

---

## 1. 通用与命名

- **必须**：类名 `PascalCase`（`OrderService`、`OrderMapper`）、方法/局部变量 `camelCase`、常量 `UPPER_SNAKE_CASE`、包名全小写（`com.{{company}}.{{product}}.order`）。
- **必须**：测试类与被测类同名加 `Test`（`OrderServiceTest`）；测试方法用"行为描述"命名（`shouldRejectOrderWhenStockInsufficient`）。
- **应该**：布尔方法/字段以 `is/has/can/should` 开头；集合字段用复数或类型名，不用 `data`/`list` 这类无信息量命名。
- **应该**：类内成员顺序：常量 → 静态成员 → 依赖（构造器注入字段）→ 构造器 → 公有方法 → 私有方法。
- **禁止**：魔法数字散落业务代码——有业务含义的数值提为具名常量或配置；`null` 判断语义化（`Optional`/`Objects.requireNonNull` 视场景）。
- **必须**：代码标识一律英文，业务名词可登记中英对照词典（放 `docs/design-docs/core-beliefs.md`）；**禁止**拼音缩写、无意义缩写与同义混用（`query`/`find` 只留一个）。分层组件与数据对象的命名遵循 §3 固定词典，DTO 禁止一物两用。

## 2. 依赖注入与对象装配

- **必须**：构造器注入。推荐 Lombok `@RequiredArgsConstructor` + `private final` 字段；**禁止**字段注入（`@Autowired` 打在字段上）与 setter 注入（历史遗留除外，需注释说明）。
- **禁止**：在需要 Spring 管理的对象里手动 `new` 依赖 Bean 自行装配（如 `new OrderService(mapper)`）；装配一律交给 Spring 容器。值对象/纯工具类不在此列。
- **应该**：`@Service`/`@Mapper`/`@RestController`/`@Component` 按语义选用；Mapper 接口统一标 `@Mapper` 或由 `@MapperScan` 统一扫描（二选一，全库一致），避免重复标注。
- **禁止**：循环依赖；出现时通过拆分职责或引入中间层解决，**不允许**用 `@Lazy` 掩盖。

## 3. 分层职责与包结构

包结构按功能域或分层组织，**二选一并全库统一**（示例采用 feature 优先 + 固定技术层子包）：

```text
com.{{company}}.{{product}}
  order/
    OrderController.java     # 仅 HTTP 适配：参数校验、调用 Service、返回 DTO
    OrderService.java        # 用例编排、事务边界、业务规则
    OrderMapper.java         # MyBatis-Plus Mapper（继承 BaseMapper / 自定义 SQL）
    Order.java               # entity：与表 t_order 对应（@TableName）
    OrderDTO.java            # 入参/出参 DTO
    OrderMQListener.java     # RabbitMQ 消费者（@RabbitListener），仅做消息适配
  common/                    # 跨域共享：异常（BizException/ErrorCode）、Result、分页、常量、幂等工具
  dict/                      # 基础字典（元数据字典表）：DictType/DictItem、DictMapper、DictController
  config/                    # 配置类：分页插件、Redis、RabbitMQ、序列化、线程池
```

- **必须**：Controller 薄——不做业务判断、不写 SQL/事务、不直接操作 Mapper；Service 承载业务规则与事务；Mapper 只做数据访问。
- **必须**：entity 只做表映射（`@TableName`、`@TableId`、`@TableLogic`、`@Version` 等），**不含业务方法**，**禁止直接作为出参**暴露给外部（用 DTO，见第 4 节）。
- **应该**：MQ 消费者类只做"取消息 → 调 Service"的适配，幂等与重试策略集中（见第 8 节）。

### 命名对照表（固定词典，全库统一）

| 组件 | 命名 | 说明 |
| --- | --- | --- |
| Controller | `OrderController` | 薄；方法=用例动词：`createOrder`/`getOrderById`/`updateOrder`/`cancelOrder`/`pageOrders`；REST 路径资源复数 `/orders` |
| Service 接口 | `OrderService` | MP 体系可 `extends IService<Order>`；方法同用例动词；事务边界所在 |
| Service 实现 | `OrderServiceImpl` | 默认 `extends ServiceImpl<OrderMapper, Order> implements OrderService` |
| Mapper（DAO） | `OrderMapper extends BaseMapper<Order>` | 同名 XML `OrderMapper.xml`；自定义方法 `selectOrderList`/`countXxx`/`updateXxxStatus`（动词+宾语） |
| Entity | `Order`（表 `t_order`，`@TableName` 显式） | 纯数据载体，见第 3/5 节 |
| 入参对象 | `OrderCreateRequest`/`OrderUpdateRequest`/`OrderPageQuery` | 分组校验用 `groups` |
| 出参对象 | `OrderResponse`（列表项 `OrderItemResponse`） | 列表统一 `PageResult<T>` |
| MQ 监听 | `OrderMqListener` | `@RabbitListener` 只做消息适配 |
| 常量类/枚举 | `OrderCacheKeys`（Redis key）、`MqDestinations`（交换机/队列）、`ErrorCode`（错误码） | 见第 6/7/8 节，禁止魔法字符串 |

方法动词词典（必须）：读 `get`（单条）/`list`（多条）/`page`（分页）/`count`；写 `create`/`update`/`delete`/`enable`/`disable`/`cancel` 等业务动词 + 宾语（`updateOrderStatus`）。同一语义全库只保留一个动词。

### DAO（Mapper）方法前缀词典（必须）

Mapper 自定义方法名**必须**以统一 SQL 动词开头，与 Service 层动词**分层隔离**（两层各用各的词表，禁止混用/相互替代）：

| 前缀 | 语义 | 示例 | 禁止替代 |
| --- | --- | --- | --- |
| `select` | 查询（单条/列表/分页） | `selectOrderPage` / `selectOrderByNo` | `get`/`find`/`query`/`fetch`（Service 层才用） |
| `count` | 计数 | `countOrderByStatus` | — |
| `insert` | 新增 | `insertOrderBatch` | `create`/`add`（Service 层动词） |
| `update` | 修改 | `updateOrderStatus` | `modify`/`edit` |
| `delete` | 删除（逻辑删除语义同为删除） | `deleteOrderById` | `remove`/`drop` |

规则（必须）：

1. 方法名 = 前缀 + 业务宾语（对象/表 + 条件限定）：`selectOrderListByStatus`、`updateOrderStatus`、`countOrderByStatus`。
2. 单条 `selectXxxById`/`selectXxxByNo`；列表 `selectXxxList...`；分页 `selectXxxPage...`（返回 `IPage<...>`）。
3. 继承自 `BaseMapper` 的内建方法（`selectById`/`insert`/`updateById`/`deleteById`/`selectPage`/`exists` 等）保持 MP 官方命名，不另起别名。
4. 禁止以 `_`/数字/非动词开头；禁止 `queryByXxx`/`findXxx`/`getXxx` 混入 Mapper。
5. 应该：用 ArchUnit 或 Checkstyle 正则断言 `mapper` 包内自定义 public 方法满足前缀词表（纳入 `./mvnw clean verify`，见第 13 节）。

### 依赖方向约束（必须，防调用环）

- 依赖只允许**单向向下**：`Controller → Service → Mapper（DAO）→ MySQL/中间件`；每一处调用只能沿箭头方向。
- **禁止反向依赖**：下层不得反向依赖上层（Mapper 不注入 Service、Service 不注入 Controller、`common/` 不被业务层反向依赖）。
- **禁止越层依赖**：Controller 不得直接使用 Mapper 等持久层对象；中间件客户端（`RedisTemplate`/`RabbitTemplate`/连接工厂）仅限 Service 层及以下使用。
- **禁止同层横向互依赖**：`XxxController` ↔ `YyyController`、`XxxService` ↔ `YyyService`、`XxxMapper` ↔ `YyyMapper` 一律禁止——横向依赖是调用环的主要来源，评审必打回。
- 跨域协作只允许以下三种出口（按优先级）：
  1. **下沉**：多个 Service 复用的无状态能力下沉到 `common/`（工具、常量、上下文、门面），各业务 Service 只依赖 common，彼此不互依赖；
  2. **事件解耦**：跨域最终一致的协作发 RabbitMQ 事件（见第 8 节），如订单发 `order.created`、库存监听处理——不要为协作而同步互相调用；
  3. **显式编排**：确需同步编排多个领域的用例，用独立编排服务 `XxxFlowService`（命名含 `Flow`/`Orchestration`，是**唯一**允许依赖多个领域 Service 的上层组件）；领域 Service **禁止**反向依赖编排服务。
- **应该**：用 ArchUnit（`archunit-junit5`）写架构测试固化本约束——分层依赖、禁止反向/越层/横向，并断言 `cycles().should().beFree()`；纳入 `./mvnw verify`（见第 13 节），实现"依赖方向违规即构建失败"。

## 4. DTO / 参数校验 / API

- **必须**：入参 DTO 用 Jakarta Validation 注解（`@NotNull`/`@NotBlank`/`@Size`/`@Email`/`@Positive` 等），Controller 参数加 `@Valid` / `@Validated`；分组校验用 `groups`。
- **必须**：出参不直接暴露 entity 与内部结构；分页返回统一结构（如 `PageResult<T>`：`items/total/page/pageSize`）。
- **应该**：字段映射集中在 DTO 转换器/`MapStruct`，不散落各处手写 getter/setter 拷贝；禁止在 entity 上为出参加临时字段。
- **必须**：对外 API 用 OpenAPI 注解（`@Tag`/`@Operation`/`@Parameter`/`@Schema`）说明语义，给出中文描述与示例值；接口变更走版本化，破坏性变更需评审。
- **必须**：API 可视化统一 **Swagger UI**（springdoc-openapi：Boot 3 用 `springdoc-openapi-starter-webmvc-ui` 2.x、Boot 4 用 3.x，版本在 pom `<properties>` 显式声明）；本地地址 `/swagger-ui.html`、OpenAPI JSON `/v3/api-docs`；文档由注解驱动，**禁止**维护与代码漂移的独立 API 文档。
- **禁止**：`@Schema` 的示例值/描述包含真实密钥、token、身份证/手机号等敏感信息；生产环境暴露 `/swagger-ui/**` 与 `/v3/api-docs/**`（按 Profile 关闭或网关白名单，见第 11 节）。
- **禁止**：DTO 直接透传 `HttpServletRequest`/`HttpServletResponse` 或把大对象整体塞进内存。

## 5. 事务与数据访问（MyBatis-Plus + MySQL）

### 事务

- **必须**：`@Transactional` 加在 Service 的公有方法上（默认仅回滚 `RuntimeException`/`Error`）；**禁止**加在 Controller 或 Mapper 接口上。
- **必须**：业务异常统一为 `BizException extends RuntimeException`（见第 6 节），默认回滚即生效；确需捕获受检异常并回滚时写 `@Transactional(rollbackFor = Exception.class)` 并注释原因。
- **应该**：只读查询标注 `@Transactional(readOnly = true)`。
- **禁止**：在事务方法内做远程调用（HTTP/Redis 业务写/MQ 发送等）或长耗时外部 IO；"事务后发消息"用 `@TransactionalEventListener`/事务同步。

### MyBatis-Plus

- **必须**：单表简单 CRUD 走 `BaseMapper` 的主键/实体级方法（`selectById`/`insert`/`updateById`/`deleteById`）；**条件构造器（`QueryWrapper`/`LambdaQueryWrapper`/`UpdateWrapper` 等）只允许出现在 `mapper/` 包内**，Service/Controller 禁止 import 与使用；条件查询一律定义为 Mapper 自定义方法（参数用查询对象 `XxxQuery`/`XxxPageQuery`），条件在方法/XML 内拼装；复杂/多表查询手写 SQL，XML 集中放 `src/main/resources/mapper/*.xml`。
- **必须**：采用 MP `IService`/`ServiceImpl` 体系时，只使用其主键/实体级方法（`getById`/`saveBatch`/`updateById`/`removeById` 等），**不调用** `list(wrapper)`/`page(wrapper)` 等条件构造器重载；此类场景改走自定义 Mapper 方法。
- **应该**：用 ArchUnit 固化——断言 `service`/`controller` 包不依赖 `com.baomidou.mybatisplus.core.conditions..`（见 §3「依赖方向约束」与第 13 节）。
- **必须**：分页经分页插件（`PaginationInnerInterceptor`，注册于 `config/`）；列表/分页必须有上限，**禁止**无 LIMIT 的"全表捞内存"式查询。
- **必须**：查询列显式（`select(...)` 或 XML 写列），**禁止**无条件 `SELECT *`（含 `selectPage` 默认全列的场景按需裁剪）。
- **应该**：逻辑删除（`@TableLogic`）、乐观锁（`@Version`）、主键策略（雪花/自增）全库统一并在 `ARCHITECTURE.md` 声明；依赖 MP 自动填充（`@TableField(fill=...)`）处理 `create_time/update_time` 等审计字段。
- **应该**：命名映射——表/字段 `snake_case`（审计字段固定 `create_time`/`update_time`、逻辑删除 `deleted`、版本 `version`，Java 端对应 `camelCase`）；开启 MP 驼峰自动映射或经 `@TableField` 显式标注；表名是否带 `t_` 前缀全库一致并在 `@TableName` 显式声明。
- **禁止**：业务代码拼接 SQL 字符串、循环单条 `insert`（用批量方法或自定义 batch）、在 Service 里直接用 `SqlSessionTemplate` 写裸 SQL（确需时收口到 Mapper）。

### MySQL

- **必须**：表与库默认 `utf8mb4`；所有 SQL 参数化（`#{}`），`${}` 只允许内部白名单值（经校验的表名/排序列），**禁止**拼接用户输入。
- **必须**：慢 SQL 关注——列表/聚合查询用 `EXPLAIN` 检查是否走索引；where 条件列按需建索引，联合索引遵守最左前缀。
- **必须**：数据库结构变更以版本化 SQL 脚本提交（`src/main/resources/db/V{{n}}__{{描述}}.sql`）；**已应用到各环境的脚本不得改写**（Flyway 校验和 / 发布纪律），只允许追加新脚本；生产 DDL 需评审并评估锁与耗时（大表变更走低峰窗口或 pt-osc/gh-ost 类工具）。
- **禁止**：事务中 `SELECT ... FOR UPDATE` 范围过大；批量写单批过大（按批拆分）；应用层做分页/聚合（交 SQL）。

### 业务字典（元数据字典）表

> 解决"可枚举业务数据的统一管理与命名"。**稳定状态机用 Java 枚举（如 `OrderStatus`），不落库**；需要运营维护、跨接口/系统复用的可枚举数据（类目/渠道/地区/标签等）落字典表——两类并存，边界明确。

- **必须（建表）**：项目初始化即建两张字典表（空结构亦可，命名沿用全库 `t_` 前缀决策）：
  - `{{t_dict_type}}`（类型）：`id`、`type_code`（唯一，如 `PRODUCT_CATEGORY`）、`type_name`、`status`、`sort_no`、`remark` + 审计字段；
  - `{{t_dict_item}}`（项）：`id`、`type_code`、`item_code`（机器值，如 `ELECTRONICS`）、`item_label`（中文展示名）、`status`、`sort_no`、`remark` + 审计字段；**唯一约束 `(type_code, item_code)`**。
- **命名规范（必须）**：`type_code`/`item_code` 统一 `UPPER_SNAKE_CASE`、语义自明；代码侧引用字典 code/值一律走常量类（如 `DictCodes`），**禁止**散落字符串。
- **访问路径（必须）**：
  - 对外只读：统一 `GET /dicts/{typeCode}`（`DictController`），返回 `item_code`/`item_label` 列表，供前端下拉与翻译；
  - 内部只读：业务 Service 可注入共享 `DictMapper` 的只读方法（`selectDictItemsByType(typeCode)` 等）——**基础数据只读例外**（非业务领域、不违反"Service 不横向依赖业务 Service"）；
  - 字典维护（写/改 label/启停）只允许经 `DictController → DictService`（管理端或初始化脚本），**禁止**业务代码直接 `insert/update` 字典表。
- **缓存（必须）**：字典列表缓存 Redis（key `dict:{{typeCode}}`，带 TTL，见第 7 节）；字典变更后**删缓存**（或版本号失效），避免脏读。
- **红线**：字典 `type_code`/`item_code` **只停用不删除**（历史数据仍引用）；不得把订单状态等业务状态机做成字典（应枚举）；新字典类型先查重命名再建。

## 6. 异常与错误码（业务错误 vs 系统错误）

> 目标：错误可分类、编码可机器消费、命名可读——杜绝"堆栈直出 / 数字码乱飞 / `200` 包业务失败"。

- **必须**：全局统一异常处理——`@RestControllerAdvice` + `@ExceptionHandler`；响应统一结构 `Result<T>`（`code`/`message`/`traceId`/`data`），错误响应不含 `data`；HTTP 状态与错误类别匹配（见分类表）。
- **必须**：业务失败抛 `BizException(ErrorCode.xxx, 参数...)`；**禁止**返回"200 + 业务 false"、禁止在 Controller 里用 `if/else` 拼错误（评审必打回）。
- **禁止**：`catch` 后吞异常（空 catch 或仅 `log.error` 不上抛不转译）；捕获后必须至少：记录 → 转译 → 上抛/返回，三者之一。

### 错误分类（必须）

| 类别 | 判定标准 | 抛出/处理方式 | HTTP | 对外 message | 日志与告警 |
| --- | --- | --- | --- | --- | --- |
| **业务错误** | 可预期、可向用户展示（资源不存在/状态不允许/库存不足/数据已占用…） | `throw new BizException(ErrorCode.ORDER_NOT_FOUND, orderId)` | 404/409/422（按语义） | 中文、面向用户 | `WARN`，一般不告警（可做频率监控） |
| **参数/校验错误** | 入参未通过 Jakarta Validation / 格式错误 | 框架异常（`MethodArgumentNotValidException` 等）由 advice 转 `2xxxxx` | 400/422 | 中文（含字段级提示） | `INFO` |
| **系统错误** | 未知/框架/基础设施（DB 不可用、NPE、超时），不可预期、不可展示 | `@ExceptionHandler(Exception)` 兜底 | 500/502/503 | 统一文案"系统繁忙，请稍后重试"+ `traceId` | `ERROR` + **告警**；堆栈只进日志 |
| **认证/授权错误** | 未登录/无权限 | 安全框架异常转 `4xxxxx` | 401/403 | 中文 | `WARN` |
| **外部依赖错误** | Redis/RabbitMQ/第三方不可用或超时 | 转 `5xxxxx`（或并入系统错误处理） | 502/504 | 同系统错误（不展示细节） | `ERROR` + 告警 |

### 错误码规范（必须）

- **单一来源**：错误码集中定义在 `common/ErrorCode` 枚举，字段含 `code`、面向用户的 `message`、`httpStatus`；**禁止**业务代码散落数字/字符串 code 与 message（对应"禁魔法字符串"）。
- **编码分段（6 位数字，首位=大类）**：`1xxxxx` 系统错误 / `2xxxxx` 参数与校验 / `3xxxxx` 业务错误 / `4xxxxx` 认证授权 / `5xxxxx` 外部依赖。
- **命名规范（大写 + 下划线，模式 `{域}_{对象/动作}_{原因}`）**：业务 `ORDER_NOT_FOUND`、`ORDER_STATUS_NOT_ALLOWED`、`INSUFFICIENT_STOCK`、`ORDER_CANNOT_CANCEL`；参数 `PARAM_INVALID`、`PARAM_MISSING`；认证 `UNAUTHORIZED`、`FORBIDDEN`；系统/外部 `SYSTEM_ERROR`、`DB_UNAVAILABLE`、`THIRD_PARTY_TIMEOUT`。命名必须自明，**禁止** `E001`/`FAIL` 之类无信息量码名。
- **消息参数化**：带业务上下文时 `throw new BizException(ErrorCode.ORDER_NOT_FOUND, orderId)`，`message` 走模板填充；禁止在错误码定义处或调用处做字符串拼接。
- **登记纪律**：新增错误码前**先查重**（同语义同码；不同域不同段）；**只增不删**（语义变化可标废弃，避免客户端/日志历史歧义）。
- **禁止**：把堆栈/类名/表名/SQL/内部路径泄漏到对外 `message`；同一个 code 表达两种以上含义；把受检/系统异常直接转 500 而不归类转译；用异常做正常流程控制。
- **应该**：对外 API 文档（Swagger）对高频错误码给出说明；`traceId` 贯穿错误响应与日志（见第 9 节）。

## 7. Redis 使用规范

- **必须**：key 统一规范 `{{域}}:{{对象}}:{{标识}}`（如 `order:detail:12345`）、全小写冒号分隔、带 TTL；按用途带类别前缀：缓存 `order:detail:{id}`、分布式锁 `lock:order:pay:{orderId}`、幂等 `idem:order:create:{orderId}`（可再扩展 `rate:`/`bloom:`）；所有前缀集中在常量类（如 `OrderCacheKeys`），禁止魔法字符串散落。
- **必须**：序列化全库统一——连接工厂/模板推荐 `GenericJackson2JsonRedisSerializer` 或 String + 显式序列化；**禁止**默认 JDK 序列化（兼容与安全风险）。
- **必须**：写库后**删缓存**而非"先更新缓存"；缓存与库不一致场景（强一致要求）评估延迟双删或版本号方案，决策记入 `design-docs/`。
- **应该**：雪崩防护（基础 TTL + 随机抖动）、击穿防护（互斥锁回填，锁用 Redisson）、穿透防护（空值短 TTL 或布隆过滤器）按需实现并统一封装，不散落各处手写。
- **禁止**：缓存密钥/token/个人敏感信息；把 Redis 当主存储（可丢数据场景）；手写 `SETNX` 裸分布式锁（无续期易死锁——用 Redisson，看门狗自动续期）。
- **禁止**：在 `@Transactional` 事务内执行关键缓存写/删（长事务占连接且回滚语义混乱）；"事务后删缓存"走事务同步。

## 8. RabbitMQ 使用规范

- **必须**：生产端开启 publisher-confirm（`spring.rabbitmq.publisher-confirm-type: correlated`），确认失败记录并补偿/告警。
- **必须**：消费端手动 ack（`AcknowledgeMode.MANUAL` 或 `auto` + 明确重试策略）+ **有限重试**（如 3 次），重试耗尽投递死信队列（DLQ）并告警；**禁止**无限重试刷爆日志、禁止吞异常不 ack（表现为消息堆积）。
- **必须**：消费处理**幂等**——at-least-once 下重复投递是常态；用业务去重键（数据库唯一索引或 Redis `SETNX` + TTL）去重，幂等键由消息内容派生并随消息投递。
- **必须**：消息体序列化统一 JSON（`Jackson2JsonMessageConverter`），**禁止** JDK 序列化；消息带 `messageId`/`traceId` 便于追踪。
- **必须**：Exchange/Queue/RoutingKey/死信命名统一登记（常量类如 `MqDestinations`），声明集中在 `config/` 的 RabbitMQ 配置类，禁止散落字符串。推荐方案（全库统一，点分小写）：
  - 业务交换机：`{{svc}}.{{域}}.exchange`（默认 topic）；
  - RoutingKey：`{{域}}.{{事件}}`（如 `order.paid`）；
  - 业务队列：`{{svc}}.{{域}}.{{事件}}.queue`（多消费者再加 `.` + 消费方名，如 `shop.order.paid.queue.inventory`）；
  - 死信：队列声明 `x-dead-letter-exchange={{svc}}.{{域}}.dlx`，死信队列 `{{svc}}.{{域}}.{{事件}}.queue.dlq`。
- **应该**：消息信封统一顶层字段 `messageId`（UUID）/`traceId`/`type`/`timestamp`/`data`，便于追踪与幂等；队列/交换机常量加注释说明用途与消费者。
- **应该**：发消息放事务提交后（`@TransactionalEventListener`/`TransactionSynchronization`），避免"事务回滚但消息已发"。
- **禁止**：在 `@RabbitListener` 内直接写业务大事务 + 远程调用组合；消费者间共享可变状态。

## 9. 日志规范

- **必须**：统一 SLF4J API；**禁止**字符串拼接打日志（`log.info("x=" + id)`），一律参数化 `log.info("x={}", id)`。
- **必须**：`log.error` 携带异常对象（`log.error("处理订单 {} 失败", id, e)`），否则丢失堆栈。
- **禁止**：打印密钥/口令/token、完整身份证/手机号等个人敏感信息；报文类日志脱敏或只打 traceId。
- **应该**：错误日志含足够定位上下文（订单号、消息 messageId、traceId）；入口/出口与 MQ 消费用 MDC/切面统一打 traceId；错误响应中的 `traceId` 与日志同源，便于排障。
- **应该**：日志级别遵循：`DEBUG` 调试、`INFO` 关键业务节点、`WARN` 可恢复问题、`ERROR` 需要人介入；生产默认 `INFO`。业务错误（第 6 节）按 `WARN` 记录、系统错误按 `ERROR` + 告警，级别不混用。

## 10. 并发与异步

- **禁止**：业务代码裸 `new Thread(...)`/手写线程池管理——需要异步用 `@Async` + 项目统一声明的 `Executor` Bean（或 Java 21+ 虚拟线程，视部署环境选型并全库统一）；跨进程异步优先 RabbitMQ，而非本地线程。
- **必须**：`@Async` 自调用失效——同 Bean 内方法互调不生效，需经代理入口调用或拆分 Bean。
- **必须**：共享可变状态（缓存、计数器、静态集合）加锁/用并发容器，并说明并发模型；`SimpleDateFormat` 等非线程安全类禁止作共享静态字段。
- **应该**：`@Scheduled` 任务默认单线程顺序执行，长任务自行评估重叠与失败补偿；与 DB/Redis/MQ 配合的批处理支持幂等与断点续跑。

## 11. 配置管理

- **必须**：配置按 Profile 拆分 `application.yml` + `application-{profile}.yml`；本地默认 `dev`（`SPRING_PROFILES_ACTIVE` 或启动参数）。
- **应该**：springdoc（Swagger UI）按 Profile 开关——`dev`/`test` 启用、`prod` 关闭（`springdoc.api-docs.enabled=false`）或由网关白名单控制；`springdoc.swagger-ui.path` 如有定制需在 `ARCHITECTURE.md`/README 声明。
- **必须**：数据源、Redis、RabbitMQ 的连接配置统一以环境变量注入（`${DB_PASSWORD}` 形式），**密钥/口令禁止入库**。
- **应该**：成组业务配置用 `@ConfigurationProperties` 类型化绑定（`@Validated` 启用校验），不散落 `@Value`；MyBatis-Plus 相关配置（逻辑删除、分页、乐观锁）集中在 `config/` 且注释说明。
- **禁止**：不同环境互相拷贝后忘改的"环境泄漏"配置——环境差异集中在少量文件并用占位符。

## 12. 测试规范

- **必须**：新增逻辑带测试；测试与被测代码同提交。
- **必须**：测试**不访问外网、不依赖本地手工服务**：
  - Service 单测：mock Mapper/依赖（Mockito），覆盖分支与异常路径（含 `BizException` 错误码断言）。
  - Controller 层：`@WebMvcTest` + mock Service（新项目用 `@MockitoBean`，勿再用已废弃的 `@MockBean`）。
  - Mapper / 跨组件（MySQL、Redis、RabbitMQ）集成测试：Testcontainers 起真实中间件（`@ServiceConnection` 自动装配连接），在 CI 执行（需 Docker）。
- **应该**：错误处理测试覆盖：业务错误返回正确 `code` 与 HTTP 状态、系统错误不泄漏堆栈且带 `traceId`。
- **必须**（业务方法严谨测试）：每个 Service 公有业务方法必须有对应 JUnit，至少覆盖：① 正常路径（断言返回值与副作用）；② **每个**业务异常分支（断言 `ErrorCode`，涉及写库时验证事务回滚，见第 6 节）；③ 边界/空/重复输入；④ 写库方法断言影响结果。**无对应测试的业务方法不得提交/合并**。
- **必须**（Controller 自测，curl）：每个新 Controller 端点开发完成后，在 `dev` Profile + `docker compose` 就绪环境下用 `curl` **真实调用**验证成功与主要失败分支；命令与期望响应记录进 MR/PR 描述后再提交。curl 冒烟**不替代** `@WebMvcTest` 与集成测试。示例（按实际接口调整）：

  ```bash
  # dev 启动后（应用监听 :{{8080}}）：
  curl -i "http://localhost:{{8080}}/orders?page=1&size=10"        # 列表：期望 200 + PageResult
  curl -i -X POST "http://localhost:{{8080}}/orders" -H "Content-Type: application/json" -d "{{请求体}}"
  curl -i "http://localhost:{{8080}}/orders/{{不存在ID}}"          # 期望：HTTP 404 + 业务 ErrorCode（3xxxxx）
  curl -i "http://localhost:{{8080}}/orders" -X POST -d "{{非法体}}" # 期望：400/422 + 参数错误码（2xxxxx）
  ```
- **应该**：MQ 消费测试验证幂等与重试（重复投递同一条消息不产生副作用）；Redis 相关测试验证缓存删除/回填行为。
- **应该**：测试数据用工厂/构建器生成，语义化（`anOrder().withStatus(PAID)`），不在每个测试里手写重复 setup。
- **禁止**：测试依赖执行顺序、共享静态状态互相污染；每个测试独立可跑（`./mvnw test -Dtest=XxxTest` 能单独通过）。
- **禁止**：为覆盖率数字硬编无断言的测试、把断言写进注释而不真正 `assertThat`。

## 13. 提交前质量门（Maven 插件固定集）

质量门由以下 **Maven 插件固定集**构成，本地与 CI 行为一致。插件版本在 `pom.xml` 的 `<pluginManagement>` 锁定（单一来源，不在文档写版本号）；**能用工具校验的规则一律写进插件配置，不散落在代码注释/Review 里**。

| 插件 | Goal | 检查内容 | 配置要点 |
| --- | --- | --- | --- |
| `com.diffplug.spotless:spotless-maven-plugin` | `spotless:check` / `spotless:apply` | 代码格式（google-java-format）、import 排序 | 排除生成代码（MyBatis-Plus Generator 产物）；apply 由开发者本地执行 |
| `org.apache.maven.plugins:maven-checkstyle-plugin` | `checkstyle:check` | 风格/命名/禁止项（System.out、魔法数字等） | 规则 XML `config/checkstyle/checkstyle.xml`，以 Google Java Style 为基底 + 团队补充 |
| `com.github.spotbugs:spotbugs-maven-plugin` | `spotbugs:check` | 缺陷模式（空指针、资源未关、并发等） | `effort=Max`；`excludeFilter.xml` 排除生成代码/已知无害项 |
| `org.apache.maven.plugins:maven-enforcer-plugin` | `enforce` | Java 版本、依赖收敛、禁 SNAPSHOT | `requireJavaVersion`、`dependencyConvergence`、`bannedDependencies` |
| `org.jacoco:jacoco-maven-plugin` | `prepare-agent` + `check` | 行覆盖率门禁 | 阈值 {{如 LINE ≥ 80%}}；排除 entity/mapper/config/生成代码，按实际包调整 |

可选扩展（不默认引入，确有需要再配）：`maven-pmd-plugin`（含 Alibaba P3C 规则集，中文团队常用）、`com.tngtech.archunit:archunit-junit5`（架构测试：分层依赖方向、无环、QueryWrapper 隔离、DAO 前缀，见 §3/§5——建议接入并纳入 `verify`）、`org.sonarsource.scanner.maven:sonar-maven-plugin`（接 SonarQube，需服务器）。

**CI 与本地统一命令**（合并/提交前必须全绿）：

```bash
# 在项目根执行：
./mvnw clean verify                 # 一键全量：格式校验 + 静态检查 + 版本规则 + 覆盖率 + 全部测试（集成测试需本机 Docker）
./mvnw spotless:apply               # 格式不通过时先本地修复
# 快速复跑单项：
./mvnw checkstyle:check spotbugs:check
```

规则：

- **必须**：任一插件失败 = 构建失败 = 禁止提交/合并；不得 `-Dskip.*` 绕过（格式/静态检查类跳过需在 MR 说明并获批）。
- **必须**：MyBatis-Plus Generator 等生成的代码从格式/静态检查中排除（在对应插件配置声明排除路径），避免"生成 → 改格式 → 再生成"循环。
- **应该**：`spotless:apply`、`checkstyle:check`、`spotbugs:check`、`jacoco:check`、`enforcer:enforce` 全部绑定到 `verify` 阶段，保证 CI 只跑 `clean verify` 即可复现本地。

## 14. Git 提交规范

- **格式（必须）**：`<type>(<scope>): <subject>`，示例：
  - `feat(order): 增加取消订单接口`
  - `fix(auth): 修复 token 刷新竞态`
  - `docs: 补充部署说明`
  - `refactor(mapper): 拆分订单查询 SQL 到 XML`
- **type（必填，小写）**：`feat` 新功能 / `fix` 缺陷修复 / `docs` 文档 / `style` 格式（不影响逻辑）/ `refactor` 重构 / `perf` 性能 / `test` 测试 / `build` 构建 / `ci` CI 变更 / `chore` 杂项 / `revert` 回滚。
- **scope（可选，小写）**：影响模块（`order`、`user`、`common`、`mapper`、`pom`）；跨模块大改可省略。
- **subject（必须）**：祈使句、≤50 字符、句末不加句号；语言（中文/英文）全库统一并在 README/AGENTS.md 声明，禁止中英混用。
- **body（需要时）**：写"为什么"（动机、取舍、影响），不复述 diff。
- **footer（需要时）**：破坏性变更 `BREAKING CHANGE: <说明>`；关联 issue `Closes #<编号>`。
- **必须遵守**：
  1. 一个提交只含一个逻辑变更，无关改动拆分或回退。
  2. 提交前通过第 13 节质量门；禁止提交含密钥、构建产物、生成代码。
  3. 不 amend / rebase 已推送的提交（维护者明确要求除外）。
- **工具化**：格式与 type 词表用 commitlint + husky（本地钩子）与 CI 校验强制；规则进配置，本文件不维护第二份。

## 15. 规则修订

- 本文件随团队实践演进：修改需在评审中说明理由，重大变更记录到 `docs/design-docs/`（ADR 或 core-beliefs），并同步真实项目 `AGENTS.md`「编码约定」章节。
- 工具能表达的规则（格式、import 顺序、commit 格式）一律写进工具配置，**不在本文件重复**，避免双份漂移。

---

> 范本结束。采用步骤：裁剪与本团队不符的条目 → 放入 `docs/CODING_STANDARDS.md` → 在 `AGENTS.md`「编码约定」确认指向 → 把「提交前质量门」命令与真实 CI 对齐。
