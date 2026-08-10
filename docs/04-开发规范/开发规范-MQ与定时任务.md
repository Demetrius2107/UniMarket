# MQ Topic 与定时任务规范 — UniMarket

> 适用于 UniMarket 统一商城前台的 RocketMQ（业务线）+ Kafka（日志/埋点线）消息主题命名、消息体约定与 XXL-Job 定时任务规范。
> 版本：v2.0 ｜ 2026-08-11（随 ADR-10 技术选型升级：RabbitMQ → RocketMQ 业务线 + Kafka 日志线）

---

## 目录

1. [总体原则](#1-总体原则)
2. [RocketMQ 结构约定](#2-rocketmq-结构约定)
3. [Topic / Tag 命名规范](#3-topic--tag-命名规范)
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
| 职责分离 | 业务一致性消息走 RocketMQ；日志/埋点海量消息走 Kafka |
| 任务可观测 | 定时任务统一接入 XXL-Job，禁止自写 Thread/定时器散落各处 |

**选型说明（ADR-10）**：双 MQ 架构——
- **RocketMQ 5.x（业务线）**：订单/支付/发奖/积分/拼团/通知等业务事件，利用事务消息、延迟消息、顺序消息能力覆盖"本地消息表 + MQ 补偿"场景。
- **Kafka 3.7+（日志/埋点线）**：行为日志、埋点统计、大数据量流式处理，与业务隔离。

---

## 2. RocketMQ 结构约定

| 元素 | 约定 |
|------|------|
| NameServer | 统一 `localhost:9876`（生产为集群地址，配置于 Nacos） |
| Topic | 业务事件主题（命名见第 3 节），统一在 RocketMQ Dashboard 或启动时创建 |
| Tag | Topic 内二级过滤标签（如 `order.pay-success` 的 Tag=`PAY`），消费者按 Tag 订阅 |
| ConsumerGroup | 消费组命名 `cg.<业务域>`，如 `cg.order`；一业务一消费组，禁止共用 |
| 消息类型 | 普通消息（默认）/ 延迟消息（`msg.setDelayTimeLevel()`）/ 事务消息（本地消息表补偿场景）/ 顺序消息（按需，如单订单流水） |
| 延迟等级 | 内置 18 级：1s/5s/10s/30s/1m/2m/3m/4m/5m/6m/7m/8m/9m/10m/20m/30m/1h/2h；重试阶梯按需取用 |

```java
// 生产示例（统一 RocketMQTemplate 封装，禁止散落裸 producer）
@Autowired private RocketMQTemplate rocketMQTemplate;

// 普通消息（业务线）
rocketMQTemplate.convertAndSend("order.pay-success:PAY", messageBody);

// 延迟消息（如超时关单预通知，延迟 30s）
Message<String> msg = MessageBuilder.withPayload(body).build();
rocketMQTemplate.syncSend("order.pay-wait-check:PAY", msg, 3000, 4); // delayLevel=4 → 30s
```

### Kafka（日志/埋点线）约定

| 元素 | 约定 |
|------|------|
| Topic | 前缀 `log.` / `track.`，如 `log.behavior`、`track.search` |
| 生产 | 应用内统一 KafkaTemplate 封装，异步发送 |
| 消费 | 按需独立消费者/下游分析，不阻塞业务主链路 |

---

## 3. Topic / Tag 命名规范

### 3.1 通用格式

```
<域>.<事件>:<Tag>
```

- 域：业务域（同 Redis 规范：`order` / `lottery` / `groupbuy` / `credit` / `coupon` / `aftersale` / `message` / `logistics`）
- 事件：过去式动作，小写连字符，表达"已完成/已发生"
- Tag：全大写，表达事件类别（如 `PAY` / `REFUND` / `GRANT` / `NOTIFY`），冒号分隔，消费者按 Tag 过滤
- 全小写（Tag 除外），点号分隔，最多三级

### 3.2 示例

| Topic:Tag | 含义 |
|-----------|------|
| `order.pay-success:PAY` | 订单支付成功 |
| `order.pay-timeout:PAY` | 订单支付超时 |
| `order.refunded:REFUND` | 订单退款完成 |
| `lottery.award-grant:GRANT` | 抽奖发奖 |
| `credit.adjust-success:CREDIT` | 积分调整成功 |
| `groupbuy.team-success:GROUP` | 拼团成团 |
| `groupbuy.team-fail:GROUP` | 拼团失败 |
| `coupon.expired:NOTIFY` | 优惠券过期提醒 |
| `aftersale.refunded:REFUND` | 售后退款完成 |
| `logistics.signed:LOGISTICS` | 物流签收 |
| `message.send:NOTIFY` | 站内消息/模板消息发送 |
| `log.behavior:TRACK` | （Kafka）行为日志 |

**禁止**：
- ❌ 用目标服务命名：`user-consumer.order`（表达"谁处理"而非"发生了什么"）
- ❌ 大小写/中文：`Order.PaySuccess`
- ❌ 无域：`paid`
- ❌ 一个 Topic 塞多种不相关事件（应用 Tag 或拆 Topic）

---

## 4. 消息体与投递约定

### 4.1 统一消息体

```json
{
  "messageId": "uuid",
  "topic": "order.pay-success",
  "tag": "PAY",
  "bizType": "ORDER_PAY",
  "payload": { },
  "occurTime": "2026-08-10 22:00:00"
}
```

| 字段 | 说明 |
|------|------|
| messageId | 全局唯一，消费者去重键 |
| topic | 主题（冗余便于排查） |
| tag | 标签（冗余便于排查） |
| bizType | 业务类型枚举，便于路由 |
| payload | 业务数据（DTO JSON） |
| occurTime | 事件发生时间 |

### 4.2 投递约定

- **业务线（RocketMQ）**：
  - 生产：**事务提交后发送**（本地消息表 + 事务，见 SDS §10.6）；高一致性场景用**事务消息**（半消息 + 本地事务确认）。
  - 发送失败：写入 `mq_task` 表，由 XXL-Job 补偿重发。
  - 消费失败：返回 `ConsumeConcurrentlyStatus.RECONSUME_LATER` 触发 RocketMQ 重试（默认 16 次，业务侧限制 ≤ 3 次自定义），超限进死信队列 `%DLQ%cg.<域>`。
- **日志线（Kafka）**：异步发送，失败丢弃并告警（日志类允许丢失，不阻塞业务）。
- 消息体 payload 使用与接口 DTO 一致的 JSON，禁止传 PO/实体（防字段泄漏）。

---

## 5. 消息 Topic 清单（模板）

> 开发阶段按模块登记，随开发补充完整。格式：Topic:Tag / 生产者 / 消费组 / 用途 / 是否需补偿。

| Topic:Tag | 生产者 | 消费组 | 用途 | 补偿 | 状态 |
|-----------|--------|--------|------|------|------|
| `order.pay-success:PAY` | 支付回调 | `cg.order` | 支付成功后续处理 | ✅ | □ |
| `order.pay-timeout:PAY` | 超时关单任务 | `cg.order` | 释放资源 | ✅ | □ |
| `order.refunded:REFUND` | 退款服务 | `cg.order` | 退积分退券 | ✅ | □ |
| `lottery.award-grant:GRANT` | 抽奖服务 | `cg.lottery` | 异步发奖 | ✅ | □ |
| `credit.adjust-success:CREDIT` | 积分服务 | `cg.credit` | 积分兑换状态更新 | ✅ | □ |
| `groupbuy.team-success:GROUP` | 成团判断 | `cg.groupbuy` | 成团回调通知 | ✅ | □ |
| `groupbuy.team-fail:GROUP` | 超时任务 | `cg.groupbuy` | 超时退款 | ✅ | □ |
| `coupon.expired:NOTIFY` | 过期扫描任务 | `cg.message` | 券到期提醒 | ❌ | □ |
| `aftersale.refunded:REFUND` | 售后服务 | `cg.order` | 逆向退券退积分 | ✅ | □ |
| `logistics.signed:LOGISTICS` | 物流回调 | `cg.order` | 自动确认收货 | ✅ | □ |
| `message.send:NOTIFY` | 各业务 | `cg.message` | 站内/模板消息 | ✅ | □ |

---

## 6. 消费幂等与重试约定

- 消费者首行做**去重**：`messageId` 写入 Redis（`idempotent:mq:{messageId}`，TTL 24h）或 DB 唯一索引；命中已处理则直接 `CONSUME_SUCCESS`。
- 业务侧幂等兜底：处理动作本身幂等（如"退券"以 userCouponId 幂等、"积分调整"以 out_biz_no 幂等）。
- 重试策略（RocketMQ）：
  - 消费失败返回 `RECONSUME_LATER` → RocketMQ 按延迟等级自动重试（业务侧限制重试 ≤ 3 次）。
  - 超过最大重试进死信队列 `%DLQ%cg.<业务域>`。
- 死信处理：XXL-Job 扫描死信队列 → 告警 + 人工介入（保留原始消息体便于排查）。
- **禁止**在消费者内吞掉异常不打日志；禁止 catch 后无条件返回 SUCCESS。

```java
// 消费者示例（去重 + 失败重试）
@RocketMQMessageListener(topic = "order.pay-success", selectorExpression = "PAY",
        consumerGroup = "cg.order")
public class PaySuccessConsumer implements RocketMQListener<MessageExt> {
    @Override
    public void onMessage(MessageExt msg) {
        String messageId = msg.getMsgId();
        // 1. 去重：命中已处理 → 直接返回
        // 2. 业务处理（幂等）
        // 3. 失败 → throw Exception（自动 RECONSUME_LATER，走重试）
    }
}
```

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
| `mq.dlq-scan` | 每 10 分钟 | 死信扫描：%DLQ% 队列 → 告警/人工介入 | msgId 幂等 | □ |
| `stock.reconcile` | 每 5 分钟 | 库存对账：Redis vs DB，以 DB 为准修正 | — | □ |
| `groupbuy.team-timeout` | 每 5 分钟 | 拼团超时：未成团队伍自动退款 | bizId 幂等 | □ |
| `coupon.expire-scan` | 每日 0:10 | 券过期扫描：EXPIRED | userCouponId 幂等 | □ |
| `logistics.poll` | 每 30 分钟 | 物流轮询：未签收订单查轨迹 | trackingNo 幂等 | □ |
| `order.auto-confirm` | 每 10 分钟 | 自动确认收货：签收 N 天后 DEAL_DONE | 状态机校验 | □ |
| `message.send-compensate` | 每 1 分钟 | 通知补偿：notify_task 重试 | uuid 幂等 | □ |

---

## 9. 监控与告警约定

- **RocketMQ**：监控 Broker 状态、Topic 积压（`rocketmq_broker_*` / Dashboard）、消费组消费进度（`%CONSUME%` 积压）、重试/死信队列消息数；积压超阈值告警。
- **Kafka**：监控 `kafka_server_brokertopicmetrics_*`、消费组 lag（`kafka_consumergroup_lag`）。
- 任务：监控 XXL-Job 失败次数、执行耗时；连续失败告警。
- 指标：Prometheus 采集，Grafana 面板分组（RocketMQ / Kafka / 任务 / 业务）。
- 日志：消息轨迹（messageId 贯穿 producer/consumer）进 ELK，支持按 messageId 检索全链路。

---

## 10. 开发检查清单

- [ ] Topic 遵循 `<域>.<事件>:<Tag>` 语义化命名，全小写（Tag 全大写）
- [ ] 消费组命名 `cg.<业务域>`，一业务一消费组，不共用
- [ ] 业务事件走 RocketMQ，日志/埋点走 Kafka，职责不混
- [ ] 消息体使用统一结构（messageId/topic/tag/bizType/payload/occurTime）
- [ ] 事务提交后才发 MQ（或事务消息）；失败入 mq_task 补偿
- [ ] 消费者首行幂等去重，失败 RECONSUME_LATER 重试 ≤ 3 次进死信
- [ ] 定时任务统一接入 XXL-Job，命名 `模块.动作`
- [ ] 任务可重跑（幂等），失败告警，留执行日志
- [ ] 新增 Topic/任务登记到第 5/8 节清单

---

*MQ Topic 与定时任务规范 v2.0 — 2026-08-11，源自《设计文档-SDS》§10.6 与 ADR-10 技术选型*
