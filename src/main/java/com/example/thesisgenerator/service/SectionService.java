package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final ThesisSectionRepository sectionRepository;
    private final ThesisRepository thesisRepository;

    /**
     * 获取论文章节树（扁平列表 → 树形结构）
     */
    public List<Map<String, Object>> getSectionTree(Long thesisId) {
        List<ThesisSection> flatList = sectionRepository
                .findByThesisIdOrderBySortOrder(thesisId);

        // 构建 id → node 映射
        Map<Long, Map<String, Object>> nodeMap = new HashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (ThesisSection s : flatList) {
            Map<String, Object> node = new HashMap<>();
            node.put("id", s.getId());
            node.put("title", s.getTitle());
            node.put("sectionKey", s.getSectionKey());
            node.put("sectionType", s.getSectionType());
            node.put("sortOrder", s.getSortOrder());
            node.put("parentId", s.getParentId());
            node.put("children", new ArrayList<Map<String, Object>>());
            nodeMap.put(s.getId(), node);
        }

        for (ThesisSection s : flatList) {
            Map<String, Object> node = nodeMap.get(s.getId());
            if (s.getParentId() != null && nodeMap.containsKey(s.getParentId())) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) nodeMap.get(s.getParentId()).get("children");
                children.add(node);
            } else {
                roots.add(node);
            }
        }

        return roots;
    }

    /**
     * 获取章节详情（含正文内容）
     */
    public ThesisSection getSection(Long thesisId, Long sectionId) {
        ThesisSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));
        if (!section.getThesisId().equals(thesisId)) {
            throw new BusinessException(403, "章节不属于该论文");
        }
        return section;
    }

    /**
     * 新增章节
     */
    @Transactional
    public ThesisSection createSection(Long thesisId, String title, Long parentId) {
        // 验证论文存在
        thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));

        // 计算 sortOrder
        int maxOrder = sectionRepository.findByThesisIdOrderBySortOrder(thesisId)
                .stream()
                .mapToInt(s -> s.getSortOrder() != null ? s.getSortOrder() : 0)
                .max()
                .orElse(0);

        ThesisSection section = new ThesisSection();
        section.setThesisId(thesisId);
        section.setTitle(title != null && !title.isBlank() ? title : "未命名章节");
        section.setSectionKey("section_" + System.currentTimeMillis());
        section.setParentId(parentId);
        section.setSortOrder(maxOrder + 1);
        section.setContent("");
        section.setSectionType(parentId == null ? "chapter" : "subchapter");

        return sectionRepository.save(section);
    }

    /**
     * 保存章节内容
     */
    @Transactional
    public ThesisSection saveSection(Long thesisId, Long sectionId, String content, String title) {
        ThesisSection section = getSection(thesisId, sectionId);

        if (content != null) {
            section.setContent(content);
        }
        if (title != null && !title.isBlank()) {
            section.setTitle(title);
        }

        return sectionRepository.save(section);
    }

    /**
     * 更新章节排序
     */
    @Transactional
    public void updateOrder(Long thesisId, List<Long> sectionIds) {
        for (int i = 0; i < sectionIds.size(); i++) {
            Long sid = sectionIds.get(i);
            ThesisSection section = sectionRepository.findById(sid)
                    .orElseThrow(() -> new BusinessException(404, "章节不存在: " + sid));
            if (!section.getThesisId().equals(thesisId)) {
                throw new BusinessException(403, "章节不属于该论文");
            }
            section.setSortOrder(i);
            sectionRepository.save(section);
        }
    }

    /**
     * 删除章节（同时删除子章节）
     */
    @Transactional
    public void deleteSection(Long thesisId, Long sectionId) {
        ThesisSection section = getSection(thesisId, sectionId);

        // 递归删除子章节
        List<ThesisSection> children = sectionRepository
                .findByThesisIdAndParentIdOrderBySortOrder(thesisId, sectionId);
        for (ThesisSection child : children) {
            deleteSection(thesisId, child.getId());
        }

        sectionRepository.delete(section);
    }
}
