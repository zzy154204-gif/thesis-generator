package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, Long> {
    List<TemplateVersion> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
    Optional<TemplateVersion> findByTemplateIdAndIsCurrentTrue(Long templateId);

    @Modifying
    @Query("UPDATE TemplateVersion tv SET tv.isCurrent = false WHERE tv.templateId = :templateId")
    void clearCurrentVersion(Long templateId);
}
