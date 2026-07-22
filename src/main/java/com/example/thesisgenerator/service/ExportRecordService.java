package com.example.thesisgenerator.service;

import com.example.thesisgenerator.entity.ExportRecord;
import com.example.thesisgenerator.repository.ExportRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 导出记录业务层
 */
@Service
@RequiredArgsConstructor
public class ExportRecordService {

    private final ExportRecordRepository repository;

    /** 记录一次导出 */
    public ExportRecord record(Long userId, Long thesisId, String thesisTitle,
                               String format, String status, String errorMessage, Long fileSize) {
        ExportRecord r = new ExportRecord();
        r.setUserId(userId);
        r.setThesisId(thesisId);
        r.setThesisTitle(thesisTitle);
        r.setFormat(format);
        r.setStatus(status);
        r.setErrorMessage(errorMessage);
        r.setFileSize(fileSize);
        return repository.save(r);
    }

    /** 获取用户的导出历史（按时间倒序） */
    public List<ExportRecord> getUserHistory(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
