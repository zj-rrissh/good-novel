-- Minimal MySQL schema for local development.
-- Keep table names aligned with com.ainovel.persistence.schema.SchemaNaming.

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    roles VARCHAR(255) NOT NULL DEFAULT 'USER',
    login_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_username (username),
    KEY idx_user_account_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_profile (
    user_id BIGINT NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    avatar_url VARCHAR(512) DEFAULT NULL,
    bio VARCHAR(1000) DEFAULT NULL,
    level INT NOT NULL DEFAULT 1,
    verified_status VARCHAR(32) NOT NULL DEFAULT 'UNVERIFIED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id),
    CONSTRAINT fk_user_profile_account FOREIGN KEY (user_id) REFERENCES user_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    to_user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    biz_type VARCHAR(64) DEFAULT NULL,
    biz_id BIGINT DEFAULT NULL,
    producer VARCHAR(64) DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    read_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_user_message_inbox (to_user_id, created_at),
    KEY idx_user_message_unread (to_user_id, read_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS novel (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    intro TEXT,
    cover_url VARCHAR(512) DEFAULT NULL,
    category_id BIGINT DEFAULT NULL,
    tag_ids VARCHAR(255) DEFAULT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    latest_chapter_id BIGINT DEFAULT NULL,
    word_count BIGINT NOT NULL DEFAULT 0,
    audit_task_id VARCHAR(64) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_novel_author_status (author_id, status),
    KEY idx_novel_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS novel_chapter (
    id BIGINT NOT NULL AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    chapter_no INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    content MEDIUMTEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    audit_task_id VARCHAR(64) DEFAULT NULL,
    published_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_novel_chapter_no (novel_id, chapter_no),
    KEY idx_novel_chapter_query (novel_id, status, chapter_no),
    CONSTRAINT fk_novel_chapter_novel FOREIGN KEY (novel_id) REFERENCES novel (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    reply_to_user_id BIGINT DEFAULT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'VISIBLE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_comment_target_page (target_type, target_id, status, created_at),
    KEY idx_comment_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reaction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    reaction_type VARCHAR(32) NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reaction_user_target (user_id, reaction_type, target_type, target_id),
    KEY idx_reaction_target_counter (reaction_type, target_type, target_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_follow_pair (user_id, target_user_id),
    KEY idx_user_follow_target (target_user_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS audit_task (
    task_id BIGINT NOT NULL AUTO_INCREMENT,
    biz_type VARCHAR(64) NOT NULL,
    biz_id BIGINT NOT NULL,
    content_snapshot MEDIUMTEXT NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    audit_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    risk_level VARCHAR(32) DEFAULT NULL,
    reason_code VARCHAR(64) DEFAULT NULL,
    reason_text VARCHAR(1024) DEFAULT NULL,
    reviewer_id BIGINT DEFAULT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    rule_version VARCHAR(32) DEFAULT NULL,
    reviewed_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (task_id),
    UNIQUE KEY uk_audit_task_hash (biz_type, biz_id, content_hash),
    KEY idx_audit_task_status (audit_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS reading_progress (
    user_id BIGINT NOT NULL,
    novel_id BIGINT NOT NULL,
    chapter_id BIGINT NOT NULL,
    progress_percent INT NOT NULL DEFAULT 0,
    page_offset BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (user_id, novel_id),
    KEY idx_reading_progress_novel (novel_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
