ALTER TABLE training_plan_item
    ADD COLUMN status_updated_at DATETIME NULL AFTER status;

CREATE INDEX idx_training_plan_item_status_updated
    ON training_plan_item (plan_id, status, status_updated_at);
