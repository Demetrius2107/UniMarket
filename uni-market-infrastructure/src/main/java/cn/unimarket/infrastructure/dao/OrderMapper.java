package cn.unimarket.infrastructure.dao;

import cn.unimarket.infrastructure.po.OrderPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单 Mapper。MyBatis-Plus {@link BaseMapper} 提供基础 CRUD。
 * <p>复杂查询走 XML 或 LambdaQueryWrapper；一律 #{} 参数绑定，禁止 ${}（数据库规范§7）。
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderPO> {
}
