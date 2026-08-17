package cn.unimarket.api.order.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建订单响应。返回订单号与试算金额，前端凭 orderId 进入支付流程。
 */
public class OrderCreateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID（雪花） */
    private String orderId;

    /** 订单总金额（元） */
    private BigDecimal totalAmount;

    /** 实付金额（元），Phase 1 等于 totalAmount */
    private BigDecimal payAmount;

    /** 订单状态 */
    private String status;

    public OrderCreateVO() {
    }

    public OrderCreateVO(String orderId, BigDecimal totalAmount, BigDecimal payAmount, String status) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.payAmount = payAmount;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
