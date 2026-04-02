SET @add_failed_login_count = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'user_account'
              AND COLUMN_NAME = 'failed_login_count'
        ),
        'SELECT 1',
        'ALTER TABLE user_account ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0'
    )
);
PREPARE stmt_add_failed_login_count FROM @add_failed_login_count;
EXECUTE stmt_add_failed_login_count;
DEALLOCATE PREPARE stmt_add_failed_login_count;

SET @add_locked_until = (
    SELECT IF(
        EXISTS(
            SELECT 1
            FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'user_account'
              AND COLUMN_NAME = 'locked_until'
        ),
        'SELECT 1',
        'ALTER TABLE user_account ADD COLUMN locked_until DATETIME(3) DEFAULT NULL'
    )
);
PREPARE stmt_add_locked_until FROM @add_locked_until;
EXECUTE stmt_add_locked_until;
DEALLOCATE PREPARE stmt_add_locked_until;

CREATE TABLE IF NOT EXISTS user_login_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT DEFAULT NULL,
    username_attempt VARCHAR(64) NOT NULL,
    success TINYINT(1) NOT NULL DEFAULT 0,
    ip_address VARCHAR(64) DEFAULT NULL,
    device_id VARCHAR(128) DEFAULT NULL,
    lock_triggered TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_login_record_user (user_id, created_at),
    KEY idx_login_record_time (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
