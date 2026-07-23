package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.TemplateRepository;
import com.example.thesisgenerator.repository.TemplateVersionRepository;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final ThesisRepository thesisRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final ThesisSectionRepository sectionRepository;
    private final TemplateRepository templateRepository;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 默认章节结构（无模板时使用）
     * <p>
     * 顶级节点包含 children 数组，支持树形嵌套。
     * "参考文献" 始终排在最后。
     */
    private static final String DEFAULT_CHAPTERS_JSON = "[" +
            "{\"title\":\"摘要\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"第一章 绪论\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"第二章 相关技术概述\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"第三章 系统设计与实现\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"第四章 实验与分析\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"第五章 总结与展望\",\"level\":1,\"children\":[]}," +
            "{\"title\":\"参考文献\",\"level\":1,\"children\":[]}" +
            "]";

    /**
     * 获取当前学生的论文列表
     */
    public List<Thesis> getPapers(Long studentId, String keyword, String sortBy) {
        Sort sort = "createdAt".equals(sortBy)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "updatedAt");

        List<Thesis> papers = thesisRepository.findByStudentIdOrderByUpdatedAtDesc(studentId);

        // 填充模板名称
        populateTemplateNames(papers);

        // 关键字搜索（内存过滤，数据量小时足够）
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            papers = papers.stream()
                    .filter(p -> p.getTitle() != null && p.getTitle().toLowerCase().contains(kw))
                    .toList();
        }

        // 排序处理
        if ("createdAt".equals(sortBy)) {
            papers = papers.stream()
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .toList();
        }

        return papers;
    }

    /**
     * 为论文列表填充 templateName 字段
     */
    private void populateTemplateNames(List<Thesis> papers) {
        var versionIds = papers.stream()
                .map(Thesis::getTemplateVersionId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (versionIds.isEmpty()) return;

        var versions = templateVersionRepository.findAllById(versionIds);
        java.util.Map<Long, TemplateVersion> versionMap = new java.util.HashMap<>();
        for (TemplateVersion tv : versions) {
            versionMap.put(tv.getId(), tv);
        }

        var templateIds = versions.stream()
                .map(TemplateVersion::getTemplateId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (templateIds.isEmpty()) return;

        var templates = templateRepository.findAllById(templateIds);
        java.util.Map<Long, String> templateNameMap = new java.util.HashMap<>();
        for (Template t : templates) {
            templateNameMap.put(t.getId(), t.getName());
        }

        for (Thesis thesis : papers) {
            if (thesis.getTemplateVersionId() == null) continue;
            TemplateVersion tv = versionMap.get(thesis.getTemplateVersionId());
            if (tv != null) {
                thesis.setTemplateName(templateNameMap.get(tv.getTemplateId()));
            }
        }
    }

    /**
     * 新建论文（自动创建默认章节结构）
     */
    @Transactional
    public Thesis createPaper(Long studentId, String title, Long collegeId, Long templateVersionId, Long teacherId) {
        Thesis thesis = new Thesis();
        thesis.setStudentId(studentId);
        thesis.setTitle(title != null && !title.isBlank() ? title : "未命名论文");
        thesis.setCollegeId(collegeId);
        thesis.setTemplateVersionId(templateVersionId);
        thesis.setTeacherId(teacherId);
        thesis.setStatus("DRAFT");
        thesis.setIsLocked(false);
        thesis = thesisRepository.save(thesis);

        // 自动创建默认章节结构
        initDefaultSections(thesis.getId(), templateVersionId);

        return thesis;
    }

    /**
     * 为新论文初始化默认章节结构
     * <p>
     * 优先使用模板的 chapterStructure；无模板时使用内置默认章节。
     * 支持嵌套 JSON（含 children 数组）和扁平 JSON（仅有 title/level）两种格式。
     * "参考文献" 始终排在最后。
     */
    private void initDefaultSections(Long thesisId, Long templateVersionId) {
        String chaptersJson = DEFAULT_CHAPTERS_JSON;

        if (templateVersionId != null) {
            var optVersion = templateVersionRepository.findById(templateVersionId);
            if (optVersion.isPresent()) {
                TemplateVersion tv = optVersion.get();
                if (tv.getChapterStructure() != null && !tv.getChapterStructure().isBlank()) {
                    chaptersJson = tv.getChapterStructure();
                }
            }
        }

        try {
            List<Map<String, Object>> chapters = objectMapper.readValue(
                    chaptersJson, new TypeReference<List<Map<String, Object>>>() {});

            // ★ 强制清理：所有一级章节的 children 必须为空数组，不允许任何默认子章节
            for (Map<String, Object> ch : chapters) {
                ch.put("children", new java.util.ArrayList<>());
            }

            // 将 "参考文献"（及其子节点）整体移到末尾
            List<Map<String, Object>> ordered = new java.util.ArrayList<>(chapters);
            int refIdx = -1;
            for (int i = 0; i < ordered.size(); i++) {
                String title = (String) ordered.get(i).get("title");
                if (title != null && title.contains("参考文献")) {
                    refIdx = i;
                    break;
                }
            }
            if (refIdx >= 0 && refIdx < ordered.size() - 1) {
                Map<String, Object> refChapter = ordered.remove(refIdx);
                ordered.add(refChapter);
            }

            // 递归创建章节（顶级 parentId = null）
            int[] sortCounter = new int[1];
            for (Map<String, Object> ch : ordered) {
                createSectionRecursive(thesisId, ch, null, sortCounter);
            }
        } catch (Exception e) {
            log.warn("解析默认章节 JSON 失败，使用兜底结构", e);
            createMinimalDefaultSections(thesisId);
        }
    }

    /**
     * 递归创建章节及其子章节
     */
    private void createSectionRecursive(Long thesisId, Map<String, Object> node,
                                        Long parentId, int[] counter) {
        String title = (String) node.get("title");
        if (title == null || title.isBlank()) return;

        ThesisSection section = new ThesisSection();
        section.setThesisId(thesisId);
        section.setTitle(title);
        section.setSectionKey("chapter" + (counter[0] + 1));
        section.setContent("");
        section.setDraftContent("");
        section.setSectionType("chapter");
        section.setParentId(parentId);
        section.setSortOrder(counter[0]++);
        section = sectionRepository.save(section);

        // 递归创建子章节
        Object childrenObj = node.get("children");
        if (childrenObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
            for (Map<String, Object> child : children) {
                createSectionRecursive(thesisId, child, section.getId(), counter);
            }
        }
    }

    /**
     * 最简默认章节（JSON 解析失败时的兜底）
     */
    private void createMinimalDefaultSections(Long thesisId) {
        String[] titles = {"摘要", "第一章 绪论", "第二章 相关技术概述",
                "第三章 系统设计与实现", "第四章 实验与分析",
                "第五章 总结与展望", "参考文献"};
        for (int i = 0; i < titles.length; i++) {
            ThesisSection section = new ThesisSection();
            section.setThesisId(thesisId);
            section.setTitle(titles[i]);
            section.setSectionKey("chapter" + (i + 1));
            section.setContent("");
            section.setDraftContent("");
            section.setSectionType("chapter");
            section.setParentId(null);
            section.setSortOrder(i);
            sectionRepository.save(section);
        }
    }

    /**
     * 获取论文详情（仅限本人）
     */
    public Thesis getPaper(Long id, Long studentId) {
        Thesis thesis = thesisRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        if (!thesis.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "无权访问该论文");
        }
        return thesis;
    }

    /**
     * 更新论文信息（仅限本人，且论文未锁定）
     */
    public Thesis updatePaper(Long id, Long studentId, String title, Long templateVersionId, Long collegeId) {
        Thesis thesis = getPaper(id, studentId);
        if (Boolean.TRUE.equals(thesis.getIsLocked())) {
            throw new BusinessException(400, "论文已锁定，无法编辑");
        }
        if (title != null && !title.isBlank()) {
            thesis.setTitle(title);
        }
        if (templateVersionId != null) {
            thesis.setTemplateVersionId(templateVersionId);
        }
        if (collegeId != null) {
            thesis.setCollegeId(collegeId);
        }
        return thesisRepository.save(thesis);
    }

    /**
     * 删除论文（仅限本人，且论文未锁定）
     */
    public void deletePaper(Long id, Long studentId) {
        Thesis thesis = getPaper(id, studentId);
        if (Boolean.TRUE.equals(thesis.getIsLocked())) {
            throw new BusinessException(400, "论文已锁定，无法删除");
        }
        thesisRepository.delete(thesis);
    }
}
