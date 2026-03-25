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

-- Seed data for local development.
-- Demo credentials:
--   reader_demo / Reader@123
--   author_demo / Author@123
--   admin_demo  / Admin@123

INSERT INTO user_account (id, username, password_hash, status, roles, login_version)
VALUES
    (1001, 'reader_demo', '$2b$10$jGQav3HMeq102vlU4U5ZtO37Wv0EljahpIsMYuiyzgvtBQ9gZozxq', 'NORMAL', 'USER', 1),
    (1002, 'author_demo', '$2b$10$NeJg7hhnZReRR5BWF5TmB.1fFvAZi.Z8duIHmf/yly4GkZrHQDjl.', 'NORMAL', 'USER,AUTHOR', 1),
    (1003, 'admin_demo', '$2b$10$/xkQ55tl1NDmBHJdwsiLMuCSGrok0SwLU/LWOje560U9zydjbwhjW', 'NORMAL', 'USER,AUTHOR,ADMIN,REVIEWER', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    status = VALUES(status),
    roles = VALUES(roles),
    login_version = VALUES(login_version),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO user_profile (user_id, nickname, avatar_url, bio, level, verified_status)
VALUES
    (1001, 'Reader Demo', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=256&q=80', 'Demo reader account for local testing.', 3, 'UNVERIFIED'),
    (1002, 'Author Demo', 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=256&q=80', 'Demo author account with seeded novels.', 5, 'VERIFIED'),
    (1003, 'Admin Demo', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=256&q=80', 'Demo admin and reviewer account.', 7, 'VERIFIED')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    avatar_url = VALUES(avatar_url),
    bio = VALUES(bio),
    level = VALUES(level),
    verified_status = VALUES(verified_status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO novel (id, author_id, title, intro, cover_url, category_id, tag_ids, status, latest_chapter_id, word_count, audit_task_id)
VALUES
    (1, 1002, 'sample novel', 'A starter fantasy story for homepage and detail-page smoke testing.', 'https://example.com/cover.png', 1, '1,2,3', 'ON_SHELF', 11001, 2180, NULL),
    (2, 1002, 'Clockwork Frontier', 'When steam engines learn to dream, the border city stops sleeping.', 'https://images.unsplash.com/photo-1512820790803-83ca734da794?auto=format&fit=crop&w=720&q=80', 2, '2,5,8', 'ON_SHELF', 11002, 2640, NULL),
    (3, 1002, 'Ocean of Ash', 'An exiled navigator returns to map a sea that burns at night.', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?auto=format&fit=crop&w=720&q=80', 3, '3,6,9', 'ON_SHELF', 11003, 2310, NULL),
    (4, 1002, 'Last Light Academy', 'A failing student discovers the school library can rewrite fate.', 'https://images.unsplash.com/photo-1516979187457-637abb4f9353?auto=format&fit=crop&w=720&q=80', 4, '4,7,10', 'ON_SHELF', 11004, 2890, NULL),
    (5, 1002, 'Night Market Chronicles', 'Each midnight bargain grants power and takes a memory.', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=720&q=80', 5, '5,11,12', 'ON_SHELF', 11005, 2475, NULL),
    (6, 1002, 'Starrail Apothecary', 'A healer rides orbital trains to cure worlds under quarantine.', 'https://images.unsplash.com/photo-1513128034602-7814ccaddd4e?auto=format&fit=crop&w=720&q=80', 6, '6,13,14', 'ON_SHELF', 11006, 2760, NULL)
ON DUPLICATE KEY UPDATE
    author_id = VALUES(author_id),
    title = VALUES(title),
    intro = VALUES(intro),
    cover_url = VALUES(cover_url),
    category_id = VALUES(category_id),
    tag_ids = VALUES(tag_ids),
    status = VALUES(status),
    latest_chapter_id = VALUES(latest_chapter_id),
    word_count = VALUES(word_count),
    audit_task_id = VALUES(audit_task_id),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO novel_chapter (id, novel_id, chapter_no, title, content, status, audit_task_id, published_at)
VALUES
    (11001, 1, 1, 'Chapter 1: Arrival', 'Rain hammered the station glass when Lin stepped into Ironbridge. The city map said this was a frontier town, but every street had five stories of history and one rumor about monsters in the river fog.', 'PUBLISHED', NULL, '2026-01-10 10:00:00.000'),
    (11002, 2, 1, 'Chapter 1: Brass Dawn', 'At dawn the boiler towers exhaled white clouds over Clockwork Frontier. Mara counted the pulses in the steam lines, because each pulse matched a name erased from the census.', 'PUBLISHED', NULL, '2026-01-10 10:10:00.000'),
    (11003, 3, 1, 'Chapter 1: Burning Tide', 'The harbor master warned that no ship survived the Ash Ocean after sunset. Rook paid double for a lantern anyway and asked where the old star charts were buried.', 'PUBLISHED', NULL, '2026-01-10 10:20:00.000'),
    (11004, 4, 1, 'Chapter 1: Borrowed Page', 'The detention room had one cracked window and one forbidden book. When Mei opened page thirteen, tomorrow changed its mind about her exam results.', 'PUBLISHED', NULL, '2026-01-10 10:30:00.000'),
    (11005, 5, 1, 'Chapter 1: Midnight Price', 'At exactly 00:00, the lanterns in Night Market turned blue. Every merchant offered one miracle and demanded one memory in return.', 'PUBLISHED', NULL, '2026-01-10 10:40:00.000'),
    (11006, 6, 1, 'Chapter 1: Quarantine Orbit', 'The starrail stopped above a silent colony where no signals had left for three weeks. Toma packed medicine, then hid his fear in a joke nobody laughed at.', 'PUBLISHED', NULL, '2026-01-10 10:50:00.000')
ON DUPLICATE KEY UPDATE
    novel_id = VALUES(novel_id),
    chapter_no = VALUES(chapter_no),
    title = VALUES(title),
    content = VALUES(content),
    status = VALUES(status),
    audit_task_id = VALUES(audit_task_id),
    published_at = VALUES(published_at),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO reading_progress (user_id, novel_id, chapter_id, progress_percent, page_offset)
VALUES
    (1001, 1, 11001, 62, 2048),
    (1001, 2, 11002, 18, 512)
ON DUPLICATE KEY UPDATE
    chapter_id = VALUES(chapter_id),
    progress_percent = VALUES(progress_percent),
    page_offset = VALUES(page_offset),
    updated_at = CURRENT_TIMESTAMP(3);
