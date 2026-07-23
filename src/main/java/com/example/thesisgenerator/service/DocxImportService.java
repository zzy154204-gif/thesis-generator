package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.entity.ThesisSection;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.repository.ThesisRepository;
import com.example.thesisgenerator.repository.ThesisSectionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyle;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Word (.docx) 论文导入解析服务
 * <p>
 * 解析上传的 docx 文件，识别标题层级，自动拆分章节并写入数据库。
 * 支持按模板规范导入：传入 templateVersionId 时，按模板定义的章节结构创建 sections，
 * 并尝试将解析出的内容映射到对应章节中。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocxImportService {

    private final ThesisRepository thesisRepository;
    private final ThesisSectionRepository sectionRepository;
    private final TemplateService templateService;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 允许的 MIME 类型 */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    /** 最大标题长度（超过此长度不作为标题） */
    private static final int MAX_HEADING_LENGTH = 50;

    /** 封面关键词（段落匹配时视为封面/元数据，不生成章节） */
    private static final Set<String> COVER_KEYWORDS = Set.of(
            "学号", "姓名", "课程名称", "课程", "学院", "学期",
            "论文题目", "指导老师", "指导教师", "指导导师",
            "专业", "年级", "班级", "学校", "大学", "院系",
            "提交日期", "完成日期", "日期", "成绩"
    );

    /** 封面元数据提取正则（学号:xxx  姓名:xxx  学院:xxx 等） */
    private static final Pattern METADATA_KEY_VALUE = Pattern.compile(
            "^(学号|姓名|学院|专业|班级|年级|课程名称|课程|指导老师|指导教师|指导导师|论文题目|学校|院系)"
                    + "[:：]\\s*(.+)$"
    );

    /** 最大封面检测段落数（仅前 N 段可能属于封面） */
    private static final int COVER_SCAN_MAX = 20;

    /** 中文数字章节标题模式：第一章、第一节、第一章 概述（支持阿拉伯数字 第1章） */
    private static final Pattern CN_CHAPTER = Pattern.compile(
            "^第[0-9一二三四五六七八九十百零〇]+[章节篇部分]\\s*.*"
    );
    /**
     * 中文数字顿号章节模式：一、二、三、十、 （顶级章节）
     * 匹配示例：一、李白道教思想的形成渊源  二、研究背景
     */
    private static final Pattern CN_NUM_CHAPTER = Pattern.compile(
            "^[一二三四五六七八九十]+[、\\.]\\s*.*"
    );
    /**
     * 阿拉伯数字章节模式：1. 2. 3、 （顶级章节）
     * 注意：要求点号后紧跟空白或中文，避免与 1.1 多级编号冲突
     */
    private static final Pattern ARABIC_CHAPTER = Pattern.compile(
            "^[0-9]+[\\.\\、](\\s+[\\u4e00-\\u9fa5]|\\s*$).*"
    );
    /** 中文数字子标题模式：（一）（二）（三）—仅保留括号形式 */
    private static final Pattern CN_SECTION = Pattern.compile(
            "^[（(][一二三四五六七八九十百零〇]+[）)]\\s*.*"
    );
    /** 数字编号子标题模式：1.1  1.1.1 */
    private static final Pattern NUM_HEADING = Pattern.compile(
            "^\\d+(\\.\\d+)+[\\s.、]?.*"
    );

    /** 学术参考文献标识 [M] [J] [D] [C] [N] [R] [S] [P] [Z] [EB/OL] 等 */
    private static final Pattern REFERENCE_TYPE_TAG = Pattern.compile(
            "\\[[A-Z]{1,3}(/[A-Z]{1,3})?\\]"
    );
    /** 参考文献序号开头模式：[1] 或 1. 后接作者 */
    private static final Pattern REFERENCE_NUMBER_PREFIX = Pattern.compile(
            "^(\\[\\d+\\]|\\d+\\.)\\s*[\\u4e00-\\u9fa5a-zA-Z].*"
    );
    /** 参考文献条目特征：含作者 + 出版社/期刊 + 年份 */
    private static final Pattern REFERENCE_ENTRY = Pattern.compile(
            ".*[\\u4e00-\\u9fa5]{2,}(?:\\．|\\.)[\\s\\S]{5,}(?:出版社|出版|\\d{4}|\\d{2}\\(\\d+\\))"
    );

    // ==================== 公开方法 ====================

    /**
     * 导入 .docx 文件（无模板）
     */
    public Thesis importDocx(MultipartFile file, String title, Long userId) {
        return importDocx(file, title, userId, null, null);
    }

    /**
     * 导入 .docx 文件（可选绑定模板版本）
     *
     * @param file              上传的 .docx 文件
     * @param title             论文标题
     * @param userId            学生用户 ID
     * @param templateVersionId 模板版本 ID（可选，为 null 时走传统标题检测）
     * @param teacherId         指导老师 ID（可选）
     * @return 已创建的 Thesis 实体
     */
    public Thesis importDocx(MultipartFile file, String title, Long userId, Long templateVersionId, Long teacherId) {
        validateFile(file);

        // 创建论文
        Thesis thesis = new Thesis();
        thesis.setStudentId(userId);
        thesis.setTitle(title != null && !title.isBlank() ? title.trim() : "未命名论文");
        thesis.setStatus("DRAFT");
        thesis.setIsLocked(false);
        thesis.setTemplateVersionId(templateVersionId);
        thesis.setTeacherId(teacherId);
        thesis = thesisRepository.save(thesis);
        final Long thesisId = thesis.getId();

        // 解析文档
        try (InputStream is = file.getInputStream(); XWPFDocument doc = new XWPFDocument(is)) {
            // 提取封面元数据（学号、姓名、学院等）
            Map<String, String> coverMeta = extractMetadata(doc.getParagraphs());
            if (!coverMeta.isEmpty()) {
                try {
                    thesis.setImportMetadata(MAPPER.writeValueAsString(coverMeta));
                    thesis = thesisRepository.save(thesis);
                } catch (Exception e) {
                    log.warn("保存导入元数据失败", e);
                }
            }

            List<SectionBlock> parsed = parseDocument(doc);

            if (templateVersionId != null) {
                // ---- 按模板结构创建章节并映射内容 ----
                createSectionsFromTemplate(thesisId, templateVersionId, parsed);
            } else if (parsed.isEmpty()) {
                // ---- 无模板 + 无检测到标题：整篇作为一个章节 ----
                String fullText = extractPlainText(doc);
                ThesisSection section = createSection(thesisId, "正文", "chapter1", fullText, 0, null);
                sectionRepository.save(section);
            } else {
                // ---- 无模板 + 标题检测：按检测结果创建章节 ----
                for (int i = 0; i < parsed.size(); i++) {
                    SectionBlock block = parsed.get(i);
                    ThesisSection section = createSection(
                            thesisId, block.heading, "chapter" + (i + 1),
                            block.bodyHtml, i, null
                    );
                    sectionRepository.save(section);
                }
            }

            log.info("论文导入完成: id={}, title={}, sections={}, templateVersionId={}",
                    thesisId, thesis.getTitle(), parsed.size(), templateVersionId);
            return thesis;

        } catch (IOException e) {
            log.error("解析 docx 文件失败", e);
            thesisRepository.delete(thesis);
            throw new BusinessException(400, "文档解析失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("导入过程异常", e);
            thesisRepository.delete(thesis);
            throw new BusinessException(400, "导入失败: " + e.getMessage());
        }
    }

    // ==================== 文件校验 ====================

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
            throw new BusinessException(400, "仅支持 .docx 格式的 Word 文档");
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new BusinessException(400, "文件类型不正确，请上传 .docx 文件");
        }
        if (file.getSize() > 50 * 1024 * 1024L) {
            throw new BusinessException(400, "文件大小超出限制（最大 50MB）");
        }
    }

    // ==================== 模板章节创建 ====================

    /**
     * 按模板定义的章节结构创建 section，并将解析出的 docx 内容映射到对应章节中
     */
    private void createSectionsFromTemplate(Long thesisId, Long templateVersionId, List<SectionBlock> parsed) {
        // 1. 加载模板章节结构
        List<ChapterDef> templateChapters = loadChapterStructure(templateVersionId);
        if (templateChapters == null || templateChapters.isEmpty()) {
            // 模板无章节定义，退化为普通导入
            log.warn("模板版本 {} 未定义章节结构，使用 docx 解析结果", templateVersionId);
            for (int i = 0; i < parsed.size(); i++) {
                SectionBlock block = parsed.get(i);
                ThesisSection section = createSection(
                        thesisId, block.heading, "chapter" + (i + 1),
                        block.bodyHtml, i, null
                );
                sectionRepository.save(section);
            }
            return;
        }

        // 2. 建立标题映射：将 docx 解析出的 heading 映射到模板章节
        //    构建模板章节标题列表 (展平，含父子)
        List<ChapterDef> flatDefs = new ArrayList<>();
        flattenChapters(templateChapters, flatDefs);

        //    为每个 parsed block 找到最匹配的模板章节
        List<Integer> matchIndex = new ArrayList<>(Collections.nCopies(parsed.size(), -1));
        Set<String> matchedTitles = new HashSet<>();

        for (int i = 0; i < parsed.size(); i++) {
            String heading = parsed.get(i).heading;
            int bestIdx = -1;
            int bestScore = 0;

            for (int j = 0; j < flatDefs.size(); j++) {
                String tplTitle = flatDefs.get(j).title;
                if (matchedTitles.contains(tplTitle)) continue; // 每个模板章节只匹配一次
                int score = titleSimilarity(heading, tplTitle);
                if (score > bestScore && score >= 50) { // 相似度 >= 50%
                    bestScore = score;
                    bestIdx = j;
                }
            }

            if (bestIdx >= 0) {
                matchIndex.set(i, bestIdx);
                matchedTitles.add(flatDefs.get(bestIdx).title);
            }
        }

        // 3. 按模板定义创建 sections，并填充已映射的内容
        int order = 0;
        for (int j = 0; j < flatDefs.size(); j++) {
            ChapterDef def = flatDefs.get(j);

            // 收集所有映射到此模板章节的 parsed block 的内容
            StringBuilder contentBuilder = new StringBuilder();
            boolean hasMapped = false;
            for (int i = 0; i < parsed.size(); i++) {
                if (matchIndex.get(i) == j) {
                    if (contentBuilder.length() > 0) contentBuilder.append("\n");
                    contentBuilder.append(parsed.get(i).bodyHtml);
                    hasMapped = true;
                }
            }

            // 如果此模板章节没有找到映射，但下一个相邻的 parsed block 也未匹配，
            // 将其内容归入此章节（优先填充未匹配的内容到最近的已匹配章节）
            if (!hasMapped) {
                // 找下一个匹配的 parsed block 之前的所有未匹配 block
                int nextMatched = -1;
                for (int i = 0; i < parsed.size(); i++) {
                    if (matchIndex.get(i) >= 0) { nextMatched = i; break; }
                }

                // 如果没有任何 parsed block 匹配任何模板章节，
                // 将所有内容填充到第一个模板章节
                if (nextMatched < 0) {
                    for (SectionBlock block : parsed) {
                        if (contentBuilder.length() > 0) contentBuilder.append("\n");
                        contentBuilder.append(block.bodyHtml);
                    }
                }
            }

            String sectionKey = "chapter" + (order + 1);
            ThesisSection section = createSection(
                    thesisId, def.title, sectionKey,
                    contentBuilder.toString(), order, null
            );
            sectionRepository.save(section);
            order++;
        }

        // 4. 处理完全未匹配到任何模板章节的 parsed blocks（追加到末尾）
        Set<Integer> matchedSet = new HashSet<>();
        for (int idx : matchIndex) {
            if (idx >= 0) matchedSet.add(idx);
        }
        if (matchedSet.size() < parsed.size()) {
            // 检查是否有完全未映射的 parsed blocks
            // 它们实际上已经被填充到第 3 步中了（fallback 到第一个章节或前后章节）
            // 如果所有模板章节都已保存，但确实有多余的内容，创建附加章节
            if (matchedSet.isEmpty()) {
                // 这种情况在上面已经全部放到第一个模板章节了，不用处理
            }
        }
    }

    /**
     * 加载模板版本的章节结构
     */
    private List<ChapterDef> loadChapterStructure(Long templateVersionId) {
        try {
            TemplateVersion version = templateService.getVersionById(templateVersionId);
            String json = version.getChapterStructure();
            if (json == null || json.isBlank() || "[]".equals(json.trim())) {
                return Collections.emptyList();
            }
            return MAPPER.readValue(json, new TypeReference<List<ChapterDef>>() {});
        } catch (Exception e) {
            log.warn("加载模板章节结构失败 templateVersionId={}", templateVersionId, e);
            return Collections.emptyList();
        }
    }

    /**
     * 展平嵌套章节结构为列表
     */
    private void flattenChapters(List<ChapterDef> chapters, List<ChapterDef> result) {
        for (ChapterDef ch : chapters) {
            result.add(ch);
            if (ch.children != null && !ch.children.isEmpty()) {
                flattenChapters(ch.children, result);
            }
        }
    }

    /**
     * 计算两个标题文本的相似度（0-100），用于标题匹配
     */
    private int titleSimilarity(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0;
        String cleanA = a.replaceAll("[\\s\\p{P}\\p{S}]", "").toLowerCase();
        String cleanB = b.replaceAll("[\\s\\p{P}\\p{S}]", "").toLowerCase();

        // 完全一致
        if (cleanA.equals(cleanB)) return 100;
        // 包含关系
        if (cleanA.contains(cleanB) || cleanB.contains(cleanA)) {
            return 80 + (Math.min(cleanA.length(), cleanB.length()) * 20 / Math.max(cleanA.length(), cleanB.length()));
        }
        // 计算公共子串比例
        int common = longestCommonSubstring(cleanA, cleanB);
        int maxLen = Math.max(cleanA.length(), cleanB.length());
        return maxLen > 0 ? common * 100 / maxLen : 0;
    }

    private int longestCommonSubstring(String a, String b) {
        int m = a.length(), n = b.length();
        int[][] dp = new int[m + 1][n + 1];
        int max = 0;
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    max = Math.max(max, dp[i][j]);
                }
            }
        }
        return max;
    }

    // ==================== 文档解析 ====================

    /**
     * 解析文档，识别章节标题并切分为章节块
     * <p>
     * 规则：
     * - 封面段落（含学号/姓名等关键词）被跳过，不生成章节
     * - 以下模式会创建新的顶级章节：
     *   1) Word Heading 1 样式
     *   2) "第N章/节/篇/部分"（第一章 绪论）
     *   3) "一、二、三、" 中文数字顿号模式
     *   4) "1. 2. 3、" 阿拉伯数字模式
     * - 子标题（（一）（二）、1.1、加粗短文本）渲染为 inline {@code <h2>/<h3>}，不新开章节
     * - 参考文献条目被跳过
     */
    private List<SectionBlock> parseDocument(XWPFDocument doc) {
        List<XWPFParagraph> paragraphs = doc.getParagraphs();

        // Phase 1: 识别正文起始位置（跳过封面段落）
        int bodyStart = findBodyStartIndex(paragraphs);

        List<SectionBlock> sections = new ArrayList<>();
        SectionBlock current = null;
        StringBuilder currentBody = new StringBuilder();

        for (int i = bodyStart; i < paragraphs.size(); i++) {
            XWPFParagraph para = paragraphs.get(i);

            // 跳过脚注/尾注
            if (isFootnoteOrEndnote(para)) continue;

            String text = para.getText().trim();
            if (text.isEmpty()) continue;

            // 跳过参考文献条目
            if (isReferenceParagraph(text)) continue;

            // 1) 检测是否为顶级章节标题（"第N章" / Heading 1）
            ChapterHeadingInfo ch = detectChapterHeading(doc, para, text);
            if (ch != null) {
                // 保存前一章节
                if (current != null) {
                    current.bodyHtml = currentBody.toString();
                    sections.add(current);
                }
                current = new SectionBlock();
                current.heading = text;
                current.level = ch.level;
                currentBody = new StringBuilder();
                continue;
            }

            // 2) 检测是否为子标题（一、 二、 加粗等 → 渲染为 <h2>/<h3>）
            SubHeadingInfo sub = detectSubHeading(doc, para, text);
            if (sub != null) {
                if (current == null) {
                    // 第一个章节尚未创建，说明正文前有无标题引言段，创建一个隐式章节
                    current = new SectionBlock();
                    current.heading = "前言";
                    current.level = 1;
                    currentBody = new StringBuilder();
                }
                currentBody.append("<h").append(sub.level).append(">")
                        .append(escapeToHtml(text))
                        .append("</h").append(sub.level).append(">\n");
                continue;
            }

            // 3) 普通正文段落
            if (current == null) {
                // 第一个章节尚未创建，说明正文前有无标题引言段
                current = new SectionBlock();
                current.heading = "前言";
                current.level = 1;
                currentBody = new StringBuilder();
            }
            currentBody.append("<p>").append(escapeToHtml(text)).append("</p>\n");
        }

        // 保存最后一章
        if (current != null) {
            current.bodyHtml = currentBody.toString();
            sections.add(current);
        }

        return sections;
    }

    /**
     * 找到正文起始索引（跳过封面段落）
     * <p>
     * 封面段落特征：包含封面关键词 或 极短且非标题的行。
     * 一旦遇到明确的章节标题（如 "第N章"、"一、"、"1."），停止跳过。
     */
    private int findBodyStartIndex(List<XWPFParagraph> paragraphs) {
        int scanEnd = Math.min(paragraphs.size(), COVER_SCAN_MAX);
        for (int i = 0; i < scanEnd; i++) {
            String text = paragraphs.get(i).getText().trim();
            if (text.isEmpty()) continue;

            // 遇到章节标题 → 正文从这里开始
            if (isAnyChapterHeading(text)) {
                return i;
            }
            // 如果没有封面关键词特征 → 认为正文已开始
            if (!isCoverParagraph(text)) {
                return i;
            }
        }
        // 前 N 段全是封面特征 → 从第 N 段开始
        return scanEnd < paragraphs.size() ? scanEnd : 0;
    }

    /**
     * 判断文本是否匹配任意一种章节标题格式
     */
    private boolean isAnyChapterHeading(String text) {
        return CN_CHAPTER.matcher(text).matches()
                || CN_NUM_CHAPTER.matcher(text).matches()
                || ARABIC_CHAPTER.matcher(text).matches();
    }

    /**
     * 判断段落是否为封面元数据
     */
    private boolean isCoverParagraph(String text) {
        if (text == null || text.isBlank()) return false;
        // 匹配封面关键词
        long matchCount = COVER_KEYWORDS.stream()
                .filter(text::contains)
                .count();
        if (matchCount >= 1) return true;
        // 极短行（封面常见格式：空行、分割线、标题装饰）
        if (text.length() <= 5) return true;
        return false;
    }

    /**
     * 从封面段落中提取元数据（学号、姓名、学院等）
     *
     * @return key-value 映射，如 {"学号": "2021001", "姓名": "张三", "学院": "文学院"}
     */
    private Map<String, String> extractMetadata(List<XWPFParagraph> paragraphs) {
        Map<String, String> metadata = new LinkedHashMap<>();
        int scanEnd = Math.min(paragraphs.size(), COVER_SCAN_MAX);
        for (int i = 0; i < scanEnd; i++) {
            String text = paragraphs.get(i).getText().trim();
            if (text.isEmpty()) continue;
            java.util.regex.Matcher m = METADATA_KEY_VALUE.matcher(text);
            if (m.matches()) {
                String key = m.group(1);
                String value = m.group(2).trim();
                // 避免空值覆盖已有值
                if (!value.isEmpty() && !metadata.containsKey(key)) {
                    metadata.put(key, value);
                }
            }
        }
        return metadata;
    }

    /**
     * 判断是否为脚注/尾注段落
     */
    private boolean isFootnoteOrEndnote(XWPFParagraph para) {
        try {
            String styleId = para.getStyle();
            if (styleId != null) {
                String lower = styleId.toLowerCase();
                if (lower.contains("footnote") || lower.contains("endnote")) {
                    return true;
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    /**
     * 判断段落是否为学术参考文献条目，若是则不应作为标题
     */
    private boolean isReferenceParagraph(String text) {
        if (text == null || text.isBlank()) return false;

        // 如果文本匹配章节标题模式且长度较短 → 视为标题，不作为参考文献
        if (text.length() <= MAX_HEADING_LENGTH && isAnyChapterHeading(text)) {
            return false;
        }

        // 特征 1: 包含参考文献类型标识 [M] [J] [D] [C] [N] [R] [S] [P] [Z] [EB/OL]
        if (REFERENCE_TYPE_TAG.matcher(text).find()) {
            return true;
        }

        // 特征 2: 序号开头 + 中文字符/英文名（如 "[1] 张三." 或 "1. Author."）
        if (REFERENCE_NUMBER_PREFIX.matcher(text).matches()) {
            return true;
        }

        // 特征 3: 包含出版社/期刊特征 + 年份的学术引用
        if (REFERENCE_ENTRY.matcher(text).matches()) {
            return true;
        }

        // 特征 4: 段落以 "参考文献" 开头但不是干净的标题（超过字数）
        if (text.startsWith("参考文献") && text.length() > MAX_HEADING_LENGTH) {
            return true;
        }

        return false;
    }

    // ==================== 标题检测（拆分为章节标题 + 子标题）====================

    /**
     * 检测是否为顶级章节标题（只有此类标题会创建新章节）
     * <p>
     * 认定条件（任一满足即可）：
     * 1. Word 内置 Heading 1 样式
     * 2. "第N章" / "第N节" / "第N篇" / "第N部分" 中文标题模式
     * 3. "一、二、三、" 中文数字顿号模式
     * 4. "1. 2. 3、" 阿拉伯数字模式
     */
    private ChapterHeadingInfo detectChapterHeading(XWPFDocument doc, XWPFParagraph para, String text) {
        // 超过最大长度 → 不是标题
        if (text.length() > MAX_HEADING_LENGTH) return null;

        // 条件 1: Word Heading 1 样式
        ChapterHeadingInfo fromStyle = detectHeading1ByStyle(doc, para);
        if (fromStyle != null) return fromStyle;

        // 条件 2: 中文 "第N章/节/篇/部分" 模式（如 "第一章 绪论"）
        if (isChapterPattern(text)) {
            return new ChapterHeadingInfo(1);
        }

        // 条件 3: "一、XXX" / "二、XXX" 中文数字顿号模式
        if (CN_NUM_CHAPTER.matcher(text).matches()) {
            return new ChapterHeadingInfo(1);
        }

        // 条件 4: "1. XXX" / "2、XXX" 阿拉伯数字模式
        if (ARABIC_CHAPTER.matcher(text).matches()) {
            return new ChapterHeadingInfo(1);
        }

        return null;
    }

    /**
     * 检测是否为子标题（在章节内部渲染为 {@code <h2>} / {@code <h3>}，不新开章节）
     * <p>
     * 认定条件（任一满足即可）：
     * 1. Word 内置 Heading 2 / Heading 3 样式
     * 2. 中文子标题模式：（一）（二）（三）—仅括号形式
     * 3. 数字编号多级模式：1.1、1.1.1、2.1（单级如 "1." 在顶级章节中处理）
     * 4. 加粗短文本（≤50 字）— 常见于论文子标题
     */
    private SubHeadingInfo detectSubHeading(XWPFDocument doc, XWPFParagraph para, String text) {
        // 超过最大长度 → 不是标题（子标题通常更短）
        if (text.length() > MAX_HEADING_LENGTH) return null;

        // 条件 1: Word Heading 2 / 3 样式
        SubHeadingInfo fromStyle = detectHeading2Or3ByStyle(doc, para);
        if (fromStyle != null) return fromStyle;

        // 条件 2: 中文子标题模式
        if (CN_SECTION.matcher(text).matches()) {
            return new SubHeadingInfo(2);
        }

        // 条件 3: 数字编号模式（如 1.1 / 1.1.1 / 1、）
        if (NUM_HEADING.matcher(text).matches()) {
            int dots = text.replaceAll("[^.]", "").length();
            return new SubHeadingInfo(Math.min(dots + 1, 3));
        }

        // 条件 4: 加粗短文本（格式启发式）
        SubHeadingInfo fromFormat = detectSubHeadingByFormat(para, text);
        if (fromFormat != null) return fromFormat;

        return null;
    }

    /** 判断文本是否匹配 "第N章/节/篇/部分" 模式（支持阿拉伯数字如"第1章"） */
    private boolean isChapterPattern(String text) {
        return CN_CHAPTER.matcher(text).matches();
    }

    /**
     * 检测 Word Heading 1 样式
     */
    private ChapterHeadingInfo detectHeading1ByStyle(XWPFDocument doc, XWPFParagraph para) {
        Integer level = getHeadingLevel(doc, para);
        if (level != null && level == 1) {
            return new ChapterHeadingInfo(1);
        }
        return null;
    }

    /**
     * 检测 Word Heading 2 / 3 样式
     */
    private SubHeadingInfo detectHeading2Or3ByStyle(XWPFDocument doc, XWPFParagraph para) {
        Integer level = getHeadingLevel(doc, para);
        if (level != null && (level == 2 || level == 3)) {
            return new SubHeadingInfo(level);
        }
        return null;
    }

    /**
     * 获取段落的内置标题级别（1-3），非标题段落返回 null
     */
    private Integer getHeadingLevel(XWPFDocument doc, XWPFParagraph para) {
        try {
            String styleId = para.getStyle();
            if (styleId == null || styleId.isEmpty()) return null;

            XWPFStyle style = doc.getStyles().getStyle(styleId);
            if (style == null || style.getName() == null) return null;

            String lower = style.getName().toLowerCase().trim();
            if (lower.startsWith("heading")) {
                String numPart = lower.replaceAll("heading\\s*", "").trim();
                try {
                    int level = Integer.parseInt(numPart);
                    return Math.min(level, 3);
                } catch (NumberFormatException e) {
                    return 1;
                }
            }
        } catch (Exception e) {
            log.debug("标题样式解析异常", e);
        }
        return null;
    }

    /**
     * 通过段落格式启发式识别子标题（加粗 + 短文本 → 判定为子标题）
     * <p>
     * 注意：不再将加粗短文本作为顶级章节（旧版中这是封面标签被误认为章节的主因）
     */
    private SubHeadingInfo detectSubHeadingByFormat(XWPFParagraph para, String text) {
        if (text.length() > MAX_HEADING_LENGTH) return null;
        if (para.getRuns() == null || para.getRuns().isEmpty()) return null;

        boolean isBold = para.getRuns().stream().anyMatch(r -> {
            try { return r.isBold(); } catch (Exception e) { return false; }
        });
        if (!isBold) return null;

        // 加粗文本作为子标题（h3）
        return new SubHeadingInfo(3);
    }

    // ==================== 章节创建 ====================

    private ThesisSection createSection(Long thesisId, String title, String sectionKey,
                                        String content, int sortOrder, Long parentId) {
        ThesisSection section = new ThesisSection();
        section.setThesisId(thesisId);
        section.setTitle(title);
        section.setSectionKey(sectionKey);
        section.setContent(content != null ? content : "");
        section.setDraftContent("");
        section.setSectionType("chapter");
        section.setParentId(parentId);
        section.setSortOrder(sortOrder);
        return section;
    }

    // ==================== 文本工具 ====================

    private String extractPlainText(XWPFDocument doc) {
        StringBuilder sb = new StringBuilder();
        for (XWPFParagraph para : doc.getParagraphs()) {
            String text = para.getText().trim();
            if (!text.isEmpty()) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(escapeToHtml(text));
            }
        }
        return wrapBodyHtml(sb.toString());
    }

    private String wrapBodyHtml(String text) {
        if (text == null || text.isBlank()) return "";
        String[] paragraphs = text.split("\n");
        StringBuilder html = new StringBuilder();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                html.append("<p>").append(trimmed).append("</p>");
            }
        }
        return html.toString();
    }

    private String escapeToHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // ==================== 内部数据结构 ====================

    /** 解析出的一个章节块 */
    private static class SectionBlock {
        String heading;
        int level;
        String bodyHtml;
    }

    /** 顶级章节标题检测结果（第N章 / Heading 1） */
    private static class ChapterHeadingInfo {
        final int level;
        ChapterHeadingInfo(int level) { this.level = level; }
    }

    /** 子标题检测结果（一、 二、 1.1、加粗文本等 → 渲染为 h2/h3） */
    private static class SubHeadingInfo {
        final int level;
        SubHeadingInfo(int level) { this.level = level; }
    }

    /** 模板章节定义（对应 JSON 结构） */
    static class ChapterDef {
        public String key;
        public String title;
        public List<ChapterDef> children = new ArrayList<>();
    }
}
