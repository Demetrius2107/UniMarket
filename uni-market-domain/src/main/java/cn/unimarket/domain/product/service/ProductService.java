package cn.unimarket.domain.product.service;

import cn.unimarket.domain.product.model.Category;
import cn.unimarket.domain.product.model.Product;
import cn.unimarket.domain.product.repository.ProductRepository;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 商品领域服务。见 SDS §6.10 领域服务清单。
 * <p>Phase 1 聚焦读侧：商品列表（分类筛选）、商品详情（含 SKU）、分类树。
 * <p>下单时的库存校验/扣减由 {@link SkuStockService} 负责，与下单流程解耦。
 */
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 查询商品详情（含 SKU 列表）。见 SRS FR-PRODUCT-02。
     * <p>下架商品仍可查详情（用于历史订单查看），但前端展示「已下架」标识。
     *
     * @param productId 商品 ID
     * @return 商品聚合（含 SKU）
     */
    public Product queryDetail(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BizException(ErrorCode.PRODUCT_NOT_FOUND, "商品不存在: " + productId));
    }

    /**
     * 分页查询商品列表。见 SRS FR-PRODUCT-01。
     * <p>默认只返回在售商品（status=ON_SHELF）；ERP 后台可传 status 查全部。
     *
     * @param categoryId 分类筛选，null 查全部
     * @param status     状态筛选，null 默认 ON_SHELF
     * @param page       页码（从 1 开始）
     * @param size       每页大小
     * @return 商品列表（不含 SKU 明细）
     */
    public List<Product> queryList(String categoryId, String status, int page, int size) {
        String queryStatus = status == null || status.isEmpty()
                ? cn.unimarket.types.enums.ProductStatus.ON_SHELF.name() : status;
        return productRepository.pageList(categoryId, queryStatus, page, size);
    }

    /**
     * 统计商品总数（配合分页）。
     */
    public long count(String categoryId, String status) {
        String queryStatus = status == null || status.isEmpty()
                ? cn.unimarket.types.enums.ProductStatus.ON_SHELF.name() : status;
        return productRepository.count(categoryId, queryStatus);
    }

    /**
     * 查询分类树。见 SRS FR-PRODUCT-03。
     *
     * @return 启用的分类列表（前端按 parent_id/level 组装树）
     */
    public List<Category> queryCategories() {
        return productRepository.findAllCategories();
    }
}
