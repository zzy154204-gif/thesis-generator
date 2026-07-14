package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    List<ReviewRecord> findByThesisIdOrderByCreatedAtDesc(Long thesisId);
}
