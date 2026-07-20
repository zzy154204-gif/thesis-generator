package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.config.RoleRequired;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
@RoleRequired("ADMIN")
public class AdminTemplateController {

    private final TemplateService templateService;

    /**
     * 获取模板列表（管理员：支持状态/关键字筛选）
     * GET /api/v1/admin/templates
     */
    @GetMapping
    public Result<List<Template>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(templateService.findAllAdmin(type, collegeId, status, keyword));
    }

    /**
     * 获取模板详情（管理员：含完整配置）
     * GET /api/v1/admin/templates/{id}
     */
    @GetMapping("/{id}")
    public Result<Template> get(@PathVariable Long id) {
        return Result.ok(templateService.findById(id));
    }

    /**
     * 创建模板
     * POST /api/v1/admin/templates
     */
    @PostMapping
    public Result<Template> create(@RequestBody Template template) {
        return Result.ok(templateService.create(template));
    }

    /**
     * 更新模板
     * PUT /api/v1/admin/templates/{id}
     */
    @PutMapping("/{id}")
    public Result<Template> update(@PathVariable Long id,
                                   @RequestBody Template template) {
        return Result.ok(templateService.update(id, template));
    }

    /**
     * 切换模板启用/停用
     * PATCH /api/v1/admin/templates/{id}/status
     */
    @PatchMapping("/{id}/status")
    public Result<Template> toggleStatus(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        return Result.ok(templateService.toggleStatus(id, body.get("status")));
    }

    /**
     * 删除模板
     * DELETE /api/v1/admin/templates/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }

    /**
     * 复制模板
     * POST /api/v1/admin/templates/{id}/duplicate
     */
    @PostMapping("/{id}/duplicate")
    public Result<Template> duplicate(@PathVariable Long id) {
        return Result.ok(templateService.duplicate(id));
    }
}
