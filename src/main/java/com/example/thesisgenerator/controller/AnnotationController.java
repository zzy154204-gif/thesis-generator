package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.config.RoleRequired;
import com.example.thesisgenerator.dto.AnnotationRequest;
import com.example.thesisgenerator.dto.AnnotationUpdateRequest;
import com.example.thesisgenerator.entity.Annotation;
import com.example.thesisgenerator.service.AnnotationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/annotations")
@RequiredArgsConstructor
public class AnnotationController {

    private final AnnotationService annotationService;

    /**
     * 教师创建批注
     * POST /api/v1/annotations
     */
    @PostMapping
    @RoleRequired("TEACHER")
    public Result<Annotation> createAnnotation(@Valid @RequestBody AnnotationRequest req,
                                                HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        Annotation annotation = annotationService.createAnnotation(
                req.getThesisId(), req.getSectionId(), teacherId,
                req.getStartOffset(), req.getTextLength(),
                req.getSelectedText(), req.getContent());
        return Result.ok(annotation);
    }

    /**
     * 教师修改批注内容
     * PUT /api/v1/annotations/{id}
     */
    @PutMapping("/{id}")
    @RoleRequired("TEACHER")
    public Result<Annotation> updateAnnotation(@PathVariable Long id,
                                                @Valid @RequestBody AnnotationUpdateRequest req) {
        return Result.ok(annotationService.updateAnnotation(id, req.getContent()));
    }

    /**
     * 教师删除批注
     * DELETE /api/v1/annotations/{id}
     */
    @DeleteMapping("/{id}")
    @RoleRequired("TEACHER")
    public Result<?> deleteAnnotation(@PathVariable Long id) {
        annotationService.deleteAnnotation(id);
        return Result.ok();
    }

    /**
     * 查看论文所有批注
     * GET /api/v1/annotations/thesis/{thesisId}
     */
    @GetMapping("/thesis/{thesisId}")
    public Result<List<Annotation>> getAnnotationsByThesis(@PathVariable Long thesisId) {
        return Result.ok(annotationService.getAnnotationsByThesis(thesisId));
    }

    /**
     * 查看论文某章节批注
     * GET /api/v1/annotations/thesis/{thesisId}/section/{sectionId}
     */
    @GetMapping("/thesis/{thesisId}/section/{sectionId}")
    public Result<List<Annotation>> getAnnotationsBySection(
            @PathVariable Long thesisId,
            @PathVariable Long sectionId) {
        return Result.ok(annotationService.getAnnotationsBySection(thesisId, sectionId));
    }
}
