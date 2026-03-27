CREATE TABLE IF NOT EXISTS admin_operation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT,
    action VARCHAR(64) NOT NULL,
    biz_type VARCHAR(64) NOT NULL,
    biz_id BIGINT NOT NULL,
    operator_id BIGINT DEFAULT NULL,
    operator_roles VARCHAR(128) DEFAULT NULL,
    from_status VARCHAR(64) DEFAULT NULL,
    to_status VARCHAR(64) DEFAULT NULL,
    reason VARCHAR(1024) DEFAULT NULL,
    trace_id VARCHAR(64) DEFAULT NULL,
    request_path VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (log_id),
    KEY idx_admin_operation_log_created (created_at, log_id),
    KEY idx_admin_operation_log_action (action, created_at),
    KEY idx_admin_operation_log_biz (biz_type, biz_id, created_at),
    KEY idx_admin_operation_log_operator (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
