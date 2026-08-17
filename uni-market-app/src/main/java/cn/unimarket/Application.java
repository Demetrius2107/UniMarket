package cn.unimarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.redisson.spring.starter.RedissonAutoConfigurationV2;

/**
 * UniMarket 统一商城前台 — 启动类
 * <p>模块化单体（DDD 六边形架构）：扫描 infra/trigger 下全部组件。</p>
 * <p>Phase 1 暂不依赖 Redis/Redisson（库存走内存 mock），排除其自动配置避免强依赖。
 * Phase 2 接入真实缓存/分布式锁时移除排除并补 Redis 配置。</p>
 */
@SpringBootApplication(exclude = {RedisAutoConfiguration.class, RedissonAutoConfigurationV2.class})
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
