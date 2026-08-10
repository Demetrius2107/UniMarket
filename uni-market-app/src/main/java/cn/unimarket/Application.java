package cn.unimarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * UniMarket 统一商城前台 — 启动类
 * <p>模块化单体（DDD 六边形架构）：扫描 infra/trigger 下全部组件。</p>
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
