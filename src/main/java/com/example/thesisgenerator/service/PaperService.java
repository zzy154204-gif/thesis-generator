package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.repository.ThesisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperService {

    private final ThesisRepository thesisRepository;

    /**
     * 获取当前学生的论文列表
     */
    public List<Thesis> getPapers(Long studentId, String keyword, String sortBy) {
        Sort sort = "createdAt".equals(sortBy)
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "updatedAt");

        List<Thesis> papers = thesisRepository.findByStudentIdOrderByUpdatedAtDesc(studentId);

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
     * 新建论文
     */
    public Thesis createPaper(Long studentId, String title, Long collegeId, Long templateVersionId) {
        Thesis thesis = new Thesis();
        thesis.setStudentId(studentId);
        thesis.setTitle(title != null && !title.isBlank() ? title : "未命名论文");
        thesis.setCollegeId(collegeId);
        thesis.setTemplateVersionId(templateVersionId);
        thesis.setStatus("DRAFT");
        thesis.setIsLocked(false);
        return thesisRepository.save(thesis);
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
