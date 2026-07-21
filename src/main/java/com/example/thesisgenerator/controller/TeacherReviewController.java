package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Annotation;
import com.example.thesisgenerator.entity.ReviewRecord;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.service.AnnotationService;
import com.example.thesisgenerator.service.ReviewRecordService;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/teacher/reviews")
@RequiredArgsConstructor
public class TeacherReviewController {

    private final ThesisRepository thesisRepository;
    private final UserRepository userRepository;
    private final AnnotationService annotationService;
    private final ReviewRecordService reviewRecordService;

    /**
     * 获取待批阅列表
     * GET /api/v1/teacher/reviews/pending
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> getPendingList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        List<Thesis> allSubmitted = thesisRepository.findByStatus("SUBMITTED");

        // 关键词过滤
        if (keyword != null && !keyword.isBlank()) {
            allSubmitted = allSubmitted.stream()
                    .filter(t -> t.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }

        int total = allSubmitted.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Thesis> pageData = start >= total ? List.of() : allSubmitted.subList(start, end);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Thesis t : pageData) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", t.getId());
            item.put("title", t.getTitle());
            item.put("status", t.getStatus());
            item.put("submittedAt", t.getUpdatedAt());
            // 获取学生信息
            User student = userRepository.findById(t.getStudentId()).orElse(null);
            item.put("studentName", student != null ? student.getRealName() : "未知");
            item.put("studentId", student != null ? student.getUsername() : "");
            item.put("course", ""); // 课程字段当前未存储
            list.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return Result.ok(result);
    }

    /**
     * 获取批阅详情（论文 + 批注）
     * GET /api/v1/teacher/reviews/{paperId}
     */
    @GetMapping("/{paperId}")
    public Result<Map<String, Object>> getReviewDetail(@PathVariable Long paperId) {
        Thesis thesis = thesisRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));

        User student = userRepository.findById(thesis.getStudentId()).orElse(null);
        List<Annotation> annotations = annotationService.getAnnotationsByThesis(paperId);

        Map<String, Object> paper = new LinkedHashMap<>();
        paper.put("id", thesis.getId());
        paper.put("title", thesis.getTitle());
        paper.put("status", thesis.getStatus());
        paper.put("studentName", student != null ? student.getRealName() : "未知");
        paper.put("studentId", student != null ? student.getUsername() : "");
        paper.put("submittedAt", thesis.getUpdatedAt());

        // 构建批注响应
        List<Map<String, Object>> annList = new ArrayList<>();
        for (Annotation a : annotations) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("content", a.getContent());
            item.put("selectedText", a.getSelectedText());
            item.put("startOffset", a.getStartOffset());
            item.put("textLength", a.getTextLength());
            item.put("sectionId", a.getSectionId());
            item.put("author", "教师");
            item.put("createdAt", a.getCreatedAt());
            annList.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paper", paper);
        result.put("annotations", annList);
        return Result.ok(result);
    }

    /**
     * 暂存评语和评分
     * POST /api/v1/teacher/reviews/{paperId}/draft
     */
    @PostMapping("/{paperId}/draft")
    public Result<Void> saveDraft(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String comment = (String) body.getOrDefault("comment", "");
        Integer score = body.get("score") != null ? Integer.parseInt(body.get("score").toString()) : null;
        String grade = (String) body.getOrDefault("grade", "");

        // 暂存：使用 action="DRAFT" 不改变论文状态
        reviewRecordService.submitReview(paperId, teacherId, comment, score, grade, "DRAFT", null);
        return Result.ok();
    }

    /**
     * 退回论文
     * POST /api/v1/teacher/reviews/{paperId}/return
     */
    @PostMapping("/{paperId}/return")
    public Result<ReviewRecord> returnPaper(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String comment = (String) body.getOrDefault("comment", "");
        String reason = (String) body.getOrDefault("reason", "");
        return Result.ok(reviewRecordService.submitReview(
                paperId, teacherId, comment, null, null, "RETURNED", reason));
    }

    /**
     * 通过论文
     * POST /api/v1/teacher/reviews/{paperId}/approve
     */
    @PostMapping("/{paperId}/approve")
    public Result<ReviewRecord> approvePaper(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        String comment = (String) body.getOrDefault("comment", "");
        Integer score = body.get("score") != null ? Integer.parseInt(body.get("score").toString()) : 0;
        String grade = (String) body.getOrDefault("grade", "");
        return Result.ok(reviewRecordService.submitReview(
                paperId, teacherId, comment, score, grade, "REVIEWED", null));
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
