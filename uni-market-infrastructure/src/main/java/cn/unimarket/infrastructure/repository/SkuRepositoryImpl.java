package cn.unimarket.infrastructure.repository;

import cn.unimarket.domain.product.model.Sku;
import cn.unimarket.domain.product.repository.SkuRepository;
import cn.unimarket.infrastructure.dao.SkuMapper;
import cn.unimarket.infrastructure.po.SkuPO;
import cn.unimarket.types.enums.SkuStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * SKU 仓储实现。
 * <p>库存扣减/回滚走 {@link SkuMapper#deductStock} 行锁条件更新（SDS §10.3 L2 层）。
 * <p>affectedRows=0 即库存不足或 SKU 不可用，由调用方判断。
 */
@Repository
public class SkuRepositoryImpl implements SkuRepository {

    private static final Logger log = LoggerFactory.getLogger(SkuRepositoryImpl.class);

    private final SkuMapper skuMapper;

    public SkuRepositoryImpl(SkuMapper skuMapper) {
        this.skuMapper = skuMapper;
    }

    @Override
    public Optional<Sku> findById(String skuId) {
        SkuPO po = skuMapper.selectOne(new LambdaQueryWrapper<SkuPO>()
                .eq(SkuPO::getSkuId, skuId));
        if (po == null) {
            return Optional.empty();
        }
        return Optional.of(toSku(po));
    }

    @Override
    public boolean deductStock(String skuId, int quantity) {
        int affected = skuMapper.deductStock(skuId, quantity);
        if (affected == 0) {
            log.warn("库存扣减失败（库存不足或SKU不可用）skuId={} quantity={}", skuId, quantity);
            return false;
        }
        log.debug("扣减库存成功 skuId={} quantity={}", skuId, quantity);
        return true;
    }

    @Override
    public void rollbackStock(String skuId, int quantity) {
        int affected = skuMapper.rollbackStock(skuId, quantity);
        if (affected == 0) {
            // SKU 不存在（可能被删），记录日志由补偿任务兜底
            log.warn("库存回滚失败（SKU不存在）skuId={} quantity={}", skuId, quantity);
            return;
        }
        log.debug("回滚库存成功 skuId={} quantity={}", skuId, quantity);
    }

    private Sku toSku(SkuPO po) {
        return new Sku(po.getSkuId(), po.getProductId(), po.getSkuName(), po.getSkuAttrs(),
                po.getPrice(), po.getStock(), po.getImage(),
                po.getStatus() == null ? null : SkuStatus.valueOf(po.getStatus()));
    }
}
