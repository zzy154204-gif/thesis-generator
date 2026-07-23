package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisReference;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.ReferenceRepository;
import com.example.thesisgenerator.repository.ThesisReferenceRepository;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThesisSectionService {

    private final ThesisSectionRepository sectionRepository;
    private final ThesisRepository thesisRepository;
    private final ThesisReferenceRepository thesisReferenceRepository;
    private final ReferenceRepository referenceRepository;
    private final Gbt7714Formatter gbt7714Formatter;

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
     * <p>
     * 顶级章节（parentId == null）会自动插入到 "参考文献" 之前，
     * 确保 "参考文献" 始终保持在章节列表末尾。
     */
    @Transactional
    public ThesisSection createSection(Long thesisId, Long userId, String title, Long parentId) {
        verifyOwner(thesisId, userId);
        verifyNotLocked(thesisId);

        if (parentId == null) {
            return createTopLevelSection(thesisId, title);
        } else {
            return createChildSection(thesisId, title, parentId);
        }
    }

    /**
     * 创建顶级章节（插入到 "参考文献" 之前）
     */
    private ThesisSection createTopLevelSection(Long thesisId, String title) {
        List<ThesisSection> topLevel = sectionRepository
                .findByThesisIdAndParentIdIsNullOrderBySortOrder(thesisId);

        // 找到 "参考文献" 章节
        ThesisSection refSection = topLevel.stream()
                .filter(s -> s.getTitle() != null && s.getTitle().contains("参考文献"))
                .findFirst().orElse(null);

        int insertOrder;
        if (refSection != null) {
            // 新章节插入到参考文献之前
            insertOrder = refSection.getSortOrder();
            // 参考文献及之后的章节向后顺移
            for (ThesisSection s : topLevel) {
                if (s.getSortOrder() >= insertOrder) {
                    s.setSortOrder(s.getSortOrder() + 1);
                }
            }
            sectionRepository.saveAll(topLevel);
        } else {
            insertOrder = topLevel.stream().mapToInt(ThesisSection::getSortOrder)
                    .max().orElse(-1) + 1;
        }

        ThesisSection section = new ThesisSection();
        section.setThesisId(thesisId);
        section.setTitle(title);
        section.setSectionKey("chapter" + (insertOrder + 1));
        section.setContent("");
        section.setDraftContent("");
        section.setSectionType("chapter");
        section.setParentId(null);
        section.setSortOrder(insertOrder);
        return sectionRepository.save(section);
    }

    /**
     * 创建子章节（挂载到指定父章节下）
     */
    private ThesisSection createChildSection(Long thesisId, String title, Long parentId) {
        List<ThesisSection> siblings = sectionRepository
                .findByThesisIdAndParentIdOrderBySortOrder(thesisId, parentId);
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
     * 删除章节及其所有子章节（递归删除）
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

        // 递归删除自身及所有子孙章节
        deleteSectionAndDescendants(thesisId, sectionId);
        log.info("已删除章节及其所有子章节: thesisId={}, sectionId={}", thesisId, sectionId);
    }

    /**
     * 递归删除指定章节及其所有子孙章节
     * 策略：先删除子节点，再删除自身
     */
    private void deleteSectionAndDescendants(Long thesisId, Long sectionId) {
        List<ThesisSection> allSections = sectionRepository.findByThesisIdOrderBySortOrder(thesisId);
        for (ThesisSection s : allSections) {
            if (sectionId.equals(s.getParentId())) {
                deleteSectionAndDescendants(thesisId, s.getId());
            }
        }
        sectionRepository.deleteById(sectionId);
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

    /**
     * 根据论文已关联的参考文献数据，自动重写 "参考文献" 章节内容
     * <p>
     * 从 {@link ThesisReference} 中读取所有引用文献，按 sortOrder 排序，
     * 通过 {@link Gbt7714Formatter} 格式化为 GB/T 7714 文本，
     * 生成 HTML 内容写入 "参考文献" 章节。
     *
     * @param thesisId 论文 ID
     * @return 更新后的参考文献章节，若无参考文献章节则返回 null
     */
    @Transactional
    public ThesisSection rebuildReferenceSection(Long thesisId) {
        // 1. 查找 "参考文献" 章节
        List<ThesisSection> sections = sectionRepository.findByThesisIdOrderBySortOrder(thesisId);
        ThesisSection refSection = sections.stream()
                .filter(s -> s.getTitle() != null && s.getTitle().contains("参考文献"))
                .findFirst()
                .orElse(null);

        if (refSection == null) {
            log.warn("论文 {} 没有「参考文献」章节，跳过重建", thesisId);
            return null;
        }

        // 2. 获取已关联的参考文献
        List<ThesisReference> refs = thesisReferenceRepository.findByThesisIdOrderBySortOrder(thesisId);

        // 3. 对每条引用，查找原始 Reference 获取 type，调用 Gbt7714Formatter 格式化
        List<String> formattedList = new ArrayList<>();
        for (int i = 0; i < refs.size(); i++) {
            ThesisReference tr = refs.get(i);
            String formatted;
            if (tr.getReferenceId() != null) {
                // 从全局文献库获取完整信息（含 type 用于正确格式化）
                Optional<Reference> optRef = referenceRepository.findById(tr.getReferenceId());
                if (optRef.isPresent()) {
                    formatted = gbt7714Formatter.format(optRef.get());
                } else {
                    // 原始文献已删除，用快照数据兜底
                    formatted = formatFromSnapshot(tr);
                }
            } else {
                formatted = formatFromSnapshot(tr);
            }
            formattedList.add(formatted);
        }

        // 4. 生成 HTML 内容
        StringBuilder html = new StringBuilder();
        for (int i = 0; i < formattedList.size(); i++) {
            html.append("<p>[").append(i + 1).append("] ")
                .append(escapeHtml(formattedList.get(i)))
                .append("</p>");
        }

        // 5. 更新章节内容
        refSection.setContent(html.toString());
        refSection = sectionRepository.save(refSection);

        log.info("论文 {} 的参考文献章节已重建，共 {} 条引用", thesisId, formattedList.size());
        return refSection;
    }

    /**
     * 从 ThesisReference 快照数据格式化参考文献（无 type 信息的兜底方案）
     */
    private String formatFromSnapshot(ThesisReference tr) {
        StringBuilder sb = new StringBuilder();
        sb.append(tr.getAuthors() != null ? tr.getAuthors() : "").append(". ");
        sb.append(tr.getTitle() != null ? tr.getTitle() : "").append(". ");
        if (tr.getJournal() != null && !tr.getJournal().isBlank()) {
            sb.append(tr.getJournal());
            if (tr.getYear() != null) {
                sb.append(", ").append(tr.getYear());
            }
            if (tr.getVolume() != null && !tr.getVolume().isBlank()) {
                sb.append(", ").append(tr.getVolume());
                if (tr.getIssue() != null && !tr.getIssue().isBlank()) {
                    sb.append("(").append(tr.getIssue()).append(")");
                }
            }
            if (tr.getPages() != null && !tr.getPages().isBlank()) {
                sb.append(": ").append(tr.getPages());
            }
        }
        sb.append(".");
        return sb.toString();
    }

    /** HTML 特殊字符转义 */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
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
