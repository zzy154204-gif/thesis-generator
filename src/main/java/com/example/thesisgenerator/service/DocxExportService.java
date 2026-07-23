package com.example.thesisgenerator.service;

import com.example.thesisgenerator.entity.Image;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.ImageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DOCX 导出服务
 * <p>
 * 将论文章节的 HTML 内容渲染为 Word (.docx) 文件，
 * 支持图片嵌入（数据库图片、本地文件、Base64、网络 URL）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxExportService {

    private final ImageRepository imageRepository;

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    private Path uploadPath;

    /** 最大图片宽度（pt），超出则等比缩放 */
    private static final double MAX_IMAGE_WIDTH_PT = 420;

    /** 匹配数据库图片 API 路径：兼容 /api/v1/images/{id}/file 以及带主机名的完整 URL */
    private static final Pattern IMAGE_ID_PATTERN = Pattern.compile("(?:https?://[^/]+)?/api/v1/images/(\\d+)/file");

    // ==================== 初始化 ====================

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("===== DocxExportService 初始化 =====");
        log.info("uploadDir   (配置值): {}", uploadDir);
        log.info("uploadPath  (绝对路径): {}", this.uploadPath);
        log.info("目录是否存在: {}", Files.exists(this.uploadPath));
        log.info("目录是否可读: {}", Files.isReadable(this.uploadPath));
        if (Files.exists(this.uploadPath)) {
            try {
                log.info("目录下文件数: {}", Files.list(this.uploadPath).count());
            } catch (IOException e) {
                log.warn("目录列表失败: {}", e.getMessage());
            }
        }
    }

    // ==================== 公开 API ====================

    /**
     * 生成 DOCX 字节数组
     *
     * @param title    论文标题
     * @param sections 章节列表（每个章节含 HTML content）
     * @return DOCX 文件的字节数组
     */
    public byte[] exportDocx(String title, List<ThesisSection> sections) throws IOException {
        try {
            return doExportDocx(title, sections);
        } catch (IOException e) {
            log.error("DOCX 导出失败: title={}, sections={}, 错误类型={}, 具体位置: ",
                    title, sections.size(), e.getClass().getSimpleName(), e);
            throw e;
        } catch (RuntimeException e) {
            log.error("DOCX 导出运行时异常: title={}, sections={}, 错误类型={}",
                    title, sections.size(), e.getClass().getSimpleName(), e);
            throw e;
        }
    }

    private byte[] doExportDocx(String title, List<ThesisSection> sections) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (XWPFDocument doc = new XWPFDocument()) {
            // ---- 标题 ----
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(22);
            setRunFont(titleRun, "宋体", "Noto Serif CJK SC");

            // ---- 章节内容 ----
            for (ThesisSection section : sections) {
                doc.createParagraph().createRun().setText(""); // 章节间空行

                // 章节标题
                XWPFParagraph heading = doc.createParagraph();
                heading.setAlignment(ParagraphAlignment.LEFT);
                XWPFRun headingRun = heading.createRun();
                headingRun.setText(section.getTitle());
                headingRun.setBold(true);
                headingRun.setFontSize(14);
                setRunFont(headingRun, "宋体", "Noto Serif CJK SC");

                // 正文（HTML → DOCX，含图片）
                if (section.getContent() != null && !section.getContent().isBlank()) {
                    htmlToDocxBody(doc, section.getContent());
                }
            }

            doc.write(baos);
        }

        byte[] bytes = baos.toByteArray();
        log.info("DOCX 生成完成: title={}, sections={}, size={} bytes", title, sections.size(), bytes.length);
        return bytes;
    }

    // ==================== HTML → DOCX 渲染 ====================

    /**
     * 将 HTML body 内容解析并写入 XWPFDocument
     */
    private void htmlToDocxBody(XWPFDocument doc, String html) {
        Document parsed = Jsoup.parse(html);
        List<Node> bodyChildren = parsed.body().childNodesCopy();

        for (Node child : bodyChildren) {
            if (child instanceof Element el) {
                String tag = el.tagName().toLowerCase();
                switch (tag) {
                    case "p"      -> renderParagraph(doc, el);
                    case "h1"     -> renderHeading(doc, el, 1, 16);
                    case "h2"     -> renderHeading(doc, el, 2, 14);
                    case "h3"     -> renderHeading(doc, el, 3, 12);
                    case "h4","h5","h6" -> renderHeading(doc, el, 4, 12);
                    case "img"    -> renderImage(doc, el);
                    case "table"  -> renderTable(doc, el);
                    case "ul"     -> renderList(doc, el, false);
                    case "ol"     -> renderList(doc, el, true);
                    case "br"     -> { /* 单独的 <br> 忽略 */ }
                    default -> {
                        if (isBlockTag(tag)) renderParagraph(doc, el);
                    }
                }
            } else if (child instanceof TextNode tn) {
                String text = tn.text().strip();
                if (!text.isEmpty()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationFirstLine(480);
                    p.createRun().setText(text);
                }
            }
        }
    }

    /** 渲染 <p> 段落（支持段落内包含 <img> 的情况） */
    private void renderParagraph(XWPFDocument doc, Element el) {
        Elements imgs = el.select("img");
        if (!imgs.isEmpty()) {
            // 段落中包含图片 —— 先渲染文本段，再逐一渲染图片
            Element clone = el.clone();
            clone.select("img").remove();
            String text = clone.text().strip();
            if (!text.isEmpty()) {
                XWPFParagraph p = doc.createParagraph();
                p.setIndentationFirstLine(480);
                renderInlineContent(p, clone);
            }
            for (Element img : imgs) {
                renderImage(doc, img);
            }
            return;
        }

        // 纯文本段落
        String text = el.text().strip();
        if (text.isEmpty()) return;

        boolean center = el.hasClass("figure-title") || el.hasClass("table-title")
                || "center".equals(el.attr("align"))
                || el.attr("style").toLowerCase().contains("text-align:center");

        XWPFParagraph p = doc.createParagraph();
        p.setIndentationFirstLine(480);
        if (center) p.setAlignment(ParagraphAlignment.CENTER);
        renderInlineContent(p, el);
    }

    private void renderHeading(XWPFDocument doc, Element el, int level, int fontSize) {
        String text = el.text().strip();
        if (text.isEmpty()) return;
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingBefore(200);
        p.setSpacingAfter(100);
        XWPFRun run = p.createRun();
        run.setBold(true);
        run.setFontSize(fontSize);
        setRunFont(run, "黑体", "Noto Sans CJK SC");
        run.setText(text);
    }

    /** 渲染 <img> 标签为独立段落（居中 + 保持宽高比） */
    private void renderImage(XWPFDocument doc, Element el) {
        String src = el.attr("src");
        if (src == null || src.isBlank()) {
            log.warn("导出图片 img 标签缺少 src 属性");
            return;
        }

        try {
            // 1) 一次性读取图片字节（避免重复 I/O）
            ImageStreamInfo info = resolveImageStream(src);
            if (info == null || info.inputStream == null) {
                log.warn("导出图片无法解析: src={}", src);
                return;
            }

            byte[] imageBytes;
            try (InputStream is = info.inputStream) {
                imageBytes = is.readAllBytes();
            }
            if (imageBytes == null || imageBytes.length == 0) {
                log.warn("导出图片内容为空: src={}", src);
                return;
            }

            // 2) 检测图片格式，不支持的格式自动转 PNG
            String fileName = info.fileName;
            int pictureType = getPictureType(fileName);
            if (pictureType == -1) {
                ImageConvertResult converted = tryConvertToPng(imageBytes, fileName);
                if (converted == null) {
                    log.warn("导出图片格式不支持且无法转换: {}", fileName);
                    return;
                }
                imageBytes = converted.bytes;
                fileName = converted.fileName;
                pictureType = converted.pictureType;
            }

            // 3) 读取自然尺寸
            int[] naturalDims;
            try (InputStream is = new ByteArrayInputStream(imageBytes)) {
                naturalDims = readImageDimensions(is, fileName);
            }
            int naturalW = (naturalDims != null && naturalDims[0] > 0) ? naturalDims[0] : 400;
            int naturalH = (naturalDims != null && naturalDims[1] > 0) ? naturalDims[1] : 300;

            // 4) 从 HTML 元素获取指定的宽度（style px 或 width 属性）
            int specifiedWidth = parseHtmlImageWidth(el);

            // 5) 计算 EMU 尺寸
            double maxEmuW = MAX_IMAGE_WIDTH_PT * Units.EMU_PER_POINT;
            int emuW, emuH;

            if (specifiedWidth > 0) {
                double htmlWidthPt = specifiedWidth * 0.75; // px → pt 近似
                double cappedPt = Math.min(htmlWidthPt, MAX_IMAGE_WIDTH_PT);
                double scale = cappedPt / htmlWidthPt;
                emuW = (int) Math.round(cappedPt * Units.EMU_PER_POINT);
                emuH = (int) Math.round((naturalH / (double) naturalW) * specifiedWidth * 0.75 * scale * Units.EMU_PER_POINT);
            } else {
                double scale = Math.min(1.0, maxEmuW / (naturalW * Units.EMU_PER_POINT));
                emuW = (int) Math.round(naturalW * Units.EMU_PER_POINT * scale);
                emuH = (int) Math.round(naturalH * Units.EMU_PER_POINT * scale);
            }

            if (emuW <= 0 || emuH <= 0) {
                log.warn("导出图片尺寸异常: {}x{} (natural={}x{}), src={}", emuW, emuH, naturalW, naturalH, src);
                return;
            }

            // 6) 嵌入图片到 DOCX
            try (InputStream is2 = new ByteArrayInputStream(imageBytes)) {
                XWPFParagraph p = doc.createParagraph();
                p.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun run = p.createRun();
                run.addPicture(is2, pictureType, fileName, emuW, emuH);
                log.info("导出成功嵌入图片: {} ({}x{} EMU, natural={}x{}, size={} bytes)",
                        fileName, emuW, emuH, naturalW, naturalH, imageBytes.length);
            }
        } catch (Exception e) {
            log.warn("导出时嵌入图片失败: src={}, ex={}", src, e.toString());
            if (log.isDebugEnabled()) {
                log.debug("导出图片异常堆栈:", e);
            }
        }
    }

    /** 不支持的图片格式 → PNG 转换结果 */
    private record ImageConvertResult(byte[] bytes, String fileName, int pictureType) {}

    private ImageConvertResult tryConvertToPng(byte[] imageBytes, String originalName) {
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bi == null) {
                log.warn("ImageIO 无法解码图片: {}", originalName);
                return null;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            String pngName = originalName.contains(".")
                    ? originalName.substring(0, originalName.lastIndexOf('.')) + ".png"
                    : originalName + ".png";
            log.info("图片格式转换成功: {} → PNG ({}x{})", originalName, bi.getWidth(), bi.getHeight());
            return new ImageConvertResult(baos.toByteArray(), pngName, XWPFDocument.PICTURE_TYPE_PNG);
        } catch (Exception e) {
            log.warn("图片格式转换失败: {}, {}", originalName, e.getMessage());
            return null;
        }
    }

    /** 从 HTML <img> 元素解析宽度 px */
    private int parseHtmlImageWidth(Element el) {
        String style = el.attr("style");
        if (style != null && !style.isBlank()) {
            Matcher m = Pattern.compile("width\\s*:\\s*(\\d+(\\.\\d+)?)\\s*px").matcher(style);
            if (m.find()) {
                return (int) Math.round(Double.parseDouble(m.group(1)));
            }
        }
        String widthAttr = el.attr("width");
        if (widthAttr != null && !widthAttr.isBlank()) {
            try {
                return Integer.parseInt(widthAttr);
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    // ==================== 表格渲染 ====================

    /** 渲染 <table> — 使用 XWPFTable 创建真正的 Word 表格 */
    private void renderTable(XWPFDocument doc, Element el) {
        // 1) 提取所有行（直接子 tr，或 thead/tbody/tfoot 包裹的 tr）
        List<Element> rows = new ArrayList<>();
        for (Element child : el.children()) {
            String tag = child.tagName().toLowerCase();
            if ("tr".equals(tag)) {
                rows.add(child);
            } else if (Set.of("thead", "tbody", "tfoot").contains(tag)) {
                for (Element trChild : child.children()) {
                    if ("tr".equals(trChild.tagName().toLowerCase())) {
                        rows.add(trChild);
                    }
                }
            }
        }
        if (rows.isEmpty()) return;

        // 2) 计算最大列数（考虑 colspan）
        int maxCols = 0;
        for (Element tr : rows) {
            int cols = 0;
            for (Element cell : tr.children()) {
                String ct = cell.tagName().toLowerCase();
                if ("td".equals(ct) || "th".equals(ct)) {
                    cols += Math.max(1, parseColspan(cell));
                }
            }
            maxCols = Math.max(maxCols, cols);
        }
        if (maxCols == 0) return;

        // 3) 创建表格
        XWPFTable table = doc.createTable(rows.size(), maxCols);
        applyTableStyle(table);

        // 4) 逐行填充单元格
        for (int i = 0; i < rows.size(); i++) {
            XWPFTableRow xRow = table.getRow(i);
            Element tr = rows.get(i);
            int colIdx = 0;

            for (Element cellEl : tr.children()) {
                String cellTag = cellEl.tagName().toLowerCase();
                if (!"td".equals(cellTag) && !"th".equals(cellTag)) continue;
                if (colIdx >= maxCols) break;

                int colspan = Math.max(1, parseColspan(cellEl));
                XWPFTableCell xCell = xRow.getCell(colIdx);

                // 设置 colspan
                if (colspan > 1) {
                    setCellColspan(xCell, colspan);
                }

                // 填写单元格内容
                renderTableCellContent(xCell, cellEl);

                colIdx += colspan;
            }
        }
    }

    /** 解析 colspan 属性值 */
    private int parseColspan(Element cell) {
        String val = cell.attr("colspan");
        if (val == null || val.isBlank()) return 1;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** 设置 XWPFTableCell 的 gridSpan（跨列数） */
    private void setCellColspan(XWPFTableCell cell, int colspan) {
        try {
            var ctTc = cell.getCTTc();
            var tcPr = ctTc.isSetTcPr() ? ctTc.getTcPr() : ctTc.addNewTcPr();
            tcPr.addNewGridSpan().setVal(BigInteger.valueOf(colspan));
        } catch (Exception e) {
            log.warn("设置 gridSpan={} 失败: {}", colspan, e.getMessage());
        }
    }

    /** 为表格应用样式：100% 宽度、黑色细线边框 */
    private void applyTableStyle(XWPFTable table) {
        try {
            var ctTbl = table.getCTTbl();
            var tblPr = ctTbl.getTblPr() == null ? ctTbl.addNewTblPr() : ctTbl.getTblPr();
            // 表格宽度 100%（百分比）
            var tblW = tblPr.getTblW() == null ? tblPr.addNewTblW() : tblPr.getTblW();
            tblW.setW(BigInteger.valueOf(5000));
            tblW.setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.PCT);

            // 边框
            var borders = tblPr.addNewTblBorders();

            var left = borders.addNewLeft();
            left.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            left.setSz(BigInteger.valueOf(4));
            left.setColor("000000");

            var right = borders.addNewRight();
            right.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            right.setSz(BigInteger.valueOf(4));
            right.setColor("000000");

            var top = borders.addNewTop();
            top.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            top.setSz(BigInteger.valueOf(4));
            top.setColor("000000");

            var bottom = borders.addNewBottom();
            bottom.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            bottom.setSz(BigInteger.valueOf(4));
            bottom.setColor("000000");

            var insideH = borders.addNewInsideH();
            insideH.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            insideH.setSz(BigInteger.valueOf(4));
            insideH.setColor("000000");

            var insideV = borders.addNewInsideV();
            insideV.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            insideV.setSz(BigInteger.valueOf(4));
            insideV.setColor("000000");
        } catch (Exception e) {
            log.warn("设置表格边框失败，使用默认样式: {}", e.getMessage());
        }
    }

    /** 填充 XWPFTableCell 内容（支持多段落和行内格式） */
    private void renderTableCellContent(XWPFTableCell cell, Element cellEl) {
        boolean isHeader = "th".equals(cellEl.tagName().toLowerCase());
        // 清空默认空白段落
        cell.removeParagraph(0);

        // 收集单元格中的子节点
        List<Node> children = cellEl.childNodesCopy();
        boolean hasBlockChildren = false;
        for (Node n : children) {
            if (n instanceof Element e) {
                String t = e.tagName().toLowerCase();
                if (Set.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "table").contains(t)) {
                    hasBlockChildren = true;
                    break;
                }
            }
        }

        if (!hasBlockChildren) {
            // 纯文本 / 只有行内元素 → 单一段落
            XWPFParagraph p = cell.addParagraph();
            setCellParagraphAlignment(p, cellEl);
            renderInlineContent(p, cellEl);
            if (isHeader) {
                for (XWPFRun run : p.getRuns()) {
                    run.setBold(true);
                }
            }
        } else {
            // 单元格内包含块级元素 → 分段渲染
            for (Node child : children) {
                if (child instanceof TextNode tn) {
                    String text = tn.text().strip();
                    if (!text.isEmpty()) {
                        XWPFParagraph p = cell.addParagraph();
                        XWPFRun run = p.createRun();
                        run.setText(text);
                        run.setFontSize(12);
                        setRunFont(run, "宋体", "Noto Serif CJK SC");
                        if (isHeader) run.setBold(true);
                    }
                } else if (child instanceof Element e) {
                    String tag = e.tagName().toLowerCase();
                    switch (tag) {
                        case "p" -> {
                            XWPFParagraph p = cell.addParagraph();
                            setCellParagraphAlignment(p, e);
                            renderInlineContent(p, e);
                            if (isHeader) {
                                for (XWPFRun run : p.getRuns()) run.setBold(true);
                            }
                        }
                        case "br" -> { /* 忽略 */ }
                        default -> {
                            XWPFParagraph p = cell.addParagraph();
                            renderInlineContent(p, e);
                        }
                    }
                }
            }
        }
    }

    /** 设置单元格段落对齐方式 */
    private void setCellParagraphAlignment(XWPFParagraph p, Element el) {
        String align = el.attr("align");
        if ((align != null && "center".equalsIgnoreCase(align))
                || el.attr("style").toLowerCase().contains("text-align:center")) {
            p.setAlignment(ParagraphAlignment.CENTER);
        } else if (align != null && "right".equalsIgnoreCase(align)) {
            p.setAlignment(ParagraphAlignment.RIGHT);
        }
    }

    /** 渲染 <ul> / <ol> */
    private void renderList(XWPFDocument doc, Element el, boolean ordered) {
        int idx = 1;
        for (Element li : el.children()) {
            if (!"li".equals(li.tagName().toLowerCase())) continue;
            String prefix = ordered ? (idx++ + ". ") : "• ";
            String text = li.text().strip();
            if (text.isEmpty()) continue;
            XWPFParagraph p = doc.createParagraph();
            p.setIndentationFirstLine(480);
            p.setIndentFromLeft(200);
            XWPFRun run = p.createRun();
            run.setText(prefix + text);
            run.setFontSize(12);
            setRunFont(run, "宋体", "Noto Serif CJK SC");
        }
    }

    /** 渲染段落内联内容 */
    private void renderInlineContent(XWPFParagraph p, Element el) {
        for (Node child : el.childNodesCopy()) {
            if (child instanceof TextNode tn) {
                String text = tn.text();
                if (!text.isBlank()) {
                    XWPFRun run = p.createRun();
                    run.setText(text);
                    run.setFontSize(12);
                    setRunFont(run, "宋体", "Noto Serif CJK SC");
                }
            } else if (child instanceof Element inline) {
                String tag = inline.tagName().toLowerCase();
                String text = inline.text();
                if (text.isBlank()) continue;

                XWPFRun run = p.createRun();
                switch (tag) {
                    case "strong","b" -> run.setBold(true);
                    case "em","i"     -> run.setItalic(true);
                    case "u"          -> run.setUnderline(UnderlinePatterns.SINGLE);
                    case "sup"        -> { run.setText(text); run.setSubscript(VerticalAlign.SUPERSCRIPT); continue; }
                    case "sub"        -> { run.setText(text); run.setSubscript(VerticalAlign.SUBSCRIPT); continue; }
                    case "br"         -> { run.addBreak(); continue; }
                }
                run.setFontSize(12);
                setRunFont(run, "宋体", "Noto Serif CJK SC");
                run.setText(text);
            }
        }
    }

    // ==================== 图片解析 ====================

    private record ImageStreamInfo(InputStream inputStream, String fileName) {}

    /**
     * 根据 img src 解析图片的 InputStream（5 级降级策略）
     * <p>
     * 0. data:image/...;base64,... — Base64 内嵌图片
     * 1. /api/v1/images/{id}/file — 查 ImageRepository 数据库
     * 2. file:/// 协议 — 本地绝对路径
     * 3. 本地相对/绝对路径 — 结合 app.upload.dir 逐步尝试
     * 4. http/https URL — 带超时下载
     * <p>
     * 注意：当 src 匹配数据库图片模式且策略 1 失败时，不会继续执行策略 2~4，
     * 避免回退到 RestTemplate 从自身下载图片（既慢又可能因鉴权失败）。
     */
    private ImageStreamInfo resolveImageStream(String src) {
        if (src == null || src.isBlank()) return null;

        // 0) Base64 data URL
        if (src.startsWith("data:")) {
            return resolveBase64(src);
        }

        // 1) 数据库 API 路径
        ImageStreamInfo dbResult = resolveFromDatabase(src);
        if (dbResult != null) return dbResult;

        // ★ 如果 src 匹配数据库图片模式但 DB 中找不到 → 直接放弃，不回退到网络下载
        if (IMAGE_ID_PATTERN.matcher(src).find()) {
            log.warn("数据库图片未找到且不再回退网络下载: src={}", src);
            return null;
        }

        // 2) file:// 协议
        if (src.startsWith("file://")) {
            return resolveFileProtocol(src);
        }

        // 3) 本地路径
        if (!src.startsWith("http://") && !src.startsWith("https://")) {
            return resolveLocalPath(src);
        }

        // 4) 网络 URL
        return resolveRemoteUrl(src);
    }

    /** Base64 data URL 解码 */
    private ImageStreamInfo resolveBase64(String src) {
        try {
            int comma = src.indexOf(',');
            if (comma < 0) {
                log.warn("Base64 data URL 格式异常: {}", src.substring(0, Math.min(50, src.length())));
                return null;
            }
            String header = src.substring(0, comma);
            String base64Data = src.substring(comma + 1);
            String ext = "png";
            if (header.contains("image/jpeg") || header.contains("image/jpg")) ext = "jpg";
            else if (header.contains("image/gif")) ext = "gif";
            else if (header.contains("image/webp")) ext = "webp";
            else if (header.contains("image/bmp")) ext = "bmp";

            byte[] decoded = Base64.getDecoder().decode(base64Data);
            log.info("Base64 图片解码成功: {} bytes, format={}", decoded.length, ext);
            return new ImageStreamInfo(new ByteArrayInputStream(decoded), "inline." + ext);
        } catch (Exception e) {
            log.warn("Base64 图片解码失败: {}", e.getMessage());
            return null;
        }
    }

    /** 数据库图片解析（含 fallback 到 uploadPath） */
    private ImageStreamInfo resolveFromDatabase(String src) {
        try {
            Matcher m = IMAGE_ID_PATTERN.matcher(src);
            if (!m.find()) return null;

            long id = Long.parseLong(m.group(1));
            var opt = imageRepository.findById(id);
            if (opt.isPresent()) {
                Image img = opt.get();
                Path filePath = Paths.get(img.getFilePath());
                if (Files.exists(filePath)) {
                    log.info("数据库图片解析成功: id={}, path={}", id, filePath);
                    return new ImageStreamInfo(new FileInputStream(filePath.toFile()), img.getStoredName());
                }
                // fallback: storedName → uploadPath
                Path fallback = uploadPath.resolve(img.getStoredName());
                if (Files.exists(fallback)) {
                    log.info("数据库图片通过 fallback 找到: {}", fallback);
                    return new ImageStreamInfo(new FileInputStream(fallback.toFile()), img.getStoredName());
                }
                log.warn("数据库图片文件不存在: id={}, path={}, fallback={}", id, filePath, fallback);
            } else {
                log.warn("数据库图片未找到: id={}", id);
            }
        } catch (Exception e) {
            log.warn("数据库图片解析异常: src={}, {}", src, e.getMessage());
        }
        return null;
    }

    /** file:// 协议解析 */
    private ImageStreamInfo resolveFileProtocol(String src) {
        try {
            Path filePath = Paths.get(URI.create(src));
            if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                String name = filePath.getFileName().toString();
                log.info("file:// 图片解析成功: {}", filePath);
                return new ImageStreamInfo(new FileInputStream(filePath.toFile()), name);
            }
            log.warn("file:// 图片不存在: {}", filePath);
        } catch (Exception e) {
            log.warn("file:// 图片解析失败: {}", e.getMessage());
        }
        return null;
    }

    /** 本地路径解析（4 种策略尝试） */
    private ImageStreamInfo resolveLocalPath(String src) {
        String cleanPath = src.startsWith("/") ? src.substring(1) : src;
        String name = cleanPath.contains("/") ? cleanPath.substring(cleanPath.lastIndexOf('/') + 1) : cleanPath;
        if (name.isEmpty()) name = "image.png";

        try {
            // A: 原样（绝对路径或相对 cwd）
            Path pathA = Paths.get(cleanPath);
            if (Files.exists(pathA) && Files.isRegularFile(pathA)) {
                log.info("本地图片(原样): {}", pathA);
                return new ImageStreamInfo(new FileInputStream(pathA.toFile()), name);
            }
            // B: uploadPath / (去除 uploads/images/ 前缀)
            Path pathB = uploadPath.resolve(cleanPath.replaceAll("^uploads/images/", ""));
            if (Files.exists(pathB) && Files.isRegularFile(pathB)) {
                log.info("本地图片(uploadPath): {}", pathB);
                return new ImageStreamInfo(new FileInputStream(pathB.toFile()), name);
            }
            // C: uploadPath / (去除 uploads/ 前缀)
            Path pathC = uploadPath.resolve(cleanPath.replaceAll("^uploads/", ""));
            if (Files.exists(pathC) && Files.isRegularFile(pathC)) {
                log.info("本地图片(uploads/fallback): {}", pathC);
                return new ImageStreamInfo(new FileInputStream(pathC.toFile()), name);
            }
            // D: uploadPath / 文件名
            Path pathD = uploadPath.resolve(name);
            if (Files.exists(pathD) && Files.isRegularFile(pathD)) {
                log.info("本地图片(storedName): {}", pathD);
                return new ImageStreamInfo(new FileInputStream(pathD.toFile()), name);
            }
        } catch (IOException e) {
            log.warn("本地路径解析异常: src={}, {}", src, e.getMessage());
            return null;
        }

        log.warn("本地图片不存在（已尝试4种策略）: src={}, uploadPath={}", src, uploadPath);
        return null;
    }

    /** 网络 URL 下载（带连接超时与读取超时，外网图片卡顿时不会永久阻塞导出线程） */
    private ImageStreamInfo resolveRemoteUrl(String src) {
        try {
            log.info("开始下载远程图片: {}", src);
            java.net.URL url = URI.create(src).toURL();
            java.net.URLConnection conn = url.openConnection();
            conn.setConnectTimeout(5_000);   // 连接超时 5 秒
            conn.setReadTimeout(10_000);     // 读取超时 10 秒

            byte[] responseBytes;
            try (InputStream is = conn.getInputStream()) {
                responseBytes = is.readAllBytes();
            }
            if (responseBytes == null || responseBytes.length == 0) {
                log.warn("远程图片下载结果为空: {}", src);
                return null;
            }
            String fileName = src.substring(src.lastIndexOf('/') + 1);
            if (fileName.isEmpty() || fileName.length() > 100) fileName = "remote.png";
            log.info("远程图片下载成功: {} ({} bytes)", src, responseBytes.length);
            return new ImageStreamInfo(new ByteArrayInputStream(responseBytes), fileName);
        } catch (java.net.SocketTimeoutException e) {
            log.warn("远程图片下载超时: src={}, timeout=5s/10s", src);
            return null;
        } catch (Exception e) {
            log.warn("远程图片下载失败: src={}, {}", src, e.getMessage());
            return null;
        }
    }

    // ==================== 工具方法 ====================

    /** 根据文件名获取 POI 图片类型常量 */
    private int getPictureType(String fileName) {
        if (fileName == null) return -1;
        String ext = fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase()
                : "";
        return switch (ext) {
            case "jpg", "jpeg" -> XWPFDocument.PICTURE_TYPE_JPEG;
            case "png"         -> XWPFDocument.PICTURE_TYPE_PNG;
            case "gif"         -> XWPFDocument.PICTURE_TYPE_GIF;
            case "bmp"         -> XWPFDocument.PICTURE_TYPE_BMP;
            case "emf"         -> XWPFDocument.PICTURE_TYPE_EMF;
            case "wmf"         -> XWPFDocument.PICTURE_TYPE_WMF;
            default -> {
                log.warn("不支持的导出图片格式: {}", ext);
                yield -1;
            }
        };
    }

    /** 读取图片实际像素尺寸 */
    private int[] readImageDimensions(InputStream is, String fileName) {
        try {
            BufferedImage bi = ImageIO.read(is);
            if (bi != null) return new int[]{bi.getWidth(), bi.getHeight()};
        } catch (Exception e) {
            log.warn("读取图片尺寸失败: {}, {}", fileName, e.getMessage());
        }
        return new int[]{200, 200};
    }

    /** 粗略判断是否块级标签 */
    private boolean isBlockTag(String tag) {
        return Set.of("div", "section", "article", "blockquote", "pre", "hr", "figure").contains(tag);
    }

    // ==================== 字体工具 ====================

    /**
     * 设置 XWPFRun 字体，同时指定中文（East-Asia）回退字体。
     * <p>
     * 这样生成的 DOCX 同时保留了 Windows 中文字体名（供 Word 用户使用）
     * 和 Linux 可用字体（供 LibreOffice PDF 转换使用），避免中文方块乱码。
     *
     * @param run           XWPFRun 实例
     * @param mainFont      主字体（如 "宋体"、"黑体"）
     * @param eastAsiaFont  East-Asia 回退字体（如 "Noto Serif CJK SC"、"Noto Sans CJK SC"）
     */
    private void setRunFont(XWPFRun run, String mainFont, String eastAsiaFont) {
        try {
            run.setFontFamily(mainFont);
            if (eastAsiaFont != null && !eastAsiaFont.isEmpty()) {
                var rpr = run.getCTR().addNewRPr();
                var fonts = rpr.addNewRFonts();
                fonts.setEastAsia(eastAsiaFont);
            }
        } catch (Exception e) {
            // East-Asia 字体设置失败不影响基本渲染
            log.debug("设置 East-Asia 字体失败: main={}, ea={}, err={}", mainFont, eastAsiaFont, e.getMessage());
        }
    }
}
