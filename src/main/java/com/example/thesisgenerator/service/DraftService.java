package com.example.thesisgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 章节草稿自动保存服务
 * 学生编辑论文时，前端定时将草稿存至 Redis，
 * 避免频繁写入 PostgreSQL，同时支持跨设备恢复编辑进度
 */
@Slf4j
@Service
public class DraftService {

    private static final Duration DRAFT_TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "draft:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    public void saveDraft(Long thesisId, Long sectionId, String draftContent) {
        if (redisTemplate == null) {
            log.warn("Redis 不可用，草稿暂存跳过");
            return;
        }
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        redisTemplate.opsForValue().set(key, draftContent, DRAFT_TTL);
    }

    public String getDraft(Long thesisId, Long sectionId) {
        if (redisTemplate == null) return null;
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }

    public void deleteDraft(Long thesisId, Long sectionId) {
        if (redisTemplate == null) return;
        redisTemplate.delete(KEY_PREFIX + thesisId + ":" + sectionId);
    }

    public void deleteAllDrafts(Long thesisId) {
        if (redisTemplate == null) return;
        String pattern = KEY_PREFIX + thesisId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
