package cn.unimarket.types.exception;

import java.io.Serial;
import java.io.Serializable;

/**
 * 错误码枚举。见《接口开发规范》§6。
 * <p>区间：200 成功 / 400~499 客户端错误 / 500~599 服务端错误 / 10000+ 业务错误 / 20000+ 支付。
 * <p>业务错误码统一维护在本枚举，禁止散落魔法数字字符串。
 */
public enum ErrorCode implements Serializable {

    // ---------- 通用 ----------
    SUCCESS(200, "success"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    SYSTEM_ERROR(500, "系统异常"),

    // ---------- 商品/库存 ----------
    STOCK_NOT_ENOUGH(10001, "库存不足"),
    PRODUCT_OFF_SHELF(10002, "商品已下架"),
    ACTIVITY_NOT_AVAILABLE(10003, "活动未开始或已结束"),

    // ---------- 优惠券 ----------
    COUPON_UNAVAILABLE(10006, "优惠券不可用或已过期"),

    // ---------- 积分 ----------
    CREDIT_NOT_ENOUGH(10007, "积分余额不足"),

    // ---------- 订单 ----------
    ORDER_NOT_FOUND(10010, "订单不存在"),
    ORDER_STATUS_ILLEGAL(10011, "订单状态非法，不允许该操作"),
    ORDER_BIZ_ID_DUPLICATE(10012, "重复下单，请勿重复提交"),
    ORDER_PRICE_CHANGED(10013, "商品价格已变动，请确认后重试"),

    // ---------- 支付 ----------
    PAY_SIGN_VERIFY_FAIL(20001, "支付回调验签失败"),
    PAY_ORDER_INVALID(20002, "支付单不存在或状态非法"),
    PAY_AMOUNT_MISMATCH(20003, "支付金额与订单金额不一致"),
    PAY_CHANNEL_NOT_SUPPORT(20004, "不支持的支付渠道");

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
