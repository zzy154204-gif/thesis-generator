package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.entity.ReviewRecord;
import com.example.thesisgenerator.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论文 REST 控制器
 * <p>
 * CRUD + 导入/导出/批阅。
 * 导出逻辑委托给 {@link DocxExportService} 和 {@link PdfExportService}。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final ThesisSectionService sectionService;
    private final AnnotationService annotationService;
    private final ReviewRecordService reviewRecordService;
    private final DocxImportService docxImportService;
    private final ExportRecordService exportRecordService;
    private final DocxExportService docxExportService;
    private final PdfExportService pdfExportService;

    // ==================== 论文 CRUD ====================

    @GetMapping
    public Result<List<Thesis>> list(HttpServletRequest request,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) String sortBy) {
        Long studentId = (Long) request.getAttribute("userId");
        return Result.ok(paperService.getPapers(studentId, keyword, sortBy));
    }

    @PostMapping
    public Result<Thesis> create(HttpServletRequest request,
                                 @RequestBody Map<String, Object> body) {
        Long studentId = (Long) request.getAttribute("userId");
        String title = (String) body.get("title");
        Long collegeId = body.get("collegeId") != null
                ? Long.valueOf(body.get("collegeId").toString()) : null;
        Long templateVersionId = body.get("templateVersionId") != null
                ? Long.valueOf(body.get("templateVersionId").toString()) : null;
        Long teacherId = body.get("teacherId") != null
                ? Long.valueOf(body.get("teacherId").toString()) : null;
        return Result.ok(paperService.createPaper(studentId, title, collegeId, templateVersionId, teacherId));
    }

    @GetMapping("/{id}")
    public Result<Thesis> get(HttpServletRequest request,
                              @PathVariable Long id) {
        Long studentId = (Long) request.getAttribute("userId");
        return Result.ok(paperService.getPaper(id, studentId));
    }

    /**
     * 获取论文批阅反馈（评语 + 批注列表）
     * GET /api/v1/papers/{id}/review-feedback
     */
    @GetMapping("/{id}/review-feedback")
    public Result<Map<String, Object>> getReviewFeedback(HttpServletRequest request,
                                                          @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        paperService.getPaper(id, userId); // 校验访问权限
        List<ReviewRecord> reviews = reviewRecordService.getReviewHistory(id);
        ReviewRecord latest = reviews.isEmpty() ? null : reviews.get(0);
        var annotations = annotationService.getAnnotationsByThesis(id);
        Map<String, Object> result = new HashMap<>();
        result.put("latestReview", latest);
        result.put("annotations", annotations);
        return Result.ok(result);
    }

    @PutMapping("/{id}")
    public Result<Thesis> update(HttpServletRequest request,
                                 @PathVariable Long id,
                                 @RequestBody Map<String, Object> body) {
        Long studentId = (Long) request.getAttribute("userId");
        String title = (String) body.get("title");
        Long templateVersionId = body.get("templateVersionId") != null
                ? Long.valueOf(body.get("templateVersionId").toString()) : null;
        Long collegeId = body.get("collegeId") != null
                ? Long.valueOf(body.get("collegeId").toString()) : null;
        return Result.ok(paperService.updatePaper(id, studentId, title, templateVersionId, collegeId));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request,
                            @PathVariable Long id) {
        Long studentId = (Long) request.getAttribute("userId");
        paperService.deletePaper(id, studentId);
        return Result.ok();
    }

    // ==================== Word 导入 ====================

    /**
     * 导入 .docx 文件并自动解析为章节
     * POST /api/v1/papers/import
     */
    @PostMapping("/import")
    public Result<Thesis> importDocx(HttpServletRequest request,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("title") String title,
                                     @RequestParam(value = "templateVersionId", required = false) Long templateVersionId,
                                     @RequestParam(value = "teacherId", required = false) Long teacherId) {
        Long userId = (Long) request.getAttribute("userId");
        Thesis thesis = docxImportService.importDocx(file, title, userId, templateVersionId, teacherId);
        return Result.ok(thesis);
    }

    // ==================== 导出（委托给专用 Service） ====================

    /**
     * 导出论文（DOCX 或 PDF）
     * <p>
     * GET /api/v1/papers/{id}/export?format=DOCX|PDF
     * <p>
     * 图片处理流程：
     * 1. {@link DocxExportService} 解析章节 HTML 中的 &lt;img&gt; 标签
     * 2. 支持：数据库图片 /api/v1/images/{id}/file、本地文件、Base64、网络 URL
     * 3. 通过 {@code run.addPicture()} 将图片二进制数据硬嵌入 Word 文档
     * 4. PDF 另经 {@link PdfExportService} 预处理将 img src 替换为
     *    file:/// 绝对路径或 Base64，确保 LibreOffice 可渲染
     */
    @GetMapping("/{id}/export")
    public void export(HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable Long id,
                       @RequestParam(defaultValue = "DOCX") String format) throws IOException {
        Long studentId = (Long) request.getAttribute("userId");
        Thesis thesis = paperService.getPaper(id, studentId);

        // 导出前重建参考文献章节内容（确保文末列表与引用数据一致）
        try {
            sectionService.rebuildReferenceSection(thesis.getId());
        } catch (Exception e) {
            log.warn("导出前重建参考文献章节失败: {}", e.getMessage());
        }

        // 获取章节内容（含 HTML）
        List<ThesisSection> sections = sectionService.getSections(thesis.getId(), studentId, "ADMIN");
        String filename = thesis.getTitle();

        if ("PDF".equalsIgnoreCase(format)) {
            // ---- PDF 导出（预处理图片 → 生成 DOCX → LibreOffice 转 PDF）----
            try {
                byte[] pdfBytes = pdfExportService.exportPdf(thesis.getTitle(), sections);
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename + ".pdf", StandardCharsets.UTF_8));
                response.getOutputStream().write(pdfBytes);
                exportRecordService.record(studentId, id, filename, "PDF", "SUCCESS", null, (long) pdfBytes.length);
            } catch (Exception e) {
                log.error("PDF 导出失败", e);
                exportRecordService.record(studentId, id, filename, "PDF", "FAILED", e.getMessage(), null);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"code\":500,\"message\":\"PDF 导出失败: " + e.getMessage().replace("\"", "'") + "\"}");
            }
            return;
        }

        // ---- DOCX 导出（图片通过 addPicture 硬嵌入）----
        try {
            byte[] docxBytes = docxExportService.exportDocx(thesis.getTitle(), sections);
            response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(filename + ".docx", StandardCharsets.UTF_8));
            response.getOutputStream().write(docxBytes);
            exportRecordService.record(studentId, id, filename, "DOCX", "SUCCESS", null, (long) docxBytes.length);
        } catch (Exception e) {
            log.error("DOCX 导出失败", e);
            exportRecordService.record(studentId, id, filename, "DOCX", "FAILED", e.getMessage(), null);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"code\":500,\"message\":\"DOCX 导出失败: " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
