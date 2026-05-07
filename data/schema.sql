CREATE DATABASE IF NOT EXISTS ai_interview_coach
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_interview_coach;

DROP TABLE IF EXISTS training_plan_item;
DROP TABLE IF EXISTS training_plan;
DROP TABLE IF EXISTS mistake_card;
DROP TABLE IF EXISTS user_weakness;
DROP TABLE IF EXISTS hint_record;
DROP TABLE IF EXISTS ai_diagnosis;
DROP TABLE IF EXISTS agent_step;
DROP TABLE IF EXISTS agent_run;
DROP TABLE IF EXISTS submission;
DROP TABLE IF EXISTS test_case;
DROP TABLE IF EXISTS problem_knowledge_point;
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
    template_code LONGTEXT,
    solution_outline TEXT,
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
    created_at DATETIME NOT NULL,
    INDEX idx_mistake_user_id (user_id, created_at),
    INDEX idx_mistake_problem_id (problem_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE training_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    agent_run_id BIGINT,
    title VARCHAR(128) NOT NULL,
    summary TEXT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    INDEX idx_training_plan_user_id (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE training_plan_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    day_index INT NOT NULL,
    knowledge_point VARCHAR(64) NOT NULL,
    problem_title VARCHAR(128),
    reason TEXT NOT NULL,
    review_focus TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    INDEX idx_training_plan_item_plan_id (plan_id, day_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
