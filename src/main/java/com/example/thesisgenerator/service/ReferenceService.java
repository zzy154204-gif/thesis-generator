package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.repository.ReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 参考文献业务逻辑层
 */
@Service
@RequiredArgsConstructor
public class ReferenceService {

    private final ReferenceRepository referenceRepository;
    private final Gbt7714Formatter formatter;

    // ========== CRUD ==========

    /** 获取所有参考文献（按年份降序） */
    public List<Reference> findAll() {
        return referenceRepository.findAllByOrderByYearDesc();
    }

    /** 根据 ID 获取单条文献 */
    public Optional<Reference> findById(Long id) {
        return referenceRepository.findById(id);
    }

    /** 新增文献 */
    public Reference create(Reference reference) {
        return referenceRepository.save(reference);
    }

    /** 更新文献 */
    public Reference update(Long id, Reference updated) {
        return referenceRepository.findById(id).map(ref -> {
            ref.setAuthors(updated.getAuthors());
            ref.setTitle(updated.getTitle());
            ref.setType(updated.getType());
            ref.setYear(updated.getYear());
            ref.setJournal(updated.getJournal());
            ref.setVolume(updated.getVolume());
            ref.setIssue(updated.getIssue());
            ref.setPages(updated.getPages());
            ref.setPublisher(updated.getPublisher());
            ref.setAddress(updated.getAddress());
            ref.setConference(updated.getConference());
            ref.setInstitution(updated.getInstitution());
            ref.setUrl(updated.getUrl());
            ref.setAccessDate(updated.getAccessDate());
            return referenceRepository.save(ref);
        }).orElseThrow(() -> new BusinessException(404, "参考文献不存在，id: " + id));
    }

    /** 删除文献 */
    public void delete(Long id) {
        if (!referenceRepository.existsById(id)) {
            throw new BusinessException(404, "参考文献不存在，id: " + id);
        }
        referenceRepository.deleteById(id);
    }

    // ========== 搜索与格式化 ==========

    /** 按作者搜索 */
    public List<Reference> searchByAuthor(String keyword) {
        return referenceRepository.findByAuthorsContaining(keyword);
    }

    /** 按标题搜索 */
    public List<Reference> searchByTitle(String keyword) {
        return referenceRepository.findByTitleContaining(keyword);
    }

    /**
     * 格式化单条文献为 GB/T 7714 字符串
     */
    public String formatById(Long id) {
        return referenceRepository.findById(id)
                .map(formatter::format)
                .orElseThrow(() -> new BusinessException(404, "参考文献不存在，id: " + id));
    }

    /**
     * 格式化所有文献为 GB/T 7714 列表
     */
    public List<String> formatAll() {
        return referenceRepository.findAllByOrderByYearDesc().stream()
                .map(formatter::format)
                .toList();
    }
}
