package cn.unimarket.types.enums;

/**
 * SKU 启用状态。见 SDS §7.2 sku.status。
 * <p>禁用的 SKU 不可下单，但历史订单快照不受影响。
 */
public enum SkuStatus {

    /** 启用 */
    ENABLE,
    /** 禁用 */
    DISABLE
}
