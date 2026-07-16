package com.example.thesisgenerator.service;

import com.example.thesisgenerator.entity.Template;
import com.example.thesisgenerator.entity.TemplateVersion;
import com.example.thesisgenerator.repository.TemplateRepository;
import com.example.thesisgenerator.repository.TemplateVersionRepository;
import com.example.thesisgenerator.repository.ThesisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository versionRepository;
    private final ThesisRepository thesisRepository;

    public List<Template> findAll(String type, Long collegeId) {
        if (type != null && collegeId != null) {
            return templateRepository.findByCollegeIdAndTypeAndEnabledTrue(collegeId, type);
        }
        if (type != null) {
            return templateRepository.findByType(type);
        }
        if (collegeId != null) {
            return templateRepository.findByCollegeIdAndEnabledTrue(collegeId);
        }
        return templateRepository.findAll();
    }

    public Template findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found: " + id));
    }

    @Transactional
    public Template create(Template template) {
        Template saved = templateRepository.save(template);
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(saved.getId());
        version.setVersionNumber("1.0");
        version.setIsCurrent(true);
        versionRepository.save(version);
        return saved;
    }

    public Template update(Long id, Template template) {
        Template existing = findById(id);
        existing.setName(template.getName());
        existing.setType(template.getType());
        existing.setCollegeId(template.getCollegeId());
        existing.setEnabled(template.getEnabled());
        return templateRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        findById(id);
        List<TemplateVersion> versions = versionRepository.findByTemplateIdOrderByCreatedAtDesc(id);
        for (TemplateVersion v : versions) {
            long count = thesisRepository.countByTemplateVersionIdAndStatusNot(v.getId(), "COMPLETED");
            if (count > 0) {
                throw new RuntimeException("Template has active thesis, cannot delete");
            }
        }
        templateRepository.deleteById(id);
    }

    public List<TemplateVersion> getVersions(Long templateId) {
        return versionRepository.findByTemplateIdOrderByCreatedAtDesc(templateId);
    }

    @Transactional
    public TemplateVersion createVersion(Long templateId) {
        TemplateVersion current = versionRepository
                .findByTemplateIdAndIsCurrentTrue(templateId)
                .orElseThrow(() -> new RuntimeException("No current version for template: " + templateId));
        String newVersion = incrementVersion(current.getVersionNumber());
        versionRepository.clearCurrentVersion(templateId);
        TemplateVersion newVer = new TemplateVersion();
        newVer.setTemplateId(templateId);
        newVer.setVersionNumber(newVersion);
        newVer.setIsCurrent(true);
        newVer.setCoverFields(current.getCoverFields());
        newVer.setChapterStructure(current.getChapterStructure());
        newVer.setFormatConfig(current.getFormatConfig());
        return versionRepository.save(newVer);
    }

    @Transactional
    public TemplateVersion activateVersion(Long templateId, Long versionId) {
        versionRepository.clearCurrentVersion(templateId);
        TemplateVersion target = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));
        target.setIsCurrent(true);
        return versionRepository.save(target);
    }

    @Transactional
    public TemplateVersion updateVersionConfig(Long versionId,
            String coverFields, String chapterStructure, String formatConfig) {
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionId));
        if (coverFields != null) version.setCoverFields(coverFields);
        if (chapterStructure != null) version.setChapterStructure(chapterStructure);
        if (formatConfig != null) version.setFormatConfig(formatConfig);
        return versionRepository.save(version);
    }

    private String incrementVersion(String current) {
        String[] parts = current.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        if (minor < 9) {
            return major + "." + (minor + 1);
        } else {
            return (major + 1) + ".0";
        }
    }
}
