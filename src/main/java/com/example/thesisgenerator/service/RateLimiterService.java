package com.example.thesisgenerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 计数器的简易限流器
 * 用于登录接口、导出接口等高消耗端点的频率控制
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 尝试获取许可
     * @param key  限流标识（如 IP + 端点）
     * @param limit  时间窗口内最大请求数
     * @param windowSeconds  时间窗口（秒）
     * @return true=放行, false=限流
     */
    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        String redisKey = "rate:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        // 第一次访问时设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= limit;
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
}
