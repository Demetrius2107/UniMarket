package cn.unimarket.types.enums;

/**
 * 订单类型。见 SRS §11.1 数据字典。
 */
public enum OrderType {

    /** 普通订单 */
    NORMAL,
    /** 拼团订单 */
    GROUP_BUY,
    /** 积分兑换订单 */
    CREDIT
}
