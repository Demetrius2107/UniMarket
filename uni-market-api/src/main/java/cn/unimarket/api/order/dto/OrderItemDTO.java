package cn.unimarket.api.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单明细请求项。
 */
public class OrderItemDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 商品 ID */
    @NotBlank(message = "商品ID不能为空")
    private String productId;

    /** SKU ID */
    @NotBlank(message = "SKU ID不能为空")
    private String skuId;

    /** 购买数量，单位件 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于0")
    private Integer quantity;

    /** 下单时单价快照（元），用于服务端校验价格是否变动（BR-04） */
    @NotNull(message = "单价不能为空")
    private BigDecimal unitPrice;

    public OrderItemDTO() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
