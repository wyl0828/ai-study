# Phase 2 Agent Workflow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the real Phase 2 Agent Workflow for AI Interview Coach Agent: analyze a saved Java submission through Tool execution, Observation, AI classification, layered hints, weakness memory, Agent steps, persistence, and SSE.

**Architecture:** Use a simple state-machine Agent rather than a black-box autonomous agent. `InterviewCoachAgent` owns `AgentContext` and calls focused `Tool<I, O>` implementations; Spring services own persistence and business state. LLM calls are isolated in `integration/ai` and only used inside semantic tools such as `ErrorClassifierTool`, `HintGeneratorTool`, and `TrainingPlannerTool`.

**Tech Stack:** Spring Boot 3.5.12, Java 17, MyBatis-Plus, MySQL 8, Redis optional, Spring MVC SSE (`SseEmitter`), JUnit 5, Mockito, Anthropic-compatible Messages API.

---

## Scope

Phase 2 must make the project genuinely Agent-first. Do not implement frontend pages in this phase. Do not add RAG, MCP, multi-agent collaboration, Docker sandbox, or multi-language execution.

The demo path after this phase:

```text
POST /api/submissions
  -> creates/runs a Java submission

GET /api/submissions/{submissionId}/diagnosis/stream
  -> InterviewCoachAgent creates AgentRun
  -> PLANNING step
  -> CodeExecutionTool re-runs JudgeService and produces Observation
  -> ErrorClassifierTool calls LLM and persists AiDiagnosis
  -> HintGeneratorTool calls LLM and persists HintRecord rows
  -> WeaknessTrackerTool updates UserWeakness and MistakeCard
  -> TrainingPlannerTool creates a short TrainingPlan
  -> SSE emits each AgentStep plus final structured result
```

---

## File Structure

### Create

- `backend/src/main/java/com/interview/coach/config/AiProperties.java`
- `backend/src/main/java/com/interview/coach/config/SseConfig.java`
- `backend/src/main/java/com/interview/coach/controller/AgentController.java`
- `backend/src/main/java/com/interview/coach/dto/AgentAnalyzeRequest.java`
- `backend/src/main/java/com/interview/coach/dto/AgentExecutionObservation.java`
- `backend/src/main/java/com/interview/coach/dto/AiDiagnosisResult.java`
- `backend/src/main/java/com/interview/coach/dto/HintGenerationResult.java`
- `backend/src/main/java/com/interview/coach/dto/TrainingPlanResult.java`
- `backend/src/main/java/com/interview/coach/entity/AgentRun.java`
- `backend/src/main/java/com/interview/coach/entity/AgentStepEntity.java`
- `backend/src/main/java/com/interview/coach/entity/AiDiagnosis.java`
- `backend/src/main/java/com/interview/coach/entity/HintRecord.java`
- `backend/src/main/java/com/interview/coach/entity/UserWeakness.java`
- `backend/src/main/java/com/interview/coach/entity/MistakeCard.java`
- `backend/src/main/java/com/interview/coach/entity/TrainingPlan.java`
- `backend/src/main/java/com/interview/coach/entity/TrainingPlanItem.java`
- `backend/src/main/java/com/interview/coach/enums/AgentRunStatusEnum.java`
- `backend/src/main/java/com/interview/coach/enums/AgentStepStatusEnum.java`
- `backend/src/main/java/com/interview/coach/enums/AgentState.java`
- `backend/src/main/java/com/interview/coach/enums/ErrorTypeEnum.java`
- `backend/src/main/java/com/interview/coach/enums/HintLevelEnum.java`
- `backend/src/main/java/com/interview/coach/integration/ai/AnthropicCompatibleClient.java`
- `backend/src/main/java/com/interview/coach/integration/ai/AiClientException.java`
- `backend/src/main/java/com/interview/coach/agent/AgentContext.java`
- `backend/src/main/java/com/interview/coach/agent/AgentStep.java`
- `backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`
- `backend/src/main/java/com/interview/coach/agent/tool/Tool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/CodeExecutionTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/ErrorClassifierTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/HintGeneratorTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/WeaknessTrackerTool.java`
- `backend/src/main/java/com/interview/coach/agent/tool/TrainingPlannerTool.java`
- `backend/src/main/java/com/interview/coach/mapper/AgentRunMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/AgentStepMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/AiDiagnosisMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/HintRecordMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/UserWeaknessMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/MistakeCardMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/TrainingPlanMapper.java`
- `backend/src/main/java/com/interview/coach/mapper/TrainingPlanItemMapper.java`
- `backend/src/main/java/com/interview/coach/service/AgentService.java`
- `backend/src/main/java/com/interview/coach/service/LearningTracker.java`
- `backend/src/main/java/com/interview/coach/service/TrainingPlanService.java`
- `backend/src/main/java/com/interview/coach/service/impl/AgentServiceImpl.java`
- `backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java`
- `backend/src/main/java/com/interview/coach/service/impl/TrainingPlanServiceImpl.java`
- `backend/src/main/java/com/interview/coach/vo/AgentAnalyzeVO.java`
- `backend/src/main/java/com/interview/coach/vo/AgentStepVO.java`
- `backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java`
- `backend/src/test/java/com/interview/coach/agent/tool/CodeExecutionToolTest.java`
- `backend/src/test/java/com/interview/coach/integration/ai/AnthropicCompatibleClientTest.java`
- `backend/src/test/java/com/interview/coach/service/LearningTrackerImplTest.java`

### Modify

- `data/schema.sql`: add Phase 2 tables.
- `backend/src/main/resources/application.yml`: add `coach.ai.*` config defaults.
- `backend/src/main/java/com/interview/coach/CoachApplication.java`: enable `AiProperties`.
- `backend/src/main/java/com/interview/coach/service/SubmissionService.java`: add submission lookup and rejudge methods.
- `backend/src/main/java/com/interview/coach/service/impl/SubmissionServiceImpl.java`: implement lookup and rejudge.
- `backend/src/main/java/com/interview/coach/service/ProblemService.java`: add `listKnowledgePointNames(Long problemId)`.
- `backend/src/main/java/com/interview/coach/service/impl/ProblemServiceImpl.java`: expose existing private knowledge point lookup.

---

## Task 1: Add Phase 2 Persistence Schema

**Files:**
- Modify: `data/schema.sql`
- Create entity/mapper files listed in File Structure

- [ ] **Step 1: Extend `data/schema.sql` with Agent and learning tables**

Insert these drops before existing `DROP TABLE IF EXISTS submission;`:

```sql
DROP TABLE IF EXISTS training_plan_item;
DROP TABLE IF EXISTS training_plan;
DROP TABLE IF EXISTS mistake_card;
DROP TABLE IF EXISTS user_weakness;
DROP TABLE IF EXISTS hint_record;
DROP TABLE IF EXISTS ai_diagnosis;
DROP TABLE IF EXISTS agent_step;
DROP TABLE IF EXISTS agent_run;
```

Insert these tables after `submission`:

```sql
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
```

- [ ] **Step 2: Add enum files**

Create `backend/src/main/java/com/interview/coach/enums/AgentRunStatusEnum.java`:

```java
package com.interview.coach.enums;

public enum AgentRunStatusEnum {
    RUNNING,
    SUCCESS,
    FAILED
}
```

Create `backend/src/main/java/com/interview/coach/enums/AgentStepStatusEnum.java`:

```java
package com.interview.coach.enums;

public enum AgentStepStatusEnum {
    RUNNING,
    SUCCESS,
    FAILED
}
```

Create `backend/src/main/java/com/interview/coach/enums/AgentState.java`:

```java
package com.interview.coach.enums;

public enum AgentState {
    PLANNING,
    CODE_EXECUTION,
    OBSERVATION,
    ERROR_CLASSIFICATION,
    HINT_GENERATION,
    MEMORY_UPDATE,
    TRAINING_PLAN,
    COMPLETED,
    FAILED
}
```

Create `backend/src/main/java/com/interview/coach/enums/ErrorTypeEnum.java`:

```java
package com.interview.coach.enums;

public enum ErrorTypeEnum {
    SYNTAX_ERROR,
    LOGIC_ERROR,
    BOUNDARY_ERROR,
    ALGORITHM_ERROR,
    TIMEOUT,
    RUNTIME_ERROR,
    SYSTEM_ERROR,
    ACCEPTED_REVIEW
}
```

Create `backend/src/main/java/com/interview/coach/enums/HintLevelEnum.java`:

```java
package com.interview.coach.enums;

public enum HintLevelEnum {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3);

    private final int value;

    HintLevelEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

- [ ] **Step 3: Add entities and mappers**

Use the existing Lombok/MyBatis-Plus style from `Submission.java`. Each entity should use `@TableName`, `@TableId(type = IdType.AUTO)`, and Java field names matching snake_case columns.

Create mapper interfaces like:

```java
package com.interview.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.coach.entity.AgentRun;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRun> {
}
```

Repeat the same `BaseMapper<T>` pattern for all Phase 2 entities.

- [ ] **Step 4: Verify schema can be imported**

Run:

```powershell
mysql -uroot -p123456 < D:\code\ai-study\data\schema.sql
```

Expected: command exits with code `0` and creates all Phase 2 tables.

- [ ] **Step 5: Compile after adding entities**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

---

## Task 2: Add Submission Rejudge Support

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/service/SubmissionService.java`
- Modify: `backend/src/main/java/com/interview/coach/service/impl/SubmissionServiceImpl.java`
- Modify: `backend/src/main/java/com/interview/coach/service/ProblemService.java`
- Modify: `backend/src/main/java/com/interview/coach/service/impl/ProblemServiceImpl.java`

- [ ] **Step 1: Extend `SubmissionService`**

Change it to:

```java
package com.interview.coach.service;

import com.interview.coach.dto.JudgeResult;
import com.interview.coach.dto.SubmitCodeRequest;
import com.interview.coach.entity.Submission;
import com.interview.coach.vo.SubmissionResultVO;

public interface SubmissionService {

    SubmissionResultVO submit(SubmitCodeRequest request);

    Submission getSubmissionOrThrow(Long submissionId);

    JudgeResult rejudge(Long submissionId);
}
```

- [ ] **Step 2: Implement lookup and rejudge**

In `SubmissionServiceImpl`, add:

```java
@Override
public Submission getSubmissionOrThrow(Long submissionId) {
    Submission submission = submissionMapper.selectById(submissionId);
    if (submission == null) {
        throw new BusinessException(404, "submission not found");
    }
    return submission;
}

@Override
@Transactional
public JudgeResult rejudge(Long submissionId) {
    Submission submission = getSubmissionOrThrow(submissionId);
    if (!LanguageEnum.isJava(submission.getLanguage())) {
        JudgeResult result = new JudgeResult();
        result.setStatus(SubmissionStatusEnum.UNSUPPORTED_LANGUAGE);
        result.setErrorMessage("Phase 1 supports Java only");
        return result;
    }
    Problem problem = problemService.getEnabledProblem(submission.getProblemId());
    List<TestCase> testCases = listTestCases(problem.getId());
    if (testCases.isEmpty()) {
        throw new BusinessException(500, "problem has no test cases");
    }
    JudgeResult judgeResult = judgeService.judgeJava(submission.getCode(), toJudgeCases(testCases));
    updateSubmission(submission, judgeResult);
    return judgeResult;
}
```

- [ ] **Step 3: Simplify `submit` to reuse `rejudge` logic**

After `createRunningSubmission(request)`, keep:

```java
Submission submission = createRunningSubmission(request);
JudgeResult judgeResult = judgeService.judgeJava(request.getCode(), toJudgeCases(testCases));
updateSubmission(submission, judgeResult);
return toSubmissionResultVO(submission, judgeResult);
```

Do not change behavior in this task beyond adding the reusable public `rejudge` method.

- [ ] **Step 4: Expose problem knowledge point lookup**

Change `ProblemService`:

```java
List<String> listKnowledgePointNames(Long problemId);
```

In `ProblemServiceImpl`, change the existing private method:

```java
private List<String> listKnowledgePointNames(Long problemId) {
```

to:

```java
@Override
public List<String> listKnowledgePointNames(Long problemId) {
```

- [ ] **Step 5: Compile**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

---

## Task 3: Add Agent Core Types and Tool Contract

**Files:**
- Create: `backend/src/main/java/com/interview/coach/agent/AgentContext.java`
- Create: `backend/src/main/java/com/interview/coach/agent/AgentStep.java`
- Create: `backend/src/main/java/com/interview/coach/agent/tool/Tool.java`
- Create DTOs listed in File Structure

- [ ] **Step 1: Add `Tool<I, O>`**

```java
package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;

public interface Tool<I, O> {

    String name();

    O execute(I input, AgentContext context);
}
```

- [ ] **Step 2: Add `AgentStep`**

```java
package com.interview.coach.agent;

import com.interview.coach.enums.AgentStepStatusEnum;
import com.interview.coach.enums.AgentState;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AgentStep {

    private AgentState state;

    private String toolName;

    private AgentStepStatusEnum status;

    private String inputSummary;

    private String outputSummary;

    private Long durationMs;

    private String errorMessage;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
```

- [ ] **Step 3: Add `AgentContext`**

```java
package com.interview.coach.agent;

import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.AiDiagnosisResult;
import com.interview.coach.dto.HintGenerationResult;
import com.interview.coach.dto.TrainingPlanResult;
import com.interview.coach.entity.Problem;
import com.interview.coach.entity.Submission;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AgentContext {

    private Long agentRunId;

    private Long submissionId;

    private Long userId;

    private Long problemId;

    private Submission submission;

    private Problem problem;

    private List<String> knowledgePoints = new ArrayList<>();

    private AgentExecutionObservation observation;

    private AiDiagnosisResult diagnosis;

    private HintGenerationResult hints;

    private TrainingPlanResult trainingPlan;

    private List<AgentStep> steps = new ArrayList<>();
}
```

- [ ] **Step 4: Add core DTOs**

Create `AgentExecutionObservation`:

```java
package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class AgentExecutionObservation {

    private String status;

    private Integer passedCount;

    private Integer totalCount;

    private Integer runtime;

    private Integer memory;

    private String errorMessage;

    private List<FailedCaseResult> failedCases = new ArrayList<>();
}
```

Create `AiDiagnosisResult`:

```java
package com.interview.coach.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AiDiagnosisResult {

    private String errorType;

    private String knowledgePoint;

    private String specificError;

    private String diagnosis;

    private String suggestion;

    private BigDecimal confidence;

    private BigDecimal weaknessScoreDelta;
}
```

Create `HintGenerationResult`:

```java
package com.interview.coach.dto;

import lombok.Data;

@Data
public class HintGenerationResult {

    private String hintLevel1;

    private String hintLevel2;

    private String hintLevel3;
}
```

Create `TrainingPlanResult`:

```java
package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TrainingPlanResult {

    private String title;

    private String summary;

    private List<TrainingPlanItemResult> items = new ArrayList<>();

    @Data
    public static class TrainingPlanItemResult {

        private Integer dayIndex;

        private String knowledgePoint;

        private String problemTitle;

        private String reason;

        private String reviewFocus;
    }
}
```

- [ ] **Step 5: Compile**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

---

## Task 4: Add Anthropic-Compatible AI Client

**Files:**
- Create: `backend/src/main/java/com/interview/coach/config/AiProperties.java`
- Modify: `backend/src/main/resources/application.yml`
- Modify: `backend/src/main/java/com/interview/coach/CoachApplication.java`
- Create: `backend/src/main/java/com/interview/coach/integration/ai/AnthropicCompatibleClient.java`
- Create: `backend/src/main/java/com/interview/coach/integration/ai/AiClientException.java`
- Test: `backend/src/test/java/com/interview/coach/integration/ai/AnthropicCompatibleClientTest.java`

- [ ] **Step 1: Add properties**

Create `AiProperties`:

```java
package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "coach.ai")
public class AiProperties {

    private String baseUrl = "https://api.anthropic.com";

    private String apiKey;

    private String model = "claude-3-5-sonnet-latest";

    private Integer maxTokens = 1200;

    private String anthropicVersion = "2023-06-01";
}
```

In `CoachApplication`, add:

```java
@EnableConfigurationProperties({PistonProperties.class, AiProperties.class})
```

If `@EnableConfigurationProperties` already exists for `PistonProperties`, add `AiProperties.class` to the existing annotation.

- [ ] **Step 2: Add YAML defaults**

In `application.yml`, add:

```yaml
coach:
  ai:
    base-url: ${AI_BASE_URL:https://api.anthropic.com}
    api-key: ${AI_API_KEY:}
    model: ${AI_MODEL:claude-3-5-sonnet-latest}
    max-tokens: ${AI_MAX_TOKENS:1200}
    anthropic-version: ${AI_ANTHROPIC_VERSION:2023-06-01}
```

- [ ] **Step 3: Implement exception**

```java
package com.interview.coach.integration.ai;

public class AiClientException extends RuntimeException {

    public AiClientException(String message) {
        super(message);
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

- [ ] **Step 4: Implement AI client**

Use Spring `RestClient` and Jackson `ObjectMapper`. The public method should be:

```java
public <T> T askJson(String systemPrompt, String userPrompt, Class<T> responseType)
```

It must:

- fail fast if `apiKey` is blank;
- send a Messages API request to `/v1/messages`;
- read the first text content block;
- extract JSON between the first `{` and last `}`;
- parse into `responseType`.

- [ ] **Step 5: Add unit test for JSON extraction**

Test `extractJson` behavior with:

```text
Here is the result:
{"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap"}
```

Expected JSON:

```json
{"errorType":"BOUNDARY_ERROR","knowledgePoint":"HashMap"}
```

- [ ] **Step 6: Run AI client tests**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -Dtest=AnthropicCompatibleClientTest test
```

Expected: test passes without making a network request.

---

## Task 5: Implement CodeExecutionTool

**Files:**
- Create: `backend/src/main/java/com/interview/coach/agent/tool/CodeExecutionTool.java`
- Test: `backend/src/test/java/com/interview/coach/agent/tool/CodeExecutionToolTest.java`

- [ ] **Step 1: Write failing unit test**

Test behavior:

- mock `SubmissionService.rejudge(11L)` to return `JudgeResult` with status `WRONG_ANSWER`;
- call `CodeExecutionTool.execute(11L, context)`;
- assert observation status, counts, and failed cases are copied.

- [ ] **Step 2: Implement tool**

```java
package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.AgentExecutionObservation;
import com.interview.coach.dto.JudgeResult;
import com.interview.coach.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CodeExecutionTool implements Tool<Long, AgentExecutionObservation> {

    private final SubmissionService submissionService;

    @Override
    public String name() {
        return "CodeExecutionTool";
    }

    @Override
    public AgentExecutionObservation execute(Long submissionId, AgentContext context) {
        JudgeResult result = submissionService.rejudge(submissionId);
        AgentExecutionObservation observation = new AgentExecutionObservation();
        observation.setStatus(result.getStatus().name());
        observation.setPassedCount(result.getPassedCount());
        observation.setTotalCount(result.getTotalCount());
        observation.setRuntime(result.getRuntime());
        observation.setMemory(result.getMemory());
        observation.setErrorMessage(result.getErrorMessage());
        observation.setFailedCases(result.getFailedCases());
        context.setObservation(observation);
        return observation;
    }
}
```

- [ ] **Step 3: Run tool test**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -Dtest=CodeExecutionToolTest test
```

Expected: test passes.

---

## Task 6: Implement AI Semantic Tools

**Files:**
- Create: `ErrorClassifierTool.java`
- Create: `HintGeneratorTool.java`
- Create: `TrainingPlannerTool.java`
- Create/modify: `TrainingPlanService.java`, `TrainingPlanServiceImpl.java`

- [ ] **Step 1: Implement `ErrorClassifierTool`**

Input: `AgentContext`.

Output: `AiDiagnosisResult`.

Rules:

- build prompt from problem title, category, knowledge points, code, observation status, failed cases, and error message;
- call `AnthropicCompatibleClient.askJson(systemPrompt, userPrompt, AiDiagnosisResult.class)`;
- default `confidence` to `0.50` if the model omits it;
- store result on `context.setDiagnosis(result)`.

- [ ] **Step 2: Implement `HintGeneratorTool`**

Input: `AgentContext`.

Output: `HintGenerationResult`.

Rules:

- prompt must explicitly forbid full Java accepted solution;
- Level 1 direction only;
- Level 2 knowledge point and likely issue;
- Level 3 pseudocode or key idea only;
- store result on `context.setHints(result)`.

- [ ] **Step 3: Implement `TrainingPlannerTool`**

Input: `AgentContext`.

Output: `TrainingPlanResult`.

Rules:

- for MVP, generate a deterministic fallback plan if AI is unavailable:

```text
Day 1: repeat the failed knowledge point
Day 2: practice one adjacent topic from the problem category
Day 3: review mistake card and retry the original problem
```

- call `TrainingPlanService.savePlan(context, result)` after generating result;
- store result on `context.setTrainingPlan(result)`.

- [ ] **Step 4: Compile**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -DskipTests compile
```

Expected: `BUILD SUCCESS`.

---

## Task 7: Implement LearningTrackerTool and Learning Persistence

**Files:**
- Create: `backend/src/main/java/com/interview/coach/agent/tool/WeaknessTrackerTool.java`
- Create: `backend/src/main/java/com/interview/coach/service/LearningTracker.java`
- Create: `backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java`
- Test: `backend/src/test/java/com/interview/coach/service/LearningTrackerImplTest.java`

- [ ] **Step 1: Define service**

```java
package com.interview.coach.service;

import com.interview.coach.agent.AgentContext;

public interface LearningTracker {

    void recordDiagnosis(AgentContext context);
}
```

- [ ] **Step 2: Implement weakness update rule**

In `LearningTrackerImpl.recordDiagnosis`:

- insert `AiDiagnosis`;
- insert three `HintRecord` rows;
- upsert `UserWeakness` by `(user_id, knowledge_point, error_type)`;
- increment `wrong_count` by `1` unless diagnosis error type is `ACCEPTED_REVIEW`;
- increment `submit_count` by `1`;
- increase `weakness_score` by `weaknessScoreDelta` or `5`;
- cap `weakness_score` at `100`;
- insert one `MistakeCard`.

- [ ] **Step 3: Implement `WeaknessTrackerTool`**

```java
package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.service.LearningTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeaknessTrackerTool implements Tool<AgentContext, AgentContext> {

    private final LearningTracker learningTracker;

    @Override
    public String name() {
        return "WeaknessTrackerTool";
    }

    @Override
    public AgentContext execute(AgentContext input, AgentContext context) {
        learningTracker.recordDiagnosis(context);
        return context;
    }
}
```

- [ ] **Step 4: Test weakness score cap**

Test that an existing `UserWeakness` with score `98` and delta `8` updates to `100`, not `106`.

- [ ] **Step 5: Run service test**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -Dtest=LearningTrackerImplTest test
```

Expected: test passes.

---

## Task 8: Implement InterviewCoachAgent and AgentService

**Files:**
- Create: `backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`
- Create: `backend/src/main/java/com/interview/coach/service/AgentService.java`
- Create: `backend/src/main/java/com/interview/coach/service/impl/AgentServiceImpl.java`
- Create VO files
- Test: `backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java`

- [ ] **Step 1: Define `AgentService`**

```java
package com.interview.coach.service;

import com.interview.coach.vo.AgentAnalyzeVO;
import java.util.function.Consumer;

public interface AgentService {

    AgentAnalyzeVO analyze(Long submissionId);

    AgentAnalyzeVO analyze(Long submissionId, Consumer<String> eventSink);
}
```

- [ ] **Step 2: Implement `InterviewCoachAgent.run`**

Method signature:

```java
public AgentContext run(Long submissionId, Consumer<AgentStep> stepSink)
```

Execution order:

```text
PLANNING
CODE_EXECUTION
OBSERVATION
ERROR_CLASSIFICATION
HINT_GENERATION
MEMORY_UPDATE
TRAINING_PLAN
COMPLETED
```

Every step must:

- create an `AgentStep` with `RUNNING`;
- persist `AgentStepEntity`;
- call `stepSink.accept(step)`;
- execute the tool;
- mark `SUCCESS` with duration;
- persist the final step state;
- call `stepSink.accept(step)` again.

On exception:

- create or update a `FAILED` step;
- mark `AgentRun` as `FAILED`;
- set context final state to `FAILED`;
- rethrow a `BusinessException(500, "agent analysis failed")`.

- [ ] **Step 3: Implement `AgentServiceImpl`**

Responsibilities:

- create `AgentRun`;
- load `Submission`, `Problem`, and knowledge points into `AgentContext`;
- call `InterviewCoachAgent.run`;
- map final context to `AgentAnalyzeVO`.

- [ ] **Step 4: Add VO classes**

`AgentStepVO` fields:

```java
private String stepName;
private String toolName;
private String status;
private String inputSummary;
private String outputSummary;
private Long durationMs;
private String errorMessage;
```

`AgentAnalyzeVO` fields:

```java
private Long agentRunId;
private Long submissionId;
private String errorType;
private String knowledgePoint;
private String specificError;
private String diagnosis;
private String hintLevel1;
private String hintLevel2;
private String hintLevel3;
private String trainingPlanTitle;
private List<AgentStepVO> steps;
```

- [ ] **Step 5: Test orchestration order**

Mock all tools. Assert states are emitted in exact order:

```text
PLANNING
CODE_EXECUTION
OBSERVATION
ERROR_CLASSIFICATION
HINT_GENERATION
MEMORY_UPDATE
TRAINING_PLAN
COMPLETED
```

- [ ] **Step 6: Run agent test**

Run:

```powershell
cd D:\code\ai-study\backend
mvn -q -Dtest=InterviewCoachAgentTest test
```

Expected: test passes.

---

## Task 9: Add Agent REST and SSE Controller

**Files:**
- Create: `backend/src/main/java/com/interview/coach/controller/AgentController.java`
- Create: `backend/src/main/java/com/interview/coach/dto/AgentAnalyzeRequest.java`
- Create: `backend/src/main/java/com/interview/coach/config/SseConfig.java`

- [ ] **Step 1: Add request DTO**

```java
package com.interview.coach.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgentAnalyzeRequest {

    @NotNull(message = "submissionId is required")
    private Long submissionId;
}
```

- [ ] **Step 2: Add controller endpoints**

Endpoints:

```http
POST /api/agent/analyze
GET /api/submissions/{submissionId}/diagnosis/stream
```

`POST /api/agent/analyze` returns `ApiResponse<AgentAnalyzeVO>`.

SSE endpoint returns `SseEmitter`.

- [ ] **Step 3: Implement SSE**

SSE event names:

```text
agent_step
done
error
```

Each step event sends `AgentStepVO` as JSON. The done event sends final `AgentAnalyzeVO`.

Use `CompletableFuture.runAsync` so the request thread returns the emitter immediately.

- [ ] **Step 4: Manual verification**

Run backend and call:

```powershell
curl.exe -N http://localhost:8080/api/submissions/11/diagnosis/stream
```

Expected events:

```text
event: agent_step
event: agent_step
event: done
```

If `AI_API_KEY` is missing, expected result is a structured error event and a failed AgentRun, not a server crash.

---

## Task 10: End-to-End Verification

**Files:**
- Modify docs only if command or env var names differ during implementation.

- [ ] **Step 1: Run full backend tests**

Run:

```powershell
cd D:\code\ai-study\backend
mvn test
```

Expected: all tests pass.

- [ ] **Step 2: Re-import schema**

Run:

```powershell
mysql -uroot -p123456 < D:\code\ai-study\data\schema.sql
mysql -uroot -p123456 < D:\code\ai-study\data\problems.sql
```

Expected: both commands exit with code `0`.

- [ ] **Step 3: Start dependencies**

Confirm:

```powershell
curl.exe http://localhost:2000/api/v2/runtimes
```

Expected: response contains Java runtime.

- [ ] **Step 4: Start backend**

Run:

```powershell
cd D:\code\ai-study\backend
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="123456"
$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:PISTON_BASE_URL="http://localhost:2000/api/v2"
$env:AI_API_KEY="<your-api-key>"
mvn spring-boot:run
```

Expected: backend starts on port `8080`.

- [ ] **Step 5: Submit buggy Two Sum code**

Use `POST http://localhost:8080/api/submissions` with a wrong Java solution that fails duplicate-element cases.

Expected: response status is `WRONG_ANSWER` and includes failed cases.

- [ ] **Step 6: Stream Agent diagnosis**

Run:

```powershell
curl.exe -N http://localhost:8080/api/submissions/{submissionId}/diagnosis/stream
```

Expected:

- `CODE_EXECUTION` step appears;
- `OBSERVATION` step includes failed count;
- `ERROR_CLASSIFICATION` step appears;
- final JSON includes `errorType`, `knowledgePoint`, `hintLevel1`, `hintLevel2`, `hintLevel3`;
- hints do not contain a full Java accepted solution.

- [ ] **Step 7: Verify database side effects**

Run SQL:

```sql
SELECT * FROM agent_run ORDER BY id DESC LIMIT 1;
SELECT step_name, status, duration_ms FROM agent_step ORDER BY id DESC LIMIT 10;
SELECT error_type, knowledge_point, confidence FROM ai_diagnosis ORDER BY id DESC LIMIT 1;
SELECT hint_level, hint_content FROM hint_record ORDER BY id DESC LIMIT 3;
SELECT knowledge_point, error_type, wrong_count, weakness_score FROM user_weakness ORDER BY id DESC LIMIT 1;
SELECT mistake_summary FROM mistake_card ORDER BY id DESC LIMIT 1;
SELECT title FROM training_plan ORDER BY id DESC LIMIT 1;
```

Expected: each query returns at least one row related to the latest submission.

---

## Self-Review

Spec coverage:

- Agent Workflow: covered by Tasks 3, 5, 6, 8.
- Tool Calling: covered by Tasks 3, 5, 6, 7.
- Observation: covered by Tasks 2 and 5.
- Memory: covered by Task 7.
- SSE steps: covered by Task 9.
- Persistence: covered by Tasks 1, 7, 8, 10.
- Verification: covered by Task 10.

Placeholder scan:

- The plan avoids implementation placeholders for Phase 2 behavior. The only angle-bracket value is `<your-api-key>` in a manual command where the real secret must come from the developer environment.

Type consistency:

- `AgentState`, `AgentContext`, `AgentStep`, `Tool<I, O>`, `CodeExecutionTool`, `ErrorClassifierTool`, `HintGeneratorTool`, `WeaknessTrackerTool`, and `TrainingPlannerTool` names are consistent across tasks and match the updated docs.
