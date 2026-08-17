package cn.unimarket.types.event;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 领域事件基类。领域内产生的事件，跨模块解耦用。
 * <p>事件发布走「本地消息表 + MQ + 定时补偿」三层保障（SDS §10.6），保证最终一致。
 * <p>这是领域语义层的事件契约，与具体 MQ 协议解耦：基础设施层负责把它投递到 RocketMQ/Kafka。
 */
public abstract class DomainEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 事件发生时间 */
    protected final LocalDateTime occurredOn;

    /** 事件唯一 ID，用于消费端幂等 */
    protected final String eventId;

    protected DomainEvent(String eventId, LocalDateTime occurredOn) {
        this.eventId = eventId;
        this.occurredOn = occurredOn == null ? LocalDateTime.now() : occurredOn;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getEventId() {
        return eventId;
    }

    /**
     * 事件类型标识，用于 MQ topic 路由与消费分发。
     */
    public abstract String eventType();
}
