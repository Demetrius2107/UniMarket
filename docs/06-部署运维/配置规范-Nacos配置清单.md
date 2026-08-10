# Nacos 配置清单规范 — UniMarket

> 适用于 UniMarket 统一商城前台的 Nacos 配置管理约定：配置分组、命名规范、清单模板与动态开关（DCC）约定。
> 目标：所有环境配置可查、可控、可动态变更，禁止硬编码与散落本地文件。
> 版本：v1.0 ｜ 2026-08-10

---

## 目录

1. [配置管理原则](#1-配置管理原则)
2. [配置分组与命名规范](#2-配置分组与命名规范)
3. [Data ID 与 Profile 约定](#3-data-id-与-profile-约定)
4. [配置项清单（模板）](#4-配置项清单模板)
5. [动态配置（DCC 降级开关）约定](#5-动态配置dcc-降级开关约定)
6. [敏感配置安全约定](#6-敏感配置安全约定)
7. [配置变更流程](#7-配置变更流程)
8. [检查清单](#8-检查清单)

---

## 1. 配置管理原则

| 原则 | 说明 |
|------|------|
| 配置即代码 | 配置统一进 Nacos，禁止散落本地 `application-*.yml`（本地兜底除外） |
| 环境隔离 | dev / test / prod 三套配置，互不串用 |
| 敏感脱敏 | 密钥/私钥/密码不进仓库，生产用环境变量注入 |
| 动态优先 | 可运行时变更的开关（限流/降级/切量）走 DCC 动态配置 |
| 变更留痕 | 配置变更走 Nacos 历史记录，重要变更记录到里程碑评审 |

**分层约定**：
- 静态配置（端口、连接串前缀）→ Nacos 普通配置。
- 动态开关（降级/限流阈值/切量比例）→ DCC（Redis Pub-Sub / Nacos 监听），运行时生效。

---

## 2. 配置分组与命名规范

### 2.1 命名格式

```
<域>.<项>
```

- 域：`spring` / `datasource` / `redis` / `mq` / `wechat` / `wxpay` / `alipay` / `kdniao` / `lottery` / `groupbuy` / `coupon` / `credit` / `risk` / `task` / `search` / `dcc`
- 全小写连字符，避免歧义。

### 2.2 配置示例（application.yml 局部）

```yaml
spring:
  datasource:
    public-url: jdbc:mysql://localhost:3306/uni_market?...   # 公共库
    shard-url-01: jdbc:mysql://localhost:3306/uni_market_01  # 分库1
    shard-url-02: jdbc:mysql://localhost:3306/uni_market_02  # 分库2
  redis:
    host: localhost
    port: 6379
    password: ${REDIS_PASSWORD:redis123}

mq:
  host: localhost
  port: 5672
  username: unimarket
  password: ${RABBITMQ_PASS:unimarket123}
  vhost: unimarket

wechat:
  app-id: ${WECHAT_APP_ID:}
  app-secret: ${WECHAT_APP_SECRET:}

risk:
  rate-limit-per-second: 1          # 每用户每秒请求上限
  blacklist-auto-threshold: 5       # 24h 超 N 次限流自动拉黑
  circuit-break-timeout-ms: 150     # 熔断超时阈值
  circuit-break-half-open-secs: 10  # 半开恢复时间
  degrade-switch: false             # DCC 降级总开关
```

---

## 3. Data ID 与 Profile 约定

| Data ID | 环境 | 说明 |
|---------|------|------|
| `unimarket-dev.yml` | 本地/开发 | 完整配置，连接本地 Docker 中间件 |
| `unimarket-test.yml` | 测试/联调 | 沙箱支付、测试 key |
| `unimarket-prod.yml` | 生产 | 生产连接串，敏感项走环境变量 |

- 应用通过 `spring.profiles.active=dev/test/prod` 选择。
- 本地开发允许 `application-local.yml` 兜底（已被 .gitignore 排除，不入库）。
- 分库分表分片数（`shard.db-count`、`shard.tb-count`）必须与代码常量一致，变更需评审。

---

## 4. 配置项清单（模板）

> 开发阶段逐模块登记，随开发补充。格式：配置项 / 默认值 / 是否动态（DCC）/ 说明。

| 分组 | 配置项 | 默认值 | 动态 | 说明 | 状态 |
|------|--------|--------|------|------|------|
| spring | `datasource.public-url` | 本地库 | ❌ | 公共库连接 | □ |
| spring | `datasource.shard-url-01/02` | 本地库 | ❌ | 分库连接 | □ |
| spring | `redis.*` | localhost | ❌ | Redis 连接 | □ |
| mq | `mq.*` | localhost | ❌ | RabbitMQ 连接 | □ |
| wechat | `wechat.app-id/secret` | — | ❌ | 公众号凭据 | □ |
| wxpay | `wxpay.mch-id` 等 | — | ❌ | 微信支付凭据 | □ |
| alipay | `alipay.app-id` 等 | — | ❌ | 支付宝凭据 | □ |
| kdniao | `kdniao.ebusiness-id` 等 | — | ❌ | 快递鸟凭据 | □ |
| pay | `pay.callback-base-url` | — | ❌ | 回调基础地址（穿透域名） | □ |
| risk | `risk.rate-limit-per-second` | 1 | ✅ | 用户级限流阈值 | □ |
| risk | `risk.blacklist-auto-threshold` | 5 | ✅ | 自动拉黑阈值 | □ |
| risk | `risk.degrade-switch` | false | ✅ | 全局降级开关 | □ |
| risk | `risk.lottery-degrade` | false | ✅ | 抽奖降级开关 | □ |
| risk | `risk.groupbuy-degrade` | false | ✅ | 拼团降级开关 | □ |
| lottery | `lottery.stock-reconcile-interval` | 5min | ✅ | 库存对账间隔 | □ |
| groupbuy | `groupbuy.team-timeout-minutes` | 24h | ✅ | 拼团有效时长 | □ |
| coupon | `coupon.calc-strategy` | greedy | ✅ | 最优券算法（greedy/combine） | □ |
| search | `search.sync-mode` | canal | ✅ | 同步方式（canal/sdk） | □ |
| task | `task.*` | — | ❌ | 任务相关参数 | □ |

---

## 5. 动态配置（DCC 降级开关）约定

- **机制**：Redis Pub-Sub（或 Nacos 监听）广播配置变更，应用无需重启。
- **命名**：`dcc:<域>:<开关名>`，如 `dcc:risk:lottery-degrade`。
- **消费方式**：应用启动时加载 + 订阅变更；配置类统一监听并刷新缓存。

```java
// 配置监听示例（统一由 ConfigListener 组件处理，业务侧只读 getter）
@Component
public class DccRiskSwitch {
    private volatile boolean lotteryDegrade;

    @PostConstruct
    public void init() {
        lotteryDegrade = configCenter.getBoolean("dcc:risk:lottery-degrade", false);
        configCenter.subscribe("dcc:risk:lottery-degrade", val -> this.lotteryDegrade = Boolean.parseBoolean(val));
    }

    public boolean isLotteryDegrade() { return lotteryDegrade; }
}
```

- **使用场景**：降级（抽奖/拼团关闭返回兜底）、切量（活动流量比例）、限流阈值调整、算法切换。
- **约定**：降级开关生效后业务返回明确提示（如"活动火爆，请稍后再试"），并记录降级日志/指标。

---

## 6. 敏感配置安全约定

- 密码/私钥/密钥/Token：Nacos 中留空或占位符，**运行时由环境变量注入**（`${REDIS_PASSWORD:}`）。
- 生产环境建议使用密钥管理（如 KMS/配置中心加密插件），禁止明文落库/落文件。
- 提交仓库的配置模板必须为脱敏占位（`.example` 后缀文件可入库，真实配置不入库）。
- 日志打印禁止输出配置明文（打印时打码）。

---

## 7. 配置变更流程

```
1. 评估影响面（是否涉及多实例/是否动态生效）
2. 修改 Nacos 配置（dev 先行验证）
3. 涉及动态开关 → 发布后立即生效并观察指标
4. 涉及静态配置 → 发布 + 重启应用（或滚动重启）
5. 记录变更到里程碑评审/变更记录（重要变更）
```

**回滚**：利用 Nacos 配置历史一键回滚；动态开关直接改回即可。

---

## 8. 检查清单

- [ ] 配置项按 `<域>.<项>` 命名，分组清晰
- [ ] 敏感项使用环境变量占位，无明文入库
- [ ] 可运行时变更的开关走 DCC，未写死在代码
- [ ] 配置变更已登记到清单模板（第 4 节）
- [ ] 分库分表分片数与代码一致
- [ ] 本地兜底配置不入库（.gitignore 覆盖）
- [ ] 回调地址基于统一 base-url 拼装

---

*Nacos 配置清单规范 v1.0 — 2026-08-10，源自《设计文档-SDS》§10.7 DCC 与《部署手册》§7*
