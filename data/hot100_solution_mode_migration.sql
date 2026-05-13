USE ai_interview_coach;

DROP PROCEDURE IF EXISTS ensure_problem_code_mode;

DELIMITER //
CREATE PROCEDURE ensure_problem_code_mode()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'problem'
          AND COLUMN_NAME = 'code_mode'
    ) THEN
        ALTER TABLE problem
        ADD COLUMN code_mode VARCHAR(32) NOT NULL DEFAULT 'solution' AFTER output_format;
    END IF;
END//
DELIMITER ;

CALL ensure_problem_code_mode();
DROP PROCEDURE IF EXISTS ensure_problem_code_mode;

UPDATE problem
SET code_mode = 'solution'
WHERE code_mode IS NULL OR code_mode <> 'solution';
