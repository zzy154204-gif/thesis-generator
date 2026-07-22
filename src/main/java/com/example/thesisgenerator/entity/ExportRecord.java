package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 导出记录 — 记录每次论文导出操作
 */
@Data
@Entity
@Table(name = "export_record")
public class ExportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 论文标题（冗余，便于展示） */
    @Column(name = "thesis_title", length = 300)
    private String thesisTitle;

    /** 导出格式：DOCX / PDF */
    @Column(nullable = false, length = 10)
    private String format;

    /** 状态：SUCCESS / FAILED */
    @Column(nullable = false, length = 20)
    private String status = "SUCCESS";

    /** 失败原因 */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /** 文件大小（字节） */
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
