package com.example.thesisgenerator.service;

import com.example.thesisgenerator.entity.Image;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.repository.ImageRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF 导出服务
 * <p>
 * 1. 预解析章节 HTML 中的 &lt;img&gt; 标签，将 src 替换为
 * file:/// 绝对路径或 Base64 内嵌数据，确保 PDF 转换引擎可访问。
 * 2. 委托 {@link DocxExportService} 生成 DOCX。
 * 3. 委托 {@link PdfConversionService} 将 DOCX 转为 PDF。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final DocxExportService docxExportService;
    private final PdfConversionService pdfConversionService;
    private final ImageRepository imageRepository;

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    private Path uploadPath;

    /** 匹配数据库图片 API 路径 */
    private static final Pattern IMAGE_ID_PATTERN = Pattern.compile("/api/v1/images/(\\d+)/file");

    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        log.info("PdfExportService 初始化: uploadPath={}", this.uploadPath);
    }

    /**
     * 导出 PDF
     * <p>
     * 流程: 章节 HTML → 图片 src 预替换（绝对路径/Base64）→ 生成 DOCX → LibreOffice 转 PDF
     *
     * @param title    论文标题
     * @param sections 章节列表（预处理后生成 DOCX）
     * @return PDF 字节数组
     */
    public byte[] exportPdf(String title, List<ThesisSection> sections) throws IOException {
        // 1) 深拷贝章节列表，预处理 HTML 中的图片 src
        List<ThesisSection> processed = sections.stream()
                .map(this::preprocessSection)
                .toList();

        // 2) 生成带嵌入图片的 DOCX
        byte[] docxBytes = docxExportService.exportDocx(title, processed);

        // 3) DOCX → PDF
        return pdfConversionService.convertDocxToPdf(docxBytes);
    }

    // ==================== HTML 图片预处理 ====================

    /**
     * 预处理单个章节的 HTML 内容：
     * 遍历所有 &lt;img src="..."&gt; 标签，将 src 替换为
     * 可被 PDF 引擎直接读取的 file:/// 绝对路径或 Base64 内嵌数据。
     */
    private ThesisSection preprocessSection(ThesisSection section) {
        String content = section.getContent();
        if (content == null || content.isBlank()) return section;

        Document doc = Jsoup.parse(content);
        boolean modified = false;

        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (src == null || src.isBlank()) continue;

            // 跳过已经是 file:// 或 data: 的 src
            if (src.startsWith("data:") || src.startsWith("file://")) continue;

            String resolved = resolveImageSrcForPdf(src);
            if (resolved != null && !resolved.equals(src)) {
                img.attr("src", resolved);
                modified = true;
                log.info("PDF 预处理替换图片 src: {} → {}", src, resolved.substring(0, Math.min(60, resolved.length())));
            } else if (resolved == null) {
                log.warn("PDF 预处理无法解析图片 src={}，保留原值", src);
            }
        }

        if (!modified) return section;

        // 创建新章节对象而不是修改原对象
        ThesisSection copy = new ThesisSection();
        copy.setId(section.getId());
        copy.setThesisId(section.getThesisId());
        copy.setTitle(section.getTitle());
        copy.setSectionKey(section.getSectionKey());
        copy.setContent(doc.body().html());
        copy.setDraftContent(section.getDraftContent());
        copy.setSectionType(section.getSectionType());
        copy.setParentId(section.getParentId());
        copy.setSortOrder(section.getSortOrder());
        return copy;
    }

    /**
     * 将图片 src 解析为 PDF 引擎可用的格式（file:/// 绝对路径或 Base64）。
     * <p>
     * 优先级：
     * 1. /api/v1/images/{id}/file → 查 DB 得到物理路径 → file:/// URI
     * 2. 本地相对路径 → 拼接 uploadPath → file:/// URI
     * 3. http/https → Base64 内嵌（避免离线 PDF 无法加载网络图片）
     * 4. 已存在的 file:// → 原样返回
     */
    private String resolveImageSrcForPdf(String src) {
        // 已经是 file://
        if (src.startsWith("file://")) return src;

        // 1) 数据库图片
        if (src.startsWith("/api/v1/images/")) {
            try {
                Matcher m = IMAGE_ID_PATTERN.matcher(src);
                if (m.find()) {
                    long id = Long.parseLong(m.group(1));
                    var opt = imageRepository.findById(id);
                    if (opt.isPresent()) {
                        Image img = opt.get();
                        Path filePath = Paths.get(img.getFilePath());
                        if (Files.exists(filePath)) {
                            return filePath.toUri().toString();
                        }
                        // fallback
                        Path fallback = uploadPath.resolve(img.getStoredName());
                        if (Files.exists(fallback)) {
                            return fallback.toUri().toString();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("PDF 预处理解析数据库图片失败: src={}, {}", src, e.getMessage());
            }
        }

        // 2) 本地路径 → file:/// URI
        if (!src.startsWith("http://") && !src.startsWith("https://")) {
            String cleanPath = src.startsWith("/") ? src.substring(1) : src;
            try {
                // 尝试原样
                Path path = Paths.get(cleanPath);
                if (Files.exists(path) && Files.isRegularFile(path)) {
                    return path.toUri().toString();
                }
                // 尝试 uploadPath
                Path path2 = uploadPath.resolve(cleanPath.replaceAll("^uploads/images/", ""));
                if (Files.exists(path2) && Files.isRegularFile(path2)) {
                    return path2.toUri().toString();
                }
                // 尝试 uploadPath + 文件名
                String name = cleanPath.contains("/") ? cleanPath.substring(cleanPath.lastIndexOf('/') + 1) : cleanPath;
                Path path3 = uploadPath.resolve(name);
                if (Files.exists(path3) && Files.isRegularFile(path3)) {
                    return path3.toUri().toString();
                }
            } catch (Exception e) {
                log.warn("PDF 预处理本地路径解析失败: src={}, {}", src, e.getMessage());
            }
        }

        // 3) 网络 URL → Base64 内嵌
        if (src.startsWith("http://") || src.startsWith("https://")) {
            try {
                // 使用 java.net.URLConnection 下载（避免依赖 RestTemplate）
                java.net.URL url = new java.net.URL(src);
                java.net.URLConnection conn = url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                byte[] imageBytes;
                try (InputStream is = conn.getInputStream()) {
                    imageBytes = is.readAllBytes();
                }
                if (imageBytes != null && imageBytes.length > 0) {
                    // 检测 MIME 类型
                    String mimeType = conn.getContentType();
                    if (mimeType == null || mimeType.isBlank()) {
                        mimeType = "image/png";
                        try {
                            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(imageBytes));
                            if (bi != null) {
                                // 写为 PNG 确保兼容
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                ImageIO.write(bi, "png", baos);
                                imageBytes = baos.toByteArray();
                                mimeType = "image/png";
                            }
                        } catch (Exception ignored) {}
                    }
                    String base64 = Base64.getEncoder().encodeToString(imageBytes);
                    log.info("PDF 预处理下载网络图片并转为 Base64: {} ({} bytes)", src, imageBytes.length);
                    return "data:" + mimeType + ";base64," + base64;
                }
            } catch (Exception e) {
                log.warn("PDF 预处理下载网络图片失败: src={}, {}", src, e.getMessage());
            }
        }

        return null;
    }
}
