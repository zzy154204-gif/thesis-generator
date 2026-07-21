package com.example.thesisgenerator.controller;

import com.example.thesisgenerator.common.Result;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.service.PaperService;
import com.example.thesisgenerator.service.PdfConversionService;
import com.example.thesisgenerator.service.ThesisSectionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/papers")
@RequiredArgsConstructor
public class PaperController {

    private final PaperService paperService;
    private final ThesisSectionService sectionService;
    private final PdfConversionService pdfConversionService;

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
        return Result.ok(paperService.createPaper(studentId, title, collegeId, templateVersionId));
    }

    @GetMapping("/{id}")
    public Result<Thesis> get(HttpServletRequest request,
                              @PathVariable Long id) {
        Long studentId = (Long) request.getAttribute("userId");
        return Result.ok(paperService.getPaper(id, studentId));
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

    // ==================== 导出 ====================

    @GetMapping("/{id}/export")
    public void export(HttpServletRequest request,
                       HttpServletResponse response,
                       @PathVariable Long id,
                       @RequestParam(defaultValue = "DOCX") String format) throws IOException {
        Long studentId = (Long) request.getAttribute("userId");
        Thesis thesis = paperService.getPaper(id, studentId);

        // 1. 先用 POI 生成 DOCX
        byte[] docxBytes = generateDocx(thesis, studentId);
        String filename = thesis.getTitle();

        if ("PDF".equalsIgnoreCase(format)) {
            try {
                byte[] pdfBytes = pdfConversionService.convertDocxToPdf(docxBytes);
                response.setContentType("application/pdf");
                response.setHeader("Content-Disposition",
                        "attachment; filename*=UTF-8''" + URLEncoder.encode(filename + ".pdf", StandardCharsets.UTF_8));
                response.getOutputStream().write(pdfBytes);
            } catch (Exception e) {
                log.error("PDF 导出失败", e);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"code\":500,\"message\":\"PDF 导出失败: " + e.getMessage().replace("\"", "'") + "\"}");
            }
            return;
        }

        // 2. DOCX 格式直接返回
        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(filename + ".docx", StandardCharsets.UTF_8));
        response.getOutputStream().write(docxBytes);
    }

    /** 利用 POI 生成 DOCX 字节数组 */
    private byte[] generateDocx(Thesis thesis, Long userId) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (XWPFDocument doc = new XWPFDocument()) {
            // 标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(thesis.getTitle());
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            titleRun.setFontFamily("宋体");

            // 获取章节内容
            try {
                List<ThesisSection> sections = sectionService.getSections(thesis.getId(), userId);
                for (ThesisSection section : sections) {
                    doc.createParagraph().createRun().setText("");

                    XWPFParagraph heading = doc.createParagraph();
                    heading.setAlignment(ParagraphAlignment.LEFT);
                    XWPFRun headingRun = heading.createRun();
                    headingRun.setText(section.getTitle());
                    headingRun.setBold(true);
                    headingRun.setFontSize(14);
                    headingRun.setFontFamily("宋体");

                    if (section.getContent() != null && !section.getContent().isBlank()) {
                        // 提取纯文本（去除 HTML 标签）
                        String plainText = section.getContent()
                                .replaceAll("<[^>]+>", "")
                                .replace("&nbsp;", " ")
                                .replaceAll("\\s+", " ")
                                .trim();

                        XWPFParagraph body = doc.createParagraph();
                        body.setIndentationFirstLine(480);
                        XWPFRun bodyRun = body.createRun();
                        bodyRun.setText(plainText);
                        bodyRun.setFontSize(12);
                        bodyRun.setFontFamily("宋体");
                    }
                }
            } catch (Exception e) {
                // 若章节加载失败，写入提示文本
                XWPFParagraph note = doc.createParagraph();
                note.createRun().setText("（章节内容加载失败，请在编辑器中完善后重新导出）");
            }

            doc.write(baos);
        }

        return baos.toByteArray();
    }

}
