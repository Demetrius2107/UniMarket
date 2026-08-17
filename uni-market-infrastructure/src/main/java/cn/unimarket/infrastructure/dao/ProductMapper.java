package cn.unimarket.infrastructure.dao;

import cn.unimarket.infrastructure.po.ProductPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品 SPU Mapper。MyBatis-Plus {@link BaseMapper} 提供基础 CRUD。
 */
@Mapper
public interface ProductMapper extends BaseMapper<ProductPO> {
}
