package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Thesis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThesisRepository extends JpaRepository<Thesis, Long> {
    List<Thesis> findByStudentIdOrderByUpdatedAtDesc(Long studentId);
    List<Thesis> findByStatusInOrderByUpdatedAtDesc(List<String> statuses);
    List<Thesis> findByStatus(String status);
    List<Thesis> findByStatusAndTeacherId(String status, Long teacherId);
    long countByTemplateVersionIdAndStatusNot(Long templateVersionId, String status);
}
