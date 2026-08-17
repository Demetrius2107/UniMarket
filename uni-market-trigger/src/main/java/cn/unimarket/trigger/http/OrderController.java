package cn.unimarket.trigger.http;

import cn.unimarket.api.order.dto.OrderCreateRequest;
import cn.unimarket.api.order.dto.OrderListRequest;
import cn.unimarket.api.order.vo.OrderCreateVO;
import cn.unimarket.api.order.vo.OrderDetailVO;
import cn.unimarket.api.order.vo.OrderListVO;
import cn.unimarket.types.constant.Constants;
import cn.unimarket.trigger.app.OrderAppService;
import cn.unimarket.types.common.PageResult;
import cn.unimarket.types.common.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单 HTTP 接口。见 SDS §8.3。
 * <p>路径前缀 {@code /api/v1/order/}（接口规范§2）。
 * <p>Controller 只做协议转换与参数校验，业务逻辑在 {@link OrderAppService}。
 * <p>Phase 1 鉴权简化：userId 从请求头 X-User-Id 取（TODO: Phase 1 后期接入 JWT 拦截器注入）。
 */
@Tag(name = "订单服务", description = "下单、查询、取消")
@RestController
@RequestMapping(Constants.API_PREFIX + "/order")
public class OrderController {

    private final OrderAppService orderAppService;

    public OrderController(OrderAppService orderAppService) {
        this.orderAppService = orderAppService;
    }

    @Operation(summary = "创建订单", description = "支持批量下单；Phase 1 普通下单，不含券/积分抵扣")
    @PostMapping("/create")
    public Response<OrderCreateVO> createOrder(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @Valid @RequestBody OrderCreateRequest request) {
        OrderCreateVO vo = orderAppService.createOrder(userId, request);
        return Response.success(vo);
    }

    @Operation(summary = "订单详情", description = "含订单明细，校验归属")
    @GetMapping("/detail")
    public Response<OrderDetailVO> queryDetail(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @Parameter(description = "订单ID", required = true)
            @RequestParam @NotBlank String orderId) {
        OrderDetailVO vo = orderAppService.queryDetail(userId, orderId);
        return Response.success(vo);
    }

    @Operation(summary = "订单列表", description = "按状态分页查询")
    @GetMapping("/list")
    public Response<PageResult<OrderListVO>> queryList(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            OrderListRequest request) {
        PageResult<OrderListVO> result = orderAppService.queryList(userId, request);
        return Response.success(result);
    }

    @Operation(summary = "取消订单", description = "仅未支付订单可取消，回滚库存")
    @PostMapping("/cancel/{orderId}")
    public Response<Void> cancel(
            @RequestHeader("X-User-Id") @NotBlank String userId,
            @PathVariable @NotBlank String orderId) {
        orderAppService.cancel(userId, orderId);
        return Response.success();
    }
}
