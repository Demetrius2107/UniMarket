package cn.unimarket.domain.product.repository;

import cn.unimarket.domain.product.model.Category;
import cn.unimarket.domain.product.model.Product;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓储端口。领域层定义契约，基础设施层提供实现（依赖倒置）。
 * <p>Phase 1 单库单表；Phase 10 接入分库分表后实现切换为带路由的版本，领域层无感。
 */
public interface ProductRepository {

    /**
     * 按商品 ID 查询商品聚合（含 SKU 列表）。
     *
     * @param productId 商品 ID
     * @return 商品聚合，不存在返回 empty
     */
    Optional<Product> findById(String productId);

    /**
     * 分页查询在售商品列表（不含 SKU 明细，明细走详情接口）。
     *
     * @param categoryId 分类筛选，null 查全部
     * @param status     状态筛选，null 查全部
     * @param page       页码（从 1 开始）
     * @param size       每页大小
     * @return 商品列表
     */
    List<Product> pageList(String categoryId, String status, int page, int size);

    /**
     * 统计商品总数（配合分页）。
     *
     * @param categoryId 分类筛选，null 查全部
     * @param status     状态筛选，null 查全部
     * @return 总数
     */
    long count(String categoryId, String status);

    /**
     * 查询全部分类（启用）。
     *
     * @return 分类列表
     */
    List<Category> findAllCategories();
}
