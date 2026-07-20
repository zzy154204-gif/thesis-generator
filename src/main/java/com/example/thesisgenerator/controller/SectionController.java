package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.service.SectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/papers/{paperId}/sections")
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    /**
     * 获取章节树
     * GET /api/v1/papers/{paperId}/sections
     */
    @GetMapping
    public Result<List<Map<String, Object>>> getSections(@PathVariable Long paperId) {
        return Result.ok(sectionService.getSectionTree(paperId));
    }

    /**
     * 新增章节
     * POST /api/v1/papers/{paperId}/sections
     */
    @PostMapping
    public Result<ThesisSection> createSection(@PathVariable Long paperId,
                                               @RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        Long parentId = body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        return Result.ok(sectionService.createSection(paperId, title, parentId));
    }

    /**
     * 获取章节详情（含内容）
     * GET /api/v1/papers/{paperId}/sections/{sectionId}
     */
    @GetMapping("/{sectionId}")
    public Result<ThesisSection> getSection(@PathVariable Long paperId,
                                            @PathVariable Long sectionId) {
        return Result.ok(sectionService.getSection(paperId, sectionId));
    }

    /**
     * 保存章节内容
     * PUT /api/v1/papers/{paperId}/sections/{sectionId}
     */
    @PutMapping("/{sectionId}")
    public Result<ThesisSection> saveSection(@PathVariable Long paperId,
                                            @PathVariable Long sectionId,
                                            @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        String title = (String) body.get("title");
        return Result.ok(sectionService.saveSection(paperId, sectionId, content, title));
    }

    /**
     * 更新章节排序
     * PUT /api/v1/papers/{paperId}/sections/order
     */
    @PutMapping("/order")
    public Result<?> updateOrder(@PathVariable Long paperId,
                                 @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> rawIds = (List<Integer>) body.get("sectionIds");
        List<Long> sectionIds = rawIds.stream()
                .map(Long::valueOf)
                .toList();
        sectionService.updateOrder(paperId, sectionIds);
        return Result.ok();
    }

    /**
     * 删除章节
     * DELETE /api/v1/papers/{paperId}/sections/{sectionId}
     */
    @DeleteMapping("/{sectionId}")
    public Result<?> deleteSection(@PathVariable Long paperId,
                                   @PathVariable Long sectionId) {
        sectionService.deleteSection(paperId, sectionId);
        return Result.ok();
    }
}
