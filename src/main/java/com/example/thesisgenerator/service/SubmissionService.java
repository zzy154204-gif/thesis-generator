package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.Submission;
import com.example.thesisgenerator.entity.Thesis;
import com.example.thesisgenerator.repository.SubmissionRepository;
import com.example.thesisgenerator.repository.ThesisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ThesisRepository thesisRepository;

    /**
     * 学生提交论文：校验论文归属、状态，生成版本号递增的提交记录
     */
    @Transactional
    public Submission submitThesis(Long thesisId, Long studentId) {
        Thesis thesis = thesisRepository.findById(thesisId)
                .orElseThrow(() -> new BusinessException(404, "论文不存在"));

        // 校验论文归属
        if (!thesis.getStudentId().equals(studentId)) {
            throw new BusinessException(403, "只能提交自己的论文");
        }

        // 只有草稿或退回状态才能提交
        if (!"DRAFT".equals(thesis.getStatus()) && !"RETURNED".equals(thesis.getStatus())
                && !"COMPLETED".equals(thesis.getStatus())) {
            throw new BusinessException(400, "当前论文状态不允许提交，状态: " + thesis.getStatus());
        }

        // 版本号递增
        long count = submissionRepository.countByThesisId(thesisId);
        int newVersion = (int) count + 1;

        Submission submission = new Submission();
        submission.setThesisId(thesisId);
        submission.setStudentId(studentId);
        submission.setVersionNumber(newVersion);
        submission = submissionRepository.save(submission);

        // 更新论文状态为已提交
        thesis.setStatus("SUBMITTED");
        thesisRepository.save(thesis);

        return submission;
    }

    /**
     * 查询某论文的全部提交历史（按时间倒序）
     */
    public List<Submission> getSubmissionHistory(Long thesisId) {
        return submissionRepository.findByThesisIdOrderBySubmittedAtDesc(thesisId);
    }
}
