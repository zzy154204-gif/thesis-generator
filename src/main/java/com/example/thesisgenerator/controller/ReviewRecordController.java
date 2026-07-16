package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.config.RoleRequired;
import com.example.thesisgenerator.dto.ReviewRequest;
import com.example.thesisgenerator.entity.ReviewRecord;
import com.example.thesisgenerator.service.ReviewRecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewRecordController {

    private final ReviewRecordService reviewRecordService;

    /**
     * 教师提交批阅结果（通过/退回+打分）
     * POST /api/v1/reviews/{thesisId}
     */
    @PostMapping("/{thesisId}")
    @RoleRequired("TEACHER")
    public Result<ReviewRecord> submitReview(@PathVariable Long thesisId,
                                              @Valid @RequestBody ReviewRequest req,
                                              HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        ReviewRecord record = reviewRecordService.submitReview(
                thesisId, teacherId,
                req.getCommentHtml(), req.getScore(), req.getGrade(),
                req.getAction(), req.getReturnReason());
        return Result.ok(record);
    }

    /**
     * 查看论文批阅历史
     * GET /api/v1/reviews/thesis/{thesisId}
     */
    @GetMapping("/thesis/{thesisId}")
    public Result<List<ReviewRecord>> getReviewHistory(@PathVariable Long thesisId) {
        return Result.ok(reviewRecordService.getReviewHistory(thesisId));
    }
}
