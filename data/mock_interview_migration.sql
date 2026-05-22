USE ai_interview_coach;

CREATE TABLE IF NOT EXISTS mock_interview_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    interviewer_style VARCHAR(32) NOT NULL DEFAULT 'BIG_TECH',
    question_count INT NOT NULL DEFAULT 3,
    answered_main_count INT NOT NULL DEFAULT 0,
    current_knowledge_card_id BIGINT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_mock_interview_session_user_time (user_id, created_at),
    INDEX idx_mock_interview_session_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS mock_interview_turn (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    knowledge_card_id BIGINT NOT NULL,
    turn_order INT NOT NULL,
    turn_type VARCHAR(32) NOT NULL,
    parent_turn_id BIGINT,
    question TEXT NOT NULL,
    user_answer TEXT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    feedback TEXT,
    performance_level VARCHAR(32),
    strength_summary TEXT,
    gap_summary TEXT,
    expression_feedback TEXT,
    interviewer_observation TEXT,
    follow_up_reason TEXT,
    hit_key_points TEXT,
    missing_key_points TEXT,
    expression_issue TEXT,
    ai_raw_json TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_mock_interview_turn_session_order (session_id, turn_order),
    INDEX idx_mock_interview_turn_card (knowledge_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @schema_name = DATABASE();
SET @add_session_style = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_session ADD COLUMN interviewer_style VARCHAR(32) NOT NULL DEFAULT ''BIG_TECH'' AFTER status',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_session' AND COLUMN_NAME = 'interviewer_style'
);
PREPARE stmt FROM @add_session_style;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_performance_level = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN performance_level VARCHAR(32) AFTER feedback',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'performance_level'
);
PREPARE stmt FROM @add_performance_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_strength_summary = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN strength_summary TEXT AFTER performance_level',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'strength_summary'
);
PREPARE stmt FROM @add_strength_summary;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_gap_summary = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN gap_summary TEXT AFTER strength_summary',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'gap_summary'
);
PREPARE stmt FROM @add_gap_summary;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_expression_feedback = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN expression_feedback TEXT AFTER gap_summary',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'expression_feedback'
);
PREPARE stmt FROM @add_expression_feedback;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_interviewer_observation = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN interviewer_observation TEXT AFTER expression_feedback',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'interviewer_observation'
);
PREPARE stmt FROM @add_interviewer_observation;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_follow_up_reason = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE mock_interview_turn ADD COLUMN follow_up_reason TEXT AFTER interviewer_observation',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @schema_name AND TABLE_NAME = 'mock_interview_turn' AND COLUMN_NAME = 'follow_up_reason'
);
PREPARE stmt FROM @add_follow_up_reason;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS mock_interview_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    average_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    summary TEXT,
    strengths TEXT,
    weaknesses TEXT,
    expression_advice TEXT,
    recommended_card_ids TEXT,
    weakness_tags TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_mock_interview_report_session (session_id),
    INDEX idx_mock_interview_report_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
