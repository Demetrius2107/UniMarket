# Java 编码规范 — UniMarket

> 适用于 UniMarket 统一商城前台的 Java 编码规范，参考阿里巴巴《Java 开发手册》落地。
> 目标：统一代码风格、降低缺陷率、提升可维护性。
> 版本：v1.0 ｜ 2026-08-10

---

## 目录

1. [总体原则](#1-总体原则)
2. [命名规范](#2-命名规范)
3. [类与代码结构](#3-类与代码结构)
4. [集合与泛型](#4-集合与泛型)
5. [并发与多线程](#5-并发与多线程)
6. [异常处理](#6-异常处理)
7. [日期与时间](#7-日期与时间)
8. [金额与精度](#8-金额与精度)
9. [String 与工具类](#9-string-与工具类)
10. [控制语句](#10-控制语句)
11. [单元测试与构建](#11-单元测试与构建)
12. [检查清单](#12-检查清单)

---

## 1. 总体原则

| 原则 | 说明 |
|------|------|
| 可读性优先 | 代码是给人读的，其次才是机器执行 |
| 命名即文档 | 好的命名 > 注释；注释说明"为什么" |
| 防御式编程 | 系统边界（入口/外部依赖）做校验，内部不过度防御 |
| 简单优先 | 不做无依据的过度设计、过度优化 |
| 单元可测 | 方法职责单一，便于测试与复用 |

**分层约定**：Controller 不写业务逻辑；Service 不写 SQL；领域规则只在 Domain 层。

---

## 2. 命名规范

### 2.1 通用规则

| 类型 | 规范 | 示例 | 反例 |
|------|------|------|------|
| 类名 | UpperCamelCase，名词 | `OrderService` | `orderService` |
| 方法名 | lowerCamelCase，动词 | `createOrder()` | `CreateOrder()` |
| 常量 | 全大写下划线 | `MAX_RETRY_COUNT` | `maxRetryCount` |
| 变量/参数 | lowerCamelCase | `userId` | `uid`、`a` |
| 包名 | 全小写，域名倒置 | `cn.unimarket.domain.order` | `com.UNIMARKET` |
| 枚举 | 类名 UpperCamelCase，值全大写 | `OrderStatus.PAY_WAIT` | — |
| 布尔 | 不用 `is` 前缀避免 getter 歧义 | `enabled` | `isEnabled` |
| 抽象类 | `Abstract` 前缀 | `AbstractDrawAlgorithm` | — |
| 接口实现 | `Impl` 后缀（或按命名） | `OrderRepositoryImpl` | — |

### 2.2 禁止事项

- ❌ 拼音/拼音缩写命名（`jiesuan`、`jg`）；国际通用词除外（`taobao` 等品牌词）。
- ❌ 无意义命名：`a`、`temp`、`data1`、`list2`。
- ❌ 大小写混用：`getUserID`（应为 `getUserId`）。
- ❌ 方法命名与实现不符（`queryXxx` 内部却做写操作）。

### 2.3 本系统领域命名对照（示例）

| 领域 | 统一命名 |
|------|----------|
| 用户标识 | `userId`（不用 `uid`/`memberId`） |
| 订单标识 | `orderId`、`outTradeNo`（外部支付号） |
| 商品 | `productId`、`skuId` |
| 活动 | `activityId`、`strategyId` |
| 积分 | `creditAccount`、`availableAmount` |
| 拼团 | `teamId`、`targetCount`、`completeCount` |

---

## 3. 类与代码结构

### 3.1 类定义顺序

```
1. static 常量
2. 实例变量
3. 构造方法
4. 静态工厂/公开方法
5. 私有方法
6. equals/hashCode/toString（如需要）
```

### 3.2 方法长度

- 方法体建议 ≤ 80 行；超过考虑拆分（提取私有方法/领域服务）。
- 一个方法只做一件事（Single Responsibility）；圈复杂度高的方法拆分。

### 3.3 DTO/PO/VO 分离

- 接口出入参用 DTO（`xxxDTO` / `xxxVO`），DB 映射用 PO，领域模型独立。
- 禁止直接透传 PO/实体到接口层（防字段泄漏/耦合）。
- DTO 字段必须注释含义与单位（见《代码注释规范》）。

### 3.4 依赖注入

- 构造器注入优先（配合 final，便于测试与不可变保证）。
- 禁止静态工具类里持有可变全局状态；`@Autowired` 字段注入仅限兼容场景。

---

## 4. 集合与泛型

| 约定 | 说明 |
|------|------|
| 指定初始容量 | `new HashMap<>(expectedSize)`，避免扩容开销 |
| 使用 `Map.Entry` 遍历 | 遍历 Map 用 `entrySet()`，不重复 `get(key)` |
| 集合判空 | 用 `CollectionUtils.isEmpty()` 统一判断，不手写 `!= null && size()>0` |
| 禁止裸类型 | 集合必须带泛型，禁止 `List list = new ArrayList()` |
| 不可变集合 | 可修改性敏感处用 `Collections.unmodifiableXxx()` 或 `List.of()` |
| 对象比较 | 用 `Objects.equals()`，不用 `==`（Long 等包装类） |
| 数组转 List | `Arrays.asList()` 是视图，不可增删；需可变用 `new ArrayList<>(Arrays.asList(...))` |
| 去重/统计 | 合理选择 Set/Map 聚合，避免 O(n²) 双重循环 |

**注意**：`Long` 在 `-128~127` 之间 `==` 可能"碰巧"成立，禁止依赖；一律 `equals` 或 `Objects.equals`。

---

## 5. 并发与多线程

| 场景 | 约定 |
|------|------|
| 线程池 | 禁止 `Executors.newFixedThreadPool()` 裸用；统一封装线程池工厂（显式队列/拒绝策略） |
| 线程安全 | 多线程共享变量用 `volatile`（可见性）/原子类（CAS）/锁（互斥），按需选择 |
| 分布式锁 | 用 Redisson（看门狗续期），禁止裸 SETNX 无过期（见《RedisKey设计规范》§6） |
| SimpleDateFormat | 线程不安全，用 `DateTimeFormatter`（java.time） |
| 并发容器 | 优先 `ConcurrentHashMap`、`CopyOnWriteArrayList`（读多写少） |
| 竞态 | 先校验后更新的复合操作必须加锁/原子操作（如库存扣减 INCR） |
| 线程局部 | `ThreadLocal` 用后必须清理（`remove()`），防内存泄漏（如 DBContextHolder） |

```java
// 线程池统一由工厂创建，禁止各业务自建
ThreadPoolExecutor executor = ThreadPoolFactory.create("raffle-award",
    core = 4, max = 8, queue = 1024, RejectedExecutionHandler = CallerRuns);
```

---

## 6. 异常处理

### 6.1 异常分类

| 类型 | 使用 | 示例 |
|------|------|------|
| 业务异常 `BizException` | 业务校验失败，携带错误码 | `throw new BizException(ErrorCode.STOCK_NOT_ENOUGH)` |
| 系统异常 | 基础设施/未知异常，由全局异常处理器兜底转 500 | 不主动抛（交由框架） |
| 第三方异常 | 外部调用失败，包装后抛出或降级 | 支付/物流调用异常 |

### 6.2 约定

- **禁止吞异常**：catch 后必须处理（重抛/记录日志/降级），禁止空 catch。
- 日志异常：`log.error("msg", e)`，必须带堆栈；禁止 `e.getMessage()` 当全部信息。
- 错误码：统一 `ErrorCode` 枚举（见《接口开发规范》§6），禁止魔法数字。
- 不捕获 `Error`（OOM 等）与 `InterruptedException` 吞掉（要恢复中断标志）。
- 方法内先校验后业务：参数校验用 Bean Validation 或显式校验，失败抛业务异常。
- 事务回滚：`@Transactional` 默认只回滚 RuntimeException；预期异常需 `rollbackFor` 明确（或保持运行时异常约定）。

```java
// 正确：业务异常带错误码
if (awardSurplusCount <= 0) {
    throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
}

// 禁止：空 catch 吞异常
try {
    callThirdParty();
} catch (Exception e) {
    // 必须处理：记录日志/重试/降级/重抛
}
```

---

## 7. 日期与时间

| 约定 | 说明 |
|------|------|
| 统一 java.time | 禁止 `Date`/`Calendar` 做业务计算（历史兼容除外） |
| 类型 | 时间字段用 `LocalDateTime`，日期用 `LocalDate` |
| 格式 | 统一 `yyyy-MM-dd HH:mm:ss`；解析用 `DateTimeFormatter`，**线程安全** |
| 时区 | 统一 `Asia/Shanghai`；入库/出参不含时区偏移歧义 |
| 存储 | MySQL 用 `datetime`；禁止存字符串时间（不可排序比较） |
| 定时/调度 | 调度时间统一由 XXL-Job cron 配置，代码不硬编码 |

---

## 8. 金额与精度

| 约定 | 说明 |
|------|------|
| 金额类型 | 一律 `BigDecimal`，禁止 `float`/`double` 存金额 |
| 单位 | 元，前后端约定一致（见《接口开发规范》） |
| 构造 | `new BigDecimal("0.01")`（字符串构造）；禁止 `new BigDecimal(0.01)` |
| 比较 | `compareTo`，禁止 `equals`（scale 不同返回 false） |
| 运算 | 加减乘除指定精度与舍入：`divide(x, 2, RoundingMode.HALF_UP)` |
| 常量 | 零用 `BigDecimal.ZERO`，避免 `new BigDecimal("0")` 重复 |
| 序列化 | JSON 序列化为字符串防精度丢失（`@JsonSerialize(ToStringSerializer)`） |

```java
// 正确
BigDecimal payAmount = totalAmount.subtract(couponDiscount).subtract(creditDeduct);

// 禁止
double pay = 99.99 - 10.01; // 浮点误差
```

---

## 9. String 与工具类

| 约定 | 说明 |
|------|------|
| 拼接 | 循环内用 `StringBuilder`；少量用 `+` 可接受 |
| 判空 | 用 `StringUtils`（Apache/Spring）统一判断，禁止散落 `!= null && !"".equals()` |
| 格式 | 用 `String.format` 或模板，禁止 `+` 拼 SQL/URL（注入风险） |
| 大文本 | 超过 500 字符的静态文本放常量/配置文件，不内联在方法里 |
| 工具类 | 通用能力优先复用 `commons-lang3`/`guava`，不自造轮子 |

---

## 10. 控制语句

| 约定 | 说明 |
|------|------|
| 条件简化 | 嵌套超 3 层必须重构（卫语句/策略/多态） |
| 卫语句 | 前置校验用卫语句提前 return，减少嵌套 |
| switch | 必须带 `default`；多分支策略用枚举/Map 代替 |
| 循环 | 禁止在循环内查库/发 MQ（改批量/异步） |
| 魔法值 | 业务数字/字符串定义常量或枚举，禁止裸值（`if (status == 3)`） |
| 三目 | 禁止嵌套三目；复杂逻辑拆方法 |

```java
// 卫语句
public void doDraw(Long userId, String activityId) {
    if (user == null) {
        throw new BizException(ErrorCode.USER_NOT_FOUND);
    }
    if (!activity.isDoing()) {
        throw new BizException(ErrorCode.ACTIVITY_NOT_ACTIVE);
    }
    // 业务...
}
```

---

## 11. 单元测试与构建

- 测试命名：`方法_场景_预期`（见《测试规范》§2）。
- 测试不依赖外部环境：Mock 掉 DB/MQ/Redis/第三方；纯逻辑直接 new。
- 禁止在测试里 sleep 等待异步（用 Mock/同步验证）。
- 构建：`mvn clean package` 必须通过；依赖版本统一在父 POM `dependencyManagement` 管理。

---

## 12. 检查清单

提交前检查：

- [ ] 命名符合 §2 规范，无拼音/无意义命名
- [ ] 类结构清晰，方法 ≤ 80 行，无超 3 层嵌套
- [ ] 集合带泛型、指定初始容量、判空用工具类
- [ ] 金额 BigDecimal，时间 java.time，无裸魔法值
- [ ] 异常不吞、错误码用枚举、日志带堆栈
- [ ] 无自建线程池/裸 Executors、无裸 SETNX 锁
- [ ] Controller/Service/Domain 分层职责正确
- [ ] 单元测试通过，核心分支有覆盖

---

*Java 编码规范 v1.0 — 2026-08-10，参考阿里巴巴《Java 开发手册》落地适配*
