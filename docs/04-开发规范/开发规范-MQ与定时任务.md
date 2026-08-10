# MQ Topic 与定时任务规范 — UniMarket

> 适用于 UniMarket 统一商城前台的 RabbitMQ 消息主题命名、消息体约定与 XXL-Job 定时任务规范。
> 版本：v1.0 ｜ 2026-08-10

---

## 目录

1. [总体原则](#1-总体原则)
2. [RabbitMQ 结构约定](#2-rabbitmq-结构约定)
3. [Topic 命名规范](#3-topic-命名规范)
4. [消息体与投递约定](#4-消息体与投递约定)
5. [消息 Topic 清单（模板）](#5-消息-topic-清单模板)
6. [消费幂等与重试约定](#6-消费幂等与重试约定)
7. [XXL-Job 任务规范](#7-xxl-job-任务规范)
8. [定时任务清单（模板）](#8-定时任务清单模板)
9. [监控与告警约定](#9-监控与告警约定)
10. [开发检查清单](#10-开发检查清单)

---

## 1. 总体原则

| 原则 | 说明 |
|------|------|
| 事件驱动 | 跨模块状态变更通过 MQ 解耦，禁止同步强耦合调用 |
| 最终一致 | 本地消息表 + MQ + 定时补偿三层保障（见 SDS §10.6） |
| 语义化命名 | Topic 表达"发生了什么"，不表达"谁去处理" |
| 幂等消费 | 消费者必须幂等，重复消息不产生副作用 |
| 任务可观测 | 定时任务统一接入 XXL-Job，禁止自写 Thread/定时器散落各处 |

**选型说明**：RabbitMQ 为唯一 MQ（对延迟敏感业务更友好）；若未来日志/埋点量大可引入 Kafka 作为第二条 MQ 线（ADR-01）。

---

## 2. RabbitMQ 结构约定

| 元素 | 约定 |
|------|------|
| Exchange | 统一使用 `unimarket.topic`（Topic 类型）；特殊场景可加 `unimarket.direct` / `unimarket.fanout` |
| Queue | 按业务消费者命名：`queue.<topic 去掉前缀>`，如 `queue.order.pay-success` |
| RoutingKey | 即 Topic（见第 3 节），`*`（一段）与 `#`（多段）通配按需 |
| 交换机绑定 | 一队列一绑定，绑定 key 与 queue 对应 topic 一致 |
| VHost | 统一 `unimarket`，测试/生产分 VHost 或独立 RabbitMQ |

```java
// 声明示例（配置类集中声明，禁止各业务自建交换机）
@Bean
public TopicExchange unimarketTopic() {
    return new TopicExchange("unimarket.topic", true, false);
}
```

---

## 3. Topic 命名规范

### 3.1 通用格式

```
<域>.<事件>
```

- 域：业务域（同 Redis 规范：`order` / `lottery` / `groupbuy` / `credit` / `coupon` / `aftersale` / `message` / `logistics`）
- 事件：过去式动作，小写连字符，表达"已完成/已发生"
- 全小写，点号分隔，最多三级

### 3.2 示例

| Topic | 含义 |
|-------|------|
| `order.pay-success` | 订单支付成功 |
| `order.pay-timeout` | 订单支付超时 |
| `order.refunded` | 订单退款完成 |
| `lottery.award-grant` | 抽奖发奖 |
| `credit.adjust-success` | 积分调整成功 |
| `groupbuy.team-success` | 拼团成团 |
| `groupbuy.team-fail` | 拼团失败 |
| `coupon.expired` | 优惠券过期提醒 |
| `aftersale.refunded` | 售后退款完成 |
| `logistics.signed` | 物流签收 |
| `message.send` | 站内消息/模板消息发送 |

**禁止**：
- ❌ 用目标服务命名：`user-consumer.order`（表达"谁处理"而非"发生了什么"）
- ❌ 大小写/中文：`Order.PaySuccess`
- ❌ 无域：`paid`

---

## 4. 消息体与投递约定

### 4.1 统一消息体

```json
{
  "messageId": "uuid",
  "topic": "order.pay-success",
  "bizType": "ORDER_PAY",
  "payload": { },
  "occurTime": "2026-08-10 22:00:00"
}
```

| 字段 | 说明 |
|------|------|
| messageId | 全局唯一，消费者去重键 |
| topic | 主题（冗余便于排查） |
| bizType | 业务类型枚举，便于路由 |
| payload | 业务数据（DTO JSON） |
| occurTime | 事件发生时间 |

### 4.2 投递约定

- 生产：**事务提交后发送**（本地消息表 + 事务，见 SDS §10.6），禁止在事务内直接发 MQ。
- 发送失败：写入 `mq_task` 表，由 XXL-Job 补偿重发。
- 消费失败：`basicNack` + 延迟重试（最多 3 次），仍失败进死信队列 `queue.<topic>.dlx`。
- 消息体 payload 使用与接口 DTO 一致的 JSON，禁止传 PO/实体（防字段泄漏）。

---

## 5. 消息 Topic 清单（模板）

> 开发阶段按模块登记，随开发补充完整。格式：Topic / 生产者 / 消费者 / 用途 / 是否需补偿。

| Topic | 生产者 | 消费者 | 用途 | 补偿 | 状态 |
|-------|--------|--------|------|------|------|
| `order.pay-success` | 支付回调 | 营销/积分/通知 | 支付成功后续处理 | ✅ | □ |
| `order.pay-timeout` | 超时关单任务 | 库存/券 | 释放资源 | ✅ | □ |
| `order.refunded` | 退款服务 | 积分/券 | 退积分退券 | ✅ | □ |
| `lottery.award-grant` | 抽奖服务 | 发奖工厂 | 异步发奖 | ✅ | □ |
| `credit.adjust-success` | 积分服务 | 订单服务 | 积分兑换状态更新 | ✅ | □ |
| `groupbuy.team-success` | 成团判断 | 通知/ERP | 成团回调通知 | ✅ | □ |
| `groupbuy.team-fail` | 超时任务 | 订单服务 | 超时退款 | ✅ | □ |
| `coupon.expired` | 过期扫描任务 | 通知 | 券到期提醒 | ❌ | □ |
| `aftersale.refunded` | 售后服务 | 积分/券 | 逆向退券退积分 | ✅ | □ |
| `logistics.signed` | 物流回调 | 订单服务 | 自动确认收货 | ✅ | □ |
| `message.send` | 各业务 | 消息中心 | 站内/模板消息 | ✅ | □ |

---

## 6. 消费幂等与重试约定

- 消费者首行做**去重**：`messageId` 写入 Redis（`idempotent:mq:{messageId}`，TTL 24h）或 DB 唯一索引；命中已处理则直接 ack。
- 业务侧幂等兜底：处理动作本身幂等（如"退券"以 userCouponId 幂等、"积分调整"以 out_biz_no 幂等）。
- 重试策略：失败 `nack` 不 requeue → 延迟队列（5s/30s/5min 三级）→ 超过 3 次进死信队列。
- 死信处理：XXL-Job 扫描死信队列 → 告警 + 人工介入（保留原始消息体便于排查）。
- **禁止**在消费者内吞掉异常不打日志。

---

## 7. XXL-Job 任务规范

| 项 | 约定 |
|----|------|
| 执行器 | 统一接入 XXL-Job；任务按模块分组（`order` / `marketing` / `coupon` / `logistics` / `mq`） |
| 命名 | `模块.业务动作`，如 `order.timeout-close`、`mq.compensate` |
| 调度 | cron 表达式集中配置于 XXL-Job 管理台，代码中不写死调度时间 |
| 幂等 | 任务处理需幂等（重复执行不产生重复扣减/重复通知） |
| 分片 | 数据量大时按分片广播/分片参数处理，避免单机全量扫描 |
| 失败告警 | 任务失败自动告警（XXL-Job 告警通道） |
| 日志 | 任务执行留痕（开始/结束/处理条数/失败明细），供排查 |

### 任务写法要点

```java
// 一个任务方法 = 一个业务动作；返回执行摘要；捕获异常避免中断
@XxlJob("order.timeout-close")
public ReturnT<String> timeoutClose(String param) {
    // 1. 查 status=PAY_WAIT 且超 30 分钟订单（分页/分片）
    // 2. 逐个：关单 + 释放库存 + 退券 + 退积分（幂等，可重跑）
    // 3. 返回 {成功数, 失败数}
}
```

---

## 8. 定时任务清单（模板）

> 源自 SDS §9 各流程补偿/清理任务，开发阶段按此登记完善。

| 任务名 | 调度 | 说明 | 幂等要点 | 状态 |
|--------|------|------|----------|------|
| `order.drop-compensate` | 每 1 分钟 | 掉单补偿：查 CREATE 支付单主动查支付渠道 | outTradeNo 唯一 | □ |
| `order.timeout-close` | 每 5 分钟 | 超时关单：PAY_WAIT>30min → CLOSE | 状态机校验 | □ |
| `mq.compensate` | 每 1 分钟 | MQ 补偿：扫 mq_task PENDING/RETRY 重发 | messageId 幂等 | □ |
| `stock.reconcile` | 每 5 分钟 | 库存对账：Redis vs DB，以 DB 为准修正 | — | □ |
| `groupbuy.team-timeout` | 每 5 分钟 | 拼团超时：未成团队伍自动退款 | bizId 幂等 | □ |
| `coupon.expire-scan` | 每日 0:10 | 券过期扫描：EXPIRED | userCouponId 幂等 | □ |
| `logistics.poll` | 每 30 分钟 | 物流轮询：未签收订单查轨迹 | trackingNo 幂等 | □ |
| `order.auto-confirm` | 每 10 分钟 | 自动确认收货：签收 N 天后 DEAL_DONE | 状态机校验 | □ |
| `message.send-compensate` | 每 1 分钟 | 通知补偿：notify_task 重试 | uuid 幂等 | □ |

---

## 9. 监控与告警约定

- MQ：监控队列积压（`rabbitmq_queue_messages`）、消费失败率、死信队列积压；积压超阈值告警。
- 任务：监控 XXL-Job 失败次数、执行耗时；连续失败告警。
- 指标：Prometheus 采集，Grafana 面板分组（MQ / 任务 / 业务）。
- 日志：消息轨迹（messageId 贯穿 producer/consumer）进 ELK，支持按 messageId 检索全链路。

---

## 10. 开发检查清单

- [ ] Topic 遵循 `<域>.<事件>` 语义化命名，全小写
- [ ] 交换机/队列在配置类集中声明，无散落自建
- [ ] 消息体使用统一结构（messageId/topic/bizType/payload/occurTime）
- [ ] 事务提交后才发 MQ；失败入 mq_task 补偿
- [ ] 消费者首行幂等去重，失败重试 ≤ 3 次进死信
- [ ] 定时任务统一接入 XXL-Job，命名 `模块.动作`
- [ ] 任务可重跑（幂等），失败告警，留执行日志
- [ ] 新增 Topic/任务登记到第 5/8 节清单

---

*MQ Topic 与定时任务规范 v1.0 — 2026-08-10，源自《设计文档-SDS》§10.6 与各流程补偿设计*
