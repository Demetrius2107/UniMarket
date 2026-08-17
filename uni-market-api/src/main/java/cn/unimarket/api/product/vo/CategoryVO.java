package cn.unimarket.api.product.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品分类 VO。对应 SRS FR-PRODUCT-03：商品分类树。
 * <p>返回扁平列表，前端按 parentId/level 组装树形结构。
 */
public class CategoryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String categoryId;
    /** 父分类ID，根分类为 0 */
    private String parentId;
    private String categoryName;
    /** 层级：1一级/2二级/3三级 */
    private Integer level;
    private Integer sortOrder;
    private String icon;

    public CategoryVO() {
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
