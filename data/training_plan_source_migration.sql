ALTER TABLE training_plan_item
    ADD COLUMN source_type VARCHAR(64) NULL AFTER review_focus,
    ADD COLUMN source_id BIGINT NULL AFTER source_type,
    ADD COLUMN source_summary TEXT NULL AFTER source_id,
    ADD INDEX idx_training_plan_item_source (source_type, source_id);
