package cn.unimarket.domain.product.model;

import cn.unimarket.types.enums.ProductStatus;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 商品聚合根。见 SDS §6.3。
 * <p>聚合内一致性：商品 SPU 与其 SKU 列表作为一个整体持久化与读取。
 * <p>上下架状态流转唯一入口为 {@link #shelve} / {@link #unShelve}，禁止外部直接 setStatus。
 * <p>Phase 1 仅支持读侧（列表/详情）+ 上下架；ERP 侧的创建/编辑在 Phase 11 补齐。
 */
public class Product implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String productId;
    private String name;
    private String subTitle;
    private String categoryId;
    private String mainImage;
    /** 轮播图 URL 列表，逗号分隔 */
    private String images;
    private String detail;
    /** 原价（元，展示用，实际售价以 SKU 为准） */
    private BigDecimal originalPrice;
    private ProductStatus status;
    private Integer sortOrder;

    /** SKU 集合（去重，按 skuId） */
    private Set<Sku> skus = new LinkedHashSet<>();

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 无参构造，供仓储重建用 */
    public Product() {
    }

    /**
     * 查询指定 SKU。
     *
     * @param skuId SKU ID
     * @return SKU，不存在返回 null
     */
    public Sku findSku(String skuId) {
        for (Sku sku : skus) {
            if (sku.getSkuId().equals(skuId)) {
                return sku;
            }
        }
        return null;
    }

    /**
     * 取指定 SKU，不存在抛业务异常。
     */
    public Sku requireSku(String skuId) {
        Sku sku = findSku(skuId);
        if (sku == null) {
            throw new BizException(ErrorCode.SKU_NOT_FOUND, "SKU不存在: " + skuId);
        }
        return sku;
    }

    /**
     * 商品是否在售。
     */
    public boolean onShelf() {
        return status == ProductStatus.ON_SHELF;
    }

    /**
     * 上架（OFF_SHELF → ON_SHELF）。
     */
    public void shelve() {
        this.status = ProductStatus.ON_SHELF;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 下架（ON_SHELF → OFF_SHELF）。下架商品不可下单。
     */
    public void unShelve() {
        this.status = ProductStatus.OFF_SHELF;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * SKU 只读视图。
     */
    public List<Sku> skuList() {
        return Collections.unmodifiableList(new ArrayList<>(skus));
    }

    // ---------- getter / setter ----------

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

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Set<Sku> getSkus() {
        return skus;
    }

    public void setSkus(Set<Sku> skus) {
        this.skus = skus == null ? new LinkedHashSet<>() : skus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}
