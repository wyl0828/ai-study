CREATE DATABASE IF NOT EXISTS ai_interview_coach
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_interview_coach;

DROP TABLE IF EXISTS training_plan_item;
DROP TABLE IF EXISTS training_plan;
DROP TABLE IF EXISTS mock_interview_report;
DROP TABLE IF EXISTS mock_interview_turn;
DROP TABLE IF EXISTS mock_interview_session;
DROP TABLE IF EXISTS rag_chunk;
DROP TABLE IF EXISTS rag_document;
DROP TABLE IF EXISTS mistake_card;
DROP TABLE IF EXISTS self_test_record;
DROP TABLE IF EXISTS user_knowledge_card_mastery;
DROP TABLE IF EXISTS user_weakness_event;
DROP TABLE IF EXISTS user_weakness;
DROP TABLE IF EXISTS hint_record;
DROP TABLE IF EXISTS ai_diagnosis;
DROP TABLE IF EXISTS agent_step;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS submission;
DROP TABLE IF EXISTS test_case;
DROP TABLE IF EXISTS problem_knowledge_point;
DROP TABLE IF EXISTS knowledge_card;
DROP TABLE IF EXISTS knowledge_point;
DROP TABLE IF EXISTS problem;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(128),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE problem (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    category VARCHAR(64) NOT NULL,
    input_format TEXT,
    output_format TEXT,
    code_mode VARCHAR(32) NOT NULL DEFAULT 'acm',
    template_code LONGTEXT,
    solution_outline TEXT,
    hint_level1 TEXT,
    hint_level2 TEXT,
    hint_level3 TEXT,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_problem_enabled_id (enabled, id),
    INDEX idx_problem_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description TEXT,
    UNIQUE KEY uk_knowledge_point_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    follow_up TEXT,
    key_points TEXT,
    difficulty VARCHAR(32) NOT NULL,
    tags VARCHAR(255),
    source_name VARCHAR(128),
    source_url VARCHAR(512),
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_knowledge_card_category (category, enabled, sort_order),
    INDEX idx_knowledge_card_enabled (enabled, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE problem_knowledge_point (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    knowledge_point_id BIGINT NOT NULL,
    UNIQUE KEY uk_problem_knowledge_point (problem_id, knowledge_point_id),
    INDEX idx_pkp_problem_id (problem_id),
    INDEX idx_pkp_knowledge_point_id (knowledge_point_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    input_data TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    is_sample TINYINT(1) NOT NULL DEFAULT 0,
    weight INT NOT NULL DEFAULT 1,
    INDEX idx_test_case_problem_id (problem_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE submission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    language VARCHAR(32) NOT NULL,
    code LONGTEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    passed_count INT NOT NULL DEFAULT 0,
    total_count INT NOT NULL DEFAULT 0,
    execution_time INT,
    memory_usage INT,
    error_message TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_submission_user_id (user_id, created_at),
    INDEX idx_submission_problem_id (problem_id, created_at),
    INDEX idx_submission_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_state VARCHAR(64) NOT NULL,
    error_message TEXT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_agent_run_submission_id (submission_id),
    INDEX idx_agent_run_user_id (user_id, created_at),
    INDEX idx_agent_run_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE agent_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_run_id BIGINT NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    tool_name VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    input_summary TEXT,
    output_summary TEXT,
    duration_ms BIGINT,
    error_message TEXT,
    started_at DATETIME NOT NULL,
    finished_at DATETIME,
    created_at DATETIME NOT NULL,
    INDEX idx_agent_step_run_id (agent_run_id, id),
    INDEX idx_agent_step_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ai_diagnosis (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_run_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    specific_error TEXT NOT NULL,
    diagnosis TEXT NOT NULL,
    suggestion TEXT,
    confidence DECIMAL(5, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    INDEX idx_ai_diagnosis_submission_id (submission_id),
    INDEX idx_ai_diagnosis_user_id (user_id, created_at),
    INDEX idx_ai_diagnosis_error_type (error_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hint_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_run_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    hint_level INT NOT NULL,
    hint_content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_hint_record_submission_id (submission_id),
    INDEX idx_hint_record_user_problem (user_id, problem_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_weakness (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    wrong_count INT NOT NULL DEFAULT 0,
    submit_count INT NOT NULL DEFAULT 0,
    weakness_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    last_wrong_at DATETIME,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_user_weakness_point_type (user_id, knowledge_point, error_type),
    INDEX idx_user_weakness_score (user_id, weakness_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_weakness_event (
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

CREATE TABLE mistake_card (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    problem_id BIGINT NOT NULL,
    submission_id BIGINT NOT NULL,
    agent_run_id BIGINT NOT NULL,
    error_type VARCHAR(64) NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    mistake_summary TEXT NOT NULL,
    correct_idea TEXT,
    fingerprint VARCHAR(255),
    repeat_count INT NOT NULL DEFAULT 1,
    last_seen_at DATETIME,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL,
    INDEX idx_mistake_user_id (user_id, created_at),
    INDEX idx_mistake_problem_id (problem_id),
    INDEX idx_mistake_fingerprint (user_id, fingerprint, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    title VARCHAR(255) NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_rag_document_source (source_type, source_id, user_id),
    INDEX idx_rag_document_problem (problem_id, status),
    INDEX idx_rag_document_user (user_id, status),
    INDEX idx_rag_document_knowledge (knowledge_point, status),
    INDEX idx_rag_document_error (error_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rag_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    metadata_json TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_rag_chunk_document (document_id, chunk_index),
    INDEX idx_rag_chunk_problem (problem_id, source_type),
    INDEX idx_rag_chunk_user (user_id, source_type),
    INDEX idx_rag_chunk_knowledge (knowledge_point, source_type),
    INDEX idx_rag_chunk_error (error_type, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_knowledge_card_mastery (
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

CREATE TABLE self_test_record (
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

CREATE TABLE mock_interview_session (
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

CREATE TABLE mock_interview_turn (
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

CREATE TABLE mock_interview_report (
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

CREATE TABLE training_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_run_id BIGINT,
    title VARCHAR(128) NOT NULL,
    summary TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    INDEX idx_training_plan_user_id (user_id, created_at),
    INDEX idx_training_plan_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE training_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    item_type VARCHAR(32) NOT NULL DEFAULT 'PROBLEM',
    knowledge_card_id BIGINT,
    day_index INT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    problem_title VARCHAR(128),
    knowledge_card_title VARCHAR(128),
    reason TEXT NOT NULL,
    review_focus TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    INDEX idx_training_plan_item_plan_id (plan_id, day_index),
    INDEX idx_training_plan_item_type (plan_id, item_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
