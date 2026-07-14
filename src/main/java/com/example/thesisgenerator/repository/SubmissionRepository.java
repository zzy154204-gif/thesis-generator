package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByThesisIdOrderBySubmittedAtDesc(Long thesisId);
    long countByThesisId(Long thesisId);
}
