package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByThesisIdOrderBySubmittedAtDesc(Long thesisId);
    long countByThesisId(Long thesisId);

    /** 获取学生的所有提交记录 */
    List<Submission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
}
