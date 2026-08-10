# ADR-10：技术选型升级（JDK 21 + Spring Boot 3 现代栈）

- **状态**：已接受（Accepted）
- **日期**：2026-08-11
- **关联文档**：《设计文档-SDS》§5、ADR-01（MQ 选型修订）、ADR-02（分库分表策略修订）
- **影响范围**：全部模块（框架/ORM/中间件/部署方式）

## 背景

原 PRD/SDS 技术栈为 Java 8 + Spring Boot 2.7.x + MyBatis 原生 + RabbitMQ 单 MQ + Docker Compose。2026-08 评估后决定升级为现代主流栈：JDK 21 + Spring Boot 3.4.x + MyBatis-Plus + 双 MQ（RocketMQ 业务线 + Kafka 日志线）+ MinIO + K8s（生产）。

## 决策

| 层面 | 原方案 | 新方案 | 理由 |
|------|--------|--------|------|
| JDK | Java 8+ | **JDK 21（LTS）** | LTS 长期支持；虚拟线程等新特性；Boot 3 官方支持 |
| 框架 | Spring Boot 2.7.x | **Spring Boot 3.4.x** | 现代主流；jakarta 命名空间；安全与性能更新 |
| 构建 | Maven（未注明版本） | **Maven 3.9.x** | 与 Boot3/JDK21 兼容 |
| ORM | MyBatis-Spring-Boot 2.1.x | **MyBatis-Plus 3.5.5+**（`mybatis-plus-spring-boot3-starter`） | 内嵌 MyBatis，分页/条件构造器提升效率；Boot3 专用 starter |
| 数据库 | MySQL 8.0 | **MySQL 8.4 LTS** | LTS 版本，长期稳定 |
| 缓存 | Redis + Redisson | **Redis 7.2 + Redisson 3.27+** | Redisson 支持 Boot3；Redis 新版特性 |
| 对象存储 | 无 | **MinIO** | 商品图/售后凭证等文件存储，补齐存储能力 |
| 消息队列 | RabbitMQ 单 MQ | **双 MQ：RocketMQ 5.x（业务线）+ Kafka 3.7+（日志/埋点线）** | 业务事件 RocketMQ（事务/延迟/顺序消息强）；Kafka 只做日志埋点大数据线，职责分离 |
| RPC | Apache Dubbo 3.x | **Apache Dubbo 3.2+** | 保留 RPC 假拆分边界；3.2+ 支持 Boot3 |
| 注册/配置 | Nacos | **Nacos（保留）** | 注册 + 配置 + DCC 动态配置 |
| 搜索 | Elasticsearch + Canal | 不变 | 商品搜索 |
| 任务调度 | XXL-Job | 不变 | 分布式任务调度 |
| 限流熔断 | Guava RateLimiter + Sentinel | **Sentinel（可辅以 RateLimiter）** | 风控四级 |
| 接口文档 | 未定 | **Knife4j 4.4+（OpenAPI3 + Swagger，jakarta 版）** | Boot3 专用版本 |
| 可观测 | Prometheus + Grafana | **+ Micrometer Tracing + Zipkin** | 补链路追踪 |
| 部署 | Docker Compose | **Docker Compose（开发/演示）→ K8s（生产）** | 生产云原生 |

### 双 MQ 职责划分

```
RocketMQ 5.x（业务线）       Kafka 3.7+（日志/埋点线）
├── 订单支付成功事件           ├── 行为日志（浏览/点击/搜索）
├── 异步发奖                  ├── 埋点统计
├── 积分调整                  └── 大数据量流式处理
├── 拼团成团/退款
└── 通知消息
```

- 业务一致性消息走 RocketMQ（延迟/事务/顺序消息强，覆盖"本地消息表 + MQ 补偿"场景）。
- 日志/埋点类海量消息走 Kafka（吞吐高，与业务隔离）。

### 兼容性注意事项（Boot3 迁移）

- 命名空间 `javax.*` → `jakarta.*`，所有依赖必须使用 Boot3 兼容版本。
- MyBatis-Plus 分页插件与自研分库分表路由拦截器的执行顺序需专项验证。
- Knife4j 必须用 4.4+（`knife4j-openapi3-jakarta-spring-boot-starter`）。

## 对既有 ADR 的影响

| ADR | 影响 |
|-----|------|
| ADR-01（选 RabbitMQ 而非 Kafka） | **部分修订**：业务线改为 RocketMQ（保留"双 MQ 职责分离"思想）；RabbitMQ 不再作为主 MQ |
| ADR-02（保留分库分表 2×4） | **修订**：先单库单表跑通（Phase 1-9），Phase 10 数据量上来再分（见 ADR-02 演进说明） |

## 后果

- 正面：技术栈现代化，符合当前主流生态；Boot3 + JDK21 性能与安全更优；MP 提升开发效率；MinIO/K8s 补齐生产级能力。
- 负面：迁移成本（javax→jakarta、依赖版本重配）；学习成本增加；K8s 生产部署复杂度高（开发阶段仍用 Docker Compose）。

## 备选方案

- 维持原 Java 8 + Boot 2.7 栈：生态成熟但偏旧，长期维护与新特性缺失。
- 仅升 Boot3 不引入双 MQ/MinIO/K8s：复杂度低，但能力缺口仍在（对象存储、日志大数据线）。

## 演进

- Phase 1-9 用 Docker Compose 单库单表跑通业务；Phase 10 引入分库分表与 K8s 部署。
- 若 RocketMQ 运维成本过高，可回退 RabbitMQ（业务线接口已抽象，见 ADR-08 思路）。
