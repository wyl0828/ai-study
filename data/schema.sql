CREATE DATABASE IF NOT EXISTS ai_interview_coach
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_interview_coach;

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
