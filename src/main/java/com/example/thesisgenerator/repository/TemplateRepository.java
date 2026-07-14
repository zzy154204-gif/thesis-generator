package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByCollegeIdAndEnabledTrue(Long collegeId);
    List<Template> findByType(String type);
    List<Template> findByCollegeIdAndTypeAndEnabledTrue(Long collegeId, String type);
    long countByCollegeId(Long collegeId);
}
