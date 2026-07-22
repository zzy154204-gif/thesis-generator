package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ThesisSectionService {

    private final ThesisSectionRepository sectionRepository;
    private final ThesisRepository thesisRepository;

    /**
     * 获取论文章节列表（按 sortOrder 排序）
     */
    public List<ThesisSection> getSections(Long thesisId, Long userId, String role) {
        verifyReadAccess(thesisId, userId, role);
        return sectionRepository.findByThesisIdOrderBySortOrder(thesisId);
    }

    /**
     * 获取单个章节
     */
    public ThesisSection getSection(Long thesisId, Long sectionId, Long userId, String role) {
        verifyReadAccess(thesisId, userId, role);
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));
    }

    /**
     * 新增章节
     */
    @Transactional
    public ThesisSection createSection(Long thesisId, Long userId, String title, Long parentId) {
        verifyOwner(thesisId, userId);
        verifyNotLocked(thesisId);

        // 计算同级下一个 sortOrder
        List<ThesisSection> siblings;
        if (parentId != null) {
            siblings = sectionRepository.findByThesisIdAndParentIdOrderBySortOrder(thesisId, parentId);
        } else {
            siblings = sectionRepository.findByThesisIdAndParentIdIsNullOrderBySortOrder(thesisId);
        }
        int nextOrder = siblings.stream().mapToInt(ThesisSection::getSortOrder).max().orElse(-1) + 1;

        ThesisSection section = new ThesisSection();
        section.setThesisId(thesisId);
        section.setTitle(title);
        section.setSectionKey("chapter" + (nextOrder + 1));
        section.setContent("");
        section.setDraftContent("");
        section.setSectionType("chapter");
        section.setParentId(parentId);
        section.setSortOrder(nextOrder);
        return sectionRepository.save(section);
    }

    /**
     * 更新章节内容（标题、正文）
     */
    @Transactional
    public ThesisSection updateSection(Long thesisId, Long sectionId, Long userId,
                                        String content, String title) {
        verifyOwner(thesisId, userId);
        verifyNotLocked(thesisId);

        ThesisSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));

        if (!section.getThesisId().equals(thesisId)) {
            throw new BusinessException(400, "章节不属于该论文");
        }

        if (content != null) {
            section.setContent(content);
        }
        if (title != null) {
            section.setTitle(title);
        }
        return sectionRepository.save(section);
    }

    /**
     * 删除章节
     */
    @Transactional
    public void deleteSection(Long thesisId, Long sectionId, Long userId) {
        verifyOwner(thesisId, userId);
        verifyNotLocked(thesisId);

        ThesisSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));

        if (!section.getThesisId().equals(thesisId)) {
            throw new BusinessException(400, "章节不属于该论文");
        }

        sectionRepository.delete(section);
    }

    /**
     * 更新章节排序（传入所有章节 ID 列表，按顺序重设 sortOrder）
     */
    @Transactional
    public void updateOrder(Long thesisId, Long userId, List<Long> sectionIds) {
        verifyOwner(thesisId, userId);
        verifyNotLocked(thesisId);

        List<ThesisSection> sections = sectionRepository.findByThesisIdOrderBySortOrder(thesisId);
        for (int i = 0; i < sectionIds.size(); i++) {
            Long id = sectionIds.get(i);
            int order = i; // effectively final for lambda
            sections.stream()
                    .filter(s -> s.getId().equals(id))
                    .findFirst()
                    .ifPresent(s -> s.setSortOrder(order));
        }
        sectionRepository.saveAll(sections);
    }

    // ===== 内部方法 =====

    private void verifyOwner(Long thesisId, Long userId) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        if (!thesis.getStudentId().equals(userId)) {
            throw new BusinessException(403, "无权操作该论文");
        }
    }

    /** 教师和管理员可读所有论文，学生只能读自己的 */
    private void verifyReadAccess(Long thesisId, Long userId, String role) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        if ("TEACHER".equals(role) || "ADMIN".equals(role)) {
            return;
        }
        if (!thesis.getStudentId().equals(userId)) {
            throw new BusinessException(403, "无权操作该论文");
        }
    }

    private void verifyNotLocked(Long thesisId) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        if (Boolean.TRUE.equals(thesis.getIsLocked())) {
            throw new BusinessException(400, "论文已锁定，无法修改");
        }
    }
}
