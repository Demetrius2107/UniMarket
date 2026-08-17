package cn.unimarket.domain.order.service;

import java.math.BigDecimal;

/**
 * 库存网关端口。下单时校验并扣减库存，由商品领域/基础设施实现。
 * <p>Phase 1 商品域尚未实现，先用 mock 实现；Phase 2 接入真实 SKU 库存后切换实现，领域层无感。
 * <p>三层库存保障（Redis INCR + DB 行锁 + 定时补偿）见 SDS §10.3，实现逐步补齐。
 */
public interface InventoryGateway {

    /**
     * 校验并扣减 SKU 库存。
     * <p>原子操作：库存不足整体失败，不部分扣减。
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return 扣减成功返回 true，库存不足返回 false
     */
    boolean deduct(String skuId, int quantity);

    /**
     * 回滚库存（订单取消/超时关单时调用）。
     *
     * @param skuId    SKU ID
     * @param quantity 回滚数量
     */
    void rollback(String skuId, int quantity);

    /**
     * 校验下单单价是否与当前 SKU 价格一致（BR-04 价格快照校验）。
     *
     * @param skuId     SKU ID
     * @param unitPrice 下单时单价快照
     * @return true 表示价格未变动
     */
    boolean verifyPrice(String skuId, BigDecimal unitPrice);
}
