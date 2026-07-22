package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.entity.ThesisReference;
import com.example.thesisgenerator.repository.ReferenceRepository;
import com.example.thesisgenerator.repository.ThesisReferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 论文学位参考文献关联服务
 */
@Service
@RequiredArgsConstructor
public class ThesisReferenceService {

    private final ThesisReferenceRepository thesisReferenceRepository;
    private final ReferenceRepository referenceRepository;

    /** 获取论文已关联的参考文献列表 */
    public List<ThesisReference> getThesisReferences(Long thesisId) {
        return thesisReferenceRepository.findByThesisIdOrderBySortOrder(thesisId);
    }

    /** 为论文添加一条参考文献（从全局文献库中引用） */
    @Transactional
    public ThesisReference addReference(Long thesisId, Long referenceId) {
        // 检查是否已添加
        List<ThesisReference> existing = thesisReferenceRepository.findByThesisIdOrderBySortOrder(thesisId);
        boolean alreadyAdded = existing.stream().anyMatch(r -> r.getReferenceId() != null && r.getReferenceId().equals(referenceId));
        if (alreadyAdded) {
            throw new BusinessException("该文献已添加到论文中");
        }

        // 从全局文献库获取数据
        Reference ref = referenceRepository.findById(referenceId)
                .orElseThrow(() -> new BusinessException("文献不存在"));

        // 创建关联记录（快照）
        ThesisReference tr = new ThesisReference();
        tr.setThesisId(thesisId);
        tr.setReferenceId(ref.getId());
        tr.setAuthors(ref.getAuthors());
        tr.setTitle(ref.getTitle());
        tr.setJournal(ref.getJournal());
        tr.setYear(ref.getYear() != null ? String.valueOf(ref.getYear()) : null);
        tr.setVolume(ref.getVolume());
        tr.setIssue(ref.getIssue());
        tr.setPages(ref.getPages());
        tr.setDoi(null);
        tr.setSortOrder(existing.size() + 1);
        return thesisReferenceRepository.save(tr);
    }

    /** 从论文中移除参考文献 */
    @Transactional
    public void removeReference(Long thesisId, Long id) {
        ThesisReference tr = thesisReferenceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("关联记录不存在"));
        if (!tr.getThesisId().equals(thesisId)) {
            throw new BusinessException("无权操作此记录");
        }
        thesisReferenceRepository.delete(tr);
        // 重排序号
        List<ThesisReference> remaining = thesisReferenceRepository.findByThesisIdOrderBySortOrder(thesisId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setSortOrder(i + 1);
        }
        thesisReferenceRepository.saveAll(remaining);
    }

    /** 更新引用序号（拖拽排序后） */
    @Transactional
    public void updateOrder(Long thesisId, List<Long> ids) {
        List<ThesisReference> refs = thesisReferenceRepository.findByThesisIdOrderBySortOrder(thesisId);
        for (int i = 0; i < ids.size(); i++) {
            final int order = i + 1;
            final Long targetId = ids.get(i);
            refs.stream()
                    .filter(r -> r.getId().equals(targetId))
                    .findFirst()
                    .ifPresent(r -> r.setSortOrder(order));
        }
        thesisReferenceRepository.saveAll(refs);
    }
}
