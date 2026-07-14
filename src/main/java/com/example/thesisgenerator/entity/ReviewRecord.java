package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "review_record")
public class ReviewRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "comment_html", columnDefinition = "TEXT")
    private String commentHtml;

    @Column
    private Integer score;

    @Column(length = 10)
    private String grade;

    @Column(nullable = false, length = 20)
    private String action; // REVIEWED, RETURNED

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
