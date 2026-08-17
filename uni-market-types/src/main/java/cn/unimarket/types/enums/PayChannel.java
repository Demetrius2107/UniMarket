package cn.unimarket.types.enums;

/**
 * 支付渠道。见 SRS §11.1 pay_channel 字段。
 * <p>新增渠道只需扩展本枚举并实现 PaymentChannel 接口（ADR-08 支付渠道抽象）。
 */
public enum PayChannel {

    /** 支付宝 */
    ALIPAY,
    /** 微信支付 */
    WXPAY
}
