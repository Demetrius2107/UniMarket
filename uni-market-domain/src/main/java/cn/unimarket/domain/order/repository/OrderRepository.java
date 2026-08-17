package cn.unimarket.domain.order.repository;

import cn.unimarket.domain.order.model.Order;

import java.util.List;
import java.util.Optional;

/**
 * 订单仓储端口。领域层定义契约，基础设施层提供实现（依赖倒置）。
 * <p>Phase 1 单库单表；Phase 10 接入分库分表后实现切换为带路由的版本，领域层无感。
 */
public interface OrderRepository {

    /**
     * 保存订单聚合（订单头 + 明细）。
     *
     * @param order 订单聚合根
     */
    void save(Order order);

    /**
     * 按订单 ID 查询订单聚合（含明细）。
     *
     * @param orderId 订单 ID
     * @return 订单聚合，不存在返回 empty
     */
    Optional<Order> findById(String orderId);

    /**
     * 按 userId + bizId 查询订单（幂等校验用，重复下单返回原单）。
     *
     * @param userId 用户 ID
     * @param bizId  业务防重 ID
     * @return 已存在的订单，不存在返回 empty
     */
    Optional<Order> findByUserBizId(String userId, String bizId);

    /**
     * 分页查询用户订单。
     *
     * @param userId 用户 ID
     * @param status 订单状态筛选，null 查全部
     * @param page   页码（从 1 开始）
     * @param size   每页大小
     * @return 订单列表（不含明细，明细走详情）
     */
    List<Order> pageByUser(String userId, String status, int page, int size);

    /**
     * 统计用户订单总数。
     *
     * @param userId 用户 ID
     * @param status 订单状态筛选，null 查全部
     * @return 总数
     */
    long countByUser(String userId, String status);

    /**
     * 更新订单状态（状态机流转落库）。
     *
     * @param order 订单聚合根
     */
    void updateStatus(Order order);
}
