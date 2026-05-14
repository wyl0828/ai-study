USE ai_interview_coach;

-- Reset demo learning history without touching problems, test cases, or knowledge cards.
-- Use this before recording or replaying the resume demo so old submissions do not pollute
-- weakness ranking, mistake cards, recent submissions, or training plans.
DELETE FROM training_plan_item;
DELETE FROM training_plan;
DELETE FROM mistake_card;
DELETE FROM self_test_record;
DELETE FROM user_knowledge_card_mastery;
DELETE FROM user_weakness_event;
DELETE FROM user_weakness;
DELETE FROM hint_record;
DELETE FROM ai_diagnosis;
DELETE FROM agent_step;
DELETE FROM agent_run;
DELETE FROM submission;

ALTER TABLE training_plan_item AUTO_INCREMENT = 1;
ALTER TABLE training_plan AUTO_INCREMENT = 1;
ALTER TABLE mistake_card AUTO_INCREMENT = 1;
ALTER TABLE self_test_record AUTO_INCREMENT = 1;
ALTER TABLE user_knowledge_card_mastery AUTO_INCREMENT = 1;
ALTER TABLE user_weakness_event AUTO_INCREMENT = 1;
ALTER TABLE user_weakness AUTO_INCREMENT = 1;
ALTER TABLE hint_record AUTO_INCREMENT = 1;
ALTER TABLE ai_diagnosis AUTO_INCREMENT = 1;
ALTER TABLE agent_step AUTO_INCREMENT = 1;
ALTER TABLE agent_run AUTO_INCREMENT = 1;
ALTER TABLE submission AUTO_INCREMENT = 1;
