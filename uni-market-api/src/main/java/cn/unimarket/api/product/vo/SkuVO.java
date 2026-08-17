package cn.unimarket.api.product.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * SKU 视图对象。商品详情接口返回的 SKU 列表项。
 * <p>对应 SRS FR-PRODUCT-02：商品信息 + SKU 列表 + 库存 + 价格。
 */
public class SkuVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String skuId;
    private String productId;
    private String skuName;
    /** 规格属性 JSON，如 {"颜色":"红","尺码":"L"} */
    private String skuAttrs;
    /** 销售单价（元） */
    private BigDecimal price;
    /** 剩余库存 */
    private Integer stock;
    /** SKU 图片URL */
    private String image;
    /** 启用状态 ENABLE/DISABLE */
    private String status;

    public SkuVO() {
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

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public String getSkuAttrs() {
        return skuAttrs;
    }

    public void setSkuAttrs(String skuAttrs) {
        this.skuAttrs = skuAttrs;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
