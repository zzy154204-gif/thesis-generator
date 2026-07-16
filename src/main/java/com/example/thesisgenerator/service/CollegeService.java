package com.example.thesisgenerator.service;

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
                .orElseThrow(() -> new RuntimeException("学院不存在"));
    }

    public College create(College college) {
        if (collegeRepository.existsByCode(college.getCode())) {
            throw new RuntimeException("学院代码已存在");
        }
        return collegeRepository.save(college);
    }

    public College update(Long id, College college) {
        College existing = findById(id);
        existing.setName(college.getName());
        // code 不可修改
        return collegeRepository.save(existing);
    }

    public void delete(Long id) {
        College college = findById(id);
        long templateCount = templateRepository.countByCollegeId(id);
        if (templateCount > 0) {
            throw new RuntimeException("该学院下存在 " + templateCount + " 个关联模板，请先处理");
        }
        collegeRepository.delete(college);
    }
}
