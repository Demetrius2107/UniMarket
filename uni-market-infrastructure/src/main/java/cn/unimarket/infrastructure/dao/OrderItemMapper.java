package cn.unimarket.infrastructure.dao;

import cn.unimarket.infrastructure.po.OrderItemPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单明细 Mapper。
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemPO> {
}
