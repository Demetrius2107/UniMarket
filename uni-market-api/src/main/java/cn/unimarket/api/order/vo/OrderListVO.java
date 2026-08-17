package cn.unimarket.api.order.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表项 VO。列表场景只返回摘要，明细走详情接口。
 */
public class OrderListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String orderId;
    private String orderType;
    private String status;

    /** 实付金额（元） */
    private BigDecimal payAmount;

    /** 首件商品名（列表展示用，冗余快照） */
    private String firstItemName;
    /** 明细总数 */
    private Integer itemCount;

    private LocalDateTime createTime;
    private LocalDateTime payTime;

    public OrderListVO() {
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public String getFirstItemName() {
        return firstItemName;
    }

    public void setFirstItemName(String firstItemName) {
        this.firstItemName = firstItemName;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }
}
