package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.service.DraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    /**
     * 保存章节草稿
     * POST /api/v1/drafts
     */
    @PostMapping
    public Result<?> saveDraft(@RequestBody Map<String, Object> body) {
        Long thesisId = Long.valueOf(body.get("thesisId").toString());
        Long sectionId = Long.valueOf(body.get("sectionId").toString());
        String content = (String) body.get("content");
        draftService.saveDraft(thesisId, sectionId, content);
        return Result.ok();
    }

    /**
     * 读取章节草稿
     * GET /api/v1/drafts?thesisId=1&sectionId=2
     */
    @GetMapping
    public Result<String> getDraft(@RequestParam Long thesisId,
                                    @RequestParam Long sectionId) {
        String draft = draftService.getDraft(thesisId, sectionId);
        return Result.ok(draft);
    }

    /**
     * 删除章节草稿
     * DELETE /api/v1/drafts?thesisId=1&sectionId=2
     */
    @DeleteMapping
    public Result<?> deleteDraft(@RequestParam Long thesisId,
                                  @RequestParam Long sectionId) {
        draftService.deleteDraft(thesisId, sectionId);
        return Result.ok();
    }
}
