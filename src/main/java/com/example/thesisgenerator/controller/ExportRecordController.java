package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.ExportRecord;
import com.example.thesisgenerator.service.ExportRecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 导出记录控制器
 */
@RestController
@RequestMapping("/api/v1/exports")
@RequiredArgsConstructor
public class ExportRecordController {

    private final ExportRecordService exportRecordService;

    /**
     * 获取当前用户的导出历史
     * GET /api/v1/exports
     */
    @GetMapping
    public Result<List<ExportRecord>> getHistory(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(exportRecordService.getUserHistory(userId));
    }
}
