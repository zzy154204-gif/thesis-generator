package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.service.ReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 参考文献 REST 控制器
 */
@RestController
@RequestMapping("/api/v1/references")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    /** 获取所有文献 */
    @GetMapping
    public Result<List<Reference>> getAll() {
        return Result.ok(referenceService.findAll());
    }

    /** 根据 ID 获取单条文献 */
    @GetMapping("/{id}")
    public Result<Reference> getById(@PathVariable Long id) {
        return referenceService.findById(id)
                .map(Result::ok)
                .orElse(Result.error(404, "参考文献不存在"));
    }

    /** 新增文献 */
    @PostMapping
    public Result<Reference> create(@RequestBody Reference reference) {
        return Result.ok(referenceService.create(reference));
    }

    /** 更新文献 */
    @PutMapping("/{id}")
    public Result<Reference> update(@PathVariable Long id, @RequestBody Reference reference) {
        return Result.ok(referenceService.update(id, reference));
    }

    /** 删除文献 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        referenceService.delete(id);
        return Result.ok();
    }

    /** 按作者搜索 */
    @GetMapping("/search/author")
    public Result<List<Reference>> searchByAuthor(@RequestParam String keyword) {
        return Result.ok(referenceService.searchByAuthor(keyword));
    }

    /** 按标题搜索 */
    @GetMapping("/search/title")
    public Result<List<Reference>> searchByTitle(@RequestParam String keyword) {
        return Result.ok(referenceService.searchByTitle(keyword));
    }

    /** 获取单条文献的 GB/T 7714 格式化文本 */
    @GetMapping("/{id}/format")
    public Result<Map<String, String>> format(@PathVariable Long id) {
        String formatted = referenceService.formatById(id);
        return Result.ok(Map.of("formatted", formatted));
    }

    /** 获取所有文献的 GB/T 7714 格式化列表 */
    @GetMapping("/format/all")
    public Result<List<String>> formatAll() {
        return Result.ok(referenceService.formatAll());
    }
}
