USE ai_interview_coach;

SET @password_hash_column_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user'
      AND COLUMN_NAME = 'password_hash'
);

SET @add_password_hash_sql := IF(
    @password_hash_column_exists = 0,
    'ALTER TABLE `user` ADD COLUMN password_hash VARCHAR(255) NULL AFTER username',
    'SELECT 1'
);
PREPARE add_password_hash_stmt FROM @add_password_hash_sql;
EXECUTE add_password_hash_stmt;
DEALLOCATE PREPARE add_password_hash_stmt;

UPDATE `user`
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    updated_at = NOW()
WHERE password_hash IS NULL OR password_hash = '' OR password_hash = 'demo-only-not-for-production';

ALTER TABLE `user` MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
