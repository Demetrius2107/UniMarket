package cn.unimarket.domain.order.service;

/**
 * 分布式 ID 生成器端口。业务 ID 用雪花算法（VARCHAR(32)），见数据库规范§5。
 * <p>基础设施层实现，Phase 1 可先用简单实现，后续替换为雪花/Leaf 均可。
 */
public interface IdGenerator {

    /**
     * 生成下一个业务 ID。
     *
     * @return 18~21 位数字字符串
     */
    String nextId();
}
