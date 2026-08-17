package cn.unimarket.types.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * 订单状态机。见 SRS BR-05 / SDS §9.9.2。
 * <pre>
 * CREATE → PAY_WAIT → PAY_SUCCESS → DEAL_DONE
 *                    ↘ CLOSE（超时30min / 用户取消）
 *          PAY_SUCCESS → REFUND（退款通过）
 * </pre>
 * 非法流转在领域层抛 {@code BizException(ORDER_STATUS_ILLEGAL)}。
 */
public enum OrderStatus {

    /** 已创建，待发起支付 */
    CREATE,
    /** 支付中，已发起支付等待回调 */
    PAY_WAIT,
    /** 支付成功 */
    PAY_SUCCESS,
    /** 交易完成（已确认收货/自动签收） */
    DEAL_DONE,
    /** 已关闭（超时关单/用户取消未支付单） */
    CLOSE,
    /** 已退款 */
    REFUND;

    /**
     * 判断从当前状态流转到目标状态是否合法。
     * <p>状态机唯一归属领域层，禁止在 Controller/Service 用裸字符串判断。
     *
     * @param target 目标状态
     * @return true 表示允许流转
     */
    public boolean canTransitTo(OrderStatus target) {
        Set<OrderStatus> allowed = ALLOWED_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }

    /** 合法流转映射表 */
    private static final java.util.Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS;

    static {
        java.util.Map<OrderStatus, Set<OrderStatus>> m = new java.util.EnumMap<>(OrderStatus.class);
        m.put(CREATE, EnumSet.of(PAY_WAIT, CLOSE));
        m.put(PAY_WAIT, EnumSet.of(PAY_SUCCESS, CLOSE));
        m.put(PAY_SUCCESS, EnumSet.of(DEAL_DONE, REFUND));
        m.put(DEAL_DONE, EnumSet.noneOf(OrderStatus.class));
        m.put(CLOSE, EnumSet.noneOf(OrderStatus.class));
        m.put(REFUND, EnumSet.noneOf(OrderStatus.class));
        ALLOWED_TRANSITIONS = java.util.Collections.unmodifiableMap(m);
    }
}
