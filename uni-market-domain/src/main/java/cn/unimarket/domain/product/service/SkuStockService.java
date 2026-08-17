package cn.unimarket.domain.product.service;

import cn.unimarket.domain.product.model.Sku;
import cn.unimarket.domain.product.repository.SkuRepository;
import cn.unimarket.types.exception.BizException;
import cn.unimarket.types.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * SKU 库存领域服务。见 SDS §10.3 三层库存保障。
 * <p>本服务实现订单域 {@link cn.unimarket.domain.order.service.InventoryGateway} 端口，
 * 使下单流程通过真实 SKU 表完成库存校验与扣减，替代 {@code MockInventoryGateway}。
 * <p>Phase 1：DB 行锁条件更新（L2）单层实现；Phase 2 接入 Redis INCR（L1）后在此扩展。
 */
public class SkuStockService {

    private static final Logger log = LoggerFactory.getLogger(SkuStockService.class);

    private final SkuRepository skuRepository;

    public SkuStockService(SkuRepository skuRepository) {
        this.skuRepository = skuRepository;
    }

    /**
     * 校验下单单价是否与当前 SKU 售价一致（BR-04 价格快照校验）。
     *
     * @param skuId     SKU ID
     * @param unitPrice 下单时单价快照
     * @return true 表示价格未变动；SKU 不存在抛业务异常
     */
    public boolean verifyPrice(String skuId, BigDecimal unitPrice) {
        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new BizException(ErrorCode.SKU_NOT_FOUND, "SKU不存在: " + skuId));
        return sku.getPrice().compareTo(unitPrice) == 0;
    }

    /**
     * 扣减 SKU 库存。原子操作：库存不足整体失败，不部分扣减。
     * <p>DB 行锁条件更新，affectedRows=0 即库存不足或 SKU 不可用。
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return true 扣减成功，false 库存不足
     */
    public boolean deduct(String skuId, int quantity) {
        boolean ok = skuRepository.deductStock(skuId, quantity);
        if (!ok) {
            log.warn("库存扣减失败 skuId={} quantity={}", skuId, quantity);
        }
        return ok;
    }

    /**
     * 回滚库存（订单取消/超时关单时调用）。
     *
     * @param skuId    SKU ID
     * @param quantity 回滚数量
     */
    public void rollback(String skuId, int quantity) {
        skuRepository.rollbackStock(skuId, quantity);
        log.debug("回滚库存 skuId={} quantity={}", skuId, quantity);
    }
}
