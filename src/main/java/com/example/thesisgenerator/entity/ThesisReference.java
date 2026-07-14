package com.example.thesisgenerator.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "thesis_reference")
public class ThesisReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thesis_id", nullable = false)
    private Long thesisId;

    @Column(nullable = false, length = 500)
    private String authors;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 300)
    private String journal;

    @Column(length = 10)
    private String year;

    @Column(length = 50)
    private String volume;

    @Column(length = 50)
    private String issue;

    @Column(length = 50)
    private String pages;

    @Column(length = 200)
    private String doi;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (sortOrder == null) sortOrder = 0;
    }
}
