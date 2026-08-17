package cn.unimarket.api.product.vo;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商品详情 VO。对应 SRS FR-PRODUCT-02：商品信息 + SKU 列表 + 库存 + 价格。
 * <p>下架商品仍可查详情（用于历史订单查看），status 字段供前端展示「已下架」标识。
 */
public class ProductDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String productId;
    private String name;
    private String subTitle;
    private String categoryId;
    private String mainImage;
    /** 轮播图URL列表，逗号分隔 */
    private String images;
    /** 富文本详情 */
    private String detail;
    /** 原价（元，展示用） */
    private BigDecimal originalPrice;
    /** 上下架状态 ON_SHELF/OFF_SHELF */
    private String status;
    /** SKU 列表 */
    private List<SkuVO> skus;

    public ProductDetailVO() {
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

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        this.mainImage = mainImage;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<SkuVO> getSkus() {
        return skus;
    }

    public void setSkus(List<SkuVO> skus) {
        this.skus = skus;
    }
}
