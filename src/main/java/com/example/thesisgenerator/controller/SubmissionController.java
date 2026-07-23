package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.config.RoleRequired;
import com.example.thesisgenerator.entity.Submission;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * 学生提交论文
     * POST /api/v1/submissions/{thesisId}
     */
    @PostMapping("/{thesisId}")
    @RoleRequired("STUDENT")
    public Result<Submission> submitThesis(@PathVariable Long thesisId,
                                           HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        Submission submission = submissionService.submitThesis(thesisId, studentId);
        return Result.ok(submission);
    }

    /**
     * 学生撤回提交
     * POST /api/v1/submissions/{thesisId}/withdraw
     */
    @PostMapping("/{thesisId}/withdraw")
    @RoleRequired("STUDENT")
    public Result<Thesis> withdrawSubmission(@PathVariable Long thesisId,
                                              HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        Thesis thesis = submissionService.withdrawSubmission(thesisId, studentId);
        return Result.ok(thesis);
    }

    /**
     * 查看论文提交历史
     * GET /api/v1/submissions/thesis/{thesisId}
     */
    @GetMapping("/thesis/{thesisId}")
    public Result<List<Submission>> getSubmissionHistory(@PathVariable Long thesisId) {
        return Result.ok(submissionService.getSubmissionHistory(thesisId));
    }

    /**
     * 获取学生所有提交记录（带论文信息）
     * GET /api/v1/submissions/records
     */
    @GetMapping("/records")
    @RoleRequired("STUDENT")
    public Result<List<Map<String, Object>>> getSubmissionRecords(HttpServletRequest request) {
        Long studentId = (Long) request.getAttribute("userId");
        return Result.ok(submissionService.getSubmissionRecords(studentId));
    }
}
