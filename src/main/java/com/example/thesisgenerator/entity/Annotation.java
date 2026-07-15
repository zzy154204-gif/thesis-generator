package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "annotation")
public class Annotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;

    @Column(name = "text_length", nullable = false)
    private Integer textLength;

    @Column(name = "selected_text", columnDefinition = "TEXT")
    private String selectedText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
