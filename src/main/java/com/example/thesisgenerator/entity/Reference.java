package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 参考文献实体
 * <p>
 * 存储用户录入的一条参考文献信息，支持 GB/T 7714 多种文献类型的字段。
 * 类型特有字段（如 journal、publisher 等）根据 type 区分使用。
 */
@Entity
@Table(name = "tb_reference")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 作者（多作者用逗号分隔，如"张三, 李四"） */
    @Column(nullable = false, length = 500)
    private String authors;

    /** 文献标题 */
    @Column(nullable = false, length = 500)
    private String title;

    /** 文献类型：J / M / C / D / EB */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReferenceType type;

    /** 出版年份 */
    private Integer year;

    // ========== 期刊文章 [J] 专用字段 ==========

    /** 期刊/刊物名称 */
    private String journal;

    /** 卷号 */
    private String volume;

    /** 期号 */
    private String issue;

    /** 页码（如 "120-135"） */
    private String pages;

    // ========== 专著/书籍 [M] 专用字段 ==========

    /** 出版社 */
    private String publisher;

    /** 出版地 */
    private String address;

    // ========== 会议论文 [C] 专用字段 ==========

    /** 会议名称 */
    private String conference;

    // ========== 学位论文 [D] 专用字段 ==========

    /** 学位授予单位 */
    private String institution;

    // ========== 网络资源 [EB] 专用字段 ==========

    /** 访问链接 */
    @Column(length = 1000)
    private String url;

    /** 引用日期 */
    private String accessDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
