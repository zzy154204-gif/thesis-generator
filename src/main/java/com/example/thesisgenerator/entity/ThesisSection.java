package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "thesis_section")
public class ThesisSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "section_key", nullable = false, length = 100)
    private String sectionKey;

    @Column(columnDefinition = "TEXT")
    private String content = "";

    @Column(name = "draft_content", columnDefinition = "TEXT")
    private String draftContent = "";

    @Column(name = "section_type", length = 50)
    private String sectionType = "chapter";

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
