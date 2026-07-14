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

    @Column(nullable = false, length = 500)
    private String title = "未命名论文";

    @Column(nullable = false, length = 20)
    private String status = "DRAFT"; // DRAFT, COMPLETED, SUBMITTED, REVIEWED, RETURNED, GENERATING

    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    @Column(name = "college_id")
    private Long collegeId;

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
