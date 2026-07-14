package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CollegeRepository extends JpaRepository<College, Long> {
    List<College> findByNameContaining(String name);
    boolean existsByCode(String code);
}
