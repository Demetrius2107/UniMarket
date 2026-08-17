package cn.unimarket.infrastructure.support;

import cn.unimarket.domain.order.service.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * ID 生成器实现。
 * <p>Phase 1 用进程内自增 + 时间戳前缀的简易实现，满足单机调试。
 * <p>Phase 10 接入分库分表后替换为雪花算法（workerId + datacenterId + 序列号），保证分布式唯一。
 */
@Component
public class DefaultIdGenerator implements IdGenerator {

    private final AtomicLong seq = new AtomicLong(0);

    @Override
    public String nextId() {
        long timestamp = System.currentTimeMillis();
        long sequence = seq.incrementAndGet();
        // 时间戳(13位) + 进程标识(2位) + 序列号(6位补零)，凑成 21 位字符串
        return String.format("%013d%02d%06d", timestamp % 10_000_000_000_000L, 1L, sequence % 1_000_000L);
    }
}
