package cn.unimarket.infrastructure.gateway;

import cn.unimarket.domain.order.service.InventoryGateway;
import cn.unimarket.domain.product.service.SkuStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 基于真实 SKU 表的库存网关实现。
 * <p>实现订单域 {@link InventoryGateway} 端口，委托 {@link SkuStockService} 完成库存校验/扣减/回滚。
 * <p>替代 {@link MockInventoryGateway}：商品域落地后，下单链路库存走真实 DB 行锁（SDS §10.3 L2）。
 * <p>Phase 2 接入 Redis 后，由 SkuStockService 在 L1 层加 Redis INCR 快速失败，本网关无需改动。
 *
 * @see MockInventoryGateway 仅保留用于无商品表的早期单测，主链路走本实现
 */
@Component
@Primary
public class SkuInventoryGateway implements InventoryGateway {

    private static final Logger log = LoggerFactory.getLogger(SkuInventoryGateway.class);

    private final SkuStockService skuStockService;

    public SkuInventoryGateway(SkuStockService skuStockService) {
        this.skuStockService = skuStockService;
    }

    @Override
    public boolean deduct(String skuId, int quantity) {
        return skuStockService.deduct(skuId, quantity);
    }

    @Override
    public void rollback(String skuId, int quantity) {
        skuStockService.rollback(skuId, quantity);
    }

    @Override
    public boolean verifyPrice(String skuId, BigDecimal unitPrice) {
        return skuStockService.verifyPrice(skuId, unitPrice);
    }
}
