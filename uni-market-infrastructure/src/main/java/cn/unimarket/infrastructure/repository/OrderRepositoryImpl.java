package cn.unimarket.infrastructure.repository;

import cn.unimarket.domain.order.model.Order;
import cn.unimarket.domain.order.model.OrderItem;
import cn.unimarket.domain.order.repository.OrderRepository;
import cn.unimarket.infrastructure.dao.OrderItemMapper;
import cn.unimarket.infrastructure.dao.OrderMapper;
import cn.unimarket.infrastructure.po.OrderItemPO;
import cn.unimarket.infrastructure.po.OrderPO;
import cn.unimarket.types.enums.OrderStatus;
import cn.unimarket.types.enums.OrderType;
import cn.unimarket.types.enums.PayChannel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 订单仓储实现。PO ↔ 聚合根转换在此完成，领域层不感知存储细节（依赖倒置）。
 * <p>订单头 + 明细一起读写：保存时先写头后批量写明细，查询时按 orderId 装配明细。
 */
@Repository
public class OrderRepositoryImpl implements OrderRepository {

    private static final Logger log = LoggerFactory.getLogger(OrderRepositoryImpl.class);

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderRepositoryImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Override
    public void save(Order order) {
        OrderPO po = toOrderPO(order);
        orderMapper.insert(po);
        // 批量插入明细
        List<OrderItemPO> itemPOs = toItemPOs(order);
        for (OrderItemPO itemPO : itemPOs) {
            orderItemMapper.insert(itemPO);
        }
        log.debug("订单落库 orderId={} itemCount={}", order.getOrderId(), itemPOs.size());
    }

    @Override
    public Optional<Order> findById(String orderId) {
        OrderPO po = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getOrderId, orderId));
        if (po == null) {
            return Optional.empty();
        }
        List<OrderItemPO> itemPOs = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getOrderId, orderId));
        return Optional.of(toOrder(po, itemPOs));
    }

    @Override
    public Optional<Order> findByUserBizId(String userId, String bizId) {
        OrderPO po = orderMapper.selectOne(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getUserId, userId)
                .eq(OrderPO::getBizId, bizId)
                .last("LIMIT 1"));
        if (po == null) {
            return Optional.empty();
        }
        List<OrderItemPO> itemPOs = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getOrderId, po.getOrderId()));
        return Optional.of(toOrder(po, itemPOs));
    }

    @Override
    public List<Order> pageByUser(String userId, String status, int page, int size) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getUserId, userId)
                .orderByDesc(OrderPO::getCreateTime);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderPO::getStatus, status);
        }
        Page<OrderPO> p = new Page<>(page, size);
        List<OrderPO> pos = orderMapper.selectPage(p, wrapper).getRecords();
        // 列表不带明细，明细走详情接口
        List<Order> orders = new ArrayList<>(pos.size());
        for (OrderPO po : pos) {
            orders.add(toOrder(po, List.of()));
        }
        return orders;
    }

    @Override
    public long countByUser(String userId, String status) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderPO::getStatus, status);
        }
        return orderMapper.selectCount(wrapper);
    }

    @Override
    public void updateStatus(Order order) {
        OrderPO po = new OrderPO();
        po.setOrderId(order.getOrderId());
        po.setStatus(order.getStatus().name());
        po.setPayChannel(order.getPayChannel() == null ? null : order.getPayChannel().name());
        po.setOutTradeNo(order.getOutTradeNo());
        po.setPayTime(order.getPayTime());
        orderMapper.update(po, new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getOrderId, order.getOrderId()));
        log.debug("订单状态更新 orderId={} status={}", order.getOrderId(), order.getStatus());
    }

    // ---------- PO ↔ 聚合根转换 ----------

    private OrderPO toOrderPO(Order order) {
        OrderPO po = new OrderPO();
        po.setOrderId(order.getOrderId());
        po.setUserId(order.getUserId());
        po.setBizId(order.getBizId());
        po.setOrderType(order.getOrderType() == null ? null : order.getOrderType().name());
        po.setActivityId(order.getActivityId());
        po.setTeamId(order.getTeamId());
        po.setTotalAmount(order.getTotalAmount());
        po.setDiscountAmount(order.getDiscountAmount());
        po.setPayAmount(order.getPayAmount());
        po.setStatus(order.getStatus() == null ? null : order.getStatus().name());
        po.setPayChannel(order.getPayChannel() == null ? null : order.getPayChannel().name());
        po.setOutTradeNo(order.getOutTradeNo());
        po.setPayTime(order.getPayTime());
        po.setAddressId(order.getAddressId());
        po.setRemark(order.getRemark());
        po.setDeleted(0);
        po.setCreateTime(order.getCreateTime());
        po.setUpdateTime(order.getUpdateTime());
        return po;
    }

    private List<OrderItemPO> toItemPOs(Order order) {
        List<OrderItemPO> list = new ArrayList<>();
        for (OrderItem item : order.itemList()) {
            OrderItemPO po = new OrderItemPO();
            po.setOrderId(order.getOrderId());
            po.setProductId(item.getProductId());
            po.setSkuId(item.getSkuId());
            po.setQuantity(item.getQuantity());
            po.setOriginalPrice(item.getOriginalPrice());
            po.setActualPrice(item.getActualPrice());
            po.setCreateTime(order.getCreateTime());
            po.setUpdateTime(order.getUpdateTime());
            list.add(po);
        }
        return list;
    }

    private Order toOrder(OrderPO po, List<OrderItemPO> itemPOs) {
        Order order = new Order();
        order.setOrderId(po.getOrderId());
        order.setUserId(po.getUserId());
        order.setBizId(po.getBizId());
        order.setOrderType(po.getOrderType() == null ? null : OrderType.valueOf(po.getOrderType()));
        order.setActivityId(po.getActivityId());
        order.setTeamId(po.getTeamId());
        order.setTotalAmount(po.getTotalAmount());
        order.setDiscountAmount(po.getDiscountAmount());
        order.setPayAmount(po.getPayAmount());
        order.setStatus(po.getStatus() == null ? null : OrderStatus.valueOf(po.getStatus()));
        order.setPayChannel(po.getPayChannel() == null ? null : PayChannel.valueOf(po.getPayChannel()));
        order.setOutTradeNo(po.getOutTradeNo());
        order.setPayTime(po.getPayTime());
        order.setAddressId(po.getAddressId());
        order.setRemark(po.getRemark());
        order.setCreateTime(po.getCreateTime());
        order.setUpdateTime(po.getUpdateTime());

        Set<OrderItem> items = new LinkedHashSet<>();
        for (OrderItemPO ipo : itemPOs) {
            items.add(new OrderItem(ipo.getProductId(), ipo.getSkuId(),
                    ipo.getQuantity(), ipo.getOriginalPrice(), ipo.getActualPrice()));
        }
        order.setItems(items);
        return order;
    }
}
