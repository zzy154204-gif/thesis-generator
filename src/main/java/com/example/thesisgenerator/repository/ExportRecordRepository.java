package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ExportRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExportRecordRepository extends JpaRepository<ExportRecord, Long> {
    List<ExportRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ExportRecord> findByThesisIdOrderByCreatedAtDesc(Long thesisId);
}
