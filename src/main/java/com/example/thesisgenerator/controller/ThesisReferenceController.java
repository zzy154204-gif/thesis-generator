package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.ThesisReference;
import com.example.thesisgenerator.service.ThesisReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 论文学位参考文献关联 Controller
 * 管理论文与全局文献库之间的关联
 */
@RestController
@RequestMapping("/api/v1/papers/{thesisId}/references")
@RequiredArgsConstructor
public class ThesisReferenceController {

    private final ThesisReferenceService thesisReferenceService;

    /**
     * 获取论文已关联的参考文献列表
     * GET /api/v1/papers/{thesisId}/references
     */
    @GetMapping
    public Result<List<ThesisReference>> list(@PathVariable Long thesisId) {
        return Result.ok(thesisReferenceService.getThesisReferences(thesisId));
    }

    /**
     * 为论文添加一条参考文献（从全局文献库引用）
     * POST /api/v1/papers/{thesisId}/references
     */
    @PostMapping
    public Result<ThesisReference> add(@PathVariable Long thesisId,
                                        @RequestBody Map<String, Object> body) {
        Long referenceId = Long.valueOf(body.get("referenceId").toString());
        return Result.ok(thesisReferenceService.addReference(thesisId, referenceId));
    }

    /**
     * 从论文中移除参考文献
     * DELETE /api/v1/papers/{thesisId}/references/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> remove(@PathVariable Long thesisId, @PathVariable Long id) {
        thesisReferenceService.removeReference(thesisId, id);
        return Result.ok();
    }

    /**
     * 更新引用排序
     * PUT /api/v1/papers/{thesisId}/references/order
     */
    @PutMapping("/order")
    public Result<?> updateOrder(@PathVariable Long thesisId,
                                  @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> ids = ((List<Object>) body.get("ids"))
                .stream().map(v -> Long.valueOf(v.toString())).toList();
        thesisReferenceService.updateOrder(thesisId, ids);
        return Result.ok();
    }
}
