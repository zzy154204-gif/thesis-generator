package com.example.thesisgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 基于 Redis 计数器的简易限流器
 * 用于登录接口、导出接口等高消耗端点的频率控制
 */
@Slf4j
@Service
public class RateLimiterService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public boolean tryAcquire(String key, int limit, int windowSeconds) {
        if (redisTemplate == null) {
            log.warn("Redis 不可用，限流跳过");
            return true;
        }
        String redisKey = "rate:" + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }
        return count != null && count <= limit;
    }

    public boolean tryLogin(String clientIp) {
        return tryAcquire(clientIp + ":login", 5, 60);
    }

    public boolean tryExport(Long userId) {
        return tryAcquire(userId + ":export", 3, 60);
    }
}
