package cn.unimarket.infrastructure.dao;

import cn.unimarket.infrastructure.po.CategoryPO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品分类 Mapper。MyBatis-Plus {@link BaseMapper} 提供基础 CRUD。
 */
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryPO> {
}
