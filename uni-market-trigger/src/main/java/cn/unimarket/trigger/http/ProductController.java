package cn.unimarket.trigger.http;

import cn.unimarket.api.product.dto.ProductListRequest;
import cn.unimarket.api.product.vo.CategoryVO;
import cn.unimarket.api.product.vo.ProductDetailVO;
import cn.unimarket.api.product.vo.ProductListVO;
import cn.unimarket.types.common.PageResult;
import cn.unimarket.types.common.Response;
import cn.unimarket.types.constant.Constants;
import cn.unimarket.trigger.app.ProductAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品 HTTP 接口。见 SDS §8.2。
 * <p>路径前缀 {@code /api/v1/product/}（接口规范§2）。
 * <p>Controller 只做协议转换与参数校验，业务逻辑在 {@link ProductAppService}。
 * <p>商品浏览为公开接口，无需鉴权（游客可浏览，见 SRS 角色定义）。
 */
@Tag(name = "商品服务", description = "商品列表、详情、分类")
@RestController
@RequestMapping(Constants.API_PREFIX + "/product")
public class ProductController {

    private final ProductAppService productAppService;

    public ProductController(ProductAppService productAppService) {
        this.productAppService = productAppService;
    }

    @Operation(summary = "商品列表", description = "分页查询在售商品，支持分类筛选")
    @GetMapping("/list")
    public Response<PageResult<ProductListVO>> queryList(ProductListRequest request) {
        PageResult<ProductListVO> result = productAppService.queryList(request);
        return Response.success(result);
    }

    @Operation(summary = "商品详情", description = "含 SKU 列表、库存、价格")
    @GetMapping("/detail")
    public Response<ProductDetailVO> queryDetail(
            @Parameter(description = "商品ID", required = true)
            @RequestParam @NotBlank String productId) {
        ProductDetailVO vo = productAppService.queryDetail(productId);
        return Response.success(vo);
    }

    @Operation(summary = "分类树", description = "返回启用分类扁平列表，前端按 parentId 组装树")
    @GetMapping("/categories")
    public Response<List<CategoryVO>> queryCategories() {
        List<CategoryVO> list = productAppService.queryCategories();
        return Response.success(list);
    }
}
