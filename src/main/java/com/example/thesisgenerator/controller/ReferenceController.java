package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.service.ReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 参考文献 REST 控制器
 */
@RestController
@RequestMapping("/api/references")
@RequiredArgsConstructor
public class ReferenceController {

    private final ReferenceService referenceService;

    /** 获取所有文献 */
    @GetMapping
    public List<Reference> getAll() {
        return referenceService.findAll();
    }

    /** 根据 ID 获取单条文献 */
    @GetMapping("/{id}")
    public ResponseEntity<Reference> getById(@PathVariable Long id) {
        return referenceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** 新增文献 */
    @PostMapping
    public ResponseEntity<Reference> create(@RequestBody Reference reference) {
        Reference created = referenceService.create(reference);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 更新文献 */
    @PutMapping("/{id}")
    public ResponseEntity<Reference> update(@PathVariable Long id, @RequestBody Reference reference) {
        try {
            Reference updated = referenceService.update(id, reference);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 删除文献 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        referenceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 按作者搜索 */
    @GetMapping("/search/author")
    public List<Reference> searchByAuthor(@RequestParam String keyword) {
        return referenceService.searchByAuthor(keyword);
    }

    /** 按标题搜索 */
    @GetMapping("/search/title")
    public List<Reference> searchByTitle(@RequestParam String keyword) {
        return referenceService.searchByTitle(keyword);
    }

    /** 获取单条文献的 GB/T 7714 格式化文本 */
    @GetMapping("/{id}/format")
    public ResponseEntity<Map<String, String>> format(@PathVariable Long id) {
        try {
            String formatted = referenceService.formatById(id);
            return ResponseEntity.ok(Map.of("formatted", formatted));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 获取所有文献的 GB/T 7714 格式化列表 */
    @GetMapping("/format/all")
    public List<String> formatAll() {
        return referenceService.formatAll();
    }
}
