package cn.unimarket.domain.order.model;

import cn.unimarket.domain.order.event.OrderPaidEvent;
import cn.unimarket.types.event.DomainEvent;
import cn.unimarket.types.enums.OrderStatus;
import cn.unimarket.types.enums.OrderType;
import cn.unimarket.types.enums.PayChannel;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 订单聚合根。见 SDS §6.2。
 * <p>聚合内一致性：订单头与明细作为一个整体持久化；状态机流转唯一入口为 {@link #transitTo}。
 * <p>金额计算唯一入口 {@link #calculateFinalPrice}：商品金额 → 减优惠券 → 减积分抵扣 → 实付金额（BR-01）。
 * Phase 1 不含券/积分抵扣，{@code discountAmount} 恒为 0。
 */
public class Order implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 订单 ID（雪花） */
    private String orderId;
    /** 用户 ID（分片键） */
    private String userId;
    /** 业务防重 ID（userId+bizId 幂等） */
    private String bizId;
    /** 订单类型 */
    private OrderType orderType;
    /** 关联营销活动 ID，可空 */
    private String activityId;
    /** 关联拼团队伍 ID，可空 */
    private String teamId;

    /** 订单明细集合（去重，按 skuId） */
    private Set<OrderItem> items = new LinkedHashSet<>();

    /** 订单总金额（元） */
    private BigDecimal totalAmount;
    /** 优惠金额（元，券+积分抵扣合计） */
    private BigDecimal discountAmount;
    /** 实付金额（元） */
    private BigDecimal payAmount;

    /** 订单状态 */
    private OrderStatus status;
    /** 支付渠道，未支付为 null */
    private PayChannel payChannel;
    /** 第三方支付单号 */
    private String outTradeNo;
    /** 支付时间 */
    private LocalDateTime payTime;

    /** 收货地址 ID */
    private String addressId;
    /** 买家备注 */
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 聚合产生的领域事件，应用层持久化后统一 pull 并发布 */
    private transient List<DomainEvent> events = new ArrayList<>();

    /** 无参构造，供仓储重建用 */
    public Order() {
    }

    private Order(String orderId, String userId, String bizId, OrderType orderType,
                  String addressId, String remark, List<OrderItem> items) {
        this.orderId = orderId;
        this.userId = userId;
        this.bizId = bizId;
        this.orderType = orderType;
        this.addressId = addressId;
        this.remark = remark;
        this.status = OrderStatus.CREATE;
        this.discountAmount = BigDecimal.ZERO.setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
        this.items = new LinkedHashSet<>(items);
        calculateFinalPrice();
        this.createTime = LocalDateTime.now();
        this.updateTime = this.createTime;
    }

    /**
     * 工厂方法：创建普通订单。Phase 1 下单入口。
     *
     * @param orderId   雪花订单 ID
     * @param userId    下单用户 ID
     * @param bizId     业务防重 ID
     * @param addressId 收货地址 ID
     * @param remark    备注
     * @param items     订单明细
     * @return 新建订单聚合（状态 CREATE）
     */
    public static Order createNormal(String orderId, String userId, String bizId,
                                     String addressId, String remark, List<OrderItem> items) {
        Objects.requireNonNull(orderId, "orderId不能为空");
        Objects.requireNonNull(userId, "userId不能为空");
        Objects.requireNonNull(bizId, "bizId不能为空");
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "订单明细不能为空");
        }
        return new Order(orderId, userId, bizId, OrderType.NORMAL, addressId, remark, items);
    }

    /**
     * 金额计算唯一入口（BR-01）。
     * <p>商品金额 → 减优惠券 → 减积分抵扣 → 实付金额。
     * <p>Phase 1：discountAmount=0，payAmount=totalAmount。券/积分在 Phase 5/6 接入时扩展此处。
     */
    public void calculateFinalPrice() {
        BigDecimal total = items.stream()
                .map(OrderItem::originalSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
        this.totalAmount = total;
        this.discountAmount = this.discountAmount == null
                ? BigDecimal.ZERO.setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP)
                : this.discountAmount;
        this.payAmount = total.subtract(this.discountAmount)
                .setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
        // 实付不能为负
        if (this.payAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.payAmount = BigDecimal.ZERO.setScale(cn.unimarket.types.constant.Constants.MONEY_SCALE, RoundingMode.HALF_UP);
        }
    }

    /**
     * 状态机流转。非法流转拒绝（BR-05）。
     * <p>这是订单状态变更的唯一入口，禁止外部直接 setStatus。
     *
     * @param target 目标状态
     * @throws BizException 非法流转
     */
    public void transitTo(OrderStatus target) {
        Objects.requireNonNull(target, "目标状态不能为空");
        if (!status.canTransitTo(target)) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL,
                    "订单状态非法流转: " + status + " → " + target);
        }
        this.status = target;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记进入支付中（CREATE → PAY_WAIT），记录支付渠道。
     */
    public void markPaying(PayChannel channel, String outTradeNo) {
        Objects.requireNonNull(channel, "支付渠道不能为空");
        transitTo(OrderStatus.PAY_WAIT);
        this.payChannel = channel;
        this.outTradeNo = outTradeNo;
    }

    /**
     * 标记支付成功（PAY_WAIT → PAY_SUCCESS），并产生 {@link OrderPaidEvent}。
     * <p>产生的领域事件由应用层在事务提交后 pull 并投递到 MQ，
     * 结算域消费此事件触发分账计算（履约计算入口）。
     *
     * @param payTime    支付时间
     * @param eventId    事件 ID（用于消费端幂等）
     */
    public void markPaid(LocalDateTime payTime, String eventId) {
        transitTo(OrderStatus.PAY_SUCCESS);
        this.payTime = payTime == null ? LocalDateTime.now() : payTime;
        this.events.add(new OrderPaidEvent(eventId, orderId, userId, payAmount,
                orderType == null ? null : orderType.name(),
                payChannel == null ? null : payChannel.name(),
                outTradeNo, this.payTime));
    }

    /**
     * 取出并清空聚合产生的领域事件。应用层在事务提交后调用，投递到 MQ。
     */
    public List<DomainEvent> pullEvents() {
        List<DomainEvent> snapshot = new ArrayList<>(events);
        events.clear();
        return snapshot;
    }

    /**
     * 取消未支付订单（CREATE/PAY_WAIT → CLOSE）。
     */
    public void cancel() {
        transitTo(OrderStatus.CLOSE);
    }

    /**
     * 明细只读视图。
     */
    public List<OrderItem> itemList() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public Set<OrderItem> getItems() {
        return items;
    }

    public void setItems(Set<OrderItem> items) {
        this.items = items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public PayChannel getPayChannel() {
        return payChannel;
    }

    public void setPayChannel(PayChannel payChannel) {
        this.payChannel = payChannel;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public LocalDateTime getPayTime() {
        return payTime;
    }

    public void setPayTime(LocalDateTime payTime) {
        this.payTime = payTime;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
