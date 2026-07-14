package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "submission")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber = 1;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
        if (versionNumber == null) versionNumber = 1;
    }
}
