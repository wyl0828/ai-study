USE ai_interview_coach;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN alter_clause_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_value, ' ', alter_clause_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

CREATE PROCEDURE add_index_if_missing(
    IN table_name_value VARCHAR(64),
    IN index_name_value VARCHAR(64),
    IN alter_clause_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND INDEX_NAME = index_name_value
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_value, ' ', alter_clause_value);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CREATE TABLE IF NOT EXISTS user_weakness_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT,
    delta_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    before_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    after_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    reason TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_weakness_event_user_time (user_id, created_at),
    INDEX idx_weakness_event_point (user_id, knowledge_point, error_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('mistake_card', 'fingerprint',
    'ADD COLUMN fingerprint VARCHAR(255) NULL');
CALL add_column_if_missing('mistake_card', 'repeat_count',
    'ADD COLUMN repeat_count INT NOT NULL DEFAULT 1');
CALL add_column_if_missing('mistake_card', 'last_seen_at',
    'ADD COLUMN last_seen_at DATETIME NULL');
CALL add_column_if_missing('mistake_card', 'status',
    'ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''OPEN''');
CALL add_index_if_missing('mistake_card', 'idx_mistake_fingerprint',
    'ADD INDEX idx_mistake_fingerprint (user_id, fingerprint, status)');

CREATE TABLE IF NOT EXISTS user_knowledge_card_mastery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_card_id BIGINT NOT NULL,
    mastery_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    self_test_count INT NOT NULL DEFAULT 0,
    last_score INT,
    last_practiced_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_card_mastery (user_id, knowledge_card_id),
    INDEX idx_user_card_mastery_score (user_id, mastery_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS self_test_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_card_id BIGINT NOT NULL,
    question_snapshot TEXT NOT NULL,
    user_answer TEXT NOT NULL,
    score INT NOT NULL,
    feedback TEXT,
    missing_key_points TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_self_test_user_card_time (user_id, knowledge_card_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CALL add_column_if_missing('training_plan', 'status',
    'ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT ''ACTIVE''');
CALL add_index_if_missing('training_plan', 'idx_training_plan_status',
    'ADD INDEX idx_training_plan_status (user_id, status)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
