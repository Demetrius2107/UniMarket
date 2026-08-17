package cn.unimarket.api.product.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品列表项 VO。列表场景只返回摘要，详情走详情接口。
 * <p>对应 SRS FR-PRODUCT-01：分页查询，支持分类筛选。
 */
public class ProductListVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String productId;
    private String name;
    /** 副标题/卖点 */
    private String subTitle;
    /** 主图URL */
    private String mainImage;
    /** 原价（元，展示用，实际售价以 SKU 为准） */
    private BigDecimal originalPrice;
    /** 最低 SKU 售价（元，前端展示「起」价） */
    private BigDecimal minPrice;
    /** 上下架状态 ON_SHELF/OFF_SHELF */
    private String status;

    public ProductListVO() {
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
