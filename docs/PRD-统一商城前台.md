# 统一商城前台 — 完整 PRD

> 融合 支付、抽奖、营销、拼团、购物车、下单、履约，形成一个可落地的完整系统设计与实现指南。

---

## 目录

1. [项目概述](#1-项目概述)
2. [四个原项目回顾与能力映射](#2-四个原项目回顾与能力映射)
3. [系统架构设计](#3-系统架构设计)
4. [领域驱动设计](#4-领域驱动设计)
5. [数据库设计](#5-数据库设计)
6. [API 接口设计](#6-api-接口设计)
7. [核心业务流程](#7-核心业务流程)
8. [关键技术实现](#8-关键技术实现)
9. [部署架构](#9-部署架构)
10. [实施路线图](#10-实施路线图)
11. [可行性分析](#11-可行性分析)

---

## 1. 项目概述

### 1.1 项目定位

**统一商城平台（UniMarket）** 是一个面向 C 端用户的、融合多种营销玩法的完整电商平台。它不仅是"一个商城"，更是一个**可运营的营销引擎**——运营人员可以通过配置化的方式创造抽奖、拼团、积分兑换、签到返利等多种营销活动，驱动用户增长和转化。

### 1.2 要解决的业务场景

| 场景 | 说明 | 来源            |
|------|------|---------------|
| 用户登录与身份体系 | 微信扫码登录、Session/Token 管理 | 支付原型          |
| 商品浏览与搜索 | 分类浏览 → ES 全文搜索 → 多维筛选 | 新设计           |
| 购物车 | 加购 → 合并 → 校验 → 一键下单 | 新设计           |
| 普通下单与支付 | 下单 → 统一收银台 → 微信/支付宝支付 → 发货 | 支付原型 + 新设计    |
| 拼团购买 | 开团/参团 → 锁单 → 支付 → 成团/退款 | 拼团原型          |
| 营销抽奖 | 签到/购物得次数 → 概率抽奖 → 发奖 | 抽奖原型 + 拼团原型   |
| 积分体系 | 行为返利积分 → 积分兑换抽奖次数/SKU | 营销原型          |
| 优惠券体系 | 发券/领券 → 下单核销 → 退单退券 | 新设计           |
| 售后服务 | 申请退款/退货 → 审核 → 退款退券退积分 | 新设计           |
| 物流跟踪 | 发货 → 快递鸟查询 → 物流状态回写 | 新设计           |
| 首页运营 | Banner → 频道页 → 活动落地页 → 商品推荐 | 新设计           |
| 消息通知 | 微信模板消息 + 站内消息中心 + 拼团回调通知 | 支付 + 拼团 + 新设计 |

### 1.3 技术目标

- **架构**：DDD 六边形架构，领域逻辑与技术框架解耦
- **扩展性**：营销玩法可插拔，新活动类型只需实现约定接口
- **高可用**：核心链路限流/熔断/降级，库存精确扣减，最终一致性保证
- **可观测**：Prometheus + Grafana 监控，ELK 日志，链路追踪

---

## 2. 项目回顾与能力映射

### 2.1 支付原型

**核心能力**：
- 微信公众号 OAuth 扫码登录
- 支付宝沙箱支付（创建支付单 → 异步回调 → 订单状态变更）
- 订单生命周期管理（CREATE → PAY_WAIT → PAY_SUCCESS → DEAL_DONE / CLOSE）
- 定时补偿（掉单补偿 + 超时关单）
- Guava EventBus 事件通知

**这个项目教什么**：
- 支付回调的最终一致性（双重补偿：异步回调 + 定时主动查询）
- 微信 OAuth 的 ticket 机制和轮询模式
- 支付表单返回 HTML 让前端直接提交
- 内网穿透在对接第三方回调时的必要性

**在新系统中的位置**：用户登录模块 + 支付模块的基础原型

### 2.2 抽奖

**核心能力**：
- 两种抽奖算法：单项概率+ 总体概率
- 规则引擎决策树（用户属性 → 过滤 → 匹配活动）
- 活动状态机（编辑 → 提审 → 通过 → 运行 → 关闭）
- 分库分表（2 库 × 4 表，按 userId 哈希路由）
- Redis 分段分布式锁 + INCR 库存扣减
- Kafka 异步发奖 + XXL-Job 消息补偿
- Dubbo RPC 服务暴露

**这个项目教什么**：
- 抽奖概率算法的数学原理和工程实现
- 决策树规则引擎的设计模式（组合模式 + 过滤器链）
- 分布式环境下的库存精确扣减（Redis 原子操作 + DB 行锁 + 分段锁）
- 分库分表的路由策略和扰动函数

**在新系统中的位置**：抽奖引擎核心 + 分库分表方案 + 规则引擎

### 2.3 营销

**核心能力**：
- 积分体系（积分账户 + 交易流水 + 正向/逆向交易）
- 行为返利（签到 → 积分/抽奖次数，支付 → 积分/抽奖次数）
- SKU 商品体系（用积分购买抽奖次数）
- 责任链规则过滤（黑名单 → 权重 → 默认概率）
- 决策树抽奖规则（N 次解锁 → 库存扣减 → 兜底奖品）
- 限流熔断（Guava RateLimiter + 黑名单 + Hystrix + DCC 降级开关）
- Elasticsearch + Canal 读写分离
- Zookeeper DCC 动态配置中心
- Prometheus + Grafana 监控

**这个项目教什么**：
- 积分账户的事务一致性（正向/逆向交易 + MQ 最终一致性）
- 多级风控体系（限流 → 黑名单 → 熔断 → 降级）
- 读写分离（Canal 同步 MySQL → ES）
- 动态配置中心的设计（无需重启即可切换开关）

**在新系统中的位置**：积分引擎 + 风控体系 + 监控体系

### 2.4 拼团

**核心能力**：
- 拼团完整生命周期（开团 → 锁单 → 支付 → 成团/超时退款）
- 四种折扣计算策略（直减/满减/折扣/N 元购）
- 责任链模式贯穿全流程（锁单链/结算链/退款链）
- Redis 库存控制（incr + setNx 双重保障）
- 本地消息表 + MQ + 定时补偿 三重最终一致性
- 人群标签定向（可见性/参与限制）
- DCC 动态配置（降级/限流/切量）
- 退单三种策略（未支付退单 / 已支付未成团退单 / 已成团退单）
- 超时自动退单定时任务

**这个项目教什么**：
- 拼团业务的核心难点：并发成团的临界判断
- 跨服务的最终一致性方案（本地消息表 + MQ + 定时补偿）
- 折扣计算的策略模式设计
- 退款链路的状态机设计

**在新系统中的位置**：拼团引擎 + 折扣引擎 + 回调通知体系

### 2.5 能力映射总览

```
                    ┌─────────────────────┐
                    │   统一营销商城平台    │
                    └─────────┬───────────┘
                              │
  ┌──────────┬──────────┬─────┴─────┬──────────┐
  │          │          │           │          │
  ▼          ▼          ▼           ▼          ▼
用户体系   商品/订单   支付收银    营销引擎   基础设施
(微信登录) (SKU/订单) (支付宝/微信)           (MQ/缓存/监控)
  │          │          │           │          │
  │          │          │    ┌──────┴──────┐   │
  │          │          │    │             │   │
  │          │          │  抽奖引擎    拼团引擎 │
  │          │          │  (Lottery) (group-buy)│
  │          │          │    │             │   │
  │          │          │  积分引擎    折扣引擎  │
  │          │          │  (big-market)(group-buy)│
  │          │          │    │             │   │
  │          │          │  风控体系    通知体系  │
  │          │          │  (big-market)(s-pay+gb)│
```

---

## 3. 系统架构设计

### 3.1 整体架构（DDD 六边形）

```
                      ┌──────────────────────────────┐
                      │         Trigger 触发器层       │
                      │  HTTP Controller / MQ Listener │
                      │   RPC Facade / XXL-Job        │
                      └──────────────┬───────────────┘
                                     │
                      ┌──────────────▼───────────────┐
                      │        Application 应用层      │
                      │    流程编排 / AOP / 事件处理    │
                      └──────────────┬───────────────┘
                                     │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
  ┌───────▼───────┐          ┌───────▼───────┐          ┌───────▼───────┐
  │  Domain 领域层  │          │   Types 类型层  │          │    API 接口层  │
  │  核心业务逻辑    │          │  枚举/常量/异常   │          │  DTO / 接口契约 │
  └───────┬───────┘          └───────────────┘          └───────────────┘
          │
  ┌───────▼──────────────────────────────────────────────────────┐
  │                  Infrastructure 基础设施层                     │
  │  DAO / Repository / Redis / MQ / ES / Canal / 外部API适配器    │
  └──────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分

```
uni-market/
├── uni-market-api/                    # RPC/DTO 接口定义
│   └── src/main/java/cn/unimarket/api/
│       ├── dto/                       # 通用 DTO
│       ├── request/                   # 请求对象
│       ├── response/                  # 响应对象
│       └── service/                   # RPC Service 接口
│
├── uni-market-types/                  # 通用类型
│   └── src/main/java/cn/unimarket/types/
│       ├── common/                    # 常量 Constants
│       ├── enums/                     # 枚举
│       ├── exception/                 # 业务异常
│       └── event/                     # 领域事件基类
│
├── uni-market-domain/                 # 领域层 ★核心★
│   └── src/main/java/cn/unimarket/domain/
│       ├── user/                      # 用户领域
│       │   ├── model/                 # 聚合根/实体/值对象
│       │   ├── repository/            # 仓储接口
│       │   └── service/               # 领域服务
│       │       ├── auth/              # 登录认证
│       │       └── account/           # 用户账户
│       ├── product/                   # 商品领域
│       │   ├── model/                 # 商品/分类/SKU
│       │   ├── repository/            # 仓储接口
│       │   └── service/               # 商品查询
│       ├── order/                     # 订单领域
│       │   ├── model/                 # 订单聚合根
│       │   ├── repository/            # 仓储接口
│       │   └── service/               # 下单/支付/退款
│       ├── marketing/                 # 营销领域 ★重点★
│       │   ├── model/                 # 活动/策略/奖品/规则树
│       │   ├── repository/            # 仓储接口
│       │   └── service/               #
│       │       ├── lottery/           # 抽奖引擎
│       │       │   ├── algorithm/     # 抽奖算法
│       │       │   ├── chain/         # 规则责任链
│       │       │   └── tree/          # 决策树引擎
│       │       ├── groupbuy/          # 拼团引擎
│       │       │   ├── lock/          # 锁单
│       │       │   ├── settlement/    # 结算
│       │       │   └── refund/        # 退款
│       │       ├── credit/            # 积分引擎
│       │       ├── rebate/            # 返利引擎
│       │       └── discount/          # 折扣引擎
│       └── payment/                   # 支付领域
│           ├── model/
│           ├── repository/            # 仓储接口(支付单)
│           └── service/               # 支付渠道抽象
│               ├── alipay/
│               └── weixinpay/
│
├── uni-market-infrastructure/         # 基础设施层
│   └── src/main/java/cn/unimarket/infrastructure/
│       ├── dao/                       # MyBatis Mapper
│       ├── po/                        # 持久化对象
│       ├── repository/                # 仓储实现
│       │   ├── user/
│       │   ├── product/
│       │   ├── order/
│       │   ├── marketing/
│       │   └── payment/
│       ├── gateway/                   # 外部服务适配器
│       │   ├── weixin/                # 微信 API（Retrofit2）
│       │   ├── alipay/                # 支付宝 SDK 封装
│       │   └── notification/          # 通知适配器
│       ├── mq/                        # 消息队列
│       │   ├── producer/              # 生产者
│       │   └── consumer/              # 消费者
│       ├── cache/                     # Redis 缓存服务
│       └── es/                        # Elasticsearch 查询
│
├── uni-market-trigger/                # 触发器层
│   └── src/main/java/cn/unimarket/trigger/
│       ├── http/                      # REST Controller
│       │   ├── UserController.java
│       │   ├── ProductController.java
│       │   ├── OrderController.java
│       │   ├── LotteryController.java
│       │   ├── GroupBuyController.java
│       │   └── CreditController.java
│       ├── rpc/                       # Dubbo RPC Facade
│       ├── mq/                        # MQ 监听器
│       ├── job/                       # XXL-Job 定时任务
│       └── portal/                    # 第三方回调入口
│           ├── WeixinPortalController.java
│           └── AlipayNotifyController.java
│
├── uni-market-app/                    # 启动与配置
│   └── src/main/java/cn/unimarket/
│       ├── Application.java           # Spring Boot 启动类
│       ├── config/                    # 配置类
│       └── aop/                       # 切面（限流/日志）
│
└── docs/
    ├── sql/                           # 所有建库建表 SQL
    ├── docker/                        # Docker Compose
    └── PRD-统一营销商城平台.md          # 本文档
```

### 3.3 技术栈

| 层面 | 技术 | 用途 |
|------|------|------|
| 基础框架 | Spring Boot 2.7.x | 应用框架 |
| JDK | Java 8+ | 开发语言 |
| ORM | MyBatis-Spring-Boot 2.1.x | 数据访问 |
| 数据库 | MySQL 8.0 | 主存储 |
| 缓存 | Redis + Redisson | 缓存、分布式锁、库存扣减 |
| 消息队列 | RabbitMQ | 异步解耦 |
| RPC | Apache Dubbo 3.x | 服务间调用 |
| 注册/配置 | Nacos | 服务发现 & 配置管理 |
| 搜索引擎 | Elasticsearch + Canal | 订单查询、用户画像 |
| 定时任务 | XXL-Job | 分布式任务调度 |
| 限流熔断 | Guava RateLimiter + Sentinel | 流量控制 |
| 监控 | Prometheus + Grafana | 系统监控 |
| 日志 | ELK (Logstash + ES + Kibana) | 日志收集分析 |
| 容器化 | Docker Compose | 环境部署 |
| 前端 | React / Vue SPA | 用户端 |

---

## 4. 领域驱动设计

### 4.1 领域全景图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         UniMarket 领域全景                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐          │
│  │   用户    │    │   商品    │    │   订单    │    │   支付    │          │
│  │  Domain  │    │  Domain  │    │  Domain  │    │  Domain  │          │
│  │          │    │          │    │          │    │          │          │
│  │·微信登录  │    │·商品CRUD │    │·下单流程  │    │·支付渠道  │          │
│  │·Token管理│    │·SKU管理  │    │·订单状态机│    │  抽象     │          │
│  │·用户画像  │    │·分类管理  │    │·退款流程  │    │·回调验签  │          │
│  │·收货地址  │    │·库存管理  │    │·订单查询  │    │·对账补偿  │          │
│  └─────┬────┘    └─────┬────┘    └─────┬────┘    └─────┬────┘          │
│        │               │               │               │               │
│        └───────────────┼───────────────┼───────────────┘               │
│                        │               │                                │
│                ┌───────▼───────────────▼───────┐                        │
│                │          营销 Domain ★        │                        │
│                │                               │                        │
│                │  ┌─────────┐  ┌─────────────┐ │                        │
│                │  │ 抽奖引擎 │  │  拼团引擎    │ │                        │
│                │  │·概率算法 │  │·开团/参团   │ │                        │
│                │  │·规则链   │  │·锁单/结算   │ │                        │
│                │  │·决策树   │  │·退款策略    │ │                        │
│                │  │·奖品发放 │  │·成团回调    │ │                        │
│                │  └────┬────┘  └──────┬──────┘ │                        │
│                │       │              │         │                        │
│                │  ┌────▼──────────────▼──────┐  │                        │
│                │  │       积分引擎           │  │                        │
│                │  │  ·积分账户              │  │                        │
│                │  │  ·行为返利(签到/支付)    │  │                        │
│                │  │  ·积分兑换(抽奖次数/SKU) │  │                        │
│                │  └─────────────────────────┘  │                        │
│                │                               │                        │
│                │  ┌─────────────────────────┐  │                        │
│                │  │       折扣引擎           │  │                        │
│                │  │  ·直减/满减/折扣/N元购   │  │                        │
│                │  │  ·优惠试算               │  │                        │
│                │  └─────────────────────────┘  │                        │
│                │                               │                        │
│                │  ┌─────────────────────────┐  │                        │
│                │  │       风控体系           │  │                        │
│                │  │  ·限流(用户/IP/接口)     │  │                        │
│                │  │  ·黑名单(自动/手动)      │  │                        │
│                │  │  ·熔断降级              │  │                        │
│                │  │  ·人群标签(定向/排除)    │  │                        │
│                │  └─────────────────────────┘  │                        │
│                └───────────────────────────────┘                        │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                        通知 Domain                                │   │
│  │  ·微信模板消息 ·系统内消息 ·拼团成团回调(HTTP/MQ) ·异步通知补偿    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 核心聚合根设计

#### 用户聚合（User）

```
User (聚合根)
├── userId: String                # 用户ID（雪花算法或自增）
├── openId: String                # 微信 openId
├── unionId: String               # 微信 unionId
├── nickname: String              # 昵称
├── avatar: String                # 头像
├── phone: String                 # 手机号
├── status: UserStatus            # 状态（正常/冻结/注销）
├── createTime: DateTime
├── addresses: List<Address>      # 收货地址（值对象集合）
└── creditAccount: CreditAccount  # 积分账户
    ├── totalAmount: BigDecimal   # 累计获得积分
    ├── availableAmount: BigDecimal # 可用积分
    └── freezeAmount: BigDecimal  # 冻结积分
```

#### 订单聚合（Order）

```
Order (聚合根)
├── orderId: String               # 订单ID
├── userId: String                # 用户ID
├── orderType: OrderType          # 订单类型（普通/拼团/积分兑换）
├── items: List<OrderItem>        # 订单明细
│   ├── productId: String
│   ├── skuId: String
│   ├── productName: String
│   ├── quantity: Integer
│   ├── originalPrice: BigDecimal # 原价
│   └── actualPrice: BigDecimal   # 实付金额
├── totalAmount: BigDecimal       # 订单总金额
├── discountAmount: BigDecimal    # 优惠金额
├── payAmount: BigDecimal         # 实付金额
├── status: OrderStatus           # CREATE → PAY_WAIT → PAY_SUCCESS → DEAL_DONE / CLOSE / REFUND
├── payChannel: PayChannel        # 支付渠道
├── payTime: DateTime
├── createTime: DateTime
├── activityId: String            # 关联营销活动ID（可选）
├── teamId: String                # 关联拼团队伍ID（可选）
└── outTradeNo: String            # 外部支付流水号
```

#### 营销活动聚合（MarketingActivity）

```
MarketingActivity (聚合根)
├── activityId: String            # 活动ID
├── activityName: String          # 活动名称
├── activityType: ActivityType    # 活动类型（抽奖/拼团/秒杀）
├── strategyId: String            # 关联策略ID
├── discountId: String            # 关联折扣ID
├── beginTime: DateTime
├── endTime: DateTime
├── status: ActivityStatus        # 编辑/提审/通过/运行/关闭
├── skuList: List<ActivitySku>    # 活动SKU
└── limitConfig: LimitConfig      # 限制配置
    ├── totalStock: Integer       # 总库存
    ├── userTakeLimit: Integer    # 用户参与次数限制
    ├── userDayLimit: Integer     # 用户每日限制
    └── userMonthLimit: Integer   # 用户每月限制
```

#### 抽奖策略聚合（Strategy）

```
Strategy (聚合根)
├── strategyId: String            # 策略ID
├── strategyMode: StrategyMode    # 策略模式（单项概率/总体概率）
├── grantType: GrantType          # 发奖方式（即时/定时/人工）
├── ruleModels: String            # 规则模型配置
├── awards: List<StrategyAward>   # 奖品概率配置
│   ├── awardId: String
│   ├── awardRate: BigDecimal     # 中奖概率
│   ├── awardCount: Integer       # 奖品总量
│   ├── awardSurplusCount: Integer # 奖品剩余量
│   └── ruleModels: String        # 奖品规则
└── rules: List<StrategyRule>     # 策略规则
    ├── ruleModel: RuleModel      # 规则类型（权重/黑名单）
    └── ruleValue: String         # 规则值
```

#### 拼团聚合（GroupBuyTeam）

```
GroupBuyTeam (聚合根)
├── teamId: String                # 拼团队伍ID
├── activityId: String            # 活动ID
├── targetCount: Integer          # 目标人数
├── completeCount: Integer        # 已完成支付人数
├── lockCount: Integer            # 已锁单人数
├── status: TeamStatus            # 拼团中/已成团/已失败/含退单
├── validTime: Integer            # 有效时间(分钟)
├── expireTime: DateTime          # 过期时间
├── startTime: DateTime           # 开团时间
├── completeTime: DateTime        # 成团时间
├── notifyUrl: String             # 成团回调URL
├── orders: List<GroupBuyOrder>   # 订单明细（拼团订单）
│   ├── userId: String
│   ├── orderId: String
│   ├── status: Integer           # 0锁单/1已完成/2退单
│   ├── outTradeNo: String
│   ├── outTradeTime: DateTime
│   └── bizId: String             # activityId_userId_count 防重
└── discount: DiscountConfig      # 折扣配置
    ├── marketPlan: String        # ZJ/MJ/ZK/N
    ├── marketExpr: String        # 优惠表达式
    └── discountAmount: BigDecimal # 折后金额
```

#### 购物车模型（Cart）

```
Cart (值对象，归属用户聚合)
├── userId: String                # 用户ID
├── items: List<CartItem>         # 购物车商品列表
│   ├── skuId: String
│   ├── productId: String
│   ├── productName: String
│   ├── skuName: String
│   ├── image: String
│   ├── price: BigDecimal         # 加入时价格
│   ├── quantity: Integer
│   ├── selected: Boolean        # 是否选中
│   ├── stock: Integer           # 当前库存（实时查询）
│   └── addTime: DateTime
└── totalSelectedAmount: BigDecimal  # 选中商品总价（实时计算）

设计决策：
- 购物车数据存储分两层：Redis（热数据，30天TTL）+ MySQL（冷备份）
- 未登录加购存在 Redis(key=cart:device:{deviceId}) → 登录后合并
- 进入购物车页时实时校验库存和价格，标记失效商品
```

#### 优惠券聚合（Coupon）

```
CouponTemplate (聚合根 — 券模板)
├── templateId: String            # 模板ID
├── couponName: String            # 券名称
├── couponType: CouponType        # 满减券 / 折扣券 / 运费券 / 商品券
├── thresholdAmount: BigDecimal   # 使用门槛金额
├── discountAmount: BigDecimal    # 优惠金额（满减券）
├── discountRate: BigDecimal      # 折扣率（折扣券，0.85=85折）
├── maxDiscountAmount: BigDecimal # 最大折扣金额（折扣券上限）
├── applicableScope: String       # 适用范围（ALL全部 / CATEGORY分类 / PRODUCT指定商品）
├── scopeValue: String            # 适用范围值（分类ID或商品ID列表JSON）
├── totalQuantity: Integer        # 发行总量
├── receivedQuantity: Integer     # 已领取量
├── usedQuantity: Integer         # 已使用量
├── userLimit: Integer            # 每人限领
├── validDays: Integer            # 有效期天数（从领取日算起）
├── startTime: DateTime           # 固定有效期-开始
├── endTime: DateTime             # 固定有效期-结束
├── status: TemplateStatus        # 草稿/已发布/已停用
└── obtainChannels: String        # 获取渠道(ACTIVITY活动发放/SIGN签到赠送/MANUAL手动领取/NEW_USER新人)

UserCoupon (实体 — 用户领到的券)
├── userCouponId: String          # 用户券ID
├── userId: String
├── templateId: String
├── status: CouponStatus          # 未使用/已锁定(下单中)/已使用/已过期/已退回
├── orderId: String               # 关联订单ID（锁定/使用时记录）
├── receiveTime: DateTime
├── useTime: DateTime
├── expireTime: DateTime
└── source: String                # 来源(活动ID/签到/手动领取)
```

#### 售后聚合（AfterSale）

```
AfterSale (聚合根)
├── afterSaleId: String           # 售后单ID
├── orderId: String               # 原订单ID
├── userId: String
├── type: AfterSaleType           # 仅退款 / 退货退款
├── reason: String                # 申请原因
├── description: String           # 问题描述
├── evidenceImages: String        # 凭证图片JSON
├── refundAmount: BigDecimal      # 退款金额
├── status: AfterSaleStatus       # 待审核→审核通过(待寄回)→寄回中→待收货→已完成 / 审核拒绝 / 已取消
├── logistics: AfterSaleLogistics # 退货物流信息
│   ├── company: String
│   ├── trackingNo: String
│   └── shipTime: DateTime
├── auditRemark: String           # 审核备注
├── createTime: DateTime
├── updateTime: DateTime
└── timeline: List<AfterSaleEvent> # 售后事件时间线
    ├── eventType: String         # APPLY申请/AUDIT审核/SHIP寄回/RECEIVE收货/REFUND退款/COMPLETE完成
    └── eventTime: DateTime

关联操作（售后通过后自动触发）：
- 退款：原路退回支付渠道 + 退积分（如果用了积分）+ 退券（如果用了券）
- 退货退款：用户先寄回 → 商家确认收货 → 再执行退款
```

#### 物流聚合（Logistics）

```
Logistics (实体，关联订单)
├── orderId: String               # 订单ID
├── trackingNo: String            # 快递单号
├── companyCode: String           # 快递公司编码（SF/YTO/ZTO/...）
├── companyName: String           # 快递公司名称
├── status: LogisticsStatus       # 待发货/运输中/派送中/已签收/异常
├── traces: List<LogisticsTrace>  # 物流轨迹
│   ├── time: DateTime
│   ├── status: String            # 轨迹状态描述
│   └── location: String          # 轨迹地点
├── shipTime: DateTime            # 发货时间
├── signTime: DateTime            # 签收时间
└── updateTime: DateTime

设计决策：
- 物流数据从第三方（快递鸟/菜鸟）拉取，不做主存储源
- 关键节点（发货/签收）通过回调/定时轮询写入本地
- 签收后自动触发"确认收货"→ 订单状态变更为 DEAL_DONE
```

### 4.3 领域服务设计

| 领域服务 | 职责 | 核心方法 |
|----------|------|----------|
| `UserAuthService` | 微信登录认证 | `getQrCodeTicket()`, `checkLogin()`, `loginByOpenId()` |
| `ProductQueryService` | 商品查询与搜索 | `queryProductList()`, `queryProductDetail()`, `searchProducts()`, `querySkuActivityProducts()` |
| `ProductSearchService` | ES 商品搜索 | `fullTextSearch()`, `filterByCategory()`, `filterByPrice()`, `sortBy()` |
| `CartService` | 购物车管理 | `addToCart()`, `updateQuantity()`, `removeItem()`, `mergeCart()`, `validateCart()` |
| `OrderService` | 订单管理 | `createOrder()`, `payCallback()`, `refundOrder()`, `cancelTimeoutOrder()` |
| `OrderFlowService` | 订单履约 | `confirmShip()`, `autoConfirmReceive()`, `completeOrder()` |
| `PaymentService` | 支付渠道抽象 | `createPayOrder()`, `unifiedCashier()`, `verifyNotify()`, `queryPayStatus()` |
| `CouponService` | 优惠券管理 | `issueCoupon()`, `receiveCoupon()`, `lockCoupon()`, `useCoupon()`, `refundCoupon()` |
| `CouponCalculateService` | 优惠计算 | `calculateBestCoupon(cart, userCoupons)` → 返回最优券组合 |
| `AfterSaleService` | 售后管理 | `applyRefund()`, `applyReturn()`, `auditAfterSale()`, `shipReturn()`, `confirmReturn()` |
| `LogisticsService` | 物流管理 | `queryTracking()`, `subscribeTracking()`, `handleTrackingCallback()` |
| `LotteryDrawService` | 抽奖执行 | `doDraw()`, `doQuantificationDraw()` |
| `LotteryAlgorithmService` | 抽奖算法 | `randomDraw(strategyId, excludeAwardIds)` |
| `RuleEngineService` | 规则引擎 | `processDecisionTree(treeId, matterMap)` |
| `GroupBuyService` | 拼团管理 | `lockOrder()`, `settlementOrder()`, `refundOrder()` |
| `GroupBuyTeamService` | 成团判断 | `tryCompleteTeam(teamId)` |
| `CreditService` | 积分管理 | `adjustCredit()`, `queryAccount()`, `payByCredit()` |
| `RebateService` | 行为返利 | `createRebateOrder(behaviorType)`, `executeRebate()` |
| `DiscountCalculateService` | 优惠计算 | `calculate(plan, expr, originalPrice)` |
| `HomePageService` | 首页配置 | `queryBanners()`, `queryChannels()`, `queryRecommendProducts()` |
| `NotificationService` | 通知发送 | `sendWeixinTemplateMessage()`, `sendInAppMessage()`, `notifyTeamSuccess()` |

---

## 5. 数据库设计

### 5.1 分库策略

```
uni_market (db00) — 公共配置库
  ├── 商品相关: product, sku, category
  ├── 营销配置: activity, strategy, award, rule_tree, discount
  ├── 行为返利: daily_behavior_rebate
  └── 人群标签: crowd_tags, crowd_tags_detail

uni_market_01 / uni_market_02 — 用户数据分库（按 userId 哈希）
  每库4张分表(_000 ~ _003) → 共 2×4 = 8 分片
  ├── user（用户基础信息）
  ├── user_address（收货地址）
  ├── user_credit_account（积分账户）
  ├── user_credit_order（积分流水）
  ├── order（订单）
  ├── order_item（订单明细）
  ├── pay_order（支付单）
  ├── lottery_order（抽奖订单）
  ├── user_award_record（中奖记录）
  ├── group_buy_order（拼团订单）
  ├── user_behavior_rebate_order（返利订单）
  ├── raffle_activity_account（活动账户）
  └── notify_task（通知任务）
```

### 5.2 核心表 DDL

#### 5.2.1 公共库（db00）

```sql
-- 商品表
CREATE TABLE `product` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` VARCHAR(32) NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(128) NOT NULL COMMENT '商品名称',
  `category_id` VARCHAR(32) NOT NULL COMMENT '分类ID',
  `description` TEXT COMMENT '商品描述',
  `main_image` VARCHAR(512) COMMENT '主图',
  `original_price` DECIMAL(10,2) NOT NULL COMMENT '原价',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1上架/0下架)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_id` (`product_id`),
  KEY `idx_category_status` (`category_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- SKU 表
CREATE TABLE `sku` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `sku_id` VARCHAR(32) NOT NULL COMMENT 'SKU ID',
  `product_id` VARCHAR(32) NOT NULL COMMENT '商品ID',
  `sku_name` VARCHAR(128) NOT NULL COMMENT 'SKU名称',
  `sku_attrs` VARCHAR(256) COMMENT 'SKU属性JSON',
  `price` DECIMAL(10,2) NOT NULL COMMENT '售价',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '库存',
  `image` VARCHAR(512) COMMENT '图片',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_id` (`sku_id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SKU表';

-- 营销活动表
CREATE TABLE `marketing_activity` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `activity_id` VARCHAR(32) NOT NULL COMMENT '活动ID',
  `activity_name` VARCHAR(64) NOT NULL COMMENT '活动名称',
  `activity_type` VARCHAR(16) NOT NULL COMMENT '活动类型(LOTTERY抽奖/GROUP_BUY拼团/FLASH_SALE秒杀)',
  `strategy_id` VARCHAR(32) COMMENT '关联策略ID(抽奖)',
  `discount_id` VARCHAR(32) COMMENT '关联折扣ID(拼团)',
  `begin_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '结束时间',
  `status` VARCHAR(16) NOT NULL DEFAULT 'EDIT' COMMENT 'EDIT编辑/ARRAIGNMENT提审/PASS通过/DOING运行中/CLOSE已关闭/REFUSE拒绝',
  `tag_id` VARCHAR(32) COMMENT '人群标签ID',
  `tag_scope` VARCHAR(16) COMMENT '标签范围(VISIBLE可见/PARTICIPATE参与)',
  `description` TEXT COMMENT '活动描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_id` (`activity_id`),
  KEY `idx_status_time` (`status`, `begin_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='营销活动表';

-- 活动 SKU 关联表
CREATE TABLE `activity_sku` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `activity_id` VARCHAR(32) NOT NULL,
  `sku_id` VARCHAR(32) NOT NULL,
  `activity_price` DECIMAL(10,2) COMMENT '活动价',
  `stock_count` INT NOT NULL DEFAULT 0 COMMENT '活动库存',
  `stock_surplus_count` INT NOT NULL DEFAULT 0 COMMENT '剩余库存',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_sku_id` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动SKU关联表';

-- 抽奖策略表
CREATE TABLE `strategy` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `strategy_id` VARCHAR(32) NOT NULL COMMENT '策略ID',
  `strategy_name` VARCHAR(64) NOT NULL COMMENT '策略名称',
  `strategy_mode` TINYINT NOT NULL DEFAULT 1 COMMENT '策略模式(1单项概率/2总体概率)',
  `grant_type` TINYINT NOT NULL DEFAULT 1 COMMENT '发奖方式(1即时/2定时/3人工)',
  `rule_models` VARCHAR(512) COMMENT '规则模型配置JSON',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_strategy_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖策略表';

-- 策略奖品配置表
CREATE TABLE `strategy_award` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `strategy_id` VARCHAR(32) NOT NULL,
  `award_id` VARCHAR(32) NOT NULL COMMENT '奖品ID',
  `award_sort` INT NOT NULL DEFAULT 1 COMMENT '排序',
  `award_count` INT NOT NULL DEFAULT 0 COMMENT '奖品总量',
  `award_surplus_count` INT NOT NULL DEFAULT 0 COMMENT '奖品剩余',
  `award_rate` DECIMAL(6,4) NOT NULL COMMENT '中奖概率',
  `rule_models` VARCHAR(256) COMMENT '奖品规则',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略奖品配置表';

-- 奖品表
CREATE TABLE `award` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `award_id` VARCHAR(32) NOT NULL,
  `award_type` TINYINT NOT NULL COMMENT '奖品类型(1文字描述/2兑换码/3优惠券/4实物商品/5积分)',
  `award_name` VARCHAR(64) NOT NULL,
  `award_content` VARCHAR(512) COMMENT '奖品内容',
  `award_config` VARCHAR(512) COMMENT '奖品配置JSON',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_award_id` (`award_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖品表';

-- 规则树表
CREATE TABLE `rule_tree` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tree_id` VARCHAR(32) NOT NULL COMMENT '规则树ID',
  `tree_name` VARCHAR(64) NOT NULL COMMENT '规则树名称',
  `tree_desc` VARCHAR(256) COMMENT '规则树描述',
  `tree_root_node_id` VARCHAR(32) NOT NULL COMMENT '根节点ID',
  `tree_node_rule_key` VARCHAR(64) COMMENT '节点规则Key',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tree_id` (`tree_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则树表';

-- 规则树节点表
CREATE TABLE `rule_tree_node` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tree_id` VARCHAR(32) NOT NULL,
  `node_id` VARCHAR(32) NOT NULL COMMENT '节点ID',
  `node_type` TINYINT NOT NULL COMMENT '节点类型(1子叶/2果实)',
  `node_value` VARCHAR(128) COMMENT '节点值(果实节点的输出)',
  `rule_key` VARCHAR(64) COMMENT '规则Key(子叶节点的过滤Key)',
  `rule_desc` VARCHAR(128) COMMENT '规则描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tree_id` (`tree_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则树节点表';

-- 规则树连线表
CREATE TABLE `rule_tree_node_line` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tree_id` VARCHAR(32) NOT NULL,
  `node_id_from` VARCHAR(32) NOT NULL COMMENT '来源节点',
  `node_id_to` VARCHAR(32) NOT NULL COMMENT '目标节点',
  `rule_limit_type` TINYINT NOT NULL COMMENT '限定类型(1:=/2:>/3:</4:>=/5:<=/6:enum)',
  `rule_limit_value` VARCHAR(128) NOT NULL COMMENT '限定值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tree_id` (`tree_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则树连线表';

-- 折扣配置表
CREATE TABLE `discount` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `discount_id` VARCHAR(32) NOT NULL COMMENT '折扣ID',
  `discount_name` VARCHAR(64) NOT NULL COMMENT '折扣名称',
  `market_plan` VARCHAR(16) NOT NULL COMMENT '优惠类型(ZJ直减/MJ满减/ZK折扣/N N元购)',
  `market_expr` VARCHAR(64) NOT NULL COMMENT '优惠表达式(如100-10/0.85/99)',
  `discount_type` VARCHAR(16) COMMENT '折扣分类',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_discount_id` (`discount_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折扣配置表';

-- 行为返利配置表
CREATE TABLE `daily_behavior_rebate` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `behavior_type` VARCHAR(16) NOT NULL COMMENT '行为类型(sign签到/pay支付/share分享)',
  `rebate_type` VARCHAR(16) NOT NULL COMMENT '返利类型(sku抽奖次数/integral积分)',
  `rebate_value` VARCHAR(64) NOT NULL COMMENT '返利值',
  `rebate_desc` VARCHAR(128) COMMENT '返利描述',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行为返利配置表';

-- 人群标签表
CREATE TABLE `crowd_tags` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tag_id` VARCHAR(32) NOT NULL COMMENT '标签ID',
  `tag_name` VARCHAR(64) NOT NULL COMMENT '标签名称',
  `tag_desc` VARCHAR(256) COMMENT '标签描述',
  `tag_type` VARCHAR(16) COMMENT '标签类型',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群标签表';

CREATE TABLE `crowd_tags_detail` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `tag_id` VARCHAR(32) NOT NULL,
  `user_id` VARCHAR(32) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tag_user` (`tag_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人群标签明细表';

-- =====================================================
-- 以下为新增模块表（v1.1 补充）
-- =====================================================

-- 优惠券模板表（公共库）
CREATE TABLE `coupon_template` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `template_id` VARCHAR(32) NOT NULL COMMENT '模板ID',
  `coupon_name` VARCHAR(64) NOT NULL COMMENT '券名称',
  `coupon_type` VARCHAR(16) NOT NULL COMMENT '券类型(MJ满减/ZK折扣/FREIGHT运费/PRODUCT商品券)',
  `threshold_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '使用门槛金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额(满减券)',
  `discount_rate` DECIMAL(3,2) COMMENT '折扣率(折扣券)',
  `max_discount_amount` DECIMAL(10,2) COMMENT '最大折扣金额(折扣券上限)',
  `applicable_scope` VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT '适用范围(ALL/CATEGORY/PRODUCT)',
  `scope_value` TEXT COMMENT '适用范围值JSON',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '发行总量',
  `received_quantity` INT NOT NULL DEFAULT 0 COMMENT '已领取量',
  `used_quantity` INT NOT NULL DEFAULT 0 COMMENT '已使用量',
  `user_limit` INT NOT NULL DEFAULT 1 COMMENT '每人限领',
  `valid_days` INT COMMENT '有效期天数(从领取日算起)',
  `start_time` DATETIME COMMENT '固定有效期-开始',
  `end_time` DATETIME COMMENT '固定有效期-结束',
  `status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT草稿/PUBLISHED已发布/DISABLED已停用',
  `obtain_channels` VARCHAR(128) COMMENT '获取渠道(逗号分隔:ACTIVITY,SIGN,MANUAL,NEW_USER)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_id` (`template_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- 用户优惠券表（分库分表）
CREATE TABLE `user_coupon_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_coupon_id` VARCHAR(32) NOT NULL COMMENT '用户券ID',
  `user_id` VARCHAR(32) NOT NULL,
  `template_id` VARCHAR(32) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'UNUSED' COMMENT 'UNUSED未使用/LOCKED已锁定/USED已使用/EXPIRED已过期/REFUNDED已退回',
  `order_id` VARCHAR(32) COMMENT '关联订单ID',
  `receive_time` DATETIME NOT NULL COMMENT '领取时间',
  `use_time` DATETIME COMMENT '使用时间',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `source` VARCHAR(64) COMMENT '来源',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_coupon_id` (`user_coupon_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- 购物车表（分库分表，冷数据备份，热数据在 Redis）
CREATE TABLE `cart_item_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `sku_id` VARCHAR(32) NOT NULL,
  `product_id` VARCHAR(32) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否选中',
  `add_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表(冷备份)';

-- 售后表（分库分表）
CREATE TABLE `after_sale_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `after_sale_id` VARCHAR(32) NOT NULL COMMENT '售后单ID',
  `order_id` VARCHAR(32) NOT NULL COMMENT '原订单ID',
  `user_id` VARCHAR(32) NOT NULL,
  `type` VARCHAR(16) NOT NULL COMMENT 'REFUND仅退款/RETURN退货退款',
  `reason` VARCHAR(128) NOT NULL COMMENT '申请原因',
  `description` TEXT COMMENT '问题描述',
  `evidence_images` TEXT COMMENT '凭证图片JSON',
  `refund_amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING_AUDIT' COMMENT 'PENDING_AUDIT待审核/AUDIT_PASS审核通过/AUDIT_REJECT审核拒绝/PENDING_SHIP待寄回/SHIPPED寄回中/RECEIVED已收货/REFUNDED已退款/COMPLETED已完成/CANCELLED已取消',
  `logistics_company` VARCHAR(64) COMMENT '退货物流公司',
  `logistics_no` VARCHAR(64) COMMENT '退货物流单号',
  `ship_time` DATETIME COMMENT '寄回时间',
  `audit_remark` VARCHAR(256) COMMENT '审核备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_after_sale_id` (`after_sale_id`),
  KEY `idx_user_order` (`user_id`, `order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后表';

-- 物流信息表（公共库）
CREATE TABLE `logistics` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` VARCHAR(32) NOT NULL COMMENT '订单ID',
  `tracking_no` VARCHAR(64) NOT NULL COMMENT '快递单号',
  `company_code` VARCHAR(16) NOT NULL COMMENT '快递公司编码',
  `company_name` VARCHAR(64) NOT NULL COMMENT '快递公司名称',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待发货/TRANSPORT运输中/DELIVERING派送中/SIGNED已签收/EXCEPTION异常',
  `traces` MEDIUMTEXT COMMENT '物流轨迹JSON',
  `ship_time` DATETIME COMMENT '发货时间',
  `sign_time` DATETIME COMMENT '签收时间',
  `last_query_time` DATETIME COMMENT '最后查询时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_tracking_no` (`tracking_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流信息表';

-- 首页 Banner 配置表（公共库）
CREATE TABLE `home_banner` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `banner_id` VARCHAR(32) NOT NULL,
  `title` VARCHAR(64) NOT NULL COMMENT '标题',
  `image_url` VARCHAR(512) NOT NULL COMMENT '图片URL',
  `link_type` VARCHAR(16) COMMENT '链接类型(PRODUCT/ACTIVITY/URL)',
  `link_value` VARCHAR(512) COMMENT '链接值',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用/0禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页Banner表';

-- 首页频道配置表（公共库）
CREATE TABLE `home_channel` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `channel_id` VARCHAR(32) NOT NULL,
  `channel_name` VARCHAR(64) NOT NULL COMMENT '频道名称',
  `channel_icon` VARCHAR(512) COMMENT '频道图标',
  `link_type` VARCHAR(16) COMMENT '链接类型',
  `link_value` VARCHAR(512) COMMENT '链接值',
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页频道表';

-- 站内消息表（分库分表）
CREATE TABLE `user_message_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `message_id` VARCHAR(32) NOT NULL COMMENT '消息ID',
  `title` VARCHAR(128) NOT NULL COMMENT '消息标题',
  `content` TEXT COMMENT '消息内容',
  `message_type` VARCHAR(16) NOT NULL COMMENT '消息类型(SYSTEM系统/ORDER订单/ACTIVITY活动/COUPON优惠券)',
  `link_url` VARCHAR(512) COMMENT '跳转链接',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读/1已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`),
  KEY `idx_user_read` (`user_id`, `is_read`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内消息表';
```

#### 5.2.2 用户分库（db01/db02，各含 _000 ~ _003 分表）

```sql
-- 用户表（分库分表）
CREATE TABLE `user_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `open_id` VARCHAR(64) NOT NULL COMMENT '微信openId',
  `union_id` VARCHAR(64) COMMENT '微信unionId',
  `nickname` VARCHAR(64) COMMENT '昵称',
  `avatar` VARCHAR(512) COMMENT '头像',
  `phone` VARCHAR(16) COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1正常/2冻结/3注销)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`),
  UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 订单表（分库分表）
CREATE TABLE `order_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` VARCHAR(32) NOT NULL COMMENT '订单ID',
  `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `order_type` VARCHAR(16) NOT NULL COMMENT '订单类型(NORMAL普通/GROUP_BUY拼团/CREDIT积分兑换)',
  `activity_id` VARCHAR(32) COMMENT '关联活动ID',
  `team_id` VARCHAR(32) COMMENT '拼团队伍ID',
  `total_amount` DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0.00 COMMENT '优惠金额',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `pay_channel` VARCHAR(16) COMMENT '支付渠道(ALIPAY/WXPAY)',
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATE' COMMENT 'CREATE/PAY_WAIT/PAY_SUCCESS/DEAL_DONE/CLOSE/REFUND',
  `pay_time` DATETIME COMMENT '支付时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_status_time` (`status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 订单明细表（分库分表）
CREATE TABLE `order_item_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` VARCHAR(32) NOT NULL,
  `product_id` VARCHAR(32) NOT NULL,
  `sku_id` VARCHAR(32) NOT NULL,
  `product_name` VARCHAR(128) NOT NULL,
  `sku_name` VARCHAR(128) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `original_price` DECIMAL(10,2) NOT NULL,
  `actual_price` DECIMAL(10,2) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 支付单表（分库分表）
CREATE TABLE `pay_order_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `pay_id` VARCHAR(32) NOT NULL COMMENT '支付单ID',
  `order_id` VARCHAR(32) NOT NULL COMMENT '订单ID',
  `user_id` VARCHAR(32) NOT NULL COMMENT '用户ID',
  `pay_channel` VARCHAR(16) NOT NULL COMMENT '支付渠道',
  `out_trade_no` VARCHAR(64) COMMENT '外部支付流水号',
  `pay_amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `pay_url` VARCHAR(2048) COMMENT '支付链接/表单',
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATE' COMMENT 'CREATE/PAYING/SUCCESS/FAIL',
  `pay_time` DATETIME COMMENT '支付完成时间',
  `notify_time` DATETIME COMMENT '回调通知时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_id` (`pay_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_out_trade_no` (`out_trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单表';

-- 用户积分账户表（分库分表）
CREATE TABLE `user_credit_account_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计积分',
  `available_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '可用积分',
  `freeze_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '冻结积分',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户表';

-- 积分流水表（分库分表）
CREATE TABLE `user_credit_order_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `order_id` VARCHAR(32) NOT NULL COMMENT '积分交易单号',
  `trade_type` VARCHAR(16) NOT NULL COMMENT '交易类型(FORWARD正向/REVERSE逆向)',
  `trade_amount` DECIMAL(12,2) NOT NULL COMMENT '交易金额',
  `trade_name` VARCHAR(64) COMMENT '交易名称',
  `out_biz_no` VARCHAR(64) COMMENT '外部业务单号',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水表';

-- 抽奖订单表（分库分表）
CREATE TABLE `lottery_order_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` VARCHAR(32) NOT NULL COMMENT '抽奖订单ID(雪花算法)',
  `user_id` VARCHAR(32) NOT NULL,
  `activity_id` VARCHAR(32) NOT NULL,
  `strategy_id` VARCHAR(32) NOT NULL,
  `take_id` VARCHAR(32) COMMENT '参与记录ID',
  `award_id` VARCHAR(32) COMMENT '奖品ID',
  `award_type` TINYINT COMMENT '奖品类型',
  `award_content` VARCHAR(512) COMMENT '奖品内容',
  `grant_state` TINYINT NOT NULL DEFAULT 1 COMMENT '发放状态(1初始/2完成/3失败)',
  `mq_state` TINYINT NOT NULL DEFAULT 0 COMMENT 'MQ状态(0未发送/1成功/2失败)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_user_activity` (`user_id`, `activity_id`),
  KEY `idx_mq_state` (`mq_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='抽奖订单表';

-- 活动参与账户表（分库分表）
CREATE TABLE `raffle_activity_account_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `activity_id` VARCHAR(32) NOT NULL,
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总次数',
  `total_count_surplus` INT NOT NULL DEFAULT 0 COMMENT '总剩余次数',
  `day_count` INT NOT NULL DEFAULT 0 COMMENT '日次数',
  `day_count_surplus` INT NOT NULL DEFAULT 0 COMMENT '日剩余次数',
  `month_count` INT NOT NULL DEFAULT 0 COMMENT '月次数',
  `month_count_surplus` INT NOT NULL DEFAULT 0 COMMENT '月剩余次数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动参与账户表';

-- 拼团队伍表（公共库，不分库分表）
CREATE TABLE `group_buy_team` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `team_id` VARCHAR(32) NOT NULL COMMENT '拼团ID',
  `activity_id` VARCHAR(32) NOT NULL COMMENT '活动ID',
  `target_count` INT NOT NULL COMMENT '成团人数',
  `complete_count` INT NOT NULL DEFAULT 0 COMMENT '已完成人数',
  `lock_count` INT NOT NULL DEFAULT 0 COMMENT '已锁单人数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态(0拼团中/1已成团/2已失败/3含退单)',
  `valid_time` INT NOT NULL COMMENT '有效时长(分钟)',
  `expire_time` DATETIME NOT NULL COMMENT '过期时间',
  `start_time` DATETIME NOT NULL COMMENT '开团时间',
  `complete_time` DATETIME COMMENT '成团时间',
  `notify_url` VARCHAR(512) COMMENT '回调通知URL',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_team_id` (`team_id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团队伍表';

-- 拼团订单明细表（分库分表）
CREATE TABLE `group_buy_order_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `team_id` VARCHAR(32) NOT NULL,
  `order_id` VARCHAR(32) NOT NULL COMMENT '关联订单ID',
  `activity_id` VARCHAR(32) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0锁单/1已完成支付/2已退单',
  `out_trade_no` VARCHAR(64) COMMENT '外部支付流水号',
  `out_trade_time` DATETIME COMMENT '支付时间',
  `biz_id` VARCHAR(128) NOT NULL COMMENT '防重键: activityId_userId_count',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拼团订单明细表';

-- 通知任务表（公共库）
CREATE TABLE `notify_task` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `team_id` VARCHAR(32) COMMENT '拼团ID',
  `order_id` VARCHAR(32) COMMENT '订单ID(退款通知)',
  `activity_id` VARCHAR(32) COMMENT '活动ID',
  `notify_category` VARCHAR(16) NOT NULL COMMENT '通知分类(TEAM_SUCCESS成团/REFUND退款)',
  `notify_type` VARCHAR(16) NOT NULL COMMENT '通知方式(HTTP/MQ)',
  `notify_status` VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/RETRY/FAIL',
  `notify_url` VARCHAR(512) COMMENT '回调URL',
  `notify_body` TEXT COMMENT '通知内容',
  `retry_count` INT NOT NULL DEFAULT 0,
  `uuid` VARCHAR(64) NOT NULL COMMENT '唯一标识',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_team_id` (`team_id`),
  KEY `idx_status` (`notify_status`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知任务表';

-- MQ消息任务表（分库分表，用于最终一致性补偿）
CREATE TABLE `mq_task_000` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` VARCHAR(32) NOT NULL,
  `message_id` VARCHAR(64) NOT NULL COMMENT '消息ID',
  `topic` VARCHAR(64) NOT NULL COMMENT '消息主题',
  `message_body` TEXT COMMENT '消息体',
  `state` TINYINT NOT NULL DEFAULT 0 COMMENT '0未发送/1成功/2失败',
  `retry_count` INT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_state` (`state`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消息任务表';
```

---

## 6. API 接口设计

### 6.1 接口总览

所有接口基础路径：`/api/v1/`

| 模块 | 路径前缀 | 说明 |
|------|----------|------|
| 用户 | `/user/` | 微信登录、用户信息 |
| 商品 | `/product/` | 商品查询 |
| 订单 | `/order/` | 下单、支付、查询 |
| 支付 | `/pay/` | 支付单创建、回调 |
| 抽奖 | `/lottery/` | 抽奖、奖品查询 |
| 拼团 | `/groupbuy/` | 开团、参团、查询 |
| 积分 | `/credit/` | 积分查询、积分兑换 |
| 返利 | `/rebate/` | 签到、行为返利 |
| 运营 | `/erp/` | 活动管理、数据查询 |

### 6.2 用户模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/weixin_qrcode_ticket` | 获取微信扫码登录 ticket |
| GET | `/user/check_login?ticket={ticket}` | 轮询检测登录状态 |
| GET | `/user/info` | 获取当前用户信息 |
| PUT | `/user/address` | 新增/修改收货地址 |
| GET | `/user/addresses` | 查询收货地址列表 |

### 6.3 商品模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/product/list` | 商品列表（分页、分类筛选） |
| GET | `/product/detail?productId={id}` | 商品详情 |
| GET | `/product/activity_products?activityId={id}` | 活动关联商品列表 |

### 6.4 购物车模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/cart/add` | 加入购物车 |
| PUT | `/cart/update` | 更新商品数量 |
| DELETE | `/cart/remove/{skuId}` | 移除商品 |
| GET | `/cart/list` | 购物车列表（实时计算价格+校验库存） |
| POST | `/cart/select` | 选中/取消选中商品 |
| POST | `/cart/clear` | 清空已失效商品 |

### 6.5 商品搜索模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/product/search?keyword={kw}&categoryId={id}&minPrice={min}&maxPrice={max}&sort={sort}&page={p}` | ES 全文搜索+筛选+排序 |
| GET | `/product/search/hot_keywords` | 热门搜索词 |
| GET | `/product/search/suggest?keyword={kw}` | 搜索建议（自动补全） |

### 6.6 订单模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/order/create` | 创建订单（支持购物车批量下单、优惠券、积分抵扣） |
| POST | `/order/preview` | 订单预览（试算优惠：券+积分+折扣） |
| POST | `/order/pay` | 发起支付（获取收银台URL） |
| POST | `/order/cashier` | 统一收银台（选择支付方式：微信/支付宝） |
| GET | `/order/detail?orderId={id}` | 订单详情（含物流信息） |
| GET | `/order/list?status={status}&page={page}` | 订单列表 |
| POST | `/order/cancel` | 取消未支付订单（回滚券+积分） |
| POST | `/order/confirm_receive` | 确认收货 |
| POST | `/order/del_flag` | 删除订单（逻辑删除） |
| POST | `/order/alipay_notify` | 支付宝支付异步回调 |
| POST | `/order/wxpay_notify` | 微信支付异步回调 |

### 6.7 优惠券模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/coupon/available` | 可领取优惠券列表 |
| POST | `/coupon/receive/{templateId}` | 领取优惠券 |
| GET | `/coupon/my` | 我的优惠券列表（按状态筛选） |
| POST | `/coupon/calculate` | 计算最优优惠券组合（购物车/下单前调用） |
| GET | `/coupon/count` | 各状态优惠券数量统计 |

### 6.8 售后模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/aftersale/apply` | 申请售后（仅退款/退货退款） |
| GET | `/aftersale/detail?afterSaleId={id}` | 售后详情 |
| GET | `/aftersale/list?status={status}` | 售后列表 |
| PUT | `/aftersale/ship_return` | 提交退货物流信息 |
| POST | `/aftersale/cancel` | 取消售后申请 |

### 6.9 物流模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/logistics/tracking?orderId={id}` | 查询物流轨迹 |

### 6.10 首页模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/home/banners` | 首页 Banner 列表 |
| GET | `/home/channels` | 首页频道入口列表 |
| GET | `/home/recommend` | 首页推荐商品（按策略：热销/新品/个性化） |
| GET | `/home/activity_tab` | 首页活动 Tab（拼团进行中/抽奖进行中） |

### 6.11 消息模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/message/list?type={type}&page={p}` | 消息列表 |
| GET | `/message/unread_count` | 未读消息数 |
| PUT | `/message/read/{messageId}` | 标记已读 |
| PUT | `/message/read_all/{type}` | 全部已读 |

### 6.12 抽奖模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/lottery/draw` | 执行抽奖 |
| POST | `/lottery/quantification_draw` | 量化人群抽奖 |
| GET | `/lottery/strategy/armory?strategyId={id}` | 策略装配(数据预热) |
| GET | `/lottery/activity/armory?activityId={id}` | 活动装配(数据预热) |
| POST | `/lottery/award_list` | 查询活动奖品列表 |
| POST | `/lottery/user_award_records` | 用户中奖记录查询 |
| POST | `/lottery/activity_account` | 查询用户活动账户(剩余次数) |

### 6.13 拼团模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/groupbuy/index` | 拼团首页配置（优惠试算、进行中队伍） |
| POST | `/groupbuy/lock_order` | 拼团锁单（开团/参团） |
| POST | `/groupbuy/settlement` | 拼团支付结算回调 |
| POST | `/groupbuy/refund` | 拼团退单 |
| GET | `/groupbuy/team_detail?teamId={id}` | 拼团详情（成员列表、进度） |
| GET | `/groupbuy/user_teams?userId={id}` | 用户参与拼团列表 |

### 6.14 积分模块

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/credit/account` | 查询积分账户 |
| POST | `/credit/exchange` | 积分兑换（抽奖次数/SKU商品） |
| POST | `/credit/records` | 积分流水查询 |

### 6.15 返利模块

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/rebate/calendar_sign` | 日历签到 |
| POST | `/rebate/is_sign_today` | 判断今日是否签到 |
| GET | `/rebate/sign_records` | 签到记录查询 |

### 6.16 运营模块（ERP）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/erp/activity/create` | 创建营销活动 |
| PUT | `/erp/activity/update` | 更新活动配置 |
| POST | `/erp/activity/submit_audit` | 提交审核 |
| POST | `/erp/activity/approve` | 审核通过 |
| POST | `/erp/activity/start` | 启动活动 |
| POST | `/erp/activity/close` | 关闭活动 |
| GET | `/erp/activity/list` | 活动列表查询 |
| PUT | `/erp/strategy/update` | 更新抽奖策略 |
| PUT | `/erp/discount/update` | 更新折扣配置 |
| POST | `/erp/coupon_template/create` | 创建优惠券模板 |
| PUT | `/erp/coupon_template/publish` | 发布优惠券模板 |
| POST | `/erp/banner/create` | 创建/编辑首页 Banner |
| PUT | `/erp/channel/update` | 更新首页频道配置 |
| POST | `/erp/product/create` | 创建/编辑商品 |
| PUT | `/erp/product/shelve/{productId}` | 商品上架/下架 |
| POST | `/erp/order/ship` | 订单发货（填写物流单号） |
| GET | `/erp/aftersale/list?status={s}` | 售后单列表 |
| POST | `/erp/aftersale/audit` | 审核售后申请 |
| POST | `/erp/aftersale/confirm_receive` | 确认收到退货 |
| GET | `/erp/data/activity_statistics` | 活动数据统计 |
| GET | `/erp/data/sales_dashboard` | 销售数据看板 |

---

## 7. 核心业务流程

### 7.1 用户微信扫码登录

```
用户端 (浏览器)                        服务端                          微信服务器
     │                                   │                               │
     ├─ GET /user/weixin_qrcode_ticket ──►│                               │
     │                                   ├─ 获取 access_token ───────────►│
     │                                   │◄── access_token ────────────────┤
     │                                   ├─ 生成二维码 ticket ────────────►│
     │                                   │◄── ticket ──────────────────────┤
     │◄── { ticket } ────────────────────┤                               │
     │                                   │                               │
     ├─ 展示微信二维码（用 ticket）        │                               │
     │                                   │                               │
     │   [用户打开微信扫码]                │                               │
     │                                   │   ◄── 扫码事件回调 ─────────────┤
     │                                   ├─ 解析 openId                   │
     │                                   ├─ 缓存 ticket → openId (Redis)   │
     │                                   │                               │
     ├─ 轮询 GET /user/check_login ─────►│                               │
     │   ?ticket={ticket}               ├─ 查 Redis 获取 openId            │
     │                                   ├─ 查 DB 或创建用户               │
     │                                   ├─ 生成 JWT Token                │
     │◄── { token, userInfo } ──────────┤                               │
```

**关键细节**：
- 使用 Redis 缓存 ticket → openId 映射关系，有效期 5 分钟
- 前端每 2 秒轮询一次 `check_login`，最多轮询 150 次（5 分钟）
- 轮询接口需要限流（同一 ticket 每 2 秒允许 1 次）

### 7.2 普通商品下单 + 支付

```
用户端                    UniMarket                          支付渠道(支付宝)
  │                         │                                    │
  ├─ POST /order/create ──►│                                    │
  │  {skuId, quantity,     ├─ 查询商品/SKU（校验价格/库存）        │
  │   addressId}           ├─ 计算订单金额                        │
  │                        ├─ 创建订单（status=CREATE）            │
  │                        ├─ INSERT order + order_item          │
  │◄── { orderId } ────────┤                                    │
  │                         │                                    │
  ├─ POST /order/pay ─────►│                                    │
  │  {orderId, payChannel} ├─ 幂等校验（已有支付单直接返回）        │
  │                        ├─ 创建支付单                          │
  │                        ├─ 调用支付渠道创建支付 ───────────────►│
  │                        │◄── 支付URL/表单 ─────────────────────┤
  │                        ├─ 更新订单 status=PAY_WAIT            │
  │◄── { payUrl } ─────────┤                                    │
  │                         │                                    │
  ├─ 跳转支付页面            │                                    │
  │  [用户完成支付]          │                                    │
  │                         │◄── 异步支付回调 ────────────────────┤
  │                         ├─ RSA256 签名验签                     │
  │                         ├─ 校验 trade_status=TRADE_SUCCESS    │
  │                         ├─ 更新订单 status=PAY_SUCCESS        │
  │                         ├─ 发送 MQ 消息(订单支付成功事件)        │
  │                         ├─ 返回 SUCCESS 给支付宝               │
  │                         │                                    │
  │◄── 跳转支付成功页        │                                    │
```

**掉单补偿机制**：

```
定时任务（每 1 分钟）：
  查询 pay_order 中 status=CREATE 且 create_time > 5分钟前的记录
  调用支付渠道 queryPayStatus(outTradeNo)
  如果有已支付但未回调的 → 补执行回调逻辑
```

**超时关单**：

```
定时任务（每 5 分钟）：
  查询 order 中 status=PAY_WAIT 且 create_time > 30分钟前的记录
  更新 status=CLOSE
  释放锁定的库存
  退券（user_coupon 状态 USED → REFUNDED）
  退积分（credit_account + amount）
```

### 7.2B 购物车 → 下单全链路

```
用户端                           UniMarket
  │                                 │
  ├─ POST /cart/add ───────────────►│
  │  {skuId, quantity=1}           ├─ 查询 SKU 信息 + 当前库存
  │                                ├─ 写入 Redis(key=cart:userId)
  │                                ├─ 返回当前购物车总览
  │◄── {cartItems, totalAmount} ───┤
  │                                 │
  │   [用户进入购物车页]              │
  ├─ GET /cart/list ───────────────►│
  │                                ├─ 读 Redis 购物车数据
  │                                ├─ 实时查每个 SKU 最新价格 + 库存
  │                                ├─ 标记失效商品(下架/售罄/价格变动)
  │                                ├─ 失效数量 > 0 → 前端提示
  │◄── {items, warnings[]} ────────┤
  │                                 │
  │   [用户选中商品，点击结算]         │
  ├─ POST /coupon/calculate ───────►│
  │  {skuIds, amounts}             ├─ 查用户可用券
  │                                ├─ 计算最优券组合
  │                                │  (满减券: 选满足条件的最大面额)
  │                                │  (折扣券: 选折扣最低+上限最高的)
  │◄── {bestCoupon, discount} ─────┤
  │                                 │
  ├─ POST /order/preview ──────────►│
  │  {skuIds, couponId, useCredit} ├─ 计算：商品金额 - 优惠券 - 积分抵扣
  │                                ├─ 校验库存/价格是否变化
  │◄── {orderPreview} ─────────────┤
  │                                 │
  ├─ POST /order/create ───────────►│
  │  {skuIds, addressId,           ├─ 锁定优惠券(coupon status: LOCKED)
  │   couponId, creditAmount}      ├─ 冻结积分(credit freeze + amount)
  │                                ├─ 扣减库存(Redis incr)
  │                                ├─ 创建订单 @Transactional
  │                                ├─ 清购物车已下单商品
  │◄── {orderId} ─────────────────┤
```

**关键细节**：
- 购物车在 Redis 中以 Hash 结构存储：`cart:{userId}` → `{skuId} → {quantity,selected,addTime}`
- 未登录加购存在 `cart:device:{deviceId}`，登录后调用 `mergeCart()` 合并
- 下单前必须做"价格快照校验"：对比下单时价格与加购时价格，浮动超过阈值的提示用户

### 7.2C 优惠券全链路

```
┌──────────────────────────────────────────────────────────────┐
│                      优惠券生命周期                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  运营创建券模板                                                │
│    │                                                         │
│    ▼                                                         │
│  ┌─────────────────┐                                         │
│  │  券模板(DRAFT)   │  →  发布(PUBLISHED)                     │
│  └────────┬────────┘                                         │
│           │                                                  │
│    用户获取渠道:                                               │
│    ┌──────┼──────┬──────────┐                                │
│    ▼      ▼      ▼          ▼                                │
│  手动领取 签到赠送 活动发放  新人自动发                          │
│    │      │      │          │                                │
│    └──────┴──────┴──────────┘                                │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐                                         │
│  │  user_coupon     │  status=UNUSED, expireTime=now+N天     │
│  └────────┬────────┘                                         │
│           │                                                  │
│    用户下单时选择使用:                                          │
│           │                                                  │
│           ▼                                                  │
│  ┌─────────────────┐                                         │
│  │  锁定(LOCKED)    │  @Transactional with order creation    │
│  └────────┬────────┘                                         │
│           │                                                  │
│    支付成功/失败:                                               │
│    ┌──────┴──────┐                                           │
│    ▼             ▼                                           │
│  支付成功       支付失败/超时取消                                │
│  status=USED   status→UNUSED(退回)                            │
│    │                                                         │
│    订单退款时:                                                 │
│    ▼                                                         │
│  status→REFUNDED(退回,可再次使用)                               │
│                                                              │
│  过期处理(定时任务):                                           │
│  ┌────────────────┐                                          │
│  │ expireTime<now  │  →  status=EXPIRED                      │
│  │ status=UNUSED   │                                         │
│  └────────────────┘                                          │
└──────────────────────────────────────────────────────────────┘
```

### 7.2D 售后流程

```
用户申请售后                          商家                          系统
    │                                  │                             │
    ├─ POST /aftersale/apply ─────────►│                             │
    │  {orderId, type, reason}        │                             │
    │                                  │                             │
    │  [仅退款]                         │                             │
    │  等待商家审核 ◄───────────────────┤                             │
    │                                  │                             │
    │              ┌─── 商家审核 ──────►│                             │
    │              │                   ├─ 通过 → status=AUDIT_PASS    │
    │              │                   │  → 自动退款(原路退回)         │
    │              │                   │  → 退券(券退回)              │
    │              │                   │  → 退积分(积分退回)           │
    │              │                   │  → status=COMPLETED         │
    │              │                   │                             │
    │              │                   ├─ 拒绝 → status=AUDIT_REJECT  │
    │              │                   │  → 原因通知用户              │
    │              │                   │                             │
    │  [退货退款]   │                   │                             │
    │  审核通过后    │                   │                             │
    │  用户寄回商品  ├─ PUT /aftersale/   │                             │
    │  填写物流单号  │   ship_return ────►│                             │
    │              │                   ├─ status=SHIPPED             │
    │              │                   │                             │
    │              │  商家收到退货 ◄────┤                             │
    │              │  POST confirm ───►│                             │
    │              │                   ├─ status=RECEIVED            │
    │              │                   ├─ 执行退款+退券+退积分          │
    │              │                   ├─ status=COMPLETED           │
    │◄─── 退款到账通知 ─────────────────┤                             │

售后单状态机：
PENDING_AUDIT → AUDIT_PASS → [PENDING_SHIP → SHIPPED → RECEIVED] → REFUNDED → COMPLETED
                     ↓
               AUDIT_REJECT
```

### 7.2E 物流追踪流程

```
商家发货                              UniMarket                       快递鸟/菜鸟
  │                                      │                                │
  ├─ POST /erp/order/ship ──────────────►│                                │
  │  {orderId, trackingNo, companyCode}  ├─ INSERT logistics              │
  │                                      ├─ 订阅物流轨迹 ────────────────►│
  │                                      ├─ 更新 order status=SHIPPED     │
  │                                      │                                │
  │                                      │◄── 轨迹更新回调 ────────────────┤
  │                                      ├─ 更新 logistics.traces JSON    │
  │                                      │                                │
  │                                      │   [如果签收]                    │
  │                                      │◄── 签收回调 ────────────────────┤
  │                                      ├─ logistics status=SIGNED       │
  │                                      ├─ order status=DEAL_DONE        │
  │                                      ├─ 自动确认收货                  │
  │                                      │                                │
  │                                      │   [定时轮询补偿]                │
  │                                      │   XXL-Job: 每2小时             │
  │                                      │   查 logistics status!=SIGNED  │
  │                                      │   调第三方查询接口 ────────────►│
  │                                      │   补同步轨迹数据                │
```

### 7.3 拼团全链路

```
┌──────────────────────────────────────────────────────────────┐
│                        拼团流程全景                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  用户A                      服务端                        用户B│
│    │                          │                             │  │
│    ├─ POST /groupbuy/index ──►│                             │  │
│    │  ←优惠试算+活动信息       │                             │  │
│    │                          │                             │  │
│    ├─ POST lock_order ───────►│                             │  │
│    │  {activityId, teamId=空} ├─ 责任链校验                   │  │
│    │                          │  [1]活动可用性                │  │
│    │                          │  [2]用户次数限制              │  │
│    │                          │  [3]Redis库存扣减             │  │
│    │                          ├─ CREAT新团队(teamId)         │  │
│    │                          ├─ INSERT group_buy_order      │  │
│    │  ← { teamId, orderId }   │                             │  │
│    │                          │                             │  │
│    ├─ 发起支付 ───────────────►│  status: 锁单                   │  │
│    │                          │                             │  │
│    │   [用户B浏览拼团首页]      │                             │  │
│    │                          │   POST /groupbuy/index ◄────┤  │
│    │                          │   ← 看到队伍进度 ◄───────────┤  │
│    │                          │                             │  │
│    │                          │   POST lock_order ◄─────────┤  │
│    │                          │   {activityId, teamId=xxx}  │  │
│    │                          ├─ 加入已有团队                 │  │
│    │                          │  updateAddLockCount          │  │
│    │                          │  ← { teamId, orderId }      │  │
│    │                          │                             │  │
│    │                          │   发起支付 ◄─────────────────┤  │
│    │                          │                             │  │
│    │                          │   POST settlement ◄─────────┤  │
│    │                          │   (支付回调)                  │  │
│    │                          ├─ 责任链校验                   │  │
│    │                          │  [1]渠道校验                  │  │
│    │                          │  [2]外部单号校验              │  │
│    │                          │  [3]交易时间校验              │  │
│    │                          ├─ completeCount++             │  │
│    │                          ├─ 判断: 是否达成目标人数?       │  │
│    │                          │   ├─ 否→ 等待更多用户         │  │
│    │                          │   └─ 是→                     │  │
│    │                          │     ├─ team status=已成团    │  │
│    │                          │     ├─ 写 notify_task        │  │
│    │                          │     ├─ 发 MQ 成团事件         │  │
│    │                          │     └─ 异步 HTTP 回调通知     │  │
│    │                          │                             │  │
│    │                          │   超时未成团                  │  │
│    │                          │   (定时任务每1分钟扫描)        │  │
│    │                          │   ├─ 查询过期拼团             │  │
│    │                          │   ├─ 更新 team status=失败   │  │
│    │                          │   └─ 逐个退单(恢复库存)       │  │
```

**并发成团的临界处理**：
```
场景：3 人团，已有 2 人支付，A 和 B 几乎同时完成支付

方案：使用 Redis 分布式锁串行化结算操作
  lockKey = "team_settlement:{teamId}"
  
  tryLock → 执行业务(completeCount+1 → 判断成团) → unlock
  
  确保同一时刻只有一个线程在做"是否成团"的判断
```

### 7.4 抽奖全链路

```
┌──────────────────────────────────────────────────────────────┐
│                        抽奖流程全景                            │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  用户请求抽奖 POST /lottery/draw                               │
│    │                                                         │
│    ▼                                                         │
│  ┌─────────────────────┐                                     │
│  │ 1. 风控检查          │  RateLimiter + 黑名单 + 熔断降级     │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 2. 活动参与(Partake) │                                     │
│  │  ·校验活动状态/时间   │                                     │
│  │  ·校验用户参与次数    │  (总次数/日次数/月次数)              │
│  │  ·Redis INCR 扣库存  │  + 分段分布式锁                     │
│  │  ·写参与记录(分库分表) │                                     │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 3. 规则责任链过滤     │                                     │
│  │  BlackListChain      │  → 黑名单用户? → 只能得兜底积分      │
│  │  RuleWeightChain     │  → 有用户专属权重? → 按权重分配概率  │
│  │  DefaultChain        │  → 默认概率抽奖                     │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 4. 决策树引擎        │                                     │
│  │  RuleLockNode        │  → 抽奖N次后才解锁某奖品            │
│  │  RuleStockNode       │  → 扣减奖品库存(DB行锁)             │
│  │  RuleLuckAwardNode   │  → 库存不足时的兜底奖品              │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 5. 执行抽奖算法       │                                     │
│  │  排除已抽空奖品列表    │                                     │
│  │  单项概率: O(1)散列   │                                     │
│  │  总体概率: 动态重算   │                                     │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 6. 结果落库 + 发奖   │                                     │
│  │  ·写中奖记录(分库分表)│                                     │
│  │  ·发送MQ发奖消息     │                                     │
│  │  ·更新MQ状态         │                                     │
│  └─────────┬───────────┘                                     │
│            ▼                                                 │
│  ┌─────────────────────┐                                     │
│  │ 7. 异步发奖(消费者)   │                                     │
│  │  根据awardType分发:   │                                     │
│  │  文字描述 → 记录     │                                     │
│  │  兑换码   → 发码     │                                     │
│  │  优惠券   → 发券     │                                     │
│  │  实物     → 创建发货单│                                     │
│  │  积分     → 调整积分  │                                     │
│  └─────────────────────┘                                     │
│                                                              │
│  MQ 发奖失败补偿:                                             │
│  XXL-Job 定时扫描 mq_state=2 的记录 → 重新发送MQ              │
└──────────────────────────────────────────────────────────────┘
```

### 7.5 积分体系全链路

```
┌──────────────────────────────────────────────────────────────┐
│                        积分体系                                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  积分获取方式:                                                │
│  ┌──────────┬──────────────┬──────────┐                      │
│  │  每日签到  │   购物返利   │  活动赠送 │                      │
│  │  +10积分  │  消费%返积分  │  不定额   │                      │
│  └──────────┴──────┬───────┴──────────┘                      │
│                    │                                         │
│                    ▼                                         │
│  ┌─────────────────────────────────────┐                    │
│  │         积分调整原子操作              │                    │
│  │  UPDATE user_credit_account         │                    │
│  │  SET available_amount =             │                    │
│  │      available_amount + #{amount},  │                    │
│  │      total_amount =                 │                    │
│  │      total_amount + #{amount}       │                    │
│  │  WHERE user_id = #{userId}          │                    │
│  │  AND available_amount + #{amount} >= 0  ← 防止透支        │
│  └─────────────────────────────────────┘                    │
│                                                              │
│  积分消费方式:                                                │
│  ┌──────────┬──────────────┐                                 │
│  │ 兑换抽奖次数│  兑换SKU商品  │                                 │
│  │ 积分→活动次数│ 积分→商品    │                                 │
│  └──────────┴──────────────┘                                 │
│                                                              │
│  积分兑换SKU流程（事务+MQ最终一致）:                             │
│                                                              │
│  1. 创建待支付订单 (UnpaidOrder)                               │
│  2. INSERT credit_order (REVERSE, -amount)                   │
│  3. UPDATE credit_account (扣减积分)                           │
│  4. INSERT behavior_rebate_order (返利订单)                    │
│  5. 发送MQ: credit_adjust_success                            │
│  6. MQ消费者: 更新订单状态→发货完成                             │
│  7. MQ发送失败→XXL-Job补偿                                    │
└──────────────────────────────────────────────────────────────┘
```

### 7.6 活动生命周期状态机

```
     ┌──────┐   提审    ┌──────────┐   通过    ┌──────┐   启动    ┌───────┐
     │ EDIT │ ────────►│ARRAIGNMENT│ ────────►│ PASS │────────►│ DOING │
     └──────┘          └────┬─────┘           └──┬───┘         └──┬────┘
          ▲                 │ 撤审               │ 拒绝           │
          │                 ▼                    ▼               │ 到期
          │            ┌──────┐              ┌──────┐            ▼
          │◄───────────│ EDIT │              │REFUSE│        ┌───────┐
          │            └──────┘              └──────┘        │ CLOSE │
          │                                                   └───┬───┘
          │                                                       │ 重新开启
          │                                                       ▼
          │                                                   ┌──────┐
          └───────────────────────────────────────────────────│ OPEN │
                                                              └──────┘

定时任务扫描：
  - activity status=PASS → 到达 beginTime → 更新为 DOING
  - activity status=DOING → 超过 endTime → 更新为 CLOSE
```

---

## 8. 关键技术实现

### 8.1 抽奖算法

#### 单项概率算法（斐波那契散列 O(1)）

```java
/**
 * 单项概率：每种奖品概率独立，抽完不影响其他奖品概率
 * 使用斐波那契散列将概率值映射到固定长度数组，O(1)定位
 */
public class SingleRateRandomDrawAlgorithm extends AbstractDrawAlgorithm {

    // 概率散列表，长度 128
    private String[] rateTuple = new String[128];

    // 散列索引缓存
    private List<String> rateTupleList;

    @Override
    public void initRateTuple(Long strategyId, List<AwardRateVO> awardRateVOList) {
        // 1. 根据概率比例填充 128 长度的数组
        //    例如：奖品A概率20% → 占据前26个位置(128*0.2)
        //          奖品B概率30% → 占据接下来38个位置(128*0.3)
        //          ...以此类推
        int cursorVal = 0;
        for (AwardRateVO awardRateVO : awardRateVOList) {
            int rateVal = awardRateVO.getAwardRate()
                .multiply(new BigDecimal(128)).intValue();
            for (int i = cursorVal + 1; i <= (rateVal + cursorVal); i++) {
                rateTuple[hashIdx(i)] = awardRateVO.getAwardId();
            }
            cursorVal += rateVal;
        }
    }

    @Override
    public String randomDraw(Long strategyId, List<String> excludeAwardIds) {
        // 2. 生成随机值 1-100
        int randVal = new SecureRandom().nextInt(100) + 1;

        // 3. 斐波那契散列 O(1)查找
        //    0x61c88647 是黄金分割比的无符号整型表示
        //    hash = (val × 0x61c88647) & (len-1)  →  均匀散列到数组索引
        int idx = (int) ((randVal * 0x61c88647L + 0x61c88647L) & (rateTuple.length - 1));

        String awardId = rateTuple[idx];

        // 4. 检查是否在排除列表
        if (excludeAwardIds.contains(awardId)) {
            return null; // 未中奖
        }
        return awardId;
    }
}
```

#### 总体概率算法（动态重算分母）

```java
/**
 * 总体概率：有库存的奖品之间按原比例均分中奖概率
 * 抽完某奖品后，概率在其他奖品间等比放大，保证必中奖
 */
public class EntiretyRateRandomDrawAlgorithm extends AbstractDrawAlgorithm {

    @Override
    public String randomDraw(Long strategyId, List<String> excludeAwardIds) {
        // 1. 获取所有奖品配置
        List<AwardRateVO> awardList = awardRateVOListByStrategyId.get(strategyId);

        // 2. 过滤掉已抽空的奖品
        List<AwardRateVO> availableAwards = awardList.stream()
            .filter(a -> !excludeAwardIds.contains(a.getAwardId()))
            .collect(Collectors.toList());

        // 3. 计算可用奖品的概率之和作为新分母
        BigDecimal denominator = availableAwards.stream()
            .map(AwardRateVO::getAwardRate)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 4. 按比例放大：awardRate / denominator * 100 → 区间概率
        int randVal = new SecureRandom().nextInt(100) + 1;
        int cursor = 0;
        for (AwardRateVO award : availableAwards) {
            int rateVal = award.getAwardRate()
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100)).intValue();
            if (randVal <= cursor + rateVal) {
                return award.getAwardId();
            }
            cursor += rateVal;
        }

        return availableAwards.get(0).getAwardId(); // 兜底第一个
    }
}
```

### 8.2 规则引擎（决策树 + 责任链）

#### 架构设计

```
                         抽奖请求进入
                              │
                              ▼
              ┌───────────────────────────────┐
              │      责任链（规则过滤）          │
              │                               │
              │  BlackListChain               │
              │  → userId在黑名单? → 返回积分   │
              │        ↓ (不在黑名单)          │
              │  RuleWeightChain              │
              │  → 用户有专属权重? → 切换权重表  │
              │        ↓ (无专属权重)          │
              │  DefaultChain                 │
              │  → 进入决策树引擎              │
              └───────────────┬───────────────┘
                              │
                              ▼
              ┌───────────────────────────────┐
              │       决策树引擎               │
              │                               │
              │  ┌─────────────────────────┐  │
              │  │  Root (子叶)             │  │
              │  │  ruleKey: rule_lock      │  │
              │  │  → 用户已抽奖次数?        │  │
              │  └───────┬───────┬─────────┘  │
              │      <3次│   ≥3次│             │
              │          ▼       ▼             │
              │  ┌─────────┐ ┌──────────────┐ │
              │  │ LockA   │ │ RuleStock    │ │
              │  │ (果实)  │ │ (子叶)        │ │
              │  │ → 奖品A │ │ → 库存检查    │ │
              │  └─────────┘ └──┬──────┬────┘ │
              │            有库存│  无库存│     │
              │                 ▼       ▼     │
              │         ┌──────────┐ ┌──────┐│
              │         │ PayPrize│ │Fallbk││
              │         │ (果实)   │ │(果实) ││
              │         │ → 中奖!  │ │→兜底 ││
              │         └──────────┘ └──────┘│
              └───────────────────────────────┘
```

### 8.3 库存扣减的并发安全

#### 三层保障模型

```
   Level 1: Redis 原子扣减（快速失败）
   ┌─────────────────────────────────────────────┐
   │  Long stock = redis.incr(                    │
   │    "activity:stock:" + activityId           │
   │  );                                          │
   │  if (stock > totalStock) {                   │
   │    redis.decr(...); // 回滚                  │
   │    throw NoStockException;                   │
   │  }                                           │
   │  // 设置活动过期时的分布式锁自动释放            │
   │  redis.setNx(                                │
   │    "stock:lock:" + activityId + ":" + stock, │
   │    "1",                                      │
   │    ttl = endTime - now()                     │
   │  );                                          │
   └─────────────────────────────────────────────┘
                    │
                    ▼
   Level 2: DB 行锁（精确扣减）
   ┌─────────────────────────────────────────────┐
   │  UPDATE strategy_award                      │
   │  SET award_surplus_count =                  │
   │      award_surplus_count - 1                │
   │  WHERE strategy_id = #{strategyId}           │
   │    AND award_id = #{awardId}                │
   │    AND award_surplus_count > 0              │
   │  → affectedRows=0 表示库存已空               │
   └─────────────────────────────────────────────┘
                    │
                    ▼
   Level 3: 定时补偿（最终一致）
   ┌─────────────────────────────────────────────┐
   │  XXL-Job 每 5 分钟扫描                       │
   │  对比 Redis 库存 与 DB 库存                   │
   │  不一致 → 以 DB 为准修正 Redis               │
   └─────────────────────────────────────────────┘
```

### 8.4 分库分表路由

```java
/**
 * 分库分表路由策略
 * 2库 × 4表 = 8分片，按 userId 哈希路由
 */
public class DBRouterStrategy {

    private final int dbCount = 2;
    private final int tbCount = 4;

    public void doRouter(String userId) {
        // 1. 计算哈希值（扰动函数减少碰撞）
        int hash = hash(userId);

        // 2. 确定库索引
        int dbIdx = hash % dbCount; // 0 → db01, 1 → db02
        DBContextHolder.setDBKey(String.format("db%02d", dbIdx + 1));

        // 3. 确定表索引
        int tbIdx = (hash / dbCount) % tbCount;
        DBContextHolder.setTBKey(String.format("_%03d", tbIdx));
    }

    private int hash(String key) {
        // 使用 HashMap 的扰动函数
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    public void clear() {
        DBContextHolder.clear();
    }
}
```

### 8.5 风控体系

```
           用户请求
              │
              ▼
  ┌───────────────────────┐
  │ L1: 限流               │
  │ Guava RateLimiter      │
  │ 每用户 1 req/s          │
  │ 超限 → 429 Too Many    │
  └───────────┬───────────┘
              │ 通过
              ▼
  ┌───────────────────────┐
  │ L2: 黑名单             │
  │ Redis Set 查询         │
  │ 在黑名单 → 拒绝/兜底    │
  │ 自动拉黑：24h 超5次限流 │
  └───────────┬───────────┘
              │ 通过
              ▼
  ┌───────────────────────┐
  │ L3: 熔断               │
  │ Sentinel/Hzstrix       │
  │ 超时150ms → 熔断       │
  │ 10s后半开尝试           │
  └───────────┬───────────┘
              │ 通过
              ▼
  ┌───────────────────────┐
  │ L4: DCC降级开关        │
  │ Zookeeper/Redis广播    │
  │ degradeSwitch=true     │
  │ → 返回兜底结果          │
  └───────────┬───────────┘
              │ 通过
              ▼
         正常业务处理
```

### 8.6 最终一致性方案

```
┌────────────────────────────────────────────────────────────────┐
│                    三层最终一致性保障                            │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  Layer 1: 本地消息表                                           │
│  ┌──────────────────────────────────────┐                     │
│  │  业务操作 + INSERT notify_task       │                     │
│  │  在同一个 @Transactional 中完成       │                     │
│  │  保证本地消息一定会写入               │                     │
│  └─────────────────┬────────────────────┘                     │
│                    │                                           │
│  Layer 2: MQ 异步投递                                          │
│  ┌─────────────────▼────────────────────┐                     │
│  │  业务事务提交后                       │                     │
│  │  发送 MQ(topic: order.paid 等)       │                     │
│  │  消费者处理下游业务                   │                     │
│  │  处理成功 → UPDATE notify_task SUCCESS│                     │
│  └─────────────────┬────────────────────┘                     │
│                    │ (MQ发送失败/消费失败)                      │
│  Layer 3: 定时补偿                                             │
│  ┌─────────────────▼────────────────────┐                     │
│  │  XXL-Job 每 1 分钟扫描                │                     │
│  │  notify_task.status = PENDING/RETRY   │                     │
│  │  重试(最多3次) → 仍失败 → 标记FAIL     │                     │
│  │  告警通知运营人工介入                  │                     │
│  └──────────────────────────────────────┘                     │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 9. 部署架构

### 9.1 Docker 基础设施

```yaml
# docker-compose-infrastructure.yml
services:
  mysql:
    image: mysql:8.0.32
    ports: ["3306:3306"]
    volumes: ["./data/mysql:/var/lib/mysql", "./sql:/docker-entrypoint-initdb.d"]
    environment:
      MYSQL_ROOT_PASSWORD: root123

  redis:
    image: redis:6.2
    ports: ["6379:6379"]
    command: redis-server --requirepass redis123

  rabbitmq:
    image: rabbitmq:3.12.9-management
    ports: ["5672:5672", "15672:15672"]

  nacos:
    image: nacos/nacos-server:v2.2.3
    ports: ["8848:8848"]
    environment:
      MODE: standalone

  elasticsearch:
    image: elasticsearch:7.17.14
    ports: ["9200:9200"]
    environment:
      discovery.type: single-node

  canal:
    image: canal/canal-server:v1.1.7
    ports: ["11111:11111"]

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.1
    ports: ["9090:9090"]

  prometheus:
    image: prom/prometheus:v2.47.2
    ports: ["9090:9090"]

  grafana:
    image: grafana/grafana:10.2.0
    ports: ["3000:3000"]

  kibana:
    image: kibana:7.17.14
    ports: ["5601:5601"]
```

### 9.2 应用部署

```
┌────────────────────────────────────────────┐
│              Nginx (80/443)                 │
│          前端SPA + API反向代理               │
└───────────────┬────────────────────────────┘
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
┌────────┐ ┌────────┐ ┌────────┐
│ App-1  │ │ App-2  │ │ App-N  │  Spring Boot 多实例
│ :8091  │ │ :8092  │ │ :809N  │  Nacos 注册, Dubbo RPC
└───┬────┘ └───┬────┘ └───┬────┘
    │          │          │
    └──────────┼──────────┘
               │
    ┌──────────┼──────────┐
    ▼          ▼          ▼
┌────────┐ ┌──────┐ ┌─────────┐
│ MySQL  │ │Redis │ │RabbitMQ │
│ (主从) │ │(哨兵)│ │(集群)   │
└────────┘ └──────┘ └─────────┘
```

---

## 10. 实施路线图

### 总览：12 阶段，约 16 周

```
Phase  1 ████ 基础骨架（登录→下单→支付）                         Week 1-2
Phase  2 ████ 商品搜索 + 购物车                                   Week 3
Phase  3 ████ 营销引擎 — 抽奖                                     Week 4-5
Phase  4 ████ 营销引擎 — 拼团                                     Week 6-7
Phase  5 ████ 积分体系                                             Week 8
Phase  6 ████ 优惠券系统                                           Week 9
Phase  7 ████ 订单履约（发货 + 物流 + 确认收货）                     Week 10
Phase  8 ████ 售后体系                                             Week 11
Phase  9 ████ 首页运营 + 消息中心                                   Week 12
Phase 10 ████ 风控与监控                                           Week 13-14
Phase 11 ████ 运营后台（ERP）                                      Week 15
Phase 12 ███  全链路压测 + 文档 + 收尾                              Week 16
```

### Phase 1：基础骨架（Week 1-2）

**目标**：搭建项目骨架，跑通最简链路

| 任务 | 内容 | 产出 |
|------|------|------|
| 项目初始化 | Maven 多模块骨架、Spring Boot 配置 | 可启动的空项目 |
| 用户模块 | 微信扫码登录 | 登录 → 获取 token → 后续请求鉴权 |
| 商品模块 | 商品 CRUD + 列表查询 + 分类管理 | 数据库建表 + API |
| 订单模块 | 最简下单流程（CREATE → PAY_WAIT → PAY_SUCCESS） | 单表操作，不涉及营销 |
| 支付模块 | 支付宝沙箱 + 微信支付适配 | 支付渠道抽象接口 + 统一收银台 |

**里程碑**：登录 → 看商品 → 下单 → 选择支付方式 → 支付 → 订单完成

### Phase 2：商品搜索 + 购物车（Week 3）

| 任务 | 内容 |
|------|------|
| ES 商品索引 | Canal 同步 MySQL → ES，建立商品索引 |
| 全文搜索 API | `/product/search` 支持关键词+分类+价格区间+排序 |
| 搜索建议 | 自动补全 + 热门搜索词 |
| 购物车 Redis 实现 | cart:userId Hash → add/update/remove/select |
| 购物车合并 | deviceId cart → userId cart 登录合并 |
| 购物车校验 | 进入购物车页实时校验库存/价格/状态 |

**里程碑**：用户搜索商品 → 加入购物车 → 勾选结算

### Phase 3：营销引擎 — 抽奖（Week 4-5）

| 任务 | 内容 |
|------|------|
| 抽奖策略配置 | strategy / strategy_award / award 表 + CRUD |
| 抽奖算法 | 实现单项概率 + 总体概率两种算法 |
| 活动管理 | marketing_activity 表 + 状态机 |
| 活动参与 | 活动账户扣减 + Redis 库存管理 |
| 规则引擎 | 决策树实现（黑名单/权重/锁次数/兜底） |
| 奖品发放 | 发奖工厂 + MQ 异步 |
| MQ 补偿 | 本地消息表 + XXL-Job 定时补偿 |

### Phase 4：营销引擎 — 拼团（Week 6-7）

| 任务 | 内容 |
|------|------|
| 折扣引擎 | 直减/满减/折扣/N 元购 4 种策略 |
| 拼团锁单 | 责任链校验 + Redis 库存控制 |
| 拼团结算 | 支付回调 → 进度更新 → 成团判断 |
| 拼团退款 | 三种退款策略 + 超时自动退款 |
| 回调通知 | HTTP/MQ 两种通知方式 + 定时补偿 |
| 拼团首页 | 优惠试算 + 进行中队伍展示 |

### Phase 5：积分体系（Week 8）

| 任务 | 内容 |
|------|------|
| 积分账户 | 积分账户表的创建和维护 |
| 积分流水 | 正向/逆向交易记录 |
| 行为返利 | 签到得积分、购物返积分 |
| 积分兑换 | 积分兑换抽奖次数、积分兑换 SKU 商品 |
| 积分在订单中的使用 | 下单时积分抵扣（与优惠券互斥/叠加规则） |

### Phase 6：优惠券系统（Week 9）

| 任务 | 内容 |
|------|------|
| 券模板管理 | 满减券/折扣券/运费券/商品券 CRUD |
| 发券渠道 | 手动领取 + 活动发放 + 签到赠送 + 新人自动发 |
| 领券/用券 | 领券（校验限领/库存）→ 锁券（下单）→ 核销（支付成功） |
| 退券逻辑 | 订单取消/退款时券的退回策略 |
| 最优券计算 | `calculateBestCoupon()` 遍历组合求最大优惠 |
| 过期处理 | XXL-Job 定时扫描 → 标记 EXPIRED |
| 券与积分互斥规则 | 下单计算优先级：拼团折扣 > 优惠券 > 积分抵扣 |

### Phase 7：订单履约（Week 10）

| 任务 | 内容 |
|------|------|
| 发货管理 | ERP 填写物流单号 → 订单状态 SHIPPED |
| 物流查询 | 对接快递鸟 API → 订阅轨迹 → 回调更新 |
| 物流补偿 | 定时轮询未签收订单的最新物流状态 |
| 自动确认收货 | 签收后 N 天自动确认（如 7 天） |
| 订单完成 | DEAL_DONE → 可评价/可申请售后 |
| 订单评价 | 评分 + 文字评价（可选，简易版） |

### Phase 8：售后体系（Week 11）

| 任务 | 内容 |
|------|------|
| 仅退款 | 申请 → 审核 → 退款(原路退回) + 退券 + 退积分 |
| 退货退款 | 申请 → 审核 → 用户寄回 → 确认收货 → 退款 + 退券 + 退积分 |
| 售后状态机 | 完整状态流转 + 事件时间线 |
| 拒绝售后 | 审核拒绝 → 原因通知 |

### Phase 9：首页运营 + 消息中心（Week 12）

| 任务 | 内容 |
|------|------|
| 首页 Banner | 轮播图 CRUD + 跳转链接配置 |
| 频道入口 | 图标 + 名称 + 排序 + 链接配置 |
| 推荐商品 | 热销/新品/活动商品接口（规则可配置） |
| 站内消息 | 消息列表 + 未读数 + 已读标记 |
| 消息触发点 | 支付成功/发货/成团/退款/优惠券到期提醒 → 自动发消息 |

### Phase 10：风控与监控（Week 13-14）

| 任务 | 内容 |
|------|------|
| 限流 | Guava RateLimiter + Sentinel |
| 黑名单 | 自动拉黑 + 手动黑名单管理 |
| 熔断降级 | Sentinel 熔断 + DCC 动态开关 |
| 分库分表 | 按 userId 的 2×4 分片路由 |
| 读写分离 | Canal 同步到 ES |
| 监控告警 | Prometheus + Grafana + 告警规则 |
| 压测 | JMH 核心链路压测 |

### Phase 11：运营后台 ERP（Week 15）

| 任务 | 内容 |
|------|------|
| 活动管理 CRUD | 创建/编辑/提审/发布活动 |
| 策略配置 | 可视化配置奖品概率和规则树 |
| 优惠券管理 | 券模板创建/发布/数据统计 |
| 商品管理 | 商品/SKU CRUD + 上下架 |
| 订单管理 | 订单列表/详情/发货操作 |
| 售后处理 | 售后审核 + 退货收货确认 |
| 首页配置 | Banner/频道编辑 |
| 数据看板 | GMV/订单量/活动转化/券核销率 |

### Phase 12：全链路压测 + 收尾（Week 16）

| 任务 | 内容 |
|------|------|
| 核心链路压测 | 登录→搜索→加购→下单→支付的完整链路压测 |
| 抽奖并发压测 | 1000 QPS 场景下的库存扣减正确性 |
| 拼团并发压测 | 临界成团场景的最终一致性 |
| 文档 | API 文档(Swagger) + 部署文档 + 架构图 |
| 演示准备 | 核心场景演示脚本 |

---

---

## 11. 可行性分析

### 11.1 一个人能做完整商城吗？

**能，但要明确边界。** 先对齐三个认知：

| 认知 | 说明 |
|------|------|
| **不做成淘宝** | 淘宝有 50+ 个微服务、上千工程师。目标是做一个**功能完整、逻辑闭环、可运行演示**的系统，不是商业级 SaaS 产品 |
| **借力四个原项目** | 四个参考项目的代码总量已经覆盖了 60%+ 的工作量。你不是从零写，是在已有轮子上造车 |
| **先骨架后血肉** | Phase 1 只有 5 个 API，跑通最简链路就能看到成果。后续每个 Phase 都是独立可交付的增量 |

### 11.2 工作量估算

```
模块                    复杂度      已有参考      自研工作量
───────────────────────────────────────────────────────────
用户登录                   ★★☆        s-pay ✅       低（改微信配置即可）
商品管理                   ★★☆        —              中（CRUD为主）
商品搜索(ES)               ★★★        big-market ✅   低（Canal配置为主）
购物车                     ★★☆        —              低（纯Redis数据结构）
订单 + 支付                ★★★        s-pay ✅       中（需要增加券/积分逻辑）
抽奖引擎                   ★★★★       Lottery ✅     低（算法可直接复用）
拼团引擎                   ★★★★       group-buy ✅   低（流程可直接复用）
积分体系                   ★★★        big-market ✅  低（账户模型可直接复用）
优惠券系统                  ★★★        —              中（全新设计但逻辑清晰）
物流追踪                   ★★☆        —              低（对接一个API即可）
售后体系                   ★★★        —              中（状态机+退款链路）
首页运营                   ★☆☆        —              低（几张配置表+查表API）
消息中心                   ★☆☆        —              低（一张表+CRUD）
风控 + 分库分表            ★★★★       Lottery+big ✅ 低（直接复用）
运营后台(ERP)              ★★☆        —              中（CRUD为主，路由即可）
───────────────────────────────────────────────────────────
总估：16 周（约 4 个月），每天投入 3-4 小时
```

### 11.3 哪些可以简化而不影响学习价值？

| 简化项 | 说明 | 对学习的影响 |
|--------|------|-------------|
| 不写前端 | 后端 API 全部用 Swagger/Postman 测试 | **无影响**。PRD 的核心是后端架构和业务逻辑 |
| 暂不做 Canal 实时同步 | Phase 2 直接用 ES SDK 写入 | 延后学习但不影响功能 |
| 不做微服务拆分 | 单体应用即可，Dubbo 接口预留 | 架构理解不受影响，后续可拆 |
| 不接真实支付 | 支付宝沙箱 + Mock 微信支付回调 | 理解支付流程即可 |
| 不做自动化 CI/CD | 手动 docker-compose up | 不影响开发和学习 |
| 不做分库分表直到数据量上来 | Phase 1-9 用单库单表 | 先跑通业务，Phase 10 再加 |

### 11.4 风险点与对策

| 风险 | 对策 |
|------|------|
| 优惠券 + 积分 + 折扣的互斥/叠加规则容易乱 | 先定规则矩阵（见下表），代码用一个 `calculateFinalPrice()` 统一入口 |
| 拼团并发成团的临界 bug | 开源的 group-buy-market 已有成熟方案，直接复用 |
| 优惠券计算的最优解算法 | 先做贪心（取最大优惠的单张券），后续优化为组合遍历 |
| 售后退款 + 退券 + 退积分的事务一致性 | 用本地消息表 + MQ 最终一致，不追求强一致 |
| 16 周的战线太长容易放弃 | 每个 Phase 产出是可独立演示的，做完即有所得 |

### 11.5 优惠/积分/券互斥规则矩阵

```
场景                    拼团折扣       优惠券         积分抵扣
─────────────────────────────────────────────────────────
普通下单                   —            ✅ 可用         ✅ 可用
拼团下单                  ✅ 生效        ❌ 不可用       ❌ 不可用
积分兑换商品               —            ❌ 不可用       ✅ 扣积分
抽奖（积分兑换次数）        —            —              ✅ 扣积分
```

**叠加规则**：普通下单时，优惠券 + 积分抵扣可同时使用。先算券后抵扣积分。
**计算顺序**：商品金额 → 减优惠券 → 减积分抵扣 → 实付金额

### 11.6 结论

**可以做，而且值得做。** 完成后你将掌握的不是"一个项目"，而是整个电商营销领域的知识图谱：

```
登录 → 商品 → 搜索 → 购物车 → 下单 → 支付 → 履约 → 售后
                    ↓                ↓
                 优惠券 ←────→ 积分抵扣
                    ↓
            ┌───────┴───────┐
            ▼               ▼
         抽奖引擎        拼团引擎
         (概率算法)      (并发控制)
            │               │
            └───────┬───────┘
                    ▼
              风控 + 监控 + 分库分表
```

这 12 个 Phase 走完，你在简历上可以写的不再是"熟悉 Spring Boot"，而是"独立设计并实现了一个完整的电商营销平台，涵盖 X 领域模型、Y 个 API、Z 种技术方案"，面试官问任何一个模块你都能对答如流。

---

## 附录

### A. 与原项目的差异和改进

| 方面 | 四个原项目 | 新系统改进 |
|------|-----------|-----------|
| 用户体系 | 各项目独立登录 | 统一微信登录 + JWT Token + 用户画像 |
| 商品管理 | 缺失或 Mock | 完整的商品/SKU/库存/分类 + ES 搜索 |
| 购物车 | 全部缺失 | Redis 热存储 + MySQL 冷备份 + 登录合并 |
| 订单体系 | 分散或简化 | 统一订单模型，关联营销活动，支持券+积分 |
| 支付渠道 | 仅支付宝或缺失 | 支付渠道抽象 + 统一收银台（支付宝+微信） |
| 优惠券 | 全部缺失 | 满减/折扣/运费/商品券 + 最优计算引擎 |
| 营销玩法 | 各自独立 | 共享活动管理 + 抽奖+拼团+积分+券联动 |
| 积分体系 | 仅 big-market | 统一积分账户，与券形成互斥规则 |
| 售后体系 | 仅基础退款 | 退款/退货退款 + 退券 + 退积分 + 审核流 |
| 物流追踪 | 全部缺失 | 对接快递鸟 + 轨迹订阅 + 自动签收 |
| 首页运营 | 全部缺失 | Banner + 频道 + 推荐 + 活动Tab |
| 消息中心 | 仅微信模板消息 | 站内消息 + 未读计数 + 事件触发 |
| 代码架构 | DDD 程度不一 | 统一 DDD 六边形架构 |
| 风控体系 | 仅 big-market | 全链路风控（限流/黑名单/熔断/降级） |
| 监控体系 | 仅 big-market | 全链路可观测 |

### B. 需要补充学习的知识点

| 知识领域 | 具体内容 | 对应项目 |
|----------|----------|----------|
| 微信 OAuth 2.0 | 授权码模式、ticket 机制、轮询登录 | 支付商城原型 |
| 支付宝开放平台 | 沙箱环境、支付单创建、RSA2 验签、异步通知 | 支付商城原型 |
| 概率算法 | 斐波那契散列、总体/单项概率、SecureRandom | Lottery |
| 决策树引擎 | 组合模式实现规则树、过滤器责任链 | Lottery + big-market |
| 分库分表 | 哈希路由、扰动函数、MyBatis 拦截器 | Lottery + big-market |
| Redis 分布式锁 | SETNX、Redisson、分段锁、锁续期 | 所有项目 |
| 本地消息表 | 事务内写消息、定时补偿、幂等消费 | group-buy-market |
| 积分账户设计 | 正向/逆向交易、冻结/可用余额、事务一致性 | big-market |
| 限流熔断 | RateLimiter、Sentinel/Hystrix、滑动窗口 | big-market |
| 动态配置 | DCC 原理（Zookeeper/Redis Pub-Sub） | big-market + group-buy-market |
| Canal 数据同步 | MySQL Binlog → ES，读写分离 | big-market |

### C. 关键设计决策记录

1. **为什么选 RabbitMQ 而不是 Kafka？** — RabbitMQ 对延迟敏感的业务（如支付回调）更友好，支持更灵活的路由。如果未来日志/埋点量大可加 Kafka 作为第二条 MQ 线。

2. **为什么保留分库分表？** — 拼团和抽奖的用户参与记录量级可达亿级，在 userId 维度分片是最自然的选择。2×4 分片足够中小规模，后续可平滑扩容到 4×8。

3. **为什么不做真正的微服务拆分？** — 本项目定位是"一个人能完成的完整系统"，模块化单体 + Dubbo RPC 假拆分即可。各领域模块间的接口已预留 RPC 边界，未来可平滑拆分为独立微服务。

4. **前端用什么？** — PRD 不限定。推荐 React + Ant Design Pro，或 Vue3 + Element Plus。抽奖动画建议用 Lottie / Canvas。

---

> **最后**：这份 PRD 的设计哲学是**先把路走通，再考虑优化**。Phase 1 只有最简单的"登录 → 下单 → 支付"，在此基础上逐渐加入抽奖、拼团、积分，每一步都可以独立运行和验证。当你按这个路线图走完 9 周，你不仅会有 1 个系统，还会有对这类业务场景的全链路理解。

---

*UniMarket PRD v2.0 — 2026-08-10*

> v2.0 更新：新增购物车、商品搜索、优惠券、售后、物流、首页运营、消息中心模块，实施路线图扩展至 12 阶段 16 周，增加可行性分析章节。
