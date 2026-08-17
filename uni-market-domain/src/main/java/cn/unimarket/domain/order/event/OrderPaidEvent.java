package cn.unimarket.domain.order.event;

import cn.unimarket.types.event.DomainEvent;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付成功事件。
 * <p>支付回调验签成功后由订单聚合产生，应用层负责投递到 MQ。
 * <p>★ 履约计算（结算中台）的入口事件 ★：结算域消费此事件，按分账规则算出各方金额。
 * <p>幂等：消费端按 {@link #orderId} 去重，同一订单重复投递不重复结算。
 */
public class OrderPaidEvent extends DomainEvent {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID */
    private final String orderId;
    /** 用户 ID（分片键） */
    private final String userId;
    /** 实付金额（元）—— 结算域算账的输入金额 */
    private final BigDecimal payAmount;
    /** 订单类型 NORMAL/GROUP_BUY/CREDIT */
    private final String orderType;
    /** 支付渠道 */
    private final String payChannel;
    /** 第三方支付单号 */
    private final String outTradeNo;
    /** 支付时间 */
    private final LocalDateTime payTime;

    public OrderPaidEvent(String eventId, String orderId, String userId, BigDecimal payAmount,
                          String orderType, String payChannel, String outTradeNo, LocalDateTime payTime) {
        super(eventId, payTime);
        this.orderId = orderId;
        this.userId = userId;
        this.payAmount = payAmount;
        this.orderType = orderType;
        this.payChannel = payChannel;
        this.outTradeNo = outTradeNo;
        this.payTime = payTime;
    }

    @Override
    public String eventType() {
        return "ORDER_PAID";
    }

    public String getOrderId() {
        return orderId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public String getOrderType() {
        return orderType;
    }

    public String getPayChannel() {
        return payChannel;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }
}
