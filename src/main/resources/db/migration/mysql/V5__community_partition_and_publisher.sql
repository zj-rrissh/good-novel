ALTER TABLE novel
    ADD COLUMN publisher_name VARCHAR(128) NOT NULL DEFAULT '平台收录' AFTER author_id;

CREATE TABLE IF NOT EXISTS novel_community_partition (
    id BIGINT NOT NULL AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    partition_key VARCHAR(32) NOT NULL,
    partition_name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_novel_partition_key (novel_id, partition_key),
    KEY idx_novel_partition_query (novel_id, status, sort_order, id),
    CONSTRAINT fk_novel_partition_novel FOREIGN KEY (novel_id) REFERENCES novel (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO novel_community_partition (novel_id, partition_key, partition_name, sort_order, status)
SELECT n.id, 'GENERAL', '综合讨论', 10, 'ACTIVE'
FROM novel n
WHERE NOT EXISTS (
    SELECT 1 FROM novel_community_partition p WHERE p.novel_id = n.id AND p.partition_key = 'GENERAL'
);

INSERT INTO novel_community_partition (novel_id, partition_key, partition_name, sort_order, status)
SELECT n.id, 'PLOT', '剧情讨论', 20, 'ACTIVE'
FROM novel n
WHERE NOT EXISTS (
    SELECT 1 FROM novel_community_partition p WHERE p.novel_id = n.id AND p.partition_key = 'PLOT'
);

INSERT INTO novel_community_partition (novel_id, partition_key, partition_name, sort_order, status)
SELECT n.id, 'CHARACTER', '人物讨论', 30, 'ACTIVE'
FROM novel n
WHERE NOT EXISTS (
    SELECT 1 FROM novel_community_partition p WHERE p.novel_id = n.id AND p.partition_key = 'CHARACTER'
);
