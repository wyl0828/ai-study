USE ai_interview_coach;

-- 本迁移脚本用于已有本地数据库升级，只执行一次。
-- 如果 knowledge_card 表已存在，请跳过 CREATE TABLE。
-- 如果 training_plan_item 中某个字段已存在，请跳过对应 ALTER TABLE 语句。

CREATE TABLE IF NOT EXISTS knowledge_card (
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

ALTER TABLE training_plan_item
    ADD COLUMN item_type VARCHAR(32) NOT NULL DEFAULT 'PROBLEM' AFTER plan_id;

ALTER TABLE training_plan_item
    ADD COLUMN knowledge_card_id BIGINT NULL AFTER item_type;

ALTER TABLE training_plan_item
    ADD COLUMN knowledge_card_title VARCHAR(128) NULL AFTER problem_title;

ALTER TABLE training_plan_item
    ADD INDEX idx_training_plan_item_type (plan_id, item_type);
