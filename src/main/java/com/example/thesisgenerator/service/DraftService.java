package com.example.thesisgenerator.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 章节草稿自动保存服务（内存版，替代 Redis）
 * 学生编辑论文时，前端定时保存草稿到内存，
 * 避免频繁写入 PostgreSQL，同时支持恢复编辑进度
 *
 * 注意：服务重启后草稿会丢失，如需持久化请改用数据库方案
 */
@Service
public class DraftService {

    private final Map<String, String> draftStore = new ConcurrentHashMap<>();
    private static final String KEY_PREFIX = "draft:";

    /**
     * 保存章节草稿到内存
     */
    public void saveDraft(Long thesisId, Long sectionId, String draftContent) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        draftStore.put(key, draftContent);
    }

    /**
     * 从内存读取草稿
     * @return 草稿内容，若无返回 null
     */
    public String getDraft(Long thesisId, Long sectionId) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        return draftStore.get(key);
    }

    /**
     * 删除草稿（论文提交后清理）
     */
    public void deleteDraft(Long thesisId, Long sectionId) {
        String key = KEY_PREFIX + thesisId + ":" + sectionId;
        draftStore.remove(key);
    }

    /**
     * 批量删除某论文所有章节草稿
     */
    public void deleteAllDrafts(Long thesisId) {
        String prefix = KEY_PREFIX + thesisId + ":";
        draftStore.keySet().removeIf(key -> key.startsWith(prefix));
    }
}
