package com.example.thesisgenerator.repository;

import com.example.thesisgenerator.entity.Reference;
import com.example.thesisgenerator.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 参考文献数据访问层
 */
@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    /** 按作者模糊搜索 */
    List<Reference> findByAuthorsContaining(String authors);

    /** 按标题模糊搜索 */
    List<Reference> findByTitleContaining(String title);

    /** 按文献类型查询 */
    List<Reference> findByType(ReferenceType type);

    /** 按年份降序排列 */
    List<Reference> findAllByOrderByYearDesc();
}
