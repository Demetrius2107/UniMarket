package cn.unimarket.infrastructure.repository;

import cn.unimarket.domain.product.model.Category;
import cn.unimarket.domain.product.model.Product;
import cn.unimarket.domain.product.model.Sku;
import cn.unimarket.domain.product.repository.ProductRepository;
import cn.unimarket.infrastructure.dao.CategoryMapper;
import cn.unimarket.infrastructure.dao.ProductMapper;
import cn.unimarket.infrastructure.dao.SkuMapper;
import cn.unimarket.infrastructure.po.CategoryPO;
import cn.unimarket.infrastructure.po.ProductPO;
import cn.unimarket.infrastructure.po.SkuPO;
import cn.unimarket.types.enums.ProductStatus;
import cn.unimarket.types.enums.SkuStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 商品仓储实现。PO ↔ 聚合根转换在此完成，领域层不感知存储细节（依赖倒置）。
 * <p>商品详情查询时按 productId 装配 SKU 列表；列表查询不带 SKU 明细。
 */
@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepositoryImpl.class);

    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final CategoryMapper categoryMapper;

    public ProductRepositoryImpl(ProductMapper productMapper, SkuMapper skuMapper, CategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    public Optional<Product> findById(String productId) {
        ProductPO po = productMapper.selectOne(new LambdaQueryWrapper<ProductPO>()
                .eq(ProductPO::getProductId, productId));
        if (po == null) {
            return Optional.empty();
        }
        // 装配 SKU 列表
        List<SkuPO> skuPOs = skuMapper.selectList(new LambdaQueryWrapper<SkuPO>()
                .eq(SkuPO::getProductId, productId)
                .orderByAsc(SkuPO::getId));
        return Optional.of(toProduct(po, skuPOs));
    }

    @Override
    public List<Product> pageList(String categoryId, String status, int page, int size) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<ProductPO>()
                .orderByDesc(ProductPO::getSortOrder)
                .orderByDesc(ProductPO::getCreateTime);
        if (categoryId != null && !categoryId.isEmpty()) {
            wrapper.eq(ProductPO::getCategoryId, categoryId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ProductPO::getStatus, status);
        }
        Page<ProductPO> p = new Page<>(page, size);
        List<ProductPO> pos = productMapper.selectPage(p, wrapper).getRecords();
        // 列表不带 SKU 明细
        List<Product> products = new ArrayList<>(pos.size());
        for (ProductPO po : pos) {
            products.add(toProduct(po, List.of()));
        }
        return products;
    }

    @Override
    public long count(String categoryId, String status) {
        LambdaQueryWrapper<ProductPO> wrapper = new LambdaQueryWrapper<ProductPO>();
        if (categoryId != null && !categoryId.isEmpty()) {
            wrapper.eq(ProductPO::getCategoryId, categoryId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ProductPO::getStatus, status);
        }
        return productMapper.selectCount(wrapper);
    }

    @Override
    public List<Category> findAllCategories() {
        List<CategoryPO> pos = categoryMapper.selectList(new LambdaQueryWrapper<CategoryPO>()
                .eq(CategoryPO::getStatus, "ENABLE")
                .orderByAsc(CategoryPO::getSortOrder));
        List<Category> categories = new ArrayList<>(pos.size());
        for (CategoryPO po : pos) {
            categories.add(toCategory(po));
        }
        return categories;
    }

    // ---------- PO ↔ 领域对象转换 ----------

    private Product toProduct(ProductPO po, List<SkuPO> skuPOs) {
        Product product = new Product();
        product.setProductId(po.getProductId());
        product.setName(po.getName());
        product.setSubTitle(po.getSubTitle());
        product.setCategoryId(po.getCategoryId());
        product.setMainImage(po.getMainImage());
        product.setImages(po.getImages());
        product.setDetail(po.getDetail());
        product.setOriginalPrice(po.getOriginalPrice());
        product.setStatus(po.getStatus() == null ? null : ProductStatus.valueOf(po.getStatus()));
        product.setSortOrder(po.getSortOrder());
        product.setCreateTime(po.getCreateTime());
        product.setUpdateTime(po.getUpdateTime());

        Set<Sku> skus = new LinkedHashSet<>();
        for (SkuPO spo : skuPOs) {
            skus.add(toSku(spo));
        }
        product.setSkus(skus);
        return product;
    }

    private Sku toSku(SkuPO po) {
        return new Sku(po.getSkuId(), po.getProductId(), po.getSkuName(), po.getSkuAttrs(),
                po.getPrice(), po.getStock(), po.getImage(),
                po.getStatus() == null ? null : SkuStatus.valueOf(po.getStatus()));
    }

    private Category toCategory(CategoryPO po) {
        Category category = new Category();
        category.setCategoryId(po.getCategoryId());
        category.setParentId(po.getParentId());
        category.setCategoryName(po.getCategoryName());
        category.setLevel(po.getLevel());
        category.setSortOrder(po.getSortOrder());
        category.setIcon(po.getIcon());
        category.setStatus(po.getStatus());
        category.setCreateTime(po.getCreateTime());
        category.setUpdateTime(po.getUpdateTime());
        return category;
    }
}
