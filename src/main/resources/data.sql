-- ============================================
-- 默认种子数据
-- ============================================

-- 学院
INSERT INTO sys_college (id, name, code) VALUES
(1, '计算机科学与技术学院', 'CS'),
(2, '电子信息工程学院', 'EE'),
(3, '数学与统计学院', 'MATH')
ON CONFLICT (id) DO NOTHING;

-- 模板
INSERT INTO template (id, name, type, college_id) VALUES
(1, '本科毕业论文标准模板', 'GRADUATION', 1),
(2, '课程设计报告模板', 'COURSE', 1),
(3, '硕士学位论文模板', 'GRADUATION', 1)
ON CONFLICT (id) DO NOTHING;

-- 模板版本
INSERT INTO template_version (id, template_id, version_number, is_current, cover_fields, chapter_structure, format_config) VALUES
(1, 1, '1.0', TRUE, '[]', '[]', '{}'),
(2, 2, '1.0', TRUE, '[]', '[]', '{}'),
(3, 3, '1.0', TRUE, '[]', '[]', '{}')
ON CONFLICT (id) DO NOTHING;
