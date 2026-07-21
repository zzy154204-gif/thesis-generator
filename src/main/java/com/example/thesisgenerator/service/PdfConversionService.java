package com.example.thesisgenerator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * LibreOffice 无头转换服务：将 DOCX 转为 PDF
 */
@Slf4j
@Service
public class PdfConversionService {

    @Value("${libreoffice.path}")
    private String libreofficePath;

    /**
     * 将 DOCX 文件转换为 PDF
     * @param docxBytes DOCX 文件的字节数组
     * @return PDF 文件的字节数组
     */
    public byte[] convertDocxToPdf(byte[] docxBytes) {
        Path tempDir = null;
        try {
            // 创建临时目录（LibreOffice 需要在独立目录中工作，避免并发冲突）
            tempDir = Files.createTempDirectory("thesis-export-");
            Path docxPath = tempDir.resolve("input.docx");
            Files.write(docxPath, docxBytes);

            // 检查 LibreOffice 是否存在
            File soffice = new File(libreofficePath);
            if (!soffice.exists()) {
                throw new RuntimeException("LibreOffice 未找到，路径: " + libreofficePath
                        + "。请确认已安装 LibreOffice，或在 application.properties 中配置 libreoffice.path");
            }

            // 构建命令
            ProcessBuilder pb = new ProcessBuilder(
                    soffice.getAbsolutePath(),
                    "--headless",
                    "--convert-to", "pdf:writer_pdf_Export",
                    "--outdir", tempDir.toAbsolutePath().toString(),
                    docxPath.toAbsolutePath().toString()
            );
            pb.directory(tempDir.toFile());
            pb.redirectErrorStream(true);

            log.info("执行 LibreOffice 转换: {}", String.join(" ", pb.command()));
            Process process = pb.start();

            // 读取输出（防止缓冲区满导致阻塞）
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("LibreOffice: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("LibreOffice 转换失败，退出码: " + exitCode);
            }

            // 读取生成的 PDF
            Path pdfPath = tempDir.resolve("input.pdf");
            if (!Files.exists(pdfPath)) {
                throw new RuntimeException("PDF 文件未生成，请检查 LibreOffice 安装");
            }

            return Files.readAllBytes(pdfPath);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PDF 转换被中断", e);
        } catch (IOException e) {
            throw new RuntimeException("PDF 转换 IO 错误: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempDir != null) {
                try {
                    try (var files = Files.walk(tempDir)) {
                        files.sorted(java.util.Comparator.reverseOrder())
                                .forEach(p -> {
                                    try {
                                        Files.deleteIfExists(p);
                                    } catch (IOException ignored) {
                                    }
                                });
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }
}
