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

-- 演示用户 (密码: admin123 / 123456 / 123456)
MERGE INTO sys_user (id, username, password, real_name, role, college_id, email, phone, enabled) KEY(id) VALUES
(1, 'admin', '$2b$10$ltFgZmkCozU9.5zTih6qaujEp/MdJEvK75A2auHX7Bry3eIwXCh/u', '管理员', 'ADMIN', 1, NULL, NULL, TRUE),
(2, 'teacher1', '$2b$10$QSP6POHfolUPxJOV3Voj5OzcNEFWhpq.wEvhZ3t3fuFbxFC0bX7yS', '张老师', 'TEACHER', 1, NULL, NULL, TRUE),
(3, 'student1', '$2b$10$QSP6POHfolUPxJOV3Voj5OzcNEFWhpq.wEvhZ3t3fuFbxFC0bX7yS', '李同学', 'STUDENT', 1, NULL, NULL, TRUE);

-- 演示论文
MERGE INTO thesis (id, student_id, template_version_id, title, status, college_id) KEY(id) VALUES
(1, 3, 1, '基于深度学习的图像识别研究', 'DRAFT', 1),
(2, 3, 2, '学生信息管理系统设计与实现', 'SUBMITTED', 1);

-- 演示章节
MERGE INTO thesis_section (id, thesis_id, title, section_key, content, sort_order) KEY(id) VALUES
(1, 1, '摘要', 'abstract', '本文研究了基于深度学习的图像识别技术...', 1),
(2, 1, '绪论', 'intro', '随着人工智能技术的快速发展...', 2);
