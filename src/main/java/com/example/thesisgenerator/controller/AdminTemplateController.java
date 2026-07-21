package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.service.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/templates")
@RequiredArgsConstructor
public class AdminTemplateController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TemplateService templateService;

    /** 管理员查看所有模板（含已停用） */
    @GetMapping
    public Result<List<Template>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long collegeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return Result.ok(templateService.findAllAdmin(type, collegeId, status, keyword));
    }

    /** 管理员获取模板详情（含版本配置） */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        Template t = templateService.findById(id);
        TemplateVersion v = templateService.getCurrentVersion(id);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("id", t.getId());
        result.put("name", t.getName());
        result.put("type", t.getType());
        result.put("collegeId", t.getCollegeId());
        result.put("description", t.getDescription());
        result.put("enabled", t.getEnabled());

        // 解析版本配置
        if (v != null) {
            result.put("coverConfig", parseJsonArray(v.getCoverFields()));
            result.put("structure", parseJsonArray(v.getChapterStructure()));
            result.put("styles", parseJsonObject(v.getFormatConfig()));
            result.put("version", v.getVersionNumber());
            result.put("versionId", v.getId());
        }
        return Result.ok(result);
    }

    /** 新增模板（含版本配置） */
    @PostMapping
    public Result<Template> create(@RequestBody Map<String, Object> body) {
        Template t = new Template();
        t.setName((String) body.get("name"));
        t.setType((String) body.get("type"));
        t.setCollegeId(body.get("collegeId") != null
                ? Long.valueOf(body.get("collegeId").toString()) : null);
        t.setDescription((String) body.get("description"));

        // 版本配置
        String coverConfig = toJson(body.get("coverConfig"));
        String structure = toJson(body.get("structure"));
        String styles = toJson(body.get("styles"));

        return Result.ok(templateService.createWithConfig(t, coverConfig, structure, styles));
    }

    /** 更新模板（含版本配置） */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Template t = templateService.findById(id);
        if (body.containsKey("name")) t.setName((String) body.get("name"));
        if (body.containsKey("type")) t.setType((String) body.get("type"));
        if (body.containsKey("collegeId")) {
            t.setCollegeId(body.get("collegeId") != null
                    ? Long.valueOf(body.get("collegeId").toString()) : null);
        }
        if (body.containsKey("description")) t.setDescription((String) body.get("description"));
        templateService.updateTemplate(t);

        // 更新版本配置（如果有）
        if (body.containsKey("coverConfig") || body.containsKey("styles") || body.containsKey("structure")) {
            Long versionId = body.get("versionId") != null
                    ? Long.valueOf(body.get("versionId").toString()) : null;
            String coverConfig = toJson(body.get("coverConfig"));
            String structure = toJson(body.get("structure"));
            String styles = toJson(body.get("styles"));
            if (versionId != null) {
                templateService.updateVersionConfig(versionId, coverConfig, structure, styles);
            }
        }

        // 返回完整数据
        return get(id);
    }

    /** 切换模板启用/停用状态 */
    @PatchMapping("/{id}/status")
    public Result<Template> toggleStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(templateService.toggleStatus(id, body.getOrDefault("status", "ENABLED")));
    }

    /** 删除模板 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return Result.ok();
    }

    /** 预览模板 */
    @GetMapping("/{id}/preview")
    public Result<Map<String, String>> preview(@PathVariable Long id) {
        Template t = templateService.findById(id);
        return Result.ok(Map.of("html", "<h1>" + t.getName() + "</h1><p>预览功能开发中</p>"));
    }

    /** 复制模板 */
    @PostMapping("/{id}/duplicate")
    public Result<Template> duplicate(@PathVariable Long id) {
        return Result.ok(templateService.duplicate(id));
    }

    // ===== 工具方法 =====
    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return List.of();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = MAPPER.readValue(json, List.class);
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.isBlank() || json.equals("{}")) return Map.of();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = MAPPER.readValue(json, Map.class);
            return map;
        } catch (Exception e) {
            return Map.of();
        }
    }
}
