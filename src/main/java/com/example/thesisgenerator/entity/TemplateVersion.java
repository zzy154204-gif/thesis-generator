package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "template_version")
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "version_number", nullable = false, length = 20)
    private String versionNumber = "1.0";

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @Column(name = "cover_fields", columnDefinition = "TEXT")
    private String coverFields = "[]";

    @Column(name = "chapter_structure", columnDefinition = "TEXT")
    private String chapterStructure = "[]";

    @Column(name = "format_config", columnDefinition = "TEXT")
    private String formatConfig = "{}";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (isCurrent == null) isCurrent = true;
        if (versionNumber == null) versionNumber = "1.0";
    }
}
