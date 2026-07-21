package com.example.thesisgenerator.service;

import com.example.thesisgenerator.common.BusinessException;
import com.example.thesisgenerator.entity.College;
import com.example.thesisgenerator.repository.CollegeRepository;
import com.example.thesisgenerator.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CollegeService {

    private final CollegeRepository collegeRepository;
    private final TemplateRepository templateRepository;

    public List<College> findAll() {
        return collegeRepository.findAll();
    }

    public College findById(Long id) {
        return collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "学院不存在: " + id));
    }

    public College create(College college) {
        if (collegeRepository.existsByCode(college.getCode())) {
            throw new BusinessException(400, "学院代码已存在: " + college.getCode());
        }
        return collegeRepository.save(college);
    }

    public College update(Long id, College college) {
        College existing = findById(id);
        existing.setName(college.getName());
        return collegeRepository.save(existing);
    }

    public void delete(Long id) {
        College college = findById(id);
        long count = templateRepository.countByCollegeId(id);
        if (count > 0) {
            throw new BusinessException(400, "该学院下存在 " + count + " 个模板，无法删除");
        }
        collegeRepository.delete(college);
    }
}
