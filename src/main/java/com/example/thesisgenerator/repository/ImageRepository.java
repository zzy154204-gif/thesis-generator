package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 图片数据访问层
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
}
