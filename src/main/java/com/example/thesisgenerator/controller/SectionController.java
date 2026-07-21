package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.service.ThesisSectionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 论文章节管理 Controller
 * 从 PaperController 拆分而来
 */
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class SectionController {

    private final ThesisSectionService sectionService;

    /**
     * 获取章节树
     * GET /api/v1/papers/{id}/sections
     */
    @GetMapping("/{id}/sections")
    public Result<List<ThesisSection>> getSections(HttpServletRequest request,
                                                    @PathVariable("id") Long thesisId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(sectionService.getSections(thesisId, userId));
    }

    /**
     * 新增章节
     * POST /api/v1/papers/{id}/sections
     */
    @PostMapping("/{id}/sections")
    public Result<ThesisSection> createSection(HttpServletRequest request,
                                                @PathVariable("id") Long thesisId,
                                                @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = (String) body.get("title");
        Long parentId = body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        return Result.ok(sectionService.createSection(thesisId, userId, title, parentId));
    }

    /**
     * 获取单个章节内容
     * GET /api/v1/papers/{id}/sections/{sectionId}
     */
    @GetMapping("/{id}/sections/{sectionId}")
    public Result<ThesisSection> getSection(HttpServletRequest request,
                                            @PathVariable("id") Long thesisId,
                                            @PathVariable Long sectionId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(sectionService.getSection(thesisId, sectionId, userId));
    }

    /**
     * 保存章节内容
     * PUT /api/v1/papers/{id}/sections/{sectionId}
     */
    @PutMapping("/{id}/sections/{sectionId}")
    public Result<ThesisSection> saveSection(HttpServletRequest request,
                                             @PathVariable("id") Long thesisId,
                                             @PathVariable Long sectionId,
                                             @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String content = (String) body.get("content");
        String title = (String) body.get("title");
        return Result.ok(sectionService.updateSection(thesisId, sectionId, userId, content, title));
    }

    /**
     * 更新章节排序
     * PUT /api/v1/papers/{id}/sections/order
     */
    @PutMapping("/{id}/sections/order")
    public Result<?> updateSectionsOrder(HttpServletRequest request,
                                         @PathVariable("id") Long thesisId,
                                         @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        @SuppressWarnings("unchecked")
        List<Long> sectionIds = ((List<Object>) body.get("sectionIds"))
                .stream().map(v -> Long.valueOf(v.toString())).toList();
        sectionService.updateOrder(thesisId, userId, sectionIds);
        return Result.ok();
    }

    /**
     * 删除章节
     * DELETE /api/v1/papers/{id}/sections/{sectionId}
     */
    @DeleteMapping("/{id}/sections/{sectionId}")
    public Result<?> deleteSection(HttpServletRequest request,
                                   @PathVariable("id") Long thesisId,
                                   @PathVariable Long sectionId) {
        Long userId = (Long) request.getAttribute("userId");
        sectionService.deleteSection(thesisId, sectionId, userId);
        return Result.ok();
    }
}
