package cn.unimarket.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置。
 * <p>指定 Mapper 扫描路径为基础设施层的 dao 包。
 */
@Configuration
@MapperScan("cn.unimarket.infrastructure.dao")
public class MybatisPlusConfig {
}
