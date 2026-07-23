package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {
    List<ReviewRecord> findByThesisIdOrderByCreatedAtDesc(Long thesisId);

    /** 获取教师的批阅记录（排除暂存草稿） */
    @Query("SELECT r FROM ReviewRecord r WHERE r.teacherId = :teacherId AND r.action IN ('REVIEWED', 'RETURNED') ORDER BY r.createdAt DESC")
    List<ReviewRecord> findReviewRecordsByTeacherId(@Param("teacherId") Long teacherId);
}
