package cn.unimarket.domain.product.repository;

import cn.unimarket.domain.product.model.Sku;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * SKU 仓储端口。领域层定义契约，基础设施层提供实现（依赖倒置）。
 * <p>库存扣减/回滚走 DB 行锁条件更新（SDS §10.3 L2 层），返回受影响行数判断成功与否。
 * <p>Phase 2 接入 Redis 后，扣减前先走 Redis INCR（L1），DB 兜底（L2）。
 */
public interface SkuRepository {

    /**
     * 按 SKU ID 查询 SKU。
     *
     * @param skuId SKU ID
     * @return SKU，不存在返回 empty
     */
    Optional<Sku> findById(String skuId);

    /**
     * 行锁条件扣减库存。
     * <p>对应 SQL：UPDATE sku SET stock = stock - #{quantity}
     *             WHERE sku_id = #{skuId} AND stock >= #{quantity} AND status = 'ENABLE'
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return true 表示扣减成功，false 表示库存不足或 SKU 不可用
     */
    boolean deductStock(String skuId, int quantity);

    /**
     * 回滚库存（订单取消/超时关单时调用）。
     *
     * @param skuId    SKU ID
     * @param quantity 回滚数量
     */
    void rollbackStock(String skuId, int quantity);
}
