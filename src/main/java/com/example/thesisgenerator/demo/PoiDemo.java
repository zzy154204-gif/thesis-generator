package com.example.thesisgenerator.demo;

import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.math.BigInteger;

/**
 * Apache POI 技术验证 Demo
 * <p>
 * 功能：创建一个包含标题、正文段落、表格的 .docx 文件，
 * 用于验证 POI 对中文字符、基本排版（字体、字号、加粗、对齐）、
 * 表格创建与样式控制的支持程度。
 * <p>
 * 运行方式：直接 main 方法运行，会在项目根目录生成 demo.docx
 */
public class PoiDemo {

    public static void main(String[] args) throws Exception {
        // 1. 创建一个新的 Word 文档对象
        XWPFDocument document = new XWPFDocument();

        // ========== 添加标题 ==========
        XWPFParagraph titleParagraph = document.createParagraph();
        // 设置标题段落对齐方式：居中
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText("基于 Spring Boot 的在线论文生成系统");
        titleRun.setBold(true);          // 加粗
        titleRun.setFontSize(22);        // 二号字 ≈ 22pt
        titleRun.setFontFamily("宋体");   // 设置中文字体
        titleRun.setColor("000000");     // 黑色

        // ========== 添加空行分隔 ==========
        addEmptyLine(document);

        // ========== 添加第一段正文 ==========
        XWPFParagraph para1 = document.createParagraph();
        para1.setAlignment(ParagraphAlignment.LEFT);
        // 首行缩进 2 字符（约等于 480 缇 = 2 * 240）
        para1.setIndentationFirstLine(480);
        XWPFRun run1 = para1.createRun();
        run1.setText("随着高等教育信息化建设的不断推进，传统论文撰写方式已难以满足当前数字化教学管理的需求。当前，学生在撰写毕业论文或课程论文时，通常需要手动调整排版格式，耗费大量时间在格式校对而非内容创作上。为了解决这一问题，本项目旨在开发一套基于 B/S 架构的在线论文自动生成工具，为学生提供模板化的论文编辑环境，并支持一键导出符合学术规范的 Word 和 PDF 文档。");
        run1.setFontSize(12);            // 小四号 ≈ 12pt
        run1.setFontFamily("宋体");

        // ========== 添加第二段正文 ==========
        XWPFParagraph para2 = document.createParagraph();
        para2.setAlignment(ParagraphAlignment.LEFT);
        para2.setIndentationFirstLine(480);
        XWPFRun run2 = para2.createRun();
        run2.setText("本系统采用前后端分离架构，后端基于 Spring Boot 4.1 框架构建，利用 Apache POI 库实现 Word 文档的自动化生成，通过 LibreOffice 无头转换模式将文档导出为 PDF 格式。前端采用现代 Web 技术提供富文本编辑体验，支持图片插入、表格编辑与参考文献管理等核心功能。系统按照用户角色划分为管理员、学生和教师三类，分别赋予模板管理、论文编辑导出和论文批阅等对应权限，形成完整的论文管理闭环。");
        run2.setFontSize(12);
        run2.setFontFamily("宋体");

        // ========== 添加第三段正文 ==========
        XWPFParagraph para3 = document.createParagraph();
        para3.setAlignment(ParagraphAlignment.LEFT);
        para3.setIndentationFirstLine(480);
        XWPFRun run3 = para3.createRun();
        run3.setText("本文档通过 Apache POI 自动生成，用于验证 POI 在以下方面的技术能力：① 中文文本的正确写入与显示；② 段落样式控制（字体、字号、加粗、对齐方式、首行缩进）；③ 表格的创建与样式设置；④ 文档整体结构的组织。验证结果将作为后续开发的技术选型依据，确保最终产品能够生成符合国家学术论文格式规范的标准文档。");
        run3.setFontSize(12);
        run3.setFontFamily("宋体");

        // ========== 添加表格 ==========
        addEmptyLine(document);

        // 创建一个 4 行 4 列的表格（带表头）
        XWPFTable table = document.createTable(4, 4);
        // 设置表格宽度：页面宽度（约 9360 缇，对应 A4 页面默认边距）
        table.setWidth("100%");

        // ---- 填充表头行 ----
        String[] headers = {"功能模块", "技术方案", "优先级", "状态"};
        for (int i = 0; i < headers.length; i++) {
            XWPFParagraph headerCell = table.getRow(0).getCell(i).getParagraphs().get(0);
            headerCell.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun = headerCell.createRun();
            headerRun.setText(headers[i]);
            headerRun.setBold(true);
            headerRun.setFontSize(10);
            headerRun.setFontFamily("宋体");
        }

        // ---- 填充数据行 ----
        String[][] data = {
                {"Word 文档生成", "Apache POI (XWPF)", "P0", "验证中"},
                {"PDF 文档导出", "LibreOffice 无头转换", "P0", "待验证"},
                {"参考文献格式化", "GB/T 7714 标准", "P1", "设计中"},
        };

        for (int row = 0; row < data.length; row++) {
            for (int col = 0; col < data[row].length; col++) {
                XWPFParagraph cellPara = table.getRow(row + 1).getCell(col).getParagraphs().get(0);
                cellPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun cellRun = cellPara.createRun();
                cellRun.setText(data[row][col]);
                cellRun.setFontSize(10);
                cellRun.setFontFamily("宋体");
            }
        }

        // ========== 保存文档 ==========
        String outputPath = "demo.docx";
        try (FileOutputStream out = new FileOutputStream(outputPath)) {
            document.write(out);
        }
        document.close();

        System.out.println("✅ demo.docx 已生成，路径：" + System.getProperty("user.dir") + "/" + outputPath);
    }

    /**
     * 在文档中添加一个空行段落
     */
    private static void addEmptyLine(XWPFDocument document) {
        XWPFParagraph empty = document.createParagraph();
        XWPFRun emptyRun = empty.createRun();
        emptyRun.setText("");
        emptyRun.setFontSize(12);
    }
}
