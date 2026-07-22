package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.ReviewRecord;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.repository.ReviewRecordRepository;
import com.example.thesisgenerator.repository.ThesisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewRecordService {

    private final ReviewRecordRepository reviewRecordRepository;
    private final ThesisRepository thesisRepository;

    /**
     * 教师提交批阅结果（通过或退回+打分）
     */
    @Transactional
    public ReviewRecord submitReview(Long thesisId, Long teacherId,
                                      String commentHtml, Integer score, String grade,
                                      String action, String returnReason) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));

        // 只有已提交状态才能批阅
        if (!"SUBMITTED".equals(thesis.getStatus())) {
            throw new BusinessException(400, "当前论文状态不允许批阅，状态: " + thesis.getStatus());
        }

        // 退回必须填写原因
        if ("RETURNED".equals(action) && (returnReason == null || returnReason.isBlank())) {
            throw new BusinessException(400, "退回操作必须填写退回原因");
        }

        // 操作类型校验
        if (!"REVIEWED".equals(action) && !"RETURNED".equals(action) && !"DRAFT".equals(action)) {
            throw new BusinessException(400, "无效的批阅操作: " + action + "，有效值: REVIEWED, RETURNED, DRAFT");
        }

        ReviewRecord record = new ReviewRecord();
        record.setThesisId(thesisId);
        record.setTeacherId(teacherId);
        record.setCommentHtml(commentHtml);
        record.setScore(score);
        record.setGrade(grade);
        record.setAction(action);
        record.setReturnReason(returnReason);
        record = reviewRecordRepository.save(record);

        // 暂存（DRAFT）不改变论文状态
        if ("DRAFT".equals(action)) {
            return record;
        }

        // 更新论文状态
        if ("REVIEWED".equals(action)) {
            thesis.setStatus("REVIEWED");
        } else {
            thesis.setStatus("RETURNED");
        }
        thesisRepository.save(thesis);

        return record;
    }

    /**
     * 查询某论文的全部批阅记录（按时间倒序）
     */
    public List<ReviewRecord> getReviewHistory(Long thesisId) {
        return reviewRecordRepository.findByThesisIdOrderByCreatedAtDesc(thesisId);
    }
}
