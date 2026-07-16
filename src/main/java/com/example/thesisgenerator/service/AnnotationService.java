package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Annotation;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.AnnotationRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnotationService {

    private final AnnotationRepository annotationRepository;
    private final ThesisSectionRepository thesisSectionRepository;

    /**
     * 教师创建批注
     */
    @Transactional
    public Annotation createAnnotation(Long thesisId, Long sectionId, Long teacherId,
                                        int startOffset, int textLength,
                                        String selectedText, String content) {
        // 校验章节存在且属于该论文
        ThesisSection section = thesisSectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(404, "章节不存在"));
        if (!section.getThesisId().equals(thesisId)) {
            throw new BusinessException(400, "章节不属于该论文");
        }

        // 偏移量合法性校验
        if (startOffset < 0) {
            throw new BusinessException(400, "起始偏移不能为负数");
        }
        if (textLength <= 0) {
            throw new BusinessException(400, "文本长度必须大于0");
        }

        Annotation annotation = new Annotation();
        annotation.setThesisId(thesisId);
        annotation.setSectionId(sectionId);
        annotation.setTeacherId(teacherId);
        annotation.setStartOffset(startOffset);
        annotation.setTextLength(textLength);
        annotation.setSelectedText(selectedText);
        annotation.setContent(content);

        return annotationRepository.save(annotation);
    }

    /**
     * 教师修改批注内容
     */
    @Transactional
    public Annotation updateAnnotation(Long annotationId, String newContent) {
        Annotation annotation = annotationRepository.findById(annotationId)
                .orElseThrow(() -> new BusinessException(404, "批注不存在"));
        annotation.setContent(newContent);
        return annotationRepository.save(annotation);
    }

    /**
     * 删除单个批注
     */
    @Transactional
    public void deleteAnnotation(Long annotationId) {
        if (!annotationRepository.existsById(annotationId)) {
            throw new BusinessException(404, "批注不存在");
        }
        annotationRepository.deleteById(annotationId);
    }

    /**
     * 查询某论文全部批注（按创建时间排序）
     */
    public List<Annotation> getAnnotationsByThesis(Long thesisId) {
        return annotationRepository.findByThesisIdOrderByCreatedAt(thesisId);
    }

    /**
     * 查询某论文某章节的全部批注（按偏移量排序）
     */
    public List<Annotation> getAnnotationsBySection(Long thesisId, Long sectionId) {
        return annotationRepository.findByThesisIdAndSectionIdOrderByStartOffset(thesisId, sectionId);
    }
}
