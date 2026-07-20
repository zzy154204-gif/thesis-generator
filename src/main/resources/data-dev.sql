-- ============================================
-- 默认种子数据（H2 兼容版，用于 dev profile）
-- ============================================

-- 学院
MERGE INTO sys_college (id, name, code) KEY(id) VALUES
(1, '计算机科学与技术学院', 'CS'),
(2, '电子信息工程学院', 'EE'),
(3, '数学与统计学院', 'MATH');

-- 模板
MERGE INTO template (id, name, type, college_id) KEY(id) VALUES
(1, '本科毕业论文标准模板', 'GRADUATION', 1),
(2, '课程设计报告模板', 'COURSE', 1),
(3, '硕士学位论文模板', 'GRADUATION', 1);

-- 模板版本
MERGE INTO template_version (id, template_id, version_number, is_current, cover_fields, chapter_structure, format_config) KEY(id) VALUES
(1, 1, '1.0', TRUE, '[]', '[]', '{}'),
(2, 2, '1.0', TRUE, '[]', '[]', '{}'),
(3, 3, '1.0', TRUE, '[]', '[]', '{}');
