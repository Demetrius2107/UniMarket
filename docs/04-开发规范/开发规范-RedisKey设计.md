# Redis Key 设计规范 — UniMarket

> 适用于 UniMarket 统一商城前台的 Redis 键命名与数据结构约定。
> 目标：全局唯一可读、按业务域分组、TTL 有据可查、运维可批量操作。
> 版本：v1.0 ｜ 2026-08-10

---

## 目录

1. [总体原则](#1-总体原则)
2. [Key 命名规范](#2-key-命名规范)
3. [数据结构选型约定](#3-数据结构选型约定)
4. [TTL 约定](#4-ttl-约定)
5. [业务域 Key 清单（模板）](#5-业务域-key-清单模板)
6. [分布式锁约定](#6-分布式锁约定)
7. [序列化与编码约定](#7-序列化与编码约定)
8. [开发检查清单](#8-开发检查清单)

---

## 1. 总体原则

| 原则 | 说明 |
|------|------|
| 全局限一 | 所有 key 遵循同一命名格式，杜绝随意拼接 |
| 业务域分组 | key 第一段为业务域，便于按前缀批量管理/监控 |
| 语义自明 | 看到 key 即可判断：哪个域、哪个实体、什么标识 |
| 明确过期 | 每个 key 必须明确 TTL（或注明长期有效的原因） |
| 禁止裸值 | 一律带业务前缀，禁止 `a:1`、`tmp123` 这类无意义 key |
| 容量可控 | 大集合（如购物车、消息）按用户维度拆分，禁止无界增长 |

**命名基调**：`域:实体:标识[:子标识]`，冒号 `:` 分隔，全部小写，多词用连字符 `-`。

---

## 2. Key 命名规范

### 2.1 通用格式

```
<域>:<实体>:<标识>[:<子标识>]
```

| 段 | 说明 | 示例 |
|----|------|------|
| 域 | 业务域缩写（下表） | `user` / `cart` / `order` |
| 实体 | 该域下的对象 | `ticket` / `stock` / `lock` |
| 标识 | 对象 ID（userId/skuId/activityId 等） | `u10001` / `sku2001` |
| 子标识 | 可选细分（日期/字段） | `20260810` / `hash` |

### 2.2 业务域缩写表

| 域缩写 | 业务域 | 示例 key |
|--------|--------|----------|
| `user` | 用户/登录 | `user:ticket:{ticketId}` |
| `cart` | 购物车 | `cart:{userId}` |
| `product` | 商品/SKU | `product:detail:{productId}` |
| `stock` | 库存 | `stock:activity:{activityId}` |
| `order` | 订单 | `order:pay:{orderId}` |
| `lottery` | 抽奖 | `lottery:strategy:{strategyId}` |
| `groupbuy` | 拼团 | `groupbuy:team:{teamId}` |
| `credit` | 积分 | `credit:account:{userId}` |
| `coupon` | 优惠券 | `coupon:user:{userId}` |
| `risk` | 风控 | `risk:blacklist:{userId}` |
| `msg` | 消息 | `msg:unread:{userId}` |
| `idempotent` | 幂等 | `idempotent:order:{bizId}` |
| `lock` | 分布式锁 | `lock:stock:{activityId}` |
| `rate` | 限流计数 | `rate:login:{ticketId}` |

### 2.3 禁止事项

- ❌ 大小写混用：`cart:User:001`
- ❌ 无域前缀：`token123`
- ❌ 使用空格/中文：`cart:张三`
- ❌ 未定 TTL 的大 key：`order:list`（应拆分或仅短期缓存）
- ❌ key 直接拼接用户输入（防注入/超长）：统一在 Service 层构造

---

## 3. 数据结构选型约定

| 类型 | 适用场景 | 本系统示例 |
|------|----------|------------|
| String | 单值、计数、简单缓存 | `product:detail:{id}`（JSON 串）、`stock:activity:{id}`（INCR 计数） |
| Hash | 对象/多字段 | `cart:{userId}`（skuId → {qty,selected,addTime}） |
| List | 有序队列/消息 | 备用；正式异步走 RabbitMQ |
| Set | 去重集合 | `risk:blacklist:{userId}`、`coupon:user:{userId}:used` |
| ZSet | 排序/榜单 | `product:hot`（销量排序）、`msg:list:{userId}`（时间排序，可选） |

**选型判断**：
- 需要"整体读写对象" → String(JSON) 或 Hash
- 需要"成员级增删改查" → Hash/Set/ZSet
- 需要"原子自增扣减" → String INCR/DECR
- 需要"过期后自动失效" → 全部依赖 TTL

---

## 4. TTL 约定

| 场景 | 建议 TTL | 说明 |
|------|----------|------|
| 登录 ticket 映射 | 5 分钟 | 与微信扫码轮询窗口一致 |
| 登录限流计数 | 60 秒 | 滑动窗口粒度 |
| 商品详情缓存 | 10 分钟 | 可短可长，价格敏感场景缩短 |
| 购物车热数据 | 30 天 | 与 SRS 冷备份策略一致（Redis 失手后 MySQL 恢复） |
| 库存计数 | 活动结束时间 | `endTime - now()`，配合分布式锁自动释放 |
| 幂等键 | 24 小时 | 覆盖回调/重试窗口即可 |
| 黑名单 | 24 小时起 | 自动拉黑 24h；手动拉黑可更长 |
| 活动预热数据 | 活动期间 | armory 装配后常驻至活动结束 |

**约定**：所有 key 在代码中必须通过常量/工具类集中定义（如 `RedisKeyBuilder`），TTL 与 key 格式同一处维护，禁止散落字符串。

---

## 5. 业务域 Key 清单（模板）

> 开发阶段按此模板逐模块登记，随开发补充完整。

| Key | 类型 | 结构/值 | TTL | 用途 | 状态 |
|-----|------|---------|-----|------|------|
| `user:ticket:{ticketId}` | String | `openId` | 5min | 扫码登录轮询 | □ |
| `user:token:{userId}` | String | JWT（可选服务端校验） | 7d | 会话 | □ |
| `cart:{userId}` | Hash | skuId → `{qty,selected,addTime}` | 30d | 购物车热数据 | □ |
| `cart:device:{deviceId}` | Hash | 同上 | 30d | 游客购物车 | □ |
| `product:detail:{productId}` | String | JSON | 10min | 商品详情缓存 | □ |
| `stock:activity:{activityId}` | String | INCR 计数 | 活动结束 | 活动库存扣减 | □ |
| `lock:stock:{activityId}` | String | `1` | 活动结束 | 库存分布式锁 | □ |
| `lottery:strategy:{strategyId}` | String | 奖品概率数组/JSON | 活动期间 | 抽奖装配预热 | □ |
| `groupbuy:team:{teamId}` | String | 队伍 JSON | 拼团过期 | 队伍进度缓存 | □ |
| `credit:account:{userId}` | String | JSON（可用/冻结） | 30min | 积分账户缓存 | □ |
| `risk:blacklist:{userId}` | Set | 拉黑原因 | 24h+ | 风控黑名单 | □ |
| `rate:login:{ticketId}` | String | 计数 | 60s | 登录轮询限流 | □ |
| `idempotent:order:{bizId}` | String | `1` | 24h | 下单幂等 | □ |
| `msg:unread:{userId}` | String | 未读数 | 7d | 消息未读计数 | □ |

---

## 6. 分布式锁约定

- 统一使用 Redisson `RLock`（看门狗续期）或 SETNX + 显式 TTL，禁止裸 SETNX 不设过期。
- 锁 key 格式：`lock:<业务>:<资源ID>`。
- 锁粒度：优先小粒度（`lock:stock:{activityId}:{awardId}`），避免全局锁。
- 锁内只做临界区操作（如扣减/判断），**禁止锁内做 IO 重操作**（MQ、外部调用移出锁外）。
- 释放：finally 中释放；活动结束场景 TTL 设 `endTime - now()` 自动失效。

```java
RLock lock = redissonClient.getLock("lock:stock:" + activityId);
boolean acquired = lock.tryLock(3, TimeUnit.SECONDS); // 等待3s
if (!acquired) {
    throw new BizException(ErrorCode.TOO_MANY_REQUESTS);
}
try {
    // 临界区：库存扣减/成团判断
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

---

## 7. 序列化与编码约定

- 值统一 JSON（Jackson），日期格式 `yyyy-MM-dd HH:mm:ss`。
- RedisTemplate 统一配置 `StringRedisSerializer`（key）+ `GenericJackson2JsonRedisSerializer`（value），避免乱码。
- 禁止在 value 里存对象类型信息泄漏（`@class`）到业务字段；复杂对象显式 DTO 序列化。
- key 中的 ID 使用与 DB 一致的字符串 ID（雪花 ID），不用自增主键裸值。

---

## 8. 开发检查清单

- [ ] key 遵循 `域:实体:标识` 格式，全小写、冒号分隔
- [ ] key 集中在 `RedisKeyBuilder`/常量类维护，无散落字符串
- [ ] 每个 key 有明确 TTL（或注释说明长期有效的理由）
- [ ] 数据类型选型符合第 3 节约定
- [ ] 分布式锁设置了过期时间且 finally 释放
- [ ] 大集合按用户维度拆分，无无界 key
- [ ] 序列化配置统一，无乱码风险
- [ ] 敏感信息（Token 原文等）不落 Redis 明文（如需存则加密）

---

*Redis Key 设计规范 v1.0 — 2026-08-10，源自《设计文档-SDS》§7.1/§10.3*
