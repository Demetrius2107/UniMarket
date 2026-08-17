package cn.unimarket.domain.product.model;

import cn.unimarket.types.enums.SkuStatus;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 商品 SKU 值对象。不可变，归属于 {@link Product} 聚合根。
 * <p>库存基准值由仓储维护，扣减走 Redis INCR + DB 行锁条件更新（SDS §10.3）。
 * <p>金额精度遵循 BR-03：BigDecimal，HALF_UP，分位 {@link cn.unimarket.types.constant.Constants#MONEY_SCALE}。
 */
public final class Sku implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String skuId;
    private final String productId;
    private final String skuName;
    /** 规格属性 JSON，如 {"颜色":"红","尺码":"L"} */
    private final String skuAttrs;
    /** 销售单价（元） */
    private final BigDecimal price;
    /** DB 基准库存 */
    private final Integer stock;
    private final String image;
    private final SkuStatus status;

    public Sku(String skuId, String productId, String skuName, String skuAttrs,
               BigDecimal price, Integer stock, String image, SkuStatus status) {
        this.skuId = Objects.requireNonNull(skuId, "skuId不能为空");
        this.productId = Objects.requireNonNull(productId, "productId不能为空");
        this.skuName = skuName;
        this.skuAttrs = skuAttrs;
        this.price = Objects.requireNonNull(price, "price不能为空");
        this.stock = stock == null ? 0 : stock;
        this.image = image;
        this.status = status == null ? SkuStatus.ENABLE : status;
    }

    /**
     * SKU 是否可售：启用且有库存。
     */
    public boolean sellable() {
        return status == SkuStatus.ENABLE && stock > 0;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getProductId() {
        return productId;
    }

    public String getSkuName() {
        return skuName;
    }

    public String getSkuAttrs() {
        return skuAttrs;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStock() {
        return stock;
    }

    public String getImage() {
        return image;
    }

    public SkuStatus getStatus() {
        return status;
    }
}
