# UniMarket 统一商城前台

> 面向 C 端用户的完整电商营销平台：融合支付、抽奖、营销、拼团、购物车、下单、履约，
> 不仅是"一个商城"，更是**可运营的营销引擎**（抽奖 / 拼团 / 积分 / 优惠券 / 签到返利）。

![Java](https://img.shields.io/badge/Java-8+-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.x-green) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) ![Redis](https://img.shields.io/badge/Redis-6.2-red) ![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.12-orange)

---

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [模块结构](#模块结构)
- [业务能力](#业务能力)
- [快速开始](#快速开始)
- [文档索引](#文档索引)
- [实施路线图](#实施路线图)
- [开发规范](#开发规范)

---

## 项目简介

UniMarket 融合四个既有原项目的能力：

| 原项目 | 迁移能力 |
|--------|----------|
| 支付原型（s-pay） | 用户登录 + 支付模块基础原型 |
| 抽奖（Lottery） | 抽奖引擎 + 分库分表方案 + 规则引擎 |
| 营销（big-market） | 积分引擎 + 风控体系 + 监控体系 |
| 拼团（group-buy-market） | 拼团引擎 + 折扣引擎 + 回调通知体系 |

架构上采用 **DDD 六边形架构**：领域逻辑与技术框架解耦，模块化单体起步，接口预留 RPC 边界，未来可平滑拆分微服务。

---

## 技术栈

| 层面 | 技术 |
|------|------|
| 基础框架 | Spring Boot 2.7.x / Java 8+ / MyBatis |
| 存储 | MySQL 8.0（分库分表 2×4） / Redis + Redisson |
| 消息/调度 | RabbitMQ / XXL-Job |
| RPC/注册 | Apache Dubbo 3.x（预留）/ Nacos |
| 搜索 | Elasticsearch + Canal |
| 风控/监控 | Guava RateLimiter / Sentinel / Prometheus + Grafana / ELK |
| 容器化 | Docker Compose |
| 前端 | React / Vue SPA（推荐，不限定） |

---

## 模块结构

```
uni-market/
├── uni-market-api/             # RPC/DTO 接口定义
├── uni-market-types/           # 通用类型（枚举/常量/异常/事件）
├── uni-market-domain/          # 领域层 ★核心★（user/product/order/marketing/payment）
├── uni-market-infrastructure/  # 基础设施层（DAO/仓储/网关/MQ/缓存/ES）
├── uni-market-trigger/         # 触发器层（HTTP/RPC/MQ/Job/回调入口）
├── uni-market-app/             # 启动与配置
└── docs/                       # 项目文档（SQL/Docker/PRD/设计/规范）
```

依赖方向：`app → trigger → (domain / infrastructure) → types`；domain 仅依赖 types 与 api。

---

## 业务能力

| 模块 | 能力 |
|------|------|
| 用户体系 | 微信扫码登录、JWT 鉴权、收货地址 |
| 商品/搜索 | 分类浏览、SKU、ES 全文搜索 + 多维筛选 |
| 购物车 | Redis 热存储 + MySQL 冷备份 + 登录合并 |
| 订单/支付 | 统一收银台（微信/支付宝）、掉单补偿、超时关单 |
| 优惠券 | 满减/折扣/运费/商品券 + 最优计算引擎 |
| 抽奖 | 单项/总体概率算法、决策树规则引擎、MQ 异步发奖 |
| 拼团 | 开团/参团/成团、四种折扣策略、三种退款策略 |
| 积分 | 积分账户、行为返利、积分兑换、订单抵扣 |
| 售后/物流 | 仅退款/退货退款、快递鸟轨迹、自动签收 |
| 首页/消息 | Banner/频道/推荐、站内消息 + 微信模板消息 |
| 运营后台 | 活动/策略/券/商品/订单/售后管理 + 数据看板 |
| 风控/监控 | 限流/黑名单/熔断/降级 + Prometheus/Grafana |

---

## 快速开始

### 1. 前置要求

- JDK 8+、Maven 3.6+、Docker + Docker Compose

### 2. 启动基础设施

```bash
cd docs/docker
docker compose up -d      # MySQL/Redis/RabbitMQ/Nacos/ES/Canal/XXL-Job/监控
docker compose ps         # 确认全部 Up/healthy
```

> 首次启动自动执行 `docs/sql` 下全部建库建表脚本。

### 3. 启动应用

```bash
mvn clean package -DskipTests
java -jar uni-market-app/target/uni-market-app-1.0.0.jar --spring.profiles.active=dev
```

### 4. 验证

- 健康检查：`curl http://localhost:8091/api/v1/health`
- Swagger：`http://localhost:8091/swagger-ui.html`
- 冒烟：登录 → 商品列表 → 下单 → 支付（沙箱）

> 完整步骤与内网穿透联调见《部署手册-环境部署》。

---

## 文档索引

> 文档按 `docs/` 子目录分类管理：需求 / 设计 / 计划 / 规范 / 测试 / 运维 / 模板。

### 01-需求

| 文档 | 说明 |
|------|------|
| `docs/01-需求/PRD-统一商城前台.md` | 产品需求文档 v2.0（12 阶段 16 周） |
| `docs/01-需求/需求文档-SRS-统一商城前台.md` | 软件需求规格说明书（FR/NFR/用户故事/业务规则/数据字典） |
| `docs/01-需求/需求追溯矩阵.csv` | 123 条需求追溯（含状态勾选列） |

### 02-设计

| 文档 | 说明 |
|------|------|
| `docs/02-设计/设计文档-SDS-统一商城前台.md` | 系统设计说明书（架构/领域/DB/API/流程/技术方案，含 Mermaid 图表） |
| `docs/02-设计/ADR/ADR-01~09` | 设计决策记录（消息队列/分库分表/架构/前端/存储/一致性/券计算/支付/优惠规则） |

### 03-计划

| 文档 | 说明 |
|------|------|
| `docs/03-计划/实施计划表-统一商城前台.md` | 12 阶段排期 + 四个月 DDL 日历 + Mermaid 甘特图 + Phase 1 逐日拆解 |

### 04-开发规范

| 文档 | 说明 |
|------|------|
| `docs/04-开发规范/开发规范-接口开发.md` | 接口开发规范（RESTful/响应/错误码/幂等） |
| `docs/04-开发规范/开发规范-接口变更管理.md` | 接口版本化/兼容性/废弃流程 |
| `docs/04-开发规范/开发规范-分支开发.md` | 分支开发与提交规范 |
| `docs/04-开发规范/开发规范-代码注释.md` | 代码注释规范 |
| `docs/04-开发规范/开发规范-Java编码.md` | Java 编码规范（阿里手册落地） |
| `docs/04-开发规范/开发规范-日志.md` | 日志规范（级别/格式/traceId/脱敏） |
| `docs/04-开发规范/开发规范-数据库设计.md` | 数据库设计规范（表/字段/索引/SQL） |
| `docs/04-开发规范/开发规范-安全编码.md` | 安全编码规范（OWASP 落地） |
| `docs/04-开发规范/开发规范-CodeReview.md` | Code Review 规范（流程/维度/记录） |
| `docs/04-开发规范/开发规范-前端开发.md` | 前端开发规范（组件/接口对接/构建） |
| `docs/04-开发规范/开发规范-RedisKey设计.md` | Redis Key 命名/结构/TTL 约定 |
| `docs/04-开发规范/开发规范-MQ与定时任务.md` | MQ Topic 与 XXL-Job 任务规范 |

### 05-测试

| 文档 | 说明 |
|------|------|
| `docs/05-测试/测试规范-测试计划与用例.md` | 测试分层/命名/E2E 用例模板 |
| `docs/05-测试/压测方案-性能压测.md` | 性能压测目标/场景/指标 |

### 06-部署运维

| 文档 | 说明 |
|------|------|
| `docs/06-部署运维/部署手册-环境部署.md` | docker-compose 与部署启动 |
| `docs/06-部署运维/第三方对接文档-微信支付宝快递鸟.md` | 第三方联调说明与配置 |
| `docs/06-部署运维/配置规范-Nacos配置清单.md` | Nacos 配置与 DCC 动态开关规范 |
| `docs/06-部署运维/CI-CD规范.md` | CI/CD 流水线与质量门禁 |
| `docs/06-部署运维/监控告警规范.md` | 监控指标与告警分级 |
| `docs/06-部署运维/发布与回滚流程.md` | 发布流程/灰度/回滚/事故响应 |

### 07-模板

| 文档 | 说明 |
|------|------|
| `docs/07-模板/模板-里程碑评审记录.md` | 里程碑验收留痕模板 |

---

## 实施路线图

12 阶段 16 周（详见《实施计划表》）：

```
Phase  1 基础骨架（登录→下单→支付）        Week 1-2
Phase  2 商品搜索 + 购物车                  Week 3
Phase  3 营销引擎 — 抽奖                    Week 4-5
Phase  4 营销引擎 — 拼团                    Week 6-7
Phase  5 积分体系                           Week 8
Phase  6 优惠券系统                         Week 9
Phase  7 订单履约（发货+物流+确认收货）      Week 10
Phase  8 售后体系                           Week 11
Phase  9 首页运营 + 消息中心                 Week 12
Phase 10 风控与监控                         Week 13-14
Phase 11 运营后台（ERP）                    Week 15
Phase 12 全链路压测 + 文档 + 收尾            Week 16
```

---

## 开发规范

- 分支：`feat/<描述>` / `fix/<描述>`，Conventional Commits 提交（见 `docs/04-开发规范/开发规范-分支开发.md`）。
- 接口：统一 `/api/v1/` 前缀 + `Response<T>` 响应 + 错误码枚举（见 `docs/04-开发规范/开发规范-接口开发.md`）。
- 注释：中文 Javadoc，注释"为什么"不注释"是什么"（见 `docs/04-开发规范/开发规范-代码注释.md`）。
- 编码：命名/集合/并发/异常/金额精度（见 `docs/04-开发规范/开发规范-Java编码.md`）。
- 日志：级别/格式/traceId 链路/敏感脱敏（见 `docs/04-开发规范/开发规范-日志.md`）。
- 安全：注入/越权/XSS/敏感信息防护（见 `docs/04-开发规范/开发规范-安全编码.md`）。
- 配置：Nacos 集中管理，敏感项环境变量注入，动态开关走 DCC（见 `docs/06-部署运维/配置规范-Nacos配置清单.md`）。

---

*UniMarket — 2026-08-10*
