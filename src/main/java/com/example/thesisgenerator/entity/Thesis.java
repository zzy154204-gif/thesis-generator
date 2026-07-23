package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "thesis")
public class Thesis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "template_version_id")
    private Long templateVersionId;

    /** 模板名称（非持久化，由 Service 层填充） */
    @Transient
    private String templateName;

    @Column(nullable = false, length = 500)
    private String title = "未命名论文";

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, COMPLETED, SUBMITTED, REVIEWED, RETURNED, GENERATING

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "college_id")
    private Long collegeId;

    /** Word 导入时从封面提取的元数据 JSON（学号、姓名、学院等） */
    @Column(name = "import_metadata", columnDefinition = "TEXT")
    private String importMetadata;

    /** 指导老师 ID（批阅权限归属） */
    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "DRAFT";
        if (isLocked == null) isLocked = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
