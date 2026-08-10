# AGENTS.md — UniMarket 全局 Agent 规范

> 本文件为 AI 编码助手（Claude Code / Codex / AtomCode / WorkBuddy 等）提供本项目的工作规范。
> 任何 Agent 在本仓库内工作前，必须先阅读本文件与下方指向的规范文档。
> 版本：v1.0 ｜ 2026-08-10

---

## 1. 项目概述

**UniMarket（统一商城前台）**：面向 C 端用户的完整电商营销平台，融合支付、抽奖、营销、拼团、购物车、下单、履约。定位是"可运营的营销引擎"——运营通过配置化活动（抽奖/拼团/积分/优惠券/签到返利）驱动增长与转化。

- 单人多模块 DDD 项目，JDK 21 / Spring Boot 3.4.x / MyBatis-Plus
- 四个原项目能力迁移：s-pay（登录/支付）、Lottery（抽奖）、big-market（积分/风控/监控）、group-buy-market（拼团/折扣）
- 12 阶段 16 周实施路线图，当前处于 **Phase 1（基础骨架：登录→下单→支付）**

## 2. 技术栈速查

| 层面 | 技术 |
|------|------|
| 框架 | Spring Boot 3.4.x（JDK 21，jakarta 命名空间）/ MyBatis-Plus 3.5.5+ |
| 存储 | MySQL 8.4 LTS（Phase 10 前单库，后期再分库分表）/ Redis 7.2 + Redisson / MinIO |
| 中间件 | RocketMQ 5.x（业务线）+ Kafka 3.7+（日志线）/ Nacos / XXL-Job / Elasticsearch + Canal |
| 风控监控 | Sentinel / Prometheus + Grafana / Micrometer Tracing / ELK |
| 接口文档 | Knife4j 4.4+（OpenAPI3 + Swagger） |
| 构建 | Maven 3.9.x 多模块 |

## 3. 仓库结构

```
uni-market/
├── uni-market-api/             # RPC/DTO 接口定义
├── uni-market-types/           # 通用类型（枚举/常量/异常/事件）
├── uni-market-domain/          # 领域层 ★核心★
├── uni-market-infrastructure/  # 基础设施层（DAO/仓储/网关/MQ/缓存/ES）
├── uni-market-trigger/         # 触发器层（HTTP/RPC/MQ/Job/回调）
├── uni-market-app/             # 启动与配置
└── docs/                       # 全部项目文档（按 01-需求 ~ 07-模板 分类）
```

依赖方向：`app → trigger → (domain / infrastructure) → types`。**领域规则只允许写在 domain 层**。

## 4. 工作前必读（按需）

| 场景 | 必读文档 |
|------|----------|
| 任何开发任务前 | `docs/01-需求/需求文档-SRS-统一商城前台.md`（对应 FR 编号） |
| 涉及架构/DB/API | `docs/02-设计/设计文档-SDS-统一商城前台.md` |
| 写代码 | `docs/04-开发规范/开发规范-Java编码.md`、`开发规范-代码注释.md` |
| 写接口 | `docs/04-开发规范/开发规范-接口开发.md` |
| 涉及数据库 | `docs/04-开发规范/开发规范-数据库设计.md` |
| 涉及 Redis | `docs/04-开发规范/开发规范-RedisKey设计.md` |
| 涉及 MQ/定时任务 | `docs/04-开发规范/开发规范-MQ与定时任务.md` |
| 涉及支付/微信/物流 | `docs/06-部署运维/第三方对接文档-微信支付宝快递鸟.md` |
| 配置类改动 | `docs/06-部署运维/配置规范-Nacos配置清单.md` |
| 测试 | `docs/05-测试/测试规范-测试计划与用例.md` |
| 计划/进度 | `docs/03-计划/实施计划表-统一商城前台.md` |

## 5. Agent 硬性规范（必须遵守）

### 5.1 架构与代码

- **DDD 分层**：领域逻辑只在 domain 层；Controller 不写业务逻辑；基础设施实现不泄漏到领域。
- **金额一律 `BigDecimal`**（字符串构造、`compareTo` 比较、HALF_UP 舍入），禁止 float/double。
- **时间一律 `java.time`（LocalDateTime）**，格式 `yyyy-MM-dd HH:mm:ss`，禁止 Date/Calendar 做业务计算。
- **幂等兜底**：支付回调/领券/签到/积分交易/拼团参团必须有唯一约束（见 SRS BR-08/19/22）。
- **库存扣减**：Redis INCR 快速失败 + DB 行锁条件更新（`... WHERE ... AND surplus_count > 0`）。
- **SQL 安全**：MyBatis 一律 `#{}`，禁止 `${}` 拼接用户输入；动态排序白名单。
- **金额计算唯一入口** `calculateFinalPrice()`，遵循互斥矩阵（见 SRS BR-01/02）。
- **锁**：分布式锁用 Redisson（带 TTL + finally 释放），禁止裸 SETNX 无过期。

### 5.2 命名与注释

- 类 UpperCamelCase、方法 lowerCamelCase、常量全大写下划线；**禁止拼音命名**。
- 注释用中文，说明"为什么"；Javadoc 覆盖公开类/方法/枚举值。
- 不做无依据的重构与过度设计；改动最小化。

### 5.3 提交规范（Conventional Commits）

```
<type>(<scope>): <中文祈使句描述>
```
- type：`feat` / `fix` / `docs` / `style` / `refactor` / `test` / `perf` / `build` / `ci` / `chore`
- **一个提交只做一件事**；提交信息带 `Co-Authored-By: AtomCode (deepseek-v4-flash) <noreply@atomgit.com>`（除非 amend/revert）。
- 禁止提交：`target/`、`.idea/` 个人文件、`node_modules/`、密钥/密码/Token（见 .gitignore）。

### 5.4 工作流程

1. **先读文档再动手**：按 §4 找到对应规范/需求，不凭记忆改代码。
2. **小步提交**：功能、修复、文档分开提交；提交前 `git status` / `git diff --stat` 检查范围。
3. **验证**：改动后运行 `mvn compile` / `mvn test`（或对应检查），确认通过再交付；未运行必须说明。
4. **不隐藏问题**：测试失败/编译错误必须修根因，禁止注释掉/弱化测试掩盖。
5. **有疑问先查**：先查代码与文档，3 轮搜索无果再向用户提问。

### 5.5 安全红线

- 不把密钥/密码/Token/私钥写入代码、注释、日志、提交。
- 手机号等敏感字段展示与日志脱敏（`SensitiveUtil`）。
- 支付/第三方回调必须验签，验签失败拒绝。

## 6. 常用命令

```bash
mvn clean compile            # 编译
mvn test                     # 单元测试
mvn clean package -DskipTests  # 打包
docker compose -f docs/docker/docker-compose-infrastructure.yml up -d  # 起中间件
git status / git diff --stat # 提交前检查
```

## 7. 当前进度与约束

- 当前阶段：**Phase 1（Week 1-2，2026-08-10 ~ 08-21）**，里程碑 M1：登录 → 下单 → 支付全链路。
- 文档体系已完备（需求/设计/规范/测试/运维），开发按《实施计划表》逐日任务推进。
- 接口统一 `/api/v1/` 前缀 + `Response<T>` 统一响应 + `ErrorCode` 枚举错误码。

---

*AGENTS.md v1.0 — 2026-08-10，适用于 Claude Code / Codex / AtomCode / WorkBuddy*
