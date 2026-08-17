package cn.unimarket.types.constant;

/**
 * 系统级常量。模块专属常量放在各自模块的 Constants 中，此处只放跨模块共享的。
 */
public final class Constants {

    private Constants() {
    }

    /** 统一接口前缀 */
    public static final String API_PREFIX = "/api/v1";

    /** Redis Key 通用前缀，见《Redis Key 设计规范》 */
    public static final String REDIS_KEY_PREFIX = "unimarket:";

    /** 订单幂等键前缀：unimarket:order:biz:{userId}:{bizId} */
    public static final String ORDER_BIZ_KEY = REDIS_KEY_PREFIX + "order:biz:";

    /** 默认分页大小 */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /** 最大分页大小 */
    public static final int MAX_PAGE_SIZE = 100;

    /** 金额小数位数（元） */
    public static final int MONEY_SCALE = 2;
}
