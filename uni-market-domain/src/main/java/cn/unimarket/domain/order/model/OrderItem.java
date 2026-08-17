package cn.unimarket.domain.order.model;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 订单明细值对象。不可变，归属于 {@link Order} 聚合根。
 * <p>金额精度见 BR-03：BigDecimal，HALF_UP，分位 {@link cn.unimarket.types.constant.Constants#MONEY_SCALE}。
 */
public final class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String productId;
    private final String skuId;
    private final Integer quantity;
    /** 下单时单价快照（元） */
    private final BigDecimal originalPrice;
    /** 实际单价（元，优惠分摊后） */
    private final BigDecimal actualPrice;

    public OrderItem(String productId, String skuId, Integer quantity, BigDecimal originalPrice, BigDecimal actualPrice) {
        this.productId = Objects.requireNonNull(productId, "productId不能为空");
        this.skuId = Objects.requireNonNull(skuId, "skuId不能为空");
        this.quantity = Objects.requireNonNull(quantity, "quantity不能为空");
        if (quantity < 1) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        this.originalPrice = Objects.requireNonNull(originalPrice, "originalPrice不能为空");
        this.actualPrice = actualPrice == null ? originalPrice : actualPrice;
    }

    /**
     * 计算明细行小计 = 实际单价 × 数量。
     */
    public BigDecimal subtotal() {
        return actualPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 计算原始小计 = 原始单价 × 数量（用于统计优惠金额）。
     */
    public BigDecimal originalSubtotal() {
        return originalPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    public String getProductId() {
        return productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public BigDecimal getActualPrice() {
        return actualPrice;
    }
}
