package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ThesisReference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThesisReferenceRepository extends JpaRepository<ThesisReference, Long> {
    List<ThesisReference> findByThesisIdOrderBySortOrder(Long thesisId);
}
