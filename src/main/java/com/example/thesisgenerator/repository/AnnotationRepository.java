package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Annotation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AnnotationRepository extends JpaRepository<Annotation, Long> {
    List<Annotation> findByThesisIdOrderByCreatedAt(Long thesisId);
    List<Annotation> findByThesisIdAndSectionIdOrderByStartOffset(Long thesisId, Long sectionId);
}
