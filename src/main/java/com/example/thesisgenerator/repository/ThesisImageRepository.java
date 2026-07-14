package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.ThesisImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThesisImageRepository extends JpaRepository<ThesisImage, Long> {
    List<ThesisImage> findByThesisId(Long thesisId);
}
