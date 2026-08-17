package cn.unimarket.domain.order.service;

import cn.unimarket.api.order.dto.OrderCreateRequest;
import cn.unimarket.api.order.dto.OrderItemDTO;
import cn.unimarket.domain.order.model.Order;
import cn.unimarket.domain.order.model.OrderItem;
import cn.unimarket.domain.order.repository.OrderRepository;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 订单领域服务。见 SDS §6.10 领域服务清单。
 * <p>下单事务边界：锁券 + 冻积分 + 扣库存 + 建单 同一事务（SRS BR-05 / SDS §7.3）。
 * Phase 1 最小闭环只含「扣库存 + 建单」，券/积分在 Phase 5/6 接入时在此方法内补齐。
 * <p>幂等：userId+bizId 唯一约束兜底，重复提交返回原订单（接口规范§9）。
 */
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryGateway inventoryGateway;
    private final IdGenerator idGenerator;

    public OrderService(OrderRepository orderRepository, InventoryGateway inventoryGateway, IdGenerator idGenerator) {
        this.orderRepository = orderRepository;
        this.inventoryGateway = inventoryGateway;
        this.idGenerator = idGenerator;
    }

    /**
     * 创建订单（FR-ORDER-02）。
     * <p>幂等优先：先按 userId+bizId 查重，已存在直接返回原单，避免重复扣库存。
     *
     * @param userId 下单用户 ID（由鉴权层注入，不可信前端）
     * @param request 下单请求
     * @return 新建或已存在的订单聚合
     */
    public Order createOrder(String userId, OrderCreateRequest request) {
        // 1. 幂等校验：先查后插，避免唯一索引冲突时已扣库存
        Optional<Order> existed = orderRepository.findByUserBizId(userId, request.getBizId());
        if (existed.isPresent()) {
            log.info("重复下单命中幂等，返回原单 userId={} bizId={} orderId={}",
                    userId, request.getBizId(), existed.get().getOrderId());
            return existed.get();
        }

        // 2. 价格快照校验（BR-04）：下单单价与当前 SKU 价格不一致则拒绝
        for (OrderItemDTO item : request.getItems()) {
            if (!inventoryGateway.verifyPrice(item.getSkuId(), item.getUnitPrice())) {
                throw new BizException(ErrorCode.ORDER_PRICE_CHANGED);
            }
        }

        // 3. 扣减库存（BR-10/11）：原子扣减，库存不足整体失败
        List<DeductRecord> deducted = new ArrayList<>(request.getItems().size());
        try {
            for (OrderItemDTO item : request.getItems()) {
                if (!inventoryGateway.deduct(item.getSkuId(), item.getQuantity())) {
                    throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "SKU库存不足: " + item.getSkuId());
                }
                deducted.add(new DeductRecord(item.getSkuId(), item.getQuantity()));
            }

            // 4. 构建订单聚合
            List<OrderItem> orderItems = request.getItems().stream()
                    .map(dto -> new OrderItem(dto.getProductId(), dto.getSkuId(),
                            dto.getQuantity(), dto.getUnitPrice(), dto.getUnitPrice()))
                    .toList();
            String orderId = idGenerator.nextId();
            Order order = Order.createNormal(orderId, userId, request.getBizId(),
                    request.getAddressId(), request.getRemark(), orderItems);

            // 5. 落库（订单头 + 明细，同事务）
            orderRepository.save(order);
            log.info("下单成功 userId={} orderId={} payAmount={}",
                    userId, orderId, order.getPayAmount());
            return order;
        } catch (Exception e) {
            // 6. 库存回滚：扣减成功的部分必须归还，避免超卖
            rollbackQuietly(deducted);
            throw e;
        }
    }

    /**
     * 查询订单详情（含明细）。
     */
    public Order queryDetail(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BizException(ErrorCode.ORDER_NOT_FOUND));
        // 归属校验（接口规范§8）：他人订单不可见
        if (!userId.equals(order.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return order;
    }

    /**
     * 分页查询用户订单（FR-ORDER-04）。
     * <p>列表不带明细，明细走详情接口；状态筛选可空。
     */
    public List<Order> queryList(String userId, String status, int page, int size) {
        return orderRepository.pageByUser(userId, status, page, size);
    }

    /**
     * 统计用户订单总数（配合分页）。
     */
    public long countByUser(String userId, String status) {
        return orderRepository.countByUser(userId, status);
    }

    /**
     * 取消未支付订单（FR-ORDER-05）。回滚券+积分+库存。
     * <p>Phase 1 只回滚库存；券/积分在 Phase 5/6 接入后在此补齐。
     */
    public void cancel(String userId, String orderId) {
        Order order = queryDetail(userId, orderId);
        order.cancel();
        orderRepository.updateStatus(order);
        // 回滚库存
        for (OrderItem item : order.itemList()) {
            inventoryGateway.rollback(item.getSkuId(), item.getQuantity());
        }
        log.info("订单取消 userId={} orderId={}", userId, orderId);
    }

    private void rollbackQuietly(List<DeductRecord> deducted) {
        for (DeductRecord r : deducted) {
            try {
                inventoryGateway.rollback(r.skuId(), r.quantity());
            } catch (Exception ex) {
                // 回滚失败由定时补偿任务兜底（L3 库存对账，SDS §10.3）
                log.error("库存回滚失败，依赖补偿任务兜底 skuId={} quantity={}", r.skuId(), r.quantity(), ex);
            }
        }
    }

    /** 已扣减记录，用于异常时回滚 */
    private record DeductRecord(String skuId, int quantity) {}
}
