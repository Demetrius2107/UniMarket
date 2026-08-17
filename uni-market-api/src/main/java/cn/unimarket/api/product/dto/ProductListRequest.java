package cn.unimarket.api.product.dto;

import java.io.Serial;
import java.io.Serializable;

/**
 * 商品列表查询请求。对应 {@code GET /api/v1/product/list}（FR-PRODUCT-01）。
 * <p>分页参数 page 从 1 开始，size 默认 10、上限 100（见接口规范§7）。
 */
public class ProductListRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分类筛选，可空（查全部） */
    private String categoryId;

    /** 页码，从 1 开始 */
    private Integer page = 1;

    /** 每页大小，默认 10，最大 100 */
    private Integer size = 10;

    public ProductListRequest() {
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
