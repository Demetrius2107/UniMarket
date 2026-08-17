package cn.unimarket.infrastructure.gateway;

import cn.unimarket.domain.order.service.InventoryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 库存网关 Mock 实现。
 * <p>Phase 1 商品/SKU 域尚未实现，先用内存 Map 模拟库存，保证下单链路可跑通。
 * <p>Phase 2 接入真实 SKU 后，替换为 Redis INCR（L1）+ DB 行锁（L2）+ 定时补偿（L3）三层实现（SDS §10.3）。
 * <p>价格校验同样走真实 SKU 表，此处 mock 通过。
 */
@Component
public class MockInventoryGateway implements InventoryGateway {

    private static final Logger log = LoggerFactory.getLogger(MockInventoryGateway.class);

    /** 内存库存表：skuId → 剩余数量。默认每个 SKU 1000 件 */
    private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();

    @Override
    public boolean deduct(String skuId, int quantity) {
        AtomicInteger n = stock.computeIfAbsent(skuId, k -> new AtomicInteger(1000));
        // CAS 扣减，模拟 Redis INCR 的原子性
        int prev;
        int next;
        do {
            prev = n.get();
            if (prev < quantity) {
                log.warn("库存不足 skuId={} need={} remain={}", skuId, quantity, prev);
                return false;
            }
            next = prev - quantity;
        } while (!n.compareAndSet(prev, next));
        log.debug("扣减库存 skuId={} quantity={} remain={}", skuId, quantity, next);
        return true;
    }

    @Override
    public void rollback(String skuId, int quantity) {
        AtomicInteger n = stock.computeIfAbsent(skuId, k -> new AtomicInteger(1000));
        n.addAndGet(quantity);
        log.debug("回滚库存 skuId={} quantity={} remain={}", skuId, quantity, n.get());
    }

    @Override
    public boolean verifyPrice(String skuId, BigDecimal unitPrice) {
        // Phase 1 mock：价格校验直接通过；Phase 2 接入 SKU 表比对当前售价
        return true;
    }
}
