package cn.unimarket.trigger.app;

import cn.unimarket.api.product.dto.ProductListRequest;
import cn.unimarket.api.product.vo.CategoryVO;
import cn.unimarket.api.product.vo.ProductDetailVO;
import cn.unimarket.api.product.vo.ProductListVO;
import cn.unimarket.api.product.vo.SkuVO;
import cn.unimarket.domain.product.model.Category;
import cn.unimarket.domain.product.model.Product;
import cn.unimarket.domain.product.model.Sku;
import cn.unimarket.domain.product.service.ProductService;
import cn.unimarket.types.common.PageResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品应用服务。应用层职责：流程编排 + VO 转换（SDS §3.2）。
 * <p>读侧场景无事务；查询逻辑在领域服务，VO 转换在此层。
 */
@Service
public class ProductAppService {

    private final ProductService productService;

    public ProductAppService(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 分页查询在售商品列表。
     */
    public PageResult<ProductListVO> queryList(ProductListRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : Math.min(request.getSize(), 100);
        List<Product> products = productService.queryList(request.getCategoryId(), null, page, size);
        long total = productService.count(request.getCategoryId(), null);
        List<ProductListVO> list = new ArrayList<>(products.size());
        for (Product p : products) {
            list.add(toListVO(p));
        }
        return new PageResult<>(total, page, size, list);
    }

    /**
     * 查询商品详情（含 SKU 列表）。
     */
    public ProductDetailVO queryDetail(String productId) {
        Product product = productService.queryDetail(productId);
        return toDetailVO(product);
    }

    /**
     * 查询分类树（扁平列表，前端组装树）。
     */
    public List<CategoryVO> queryCategories() {
        List<Category> categories = productService.queryCategories();
        List<CategoryVO> list = new ArrayList<>(categories.size());
        for (Category c : categories) {
            list.add(toCategoryVO(c));
        }
        return list;
    }

    // ---------- VO 转换 ----------

    private ProductListVO toListVO(Product product) {
        ProductListVO vo = new ProductListVO();
        vo.setProductId(product.getProductId());
        vo.setName(product.getName());
        vo.setSubTitle(product.getSubTitle());
        vo.setMainImage(product.getMainImage());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStatus(product.getStatus() == null ? null : product.getStatus().name());
        // 列表不带 SKU 明细（明细走详情），最低售价无数据时回退到原价
        BigDecimal minPrice = null;
        for (Sku sku : product.skuList()) {
            if (minPrice == null || sku.getPrice().compareTo(minPrice) < 0) {
                minPrice = sku.getPrice();
            }
        }
        vo.setMinPrice(minPrice != null ? minPrice : product.getOriginalPrice());
        return vo;
    }

    private ProductDetailVO toDetailVO(Product product) {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setProductId(product.getProductId());
        vo.setName(product.getName());
        vo.setSubTitle(product.getSubTitle());
        vo.setCategoryId(product.getCategoryId());
        vo.setMainImage(product.getMainImage());
        vo.setImages(product.getImages());
        vo.setDetail(product.getDetail());
        vo.setOriginalPrice(product.getOriginalPrice());
        vo.setStatus(product.getStatus() == null ? null : product.getStatus().name());
        List<SkuVO> skus = new ArrayList<>();
        for (Sku sku : product.skuList()) {
            skus.add(toSkuVO(sku));
        }
        vo.setSkus(skus);
        return vo;
    }

    private SkuVO toSkuVO(Sku sku) {
        SkuVO vo = new SkuVO();
        vo.setSkuId(sku.getSkuId());
        vo.setProductId(sku.getProductId());
        vo.setSkuName(sku.getSkuName());
        vo.setSkuAttrs(sku.getSkuAttrs());
        vo.setPrice(sku.getPrice());
        vo.setStock(sku.getStock());
        vo.setImage(sku.getImage());
        vo.setStatus(sku.getStatus() == null ? null : sku.getStatus().name());
        return vo;
    }

    private CategoryVO toCategoryVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setParentId(category.getParentId());
        vo.setCategoryName(category.getCategoryName());
        vo.setLevel(category.getLevel());
        vo.setSortOrder(category.getSortOrder());
        vo.setIcon(category.getIcon());
        return vo;
    }
}
