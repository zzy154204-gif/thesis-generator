-- ============================================
-- 论文生成系统 - 数据库初始化脚本
-- ============================================

-- 学院表
CREATE TABLE IF NOT EXISTS sys_college (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'STUDENT', 'TEACHER')),
    college_id BIGINT REFERENCES sys_college(id),
    email VARCHAR(100),
    phone VARCHAR(20),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 论文模板表
CREATE TABLE IF NOT EXISTS template (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('GRADUATION', 'COURSE', 'PROJECT')),
    college_id BIGINT REFERENCES sys_college(id),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模板版本表
CREATE TABLE IF NOT EXISTS template_version (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES template(id) ON DELETE CASCADE,
    version_number VARCHAR(20) NOT NULL DEFAULT '1.0',
    is_current BOOLEAN DEFAULT TRUE,
    cover_fields JSONB NOT NULL DEFAULT '[]',
    chapter_structure JSONB NOT NULL DEFAULT '[]',
    format_config JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 论文表
CREATE TABLE IF NOT EXISTS thesis (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES sys_user(id),
    template_version_id BIGINT REFERENCES template_version(id),
    title VARCHAR(500) NOT NULL DEFAULT '未命名论文',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'COMPLETED', 'SUBMITTED', 'REVIEWED', 'RETURNED', 'GENERATING')),
    is_locked BOOLEAN DEFAULT FALSE,
    college_id BIGINT REFERENCES sys_college(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 论文章节表
CREATE TABLE IF NOT EXISTS thesis_section (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    section_key VARCHAR(100) NOT NULL,
    content TEXT DEFAULT '',
    draft_content TEXT DEFAULT '',
    section_type VARCHAR(50) DEFAULT 'chapter',
    parent_id BIGINT REFERENCES thesis_section(id),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 参考文献表
CREATE TABLE IF NOT EXISTS thesis_reference (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    authors VARCHAR(500) NOT NULL,
    title VARCHAR(500) NOT NULL,
    journal VARCHAR(300),
    year VARCHAR(10),
    volume VARCHAR(50),
    issue VARCHAR(50),
    pages VARCHAR(50),
    doi VARCHAR(200),
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 图片资源表
CREATE TABLE IF NOT EXISTS thesis_image (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    section_id BIGINT REFERENCES thesis_section(id),
    original_name VARCHAR(500) NOT NULL,
    stored_path VARCHAR(500) NOT NULL,
    url VARCHAR(500) NOT NULL,
    file_size BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 提交记录表
CREATE TABLE IF NOT EXISTS submission (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES sys_user(id),
    version_number INT NOT NULL DEFAULT 1,
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 批注表
CREATE TABLE IF NOT EXISTS annotation (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    section_id BIGINT NOT NULL REFERENCES thesis_section(id),
    teacher_id BIGINT NOT NULL REFERENCES sys_user(id),
    start_offset INT NOT NULL,
    text_length INT NOT NULL,
    selected_text TEXT,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 批阅记录表
CREATE TABLE IF NOT EXISTS review_record (
    id BIGSERIAL PRIMARY KEY,
    thesis_id BIGINT NOT NULL REFERENCES thesis(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES sys_user(id),
    comment_html TEXT,
    score INT CHECK (score >= 0 AND score <= 100),
    grade VARCHAR(10),
    action VARCHAR(20) NOT NULL CHECK (action IN ('REVIEWED', 'RETURNED')),
    return_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 章节草稿历史表（自动保存版本回退）
CREATE TABLE IF NOT EXISTS thesis_section_draft_history (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES thesis_section(id) ON DELETE CASCADE,
    draft_content TEXT,
    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_user_role ON sys_user(role);
CREATE INDEX IF NOT EXISTS idx_template_college ON template(college_id);
CREATE INDEX IF NOT EXISTS idx_template_type ON template(type);
CREATE INDEX IF NOT EXISTS idx_thesis_student ON thesis(student_id);
CREATE INDEX IF NOT EXISTS idx_thesis_status ON thesis(status);
CREATE INDEX IF NOT EXISTS idx_section_thesis ON thesis_section(thesis_id);
CREATE INDEX IF NOT EXISTS idx_annotation_thesis ON annotation(thesis_id);
CREATE INDEX IF NOT EXISTS idx_review_thesis ON review_record(thesis_id);

-- 独立参考文献库表（tb_ 前缀，不依赖论文ID，通用管理用）
CREATE TABLE IF NOT EXISTS tb_reference (
    id BIGSERIAL PRIMARY KEY,
    authors VARCHAR(500) NOT NULL,
    title VARCHAR(500) NOT NULL,
    type VARCHAR(10) NOT NULL,
    year INT,
    journal VARCHAR(500),
    volume VARCHAR(50),
    issue VARCHAR(50),
    pages VARCHAR(50),
    publisher VARCHAR(500),
    address VARCHAR(500),
    conference VARCHAR(500),
    institution VARCHAR(500),
    url VARCHAR(1000),
    access_date VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 独立图片资源表
CREATE TABLE IF NOT EXISTS tb_image (
    id BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(500) NOT NULL,
    stored_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT,
    content_type VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
