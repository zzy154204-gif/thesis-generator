package com.example.thesisgenerator.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的简易限流器（替代 Redis 版本）
 * 用于登录接口、导出接口等高消耗端点的频率控制
 */
@Service
public class RateLimiterService {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    /**
     * 尝试获取许可
     * @param key  限流标识（如 IP + 端点）
     * @param limit  时间窗口内最大请求数
     * @param windowSeconds  时间窗口（秒）
     * @return true=放行, false=限流
     */
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        long now = System.currentTimeMillis();
        WindowCounter counter = counters.compute(key, (k, v) -> {
            if (v == null || now - v.windowStart > windowSeconds * 1000L) {
                return new WindowCounter(now, 1);
            }
            v.count++;
            return v;
        });
        return counter.count <= limit;
    }

    /**
     * 登录限流：同一 IP 60 秒内最多 5 次
     */
    public boolean tryLogin(String clientIp) {
        return tryAcquire(clientIp + ":login", 5, 60);
    }

    /**
     * 导出限流：同一用户 60 秒内最多 3 次
     */
    public boolean tryExport(Long userId) {
        return tryAcquire(userId + ":export", 3, 60);
    }

    private static class WindowCounter {
        final long windowStart;
        int count;

        WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
