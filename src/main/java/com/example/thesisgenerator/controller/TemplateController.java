package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    // ===== 模板 CRUD =====
    @GetMapping
    public Result<List<Template>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long collegeId) {
        return Result.ok(templateService.findAll(type, collegeId));
    }

    @GetMapping("/{id}")
    public Result<Template> get(@PathVariable Long id) {
        return Result.ok(templateService.findById(id));
    }

    @PostMapping
    public Result<Template> create(@RequestBody Template template) {
        return Result.ok(templateService.create(template));
    }

    @PutMapping("/{id}")
    public Result<Template> update(@PathVariable Long id, @RequestBody Template template) {
        return Result.ok(templateService.update(id, template));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }

    // ===== 版本管理 =====
    @GetMapping("/{id}/versions")
    public Result<List<TemplateVersion>> versions(@PathVariable Long id) {
        return Result.ok(templateService.getVersions(id));
    }

    @PostMapping("/{id}/versions")
    public Result<TemplateVersion> createVersion(@PathVariable Long id) {
        return Result.ok(templateService.createVersion(id));
    }

    @PutMapping("/{id}/versions/{vid}/activate")
    public Result<TemplateVersion> activateVersion(
            @PathVariable Long id, @PathVariable Long vid) {
        return Result.ok(templateService.activateVersion(id, vid));
    }

    @PutMapping("/{id}/versions/{vid}")
    public Result<TemplateVersion> updateVersionConfig(
            @PathVariable Long id, @PathVariable Long vid,
            @RequestBody Map<String, String> config) {
        return Result.ok(templateService.updateVersionConfig(vid,
                config.get("coverFields"),
                config.get("chapterStructure"),
                config.get("formatConfig")));
    }
}
