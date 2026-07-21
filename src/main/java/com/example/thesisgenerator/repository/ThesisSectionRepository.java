package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ThesisSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThesisSectionRepository extends JpaRepository<ThesisSection, Long> {
    List<ThesisSection> findByThesisIdOrderBySortOrder(Long thesisId);
    List<ThesisSection> findByThesisIdAndParentIdOrderBySortOrder(Long thesisId, Long parentId);
    List<ThesisSection> findByThesisIdAndParentIdIsNullOrderBySortOrder(Long thesisId);
}
