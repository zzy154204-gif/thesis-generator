package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.service.PaperService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;

    /**
     * 获取论文列表
     * GET /api/v1/papers?keyword=&sortBy=
     */
    @GetMapping
    public Result<List<Thesis>> list(HttpServletRequest request,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String sortBy) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.ok(paperService.getPapers(studentId, keyword, sortBy));
    }

    /**
     * 新建论文
     * POST /api/v1/papers
     */
    @PostMapping
    public Result<Thesis> create(HttpServletRequest request,
                                  @RequestBody Map<String, Object> body) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "请先登录");
        }
        String title = (String) body.get("title");
        Long collegeId = body.get("collegeId") != null
                ? Long.valueOf(body.get("collegeId").toString()) : null;
        Long templateVersionId = body.get("templateVersionId") != null
                ? Long.valueOf(body.get("templateVersionId").toString()) : null;
        return Result.ok(paperService.createPaper(studentId, title, collegeId, templateVersionId));
    }

    /**
     * 获取论文详情
     * GET /api/v1/papers/{id}
     */
    @GetMapping("/{id}")
    public Result<Thesis> get(HttpServletRequest request,
                               @PathVariable Long id) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "请先登录");
        }
        return Result.ok(paperService.getPaper(id, studentId));
    }

    /**
     * 更新论文信息
     * PUT /api/v1/papers/{id}
     */
    @PutMapping("/{id}")
    public Result<Thesis> update(HttpServletRequest request,
                                  @PathVariable Long id,
                                  @RequestBody Map<String, Object> body) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "请先登录");
        }
        String title = (String) body.get("title");
        Long templateVersionId = body.get("templateVersionId") != null
                ? Long.valueOf(body.get("templateVersionId").toString()) : null;
        Long collegeId = body.get("collegeId") != null
                ? Long.valueOf(body.get("collegeId").toString()) : null;
        return Result.ok(paperService.updatePaper(id, studentId, title, templateVersionId, collegeId));
    }

    /**
     * 删除论文
     * DELETE /api/v1/papers/{id}
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request,
                             @PathVariable Long id) {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            return Result.error(401, "请先登录");
        }
        paperService.deletePaper(id, studentId);
        return Result.ok();
    }

    /**
     * 导出论文
     * GET /api/v1/papers/{id}/export?format=DOCX|PDF
     */
    @GetMapping("/{id}/export")
    public void export(HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable Long id,
                       @RequestParam(defaultValue = "DOCX") String format) throws IOException {
        Long studentId = (Long) request.getAttribute("userId");
        if (studentId == null) {
            response.sendError(401, "请先登录");
            return;
        }
        Thesis thesis = paperService.getPaper(id, studentId);

        if ("PDF".equalsIgnoreCase(format)) {
            // PDF 导出暂未实现，返回提示
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":501,\"message\":\"PDF 导出暂未实现，请使用 DOCX 格式\"}");
            return;
        }

        // DOCX 导出
        String filename = thesis.getTitle() + ".docx";
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (XWPFDocument doc = new XWPFDocument();
             OutputStream out = response.getOutputStream()) {

            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(thesis.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("宋体");

            // 空行
            XWPFParagraph empty = doc.createParagraph();
            empty.createRun().setText("");

            // 正文占位
            XWPFParagraph bodyPara = doc.createParagraph();
            bodyPara.setIndentationFirstLine(480);
            XWPFRun bodyRun = bodyPara.createRun();
            bodyRun.setText("（此处为论文正文内容，请在编辑器中完善各章节后重新导出）");
            bodyRun.setFontSize(12);
            bodyRun.setFontFamily("宋体");

            doc.write(out);
        }
    }
}
