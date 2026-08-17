package cn.unimarket.api.order.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细 VO。
 */
public class OrderItemVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String skuId;
    private String productId;
    private Integer quantity;
    /** 原始单价快照（元） */
    private BigDecimal originalPrice;
    /** 实际单价（元，扣除优惠分摊后） */
    private BigDecimal actualPrice;

    public OrderItemVO() {
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }

    public void setActualPrice(BigDecimal actualPrice) {
        this.actualPrice = actualPrice;
    }
}
