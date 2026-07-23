package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Annotation;
import com.example.thesisgenerator.entity.College;
import com.example.thesisgenerator.entity.ReviewRecord;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.User;
import com.example.thesisgenerator.service.AnnotationService;
import com.example.thesisgenerator.service.ReviewRecordService;
import com.example.thesisgenerator.repository.CollegeRepository;
import com.example.thesisgenerator.repository.TemplateRepository;
import com.example.thesisgenerator.repository.TemplateVersionRepository;
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
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateRepository templateRepository;
    private final CollegeRepository collegeRepository;

    /**
     * 获取待批阅列表（仅限当前登录教师的论文）
     * GET /api/v1/teacher/reviews/pending
     */
    @GetMapping("/pending")
    public Result<Map<String, Object>> getPendingList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Long teacherId = (Long) request.getAttribute("userId");
        List<Thesis> allSubmitted = thesisRepository.findByStatusAndTeacherId("SUBMITTED", teacherId);

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
            item.put("templateName", resolveTemplateName(t.getTemplateVersionId()));
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
     * 获取批阅详情（论文 + 批注，仅限当前教师的论文）
     * GET /api/v1/teacher/reviews/{paperId}
     */
    @GetMapping("/{paperId}")
    public Result<Map<String, Object>> getReviewDetail(
            @PathVariable Long paperId,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        Thesis thesis = thesisRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        // 权限校验：只能查看自己指导的论文
        if (!teacherId.equals(thesis.getTeacherId())) {
            throw new BusinessException(403, "无权访问该论文");
        }

        User student = userRepository.findById(thesis.getStudentId()).orElse(null);
        List<Annotation> annotations = annotationService.getAnnotationsByThesis(paperId);

        Map<String, Object> paper = new LinkedHashMap<>();
        paper.put("id", thesis.getId());
        paper.put("title", thesis.getTitle());
        paper.put("status", thesis.getStatus());
        paper.put("studentName", student != null ? student.getRealName() : "未知");
        paper.put("studentId", student != null ? student.getUsername() : "");
        paper.put("submittedAt", thesis.getUpdatedAt());
        paper.put("templateName", resolveTemplateName(thesis.getTemplateVersionId()));

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
     * 暂存评语和评分（仅限自己的论文）
     * POST /api/v1/teacher/reviews/{paperId}/draft
     */
    @PostMapping("/{paperId}/draft")
    public Result<Void> saveDraft(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        checkPaperOwnership(paperId, teacherId);
        String comment = (String) body.getOrDefault("comment", "");
        Integer score = body.get("score") != null ? Integer.parseInt(body.get("score").toString()) : null;
        String grade = (String) body.getOrDefault("grade", "");

        // 暂存：使用 action="DRAFT" 不改变论文状态
        reviewRecordService.submitReview(paperId, teacherId, comment, score, grade, "DRAFT", null);
        return Result.ok();
    }

    /**
     * 退回论文（仅限自己的论文）
     * POST /api/v1/teacher/reviews/{paperId}/return
     */
    @PostMapping("/{paperId}/return")
    public Result<ReviewRecord> returnPaper(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        checkPaperOwnership(paperId, teacherId);
        String comment = (String) body.getOrDefault("comment", "");
        String reason = (String) body.getOrDefault("reason", "");
        return Result.ok(reviewRecordService.submitReview(
                paperId, teacherId, comment, null, null, "RETURNED", reason));
    }

    /**
     * 通过论文（仅限自己的论文）
     * POST /api/v1/teacher/reviews/{paperId}/approve
     */
    @PostMapping("/{paperId}/approve")
    public Result<ReviewRecord> approvePaper(
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        checkPaperOwnership(paperId, teacherId);
        String comment = (String) body.getOrDefault("comment", "");
        Integer score = body.get("score") != null ? Integer.parseInt(body.get("score").toString()) : 0;
        String grade = (String) body.getOrDefault("grade", "");
        return Result.ok(reviewRecordService.submitReview(
                paperId, teacherId, comment, score, grade, "REVIEWED", null));
    }

    /**
     * 获取批阅历史（仅限自己的论文）
     * GET /api/v1/teacher/reviews/{paperId}/history
     */
    @GetMapping("/{paperId}/history")
    public Result<List<ReviewRecord>> getHistory(
            @PathVariable Long paperId,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        checkPaperOwnership(paperId, teacherId);
        return Result.ok(reviewRecordService.getReviewHistory(paperId));
    }

    /**
     * 获取教师所有批阅记录
     * GET /api/v1/teacher/reviews/records?page=&size=
     */
    @GetMapping("/records")
    public Result<Map<String, Object>> getReviewRecords(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long teacherId = (Long) request.getAttribute("userId");

        // 查询教师的所有正式批阅记录（排除 DRAFT）
        List<ReviewRecord> allRecords = reviewRecordService.getReviewRecordsByTeacher(teacherId);

        int total = allRecords.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<ReviewRecord> pageData = start >= total ? List.of() : allRecords.subList(start, end);

        // 批量加载论文和用户信息
        Map<Long, Thesis> thesisMap = new HashMap<>();
        for (ReviewRecord r : pageData) {
            if (!thesisMap.containsKey(r.getThesisId())) {
                thesisRepository.findById(r.getThesisId()).ifPresent(t -> thesisMap.put(t.getId(), t));
            }
        }
        Map<Long, User> userMap = new HashMap<>();
        for (Thesis t : thesisMap.values()) {
            if (!userMap.containsKey(t.getStudentId())) {
                userRepository.findById(t.getStudentId()).ifPresent(u -> userMap.put(u.getId(), u));
            }
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (ReviewRecord r : pageData) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("thesisId", r.getThesisId());
            item.put("action", r.getAction());
            item.put("score", r.getScore());
            item.put("grade", r.getGrade());
            item.put("commentHtml", r.getCommentHtml());
            item.put("returnReason", r.getReturnReason());
            item.put("createdAt", r.getCreatedAt());

            Thesis thesis = thesisMap.get(r.getThesisId());
            if (thesis != null) {
                item.put("thesisTitle", thesis.getTitle());
                item.put("thesisStatus", thesis.getStatus());
                User student = userMap.get(thesis.getStudentId());
                item.put("studentName", student != null ? student.getRealName() : "未知");
                item.put("studentUsername", student != null ? student.getUsername() : "");
            } else {
                item.put("thesisTitle", "已删除的论文");
                item.put("studentName", "未知");
            }
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
     * 获取教师所有批阅记录列表（不分页，供前端详情查看）
     * GET /api/v1/teacher/reviews/records/all
     */
    @GetMapping("/records/all")
    public Result<List<ReviewRecord>> getAllReviewRecords(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.ok(reviewRecordService.getReviewRecordsByTeacher(teacherId));
    }

    /**
     * 获取所有教师列表（供学生选择指导老师时使用）
     * GET /api/v1/teacher/reviews/teachers?collegeId=
     */
    @GetMapping("/teachers")
    public Result<List<Map<String, Object>>> getTeachers(
            @RequestParam(required = false) Long collegeId) {
        // 加载学院名称映射
        Map<Long, String> collegeNames = new HashMap<>();
        if (!userRepository.findByRole("TEACHER").isEmpty()) {
            try {
                var colleges = collegeRepository.findAll();
                colleges.forEach(c -> collegeNames.put(c.getId(), c.getName()));
            } catch (Exception ignored) {}
        }

        List<User> allTeachers;
        if (collegeId != null) {
            allTeachers = userRepository.findByRoleAndCollegeId("TEACHER", collegeId);
        } else {
            allTeachers = userRepository.findByRole("TEACHER");
        }
        List<Map<String, Object>> list = allTeachers.stream().map(u -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", u.getId());
            item.put("realName", u.getRealName());
            item.put("teacherNo", u.getTeacherNo());
            item.put("collegeId", u.getCollegeId());
            item.put("college", collegeNames.getOrDefault(u.getCollegeId(), ""));
            return item;
        }).toList();
        return Result.ok(list);
    }

    // ===== 工具方法 =====

    /**
     * 校验当前教师是否有权操作该论文
     */
    private void checkPaperOwnership(Long paperId, Long teacherId) {
        Thesis thesis = thesisRepository.findById(paperId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));
        if (!teacherId.equals(thesis.getTeacherId())) {
            throw new BusinessException(403, "无权操作该论文");
        }
    }

    /**
     * 根据 templateVersionId 解析模板名称
     */
    private String resolveTemplateName(Long templateVersionId) {
        if (templateVersionId == null) return null;
        try {
            var tvOpt = templateVersionRepository.findById(templateVersionId);
            if (tvOpt.isEmpty()) return null;
            var templateOpt = templateRepository.findById(tvOpt.get().getTemplateId());
            return templateOpt.map(Template::getName).orElse(null);
        } catch (Exception e) {
            log.warn("解析模板名称失败: templateVersionId={}", templateVersionId);
            return null;
        }
    }
}
