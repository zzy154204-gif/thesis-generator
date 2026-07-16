package com.example.thesisgenerator.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 章节草稿自动保存服务
 * 学生编辑论文时，前端定时将草稿存至 Redis，
 * 避免频繁写入 PostgreSQL，同时支持跨设备恢复编辑进度
 */
@Service
@RequiredArgsConstructor
public class DraftService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final Duration DRAFT_TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "draft:";

    /**
     * 保存章节草稿到 Redis
     */
    public void saveDraft(Long thesisId, Long sectionId, String draftContent) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        redisTemplate.opsForValue().set(key, draftContent, DRAFT_TTL);
    }

    /**
     * 从 Redis 读取草稿
     * @return 草稿内容，若无返回 null
     */
    public String getDraft(Long thesisId, Long sectionId) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? val.toString() : null;
    }

    /**
     * 删除草稿（论文提交后清理）
     */
    public void deleteDraft(Long thesisId, Long sectionId) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        redisTemplate.delete(key);
    }

    /**
     * 批量删除某论文所有章节草稿
     */
    public void deleteAllDrafts(Long thesisId) {
        String pattern = KEY_PREFIX + thesisId + ":*";
        // 使用 SCAN 命令避免阻塞 Redis
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}
