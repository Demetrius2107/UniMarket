package cn.unimarket.config;

import cn.unimarket.domain.order.repository.OrderRepository;
import cn.unimarket.domain.order.service.IdGenerator;
import cn.unimarket.domain.order.service.InventoryGateway;
import cn.unimarket.domain.order.service.OrderService;
import cn.unimarket.domain.product.repository.ProductRepository;
import cn.unimarket.domain.product.repository.SkuRepository;
import cn.unimarket.domain.product.service.ProductService;
import cn.unimarket.domain.product.service.SkuStockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 领域服务 Bean 装配。
 * <p>领域层保持纯净（不依赖 Spring），由 app 层负责把领域服务及其依赖装配为 Bean。
 * <p>新增领域服务时在此补齐装配；服务一多可拆为按模块的 @Configuration 类。
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public OrderService orderService(OrderRepository orderRepository,
                                     InventoryGateway inventoryGateway,
                                     IdGenerator idGenerator) {
        return new OrderService(orderRepository, inventoryGateway, idGenerator);
    }

    @Bean
    public ProductService productService(ProductRepository productRepository) {
        return new ProductService(productRepository);
    }

    @Bean
    public SkuStockService skuStockService(SkuRepository skuRepository) {
        return new SkuStockService(skuRepository);
    }
}
