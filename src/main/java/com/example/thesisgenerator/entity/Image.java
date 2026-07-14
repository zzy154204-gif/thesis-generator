package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 图片实体
 * <p>
 * 记录用户上传的图片元信息，图片文件本身存储在本地磁盘。
 */
@Entity
@Table(name = "tb_image")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 原始文件名（用户上传时的名字） */
    @Column(nullable = false)
    private String originalName;

    /** 存储文件名（UUID 生成，防止重名冲突） */
    @Column(nullable = false)
    private String storedName;

    /** 文件存储路径 */
    @Column(nullable = false)
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件 MIME 类型 */
    private String contentType;

    /** 上传时间 */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
