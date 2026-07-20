package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.config.RoleRequired;
import com.example.thesisgenerator.dto.AnnotationRequest;
import com.example.thesisgenerator.dto.AnnotationUpdateRequest;
import com.example.thesisgenerator.dto.ReviewRequest;
import com.example.thesisgenerator.entity.*;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.UserRepository;
import com.example.thesisgenerator.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/teacher/reviews")
@RoleRequired("TEACHER")
@RequiredArgsConstructor
public class TeacherReviewController {

    private final ThesisRepository thesisRepository;
    private final UserRepository userRepository;
    private final ReviewRecordService reviewRecordService;
    private final AnnotationService annotationService;

    /**
     * 获取待批阅列表
     * GET /api/v1/teacher/reviews/pending
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> getPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String course) {

        List<Thesis> all = thesisRepository.findByStatusInOrderByUpdatedAtDesc(
                List.of("SUBMITTED", "REVIEWING"));

        // 关键字过滤
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            all = all.stream()
                    .filter(t -> t.getTitle() != null && t.getTitle().toLowerCase().contains(kw))
                    .toList();
        }

        // 构建返回数据
        List<Map<String, Object>> list = new ArrayList<>();
        for (Thesis t : all) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("title", t.getTitle());
            item.put("studentId", t.getStudentId());
            item.put("status", t.getStatus());
            item.put("submittedAt", t.getUpdatedAt());

            // 获取学生信息
            userRepository.findById(t.getStudentId()).ifPresent(user -> {
                item.put("studentName", user.getRealName());
                item.put("studentSid", user.getUsername());
            });

            // course 字段（从 college 获取）
            item.put("course", course != null ? course : "");

            list.add(item);
        }

        // 简易分页
        int total = list.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Map<String, Object>> paged = from < total ? list.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", paged);
        result.put("total", total);
        return Result.ok(result);
    }

    /**
     * 获取论文批阅详情（论文信息 + 批注列表）
     * GET /api/v1/teacher/reviews/{paperId}
     */
    @GetMapping("/{paperId}")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long paperId) {
        Thesis thesis = thesisRepository.findById(paperId)
                .orElseThrow(() -> new RuntimeException("论文不存在"));

        Map<String, Object> paper = new LinkedHashMap<>();
        paper.put("id", thesis.getId());
        paper.put("title", thesis.getTitle());
        paper.put("studentId", thesis.getStudentId());
        paper.put("status", thesis.getStatus());
        paper.put("submittedAt", thesis.getUpdatedAt());

        userRepository.findById(thesis.getStudentId()).ifPresent(user -> {
            paper.put("studentName", user.getRealName());
            paper.put("studentSid", user.getUsername());
        });

        List<Annotation> annotations = annotationService.getAnnotationsByThesis(paperId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paper", paper);
        result.put("annotations", annotations);
        return Result.ok(result);
    }

    /**
     * 添加批注
     * POST /api/v1/teacher/reviews/{paperId}/annotations
     */
    @PostMapping("/{paperId}/annotations")
    public Result<Annotation> addAnnotation(@PathVariable Long paperId,
                                            @RequestBody Map<String, Object> body,
                                            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        Long sectionId = body.get("sectionId") != null
                ? Long.valueOf(body.get("sectionId").toString()) : 1L;
        int startOffset = body.get("startOffset") != null
                ? Integer.parseInt(body.get("startOffset").toString()) : 0;
        int textLength = body.get("textLength") != null
                ? Integer.parseInt(body.get("textLength").toString()) : 1;
        String selectedText = (String) body.getOrDefault("selectedText", "");
        String content = (String) body.get("content");

        return Result.ok(annotationService.createAnnotation(
                paperId, sectionId, teacherId,
                startOffset, textLength, selectedText, content));
    }

    /**
     * 更新批注
     * PUT /api/v1/teacher/reviews/{paperId}/annotations/{annotationId}
     */
    @PutMapping("/{paperId}/annotations/{annotationId}")
    public Result<Annotation> updateAnnotation(@PathVariable Long paperId,
                                               @PathVariable Long annotationId,
                                               @RequestBody Map<String, String> body) {
        return Result.ok(annotationService.updateAnnotation(annotationId, body.get("content")));
    }

    /**
     * 删除批注
     * DELETE /api/v1/teacher/reviews/{paperId}/annotations/{annotationId}
     */
    @DeleteMapping("/{paperId}/annotations/{annotationId}")
    public Result<?> deleteAnnotation(@PathVariable Long paperId,
                                      @PathVariable Long annotationId) {
        annotationService.deleteAnnotation(annotationId);
        return Result.ok();
    }

    /**
     * 暂存评语和评分（实际就是草稿，这里简单返回成功）
     * POST /api/v1/teacher/reviews/{paperId}/draft
     */
    @PostMapping("/{paperId}/draft")
    public Result<?> saveDraft(@PathVariable Long paperId,
                               @RequestBody Map<String, Object> body) {
        // 暂存功能暂不持久化，由前端 localStorage 处理
        return Result.ok();
    }

    /**
     * 退回论文
     * POST /api/v1/teacher/reviews/{paperId}/return
     */
    @PostMapping("/{paperId}/return")
    public Result<ReviewRecord> returnPaper(@PathVariable Long paperId,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String comment = body.getOrDefault("comment", "");
        ReviewRecord record = reviewRecordService.submitReview(
                paperId, teacherId, comment, null, null,
                "RETURNED", comment);
        return Result.ok(record);
    }

    /**
     * 通过论文
     * POST /api/v1/teacher/reviews/{paperId}/approve
     */
    @PostMapping("/{paperId}/approve")
    public Result<ReviewRecord> approvePaper(@PathVariable Long paperId,
                                             @RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String comment = (String) body.getOrDefault("comment", "");
        int score = body.get("score") != null
                ? Integer.parseInt(body.get("score").toString()) : 0;
        String grade = (String) body.getOrDefault("grade", "");
        ReviewRecord record = reviewRecordService.submitReview(
                paperId, teacherId, comment, score, grade,
                "REVIEWED", null);
        return Result.ok(record);
    }

    /**
     * 获取批阅历史
     * GET /api/v1/teacher/reviews/{paperId}/history
     */
    @GetMapping("/{paperId}/history")
    public Result<List<ReviewRecord>> getHistory(@PathVariable Long paperId) {
        return Result.ok(reviewRecordService.getReviewHistory(paperId));
    }
}
