package cn.unimarket.infrastructure.dao;

import cn.unimarket.infrastructure.po.SkuPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 SKU Mapper。MyBatis-Plus {@link BaseMapper} 提供基础 CRUD。
 * <p>库存扣减走自定义条件更新（行锁），见 {@link #deductStock}。
 */
@Mapper
public interface SkuMapper extends BaseMapper<SkuPO> {

    /**
     * 行锁条件扣减库存。
     * <p>WHERE stock >= #{quantity} 兜底超卖；affectedRows=0 即库存不足（数据库规范§6）。
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return 受影响行数，0 表示库存不足或 SKU 不可用
     */
    @Update("UPDATE sku SET stock = stock - #{quantity}, update_time = NOW() " +
            "WHERE sku_id = #{skuId} AND stock >= #{quantity} AND status = 'ENABLE' AND deleted = 0")
    int deductStock(@Param("skuId") String skuId, @Param("quantity") int quantity);

    /**
     * 回滚库存（订单取消/超时关单时调用）。
     *
     * @param skuId    SKU ID
     * @param quantity 回滚数量
     * @return 受影响行数
     */
    @Update("UPDATE sku SET stock = stock + #{quantity}, update_time = NOW() " +
            "WHERE sku_id = #{skuId} AND deleted = 0")
    int rollbackStock(@Param("skuId") String skuId, @Param("quantity") int quantity);
}
