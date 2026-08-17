package cn.unimarket.trigger.app;

import cn.unimarket.api.order.dto.OrderCreateRequest;
import cn.unimarket.api.order.dto.OrderListRequest;
import cn.unimarket.api.order.vo.OrderCreateVO;
import cn.unimarket.api.order.vo.OrderDetailVO;
import cn.unimarket.api.order.vo.OrderItemVO;
import cn.unimarket.api.order.vo.OrderListVO;
import cn.unimarket.domain.order.model.Order;
import cn.unimarket.domain.order.model.OrderItem;
import cn.unimarket.domain.order.service.OrderService;
import cn.unimarket.types.common.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 订单应用服务。应用层职责：流程编排 + 事务边界 + VO 转换（SDS §3.2）。
 * <p>领域规则不写在此处；事务由本层包裹（领域层保持纯净，不依赖 Spring）。
 */
@Service
public class OrderAppService {

    private final OrderService orderService;

    public OrderAppService(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 创建订单。事务边界覆盖「扣库存 + 建单」，任一失败整体回滚（SRS BR-05）。
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderCreateVO createOrder(String userId, OrderCreateRequest request) {
        Order order = orderService.createOrder(userId, request);
        return new OrderCreateVO(order.getOrderId(), order.getTotalAmount(),
                order.getPayAmount(), order.getStatus().name());
    }

    /**
     * 查询订单详情。
     */
    public OrderDetailVO queryDetail(String userId, String orderId) {
        Order order = orderService.queryDetail(userId, orderId);
        return toDetailVO(order);
    }

    /**
     * 分页查询订单列表。
     */
    public PageResult<OrderListVO> queryList(String userId, OrderListRequest request) {
        int page = request.getPage() == null || request.getPage() < 1 ? 1 : request.getPage();
        int size = request.getSize() == null || request.getSize() < 1 ? 10 : Math.min(request.getSize(), 100);
        List<Order> orders = orderService.queryList(userId, request.getStatus(), page, size);
        long total = orderService.countByUser(userId, request.getStatus());
        List<OrderListVO> list = new ArrayList<>(orders.size());
        for (Order o : orders) {
            list.add(toListVO(o));
        }
        return new PageResult<>(total, page, size, list);
    }

    /**
     * 取消未支付订单。事务边界覆盖「状态流转 + 库存回滚」。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String userId, String orderId) {
        orderService.cancel(userId, orderId);
    }

    // ---------- VO 转换 ----------

    private OrderDetailVO toDetailVO(Order order) {
        OrderDetailVO vo = new OrderDetailVO();
        vo.setOrderId(order.getOrderId());
        vo.setUserId(order.getUserId());
        vo.setOrderType(order.getOrderType() == null ? null : order.getOrderType().name());
        vo.setStatus(order.getStatus() == null ? null : order.getStatus().name());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setPayChannel(order.getPayChannel() == null ? null : order.getPayChannel().name());
        vo.setOutTradeNo(order.getOutTradeNo());
        vo.setPayTime(order.getPayTime());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        List<OrderItemVO> items = new ArrayList<>();
        for (OrderItem item : order.itemList()) {
            OrderItemVO iv = new OrderItemVO();
            iv.setSkuId(item.getSkuId());
            iv.setProductId(item.getProductId());
            iv.setQuantity(item.getQuantity());
            iv.setOriginalPrice(item.getOriginalPrice());
            iv.setActualPrice(item.getActualPrice());
            items.add(iv);
        }
        vo.setItems(items);
        return vo;
    }

    private OrderListVO toListVO(Order order) {
        OrderListVO vo = new OrderListVO();
        vo.setOrderId(order.getOrderId());
        vo.setOrderType(order.getOrderType() == null ? null : order.getOrderType().name());
        vo.setStatus(order.getStatus() == null ? null : order.getStatus().name());
        vo.setPayAmount(order.getPayAmount());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        return vo;
    }
}
