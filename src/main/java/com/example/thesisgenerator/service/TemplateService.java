package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
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
            return templateRepository.findByTypeAndEnabledTrue(type);
        }
        if (collegeId != null) {
            return templateRepository.findByCollegeIdAndEnabledTrue(collegeId);
        }
        return templateRepository.findByEnabledTrue();
    }

    /** 仅返回已启用的模板（与 findAll 逻辑相同） */
    public List<Template> findAvailable(String type, Long collegeId) {
        return findAll(type, collegeId);
    }

    public Template findById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "模板不存在: " + id));
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
                throw new BusinessException(400, "该模板下存在未完成的论文，无法删除");
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
                .orElseThrow(() -> new BusinessException(404, "模板无当前版本: " + templateId));
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
                .orElseThrow(() -> new BusinessException(404, "版本不存在: " + versionId));
        target.setIsCurrent(true);
        return versionRepository.save(target);
    }

    @Transactional
    public TemplateVersion updateVersionConfig(Long versionId,
            String coverFields, String chapterStructure, String formatConfig) {
        TemplateVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(404, "版本不存在: " + versionId));
        if (coverFields != null) version.setCoverFields(coverFields);
        if (chapterStructure != null) version.setChapterStructure(chapterStructure);
        if (formatConfig != null) version.setFormatConfig(formatConfig);
        return versionRepository.save(version);
    }

    /** 管理员查询（含已停用、关键词过滤） */
    public List<Template> findAllAdmin(String type, Long collegeId, String status, String keyword) {
        // 先按 type/collegeId 过滤
        List<Template> all;
        if (type != null && collegeId != null) {
            all = templateRepository.findByCollegeIdAndType(collegeId, type);
        } else if (type != null) {
            all = templateRepository.findByType(type);
        } else if (collegeId != null) {
            all = templateRepository.findByCollegeId(collegeId);
        } else {
            all = templateRepository.findAll();
        }
        // 按状态过滤
        if ("ENABLED".equalsIgnoreCase(status)) {
            all = all.stream().filter(Template::getEnabled).toList();
        } else if ("DISABLED".equalsIgnoreCase(status)) {
            all = all.stream().filter(t -> !t.getEnabled()).toList();
        }
        // 按关键词过滤
        if (keyword != null && !keyword.isBlank()) {
            all = all.stream()
                    .filter(t -> t.getName().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }
        return all;
    }

    /** 切换启用/停用 */
    @Transactional
    public Template toggleStatus(Long id, String status) {
        Template t = findById(id);
        t.setEnabled("ENABLED".equalsIgnoreCase(status));
        return templateRepository.save(t);
    }

    /** 创建模板并保存版本配置 */
    @Transactional
    public Template createWithConfig(Template template, String coverFields,
                                      String chapterStructure, String formatConfig) {
        Template saved = templateRepository.save(template);
        TemplateVersion version = new TemplateVersion();
        version.setTemplateId(saved.getId());
        version.setVersionNumber("1.0");
        version.setIsCurrent(true);
        version.setCoverFields(coverFields);
        version.setChapterStructure(chapterStructure);
        version.setFormatConfig(formatConfig);
        versionRepository.save(version);
        return saved;
    }

    /** 获取当前版本 */
    public TemplateVersion getCurrentVersion(Long templateId) {
        return versionRepository.findByTemplateIdAndIsCurrentTrue(templateId).orElse(null);
    }

    /** 更新模板基本信息 */
    @Transactional
    public Template updateTemplate(Template template) {
        return templateRepository.save(template);
    }

    /** 复制模板 */
    @Transactional
    public Template duplicate(Long id) {
        Template source = findById(id);
        Template copy = new Template();
        copy.setName(source.getName() + " (副本)");
        copy.setType(source.getType());
        copy.setCollegeId(source.getCollegeId());
        copy.setEnabled(false);
        Template saved = templateRepository.save(copy);
        // 复制最新版本
        getVersions(id).stream()
                .findFirst().ifPresent(v -> {
                    TemplateVersion nv = new TemplateVersion();
                    nv.setTemplateId(saved.getId());
                    nv.setVersionNumber("1.0");
                    nv.setIsCurrent(true);
                    nv.setCoverFields(v.getCoverFields());
                    nv.setChapterStructure(v.getChapterStructure());
                    nv.setFormatConfig(v.getFormatConfig());
                    versionRepository.save(nv);
                });
        return saved;
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
