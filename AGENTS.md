# AGENTS.md

## Project Identity

This repository is for **AI Interview Coach Agent**, a Java code diagnosis and interview-training system built around an Agent Workflow.

The project must not drift into a generic LeetCode clone, a generic AI chatbot, a broad education platform, or a Spring Boot wrapper around an LLM API. Its core value is the Agent-driven interview-training loop:

```text
agent task -> planner -> tool call -> observation -> error diagnosis -> layered hints -> weakness memory -> training plan
```

Current product distinction:

- Problem-level layered hints are preset problem content shown on the left problem panel.
- AI diagnosis is generated only after a failed submission and explains this user's current error on the right result panel.
- Backend Agent hint fields and `hint_record` persistence remain part of the Agent workflow, but the frontend no longer shows a separate right-side "layered hints" tab.

Primary audience:

- Java backend job seekers
- Student developers preparing for backend interviews
- Interviewers reviewing this as a resume project

Primary resume focus:

- Spring Boot backend design
- Code execution service integration
- Agent Workflow, Tool Calling, Observation, and Memory design
- MySQL data modeling
- MyBatis-Plus mapper and SQL-layer design
- Redis caching and temporary state
- SSE streaming
- Agent step / trace recording
- Clear demo flow

## Source Documents

Use these documents as the source of truth:

- `docs/AI-Interview-Coach.md`: project design, database tables, API design, Agent workflow, resume packaging, interview talking points
- `docs/IMPLEMENTATION_PLAN.md`: implementation phases, directory structure, acceptance criteria, prompts, risks, demo script
- `docs/API.md`: current implemented REST/SSE API surface
- `docs/PROJECT_STATUS.md`: current accomplishments, progress assessment, risks, and next-step outline

If this file conflicts with those documents, prefer this file for engineering constraints and MVP discipline, then update the docs only when the user explicitly asks.

## Current Implementation Status

As of Phase 4 plus the Solution-mode pilot and prompt/diagnosis UI cleanup, the project has a demoable end-to-end Agent workflow, real Dashboard data, a mixed ACM/Solution Java submission model, and clearer frontend separation between preset hints and AI diagnosis:

```text
POST /api/submissions
  -> persist original Java submission
  -> wrap Solution-mode problems 102/103/104 into Main.java for Piston
  -> judge Java submission through Piston

POST /api/agent/analyze
  -> run the Agent workflow synchronously for the current frontend demo flow
  -> frontend displays error type, knowledge point, diagnosis, improvement suggestion, training plan title, and trace steps
  -> frontend does not show hintLevel1/2/3 as a separate right-side tab

GET /api/submissions/{submissionId}/diagnosis/stream
  -> create AgentRun
  -> emit AgentStep events through SSE
  -> rejudge submission through CodeExecutionTool
  -> classify error through AI
  -> persist diagnosis, weakness memory, and mistake card
  -> create deterministic 3-day training plan (optional, failure does not block)
  -> emit final AgentAnalyzeVO

Frontend SSE integration:
  -> fetch + ReadableStream (not EventSource, for future Authorization header)
  -> real-time step display during analysis
  -> done event shows final diagnosis result
  -> onEnd fallback ensures loading state is cleared
  -> streamId protection prevents old requests from overwriting new results
```

Current implemented controllers:

- `ProblemController`
- `SubmissionController`
- `AgentController`
- `UserController`

Not yet exposed as REST controllers:

- single-hint lookup
- accepted-code review
- manual training plan regeneration

## Fixed Technical Stack

Do not re-open these decisions unless the user explicitly asks.

Frontend:

- Next.js 14
- Tailwind CSS
- Monaco Editor
- App Router

Backend:

- Spring Boot 3
- Java 17
- MySQL 8
- MyBatis-Plus
- Redis
- Server-Sent Events

AI:

- Anthropic-compatible API
- Structured JSON outputs whenever AI results are persisted or consumed by business logic
- AI is used for error classification, Agent hint data, code review, and training plan generation
- Current frontend treats problem-level layered hints as preset content, not as an on-click AI generation flow

Code execution:

- Piston API for MVP
- Java only for v1
- Keep a replaceable execution abstraction so Docker sandbox support can be added later

Default project structure:

```text
frontend/
backend/
data/
docs/
```

Backend package structure:

```text
backend/src/main/java/com/interview/coach/
├── controller/
├── service/
│   └── impl/
├── agent/
│   └── tool/
├── integration/
│   ├── piston/
│   └── ai/
├── entity/
├── mapper/
├── dto/
├── vo/
├── enums/
├── config/
├── handler/
└── CoachApplication.java
```

## MVP Priority

Always optimize for a demoable end-to-end loop before adding breadth.

Priority order:

1. Complete demo loop: submit Java code, run tests, diagnose error, show preset hints, record weakness, generate plan.
2. AI diagnosis and three-level hint quality.
3. Weakness tracking and training plan.
4. Problem count, dashboard polish, charts, UI animations.

The MVP should start with 5-10 problems. Do not spend early development time filling a 30-problem database if the training loop is not stable.

## Backend Rules

Spring Boot is the resume center of this project. Backend decisions should be easy to explain in an interview.

Use clear service boundaries:

- `ProblemService` for problem lookup and metadata
- `SubmissionService` for submission lifecycle
- `JudgeService` for code execution orchestration
- `AgentService` for AI workflow orchestration
- `LearningTracker` for weakness and mistake tracking
- `TrainingPlanService` for training plan generation

Use Java-backend-style package responsibilities:

- `controller`: receive HTTP requests and return VO responses
- `service`: business interfaces
- `service/impl`: business implementations and orchestration
- `agent`: Agent orchestration classes such as `InterviewCoachAgent`, `AgentState`, `AgentContext`, and `AgentStep`
- `agent/tool`: Tool implementations such as `CodeExecutionTool`, `ErrorClassifierTool`, `WeaknessTrackerTool`, and `TrainingPlannerTool`
- `integration/piston`: Piston API client and request/response adapters
- `integration/ai`: Anthropic-compatible API client and model adapters
- `entity`: MySQL table mapping objects
- `mapper`: MyBatis-Plus mapper interfaces
- `dto`: request DTOs and internal command objects
- `vo`: response view objects
- `enums`: status, difficulty, language, error type, and hint level enums
- `config`: framework, API, Redis, CORS, MyBatis-Plus, and SSE configuration
- `handler`: global exception handling and unified API response handling

Code execution must go through an abstraction. Do not call Piston directly from controllers or Agent tools.

Controllers should stay thin:

- validate request shape
- call service layer
- receive DTOs
- return VOs

Services should own business decisions:

- status transitions
- test result parsing
- Agent state transitions
- Tool execution order
- weakness updates
- training plan generation
- AI result persistence

Persistence rules:

- Use MyBatis-Plus as the default MySQL access layer.
- Put database access interfaces under a `mapper` package.
- Put table mapping classes under an `entity` package, not `model`.
- Prefer `BaseMapper<T>` for simple CRUD.
- Use custom SQL only when query conditions or joins become clearer than wrapper code.
- Keep SQL and persistence logic out of controllers.

Use DTOs for API requests and VOs for API responses. Do not expose entity objects directly from controllers.

## Frontend Rules

Keep the frontend practical and simple. The user has limited frontend experience, so avoid unnecessary UI complexity.

Required pages:

- `/`: problem list
- `/problem/[id]`: problem detail, preset layered hints, code editor, test result, AI diagnosis
- `/dashboard`: weakness list, recent submissions, training plan, mistake cards

Frontend priorities:

1. The problem page must work first.
2. Dashboard can start as simple lists.
3. Charts are optional.
4. UI animation is not a priority.

Use Monaco Editor only where code editing is needed. Do not overbuild a complete IDE.

Problem draft and template rules:

- For v1, keep draft persistence in `frontend/lib/draft.ts` backed by localStorage.
- Store only temporary browser-side state: code, last submission result, last AI diagnosis, and code snapshots.
- Do not put durable training data in localStorage; submissions, diagnoses, hints, weaknesses, mistake cards, and training plans remain MySQL-backed.
- `ProblemWorkspace.tsx`, `CodeEditor.tsx`, and result panels must not call localStorage directly except through `frontend/lib/draft.ts`.
- `/problem/[id]` should load code templates through browser-side `GET /api/problems/{id}/template`; do not reintroduce hard-coded `DEFAULT_CODE` templates in page components.
- The reset button should clear the current problem draft and re-read the backend template.

Problem hint and diagnosis UI rules:

- Problem-level layered hints are served from the backend `problem` table via `GET /api/problems/{id}` (`presetHints` field), with fallback to `frontend/lib/problemHints.ts`.
- The left problem panel should show Level 1 / Level 2 / Level 3 preset hints; all levels default collapsed and expand on click.
- The right result panel should only expose "测试结果" and "AI 诊断" tabs.
- Do not reintroduce a right-side "分层提示" tab unless the product direction explicitly changes.
- Do not call AI when the user expands preset problem hints.
- AI diagnosis should focus on current submission error type, knowledge point, diagnosis, improvement suggestion, training plan, and Agent trace steps.

## AI Agent Rules

The Agent must behave like an interview coach, not an answer generator.

The Agent must be implemented as an explainable workflow, not as a single prompt call. Prefer a simple state machine plus Tool chain for MVP:

```text
Planner -> CodeExecutionTool -> Observation -> ErrorClassifierTool -> WeaknessTrackerTool -> TrainingPlannerTool
```

`WeaknessTrackerTool` and `TrainingPlannerTool` are optional steps — their failure does not block the final diagnosis result.

Agent concepts:

- `AgentContext` carries submission, problem, execution result, diagnosis, hints, weakness update, and training plan data.
- `AgentStep` records each step name, tool name, status, input summary, output summary, duration, and error message.
- `Tool` implementations must have clear inputs and outputs and should call service-layer abstractions instead of controllers.
- `CodeExecutionTool` must call a service-layer execution abstraction such as `SubmissionService.rejudge(...)`, which in turn uses `JudgeService`; it must not call Piston directly.
- LLM calls belong only in tools that need semantic judgment, such as error classification, hint generation, code review, or training plan generation.
- Tool outputs that affect business state must be converted into structured data before persistence.

AI must not directly provide full accepted Java solutions by default.

Use layered hints:

- Level 1: direction only
- Level 2: related knowledge point and likely issue
- Level 3: pseudocode or key idea, not full Java answer
- For the current MVP UI, problem-level hints are preset and shown on the left; AI-generated hint fields are still persisted for Agent workflow completeness and future expansion.

Persist AI outputs only after converting them to structured data. Prefer JSON fields such as:

- `errorType`
- `knowledgePoint`
- `specificError`
- `diagnosis`
- `hintLevel1`
- `hintLevel2`
- `hintLevel3`
- `confidence`
- `weaknessScoreDelta`

AI output should support the learning loop:

```text
failed submission -> tool observation -> diagnosis -> hints -> weakness update -> mistake card -> training plan
```

Frontend display should present this as:

```text
problem preset hints -> failed submission -> test result -> AI diagnosis -> weakness memory -> training plan
```

## Code Execution Rules

Use Piston API for the MVP.

The first version supports Java only. Do not add Python, JavaScript, C++, or multi-language execution unless the user explicitly changes scope.

Current Java submission modes:

- `problemId=101/105/106/107/108` use ACM mode: user submits complete `public class Main`.
- `problemId=102/103/104` use Solution mode: user submits non-public `class Solution`.
- `SubmissionServiceImpl` must save the original user code to `submission.code`.
- Only code sent to `JudgeService/Piston` should be wrapped by `CodeWrapper`.
- Do not add a `code_mode` field or REST parameter unless the user explicitly asks for a broader schema migration.

Keep execution replaceable:

- define a service-level execution interface
- implement a Piston-backed version first
- leave Docker sandbox as a later extension

Execution results should include enough information for AI diagnosis:

- status
- passed count
- total count
- compile error
- runtime error
- failed cases
- expected output
- actual output
- runtime
- memory

## Data And Persistence Rules

MySQL stores durable training data:

- users
- problems
- knowledge points
- test cases
- submissions
- agent runs
- agent steps
- AI diagnoses
- hint records
- user weaknesses
- training plans
- training plan items
- mistake cards

Redis may be used for:

- hot problem list cache
- problem detail cache
- temporary Agent context
- user recent training state

Do not rely on Redis as the only storage for anything needed in the resume demo.

## SSE Rules

Use SSE for AI diagnosis streaming.

SSE is for server-to-browser updates such as:

- "planning agent steps"
- "running code execution tool"
- "observing failed test cases"
- "analyzing test result"
- "classifying error type"
- "updating weakness memory"
- "generating training plan"
- final structured diagnosis summary

Current SSE event names:

- `agent_step`: `AgentStepVO`
- `done`: `AgentAnalyzeVO`
- `error`: `ApiResponse<Void>`

Do not use WebSocket for v1 unless the user explicitly asks. SSE is simpler and easier to explain.

## Testing And Verification

Before claiming a feature is complete, verify it with the smallest meaningful test or manual flow.

Backend verification:

- problem list endpoint works
- problem detail endpoint works
- Java submission can be executed through Piston
- compile error is returned correctly
- wrong answer includes failed case information
- submission is persisted

AI verification:

- a known Two Sum bug is classified as a hash map or boundary issue
- a known recursion bug is classified as a tree or recursion issue
- AI returns valid JSON for persisted outputs
- hints do not reveal the full Java answer

Frontend verification:

- problem page loads
- editor accepts code
- submit button calls backend
- test result is displayed
- preset layered hints are visible on the left problem panel and all levels are collapsed by default
- AI diagnosis is displayed after a failed submission through the synchronous analyze endpoint
- the right result panel has no separate layered hints tab
- backend SSE diagnosis stream remains available for API-level demonstration
- preset layered hints can be expanded manually without calling AI
- draft code, last result, and last diagnosis can be restored after refresh
- stale diagnosis warning appears after editing code that differs from the diagnosis snapshot

Demo verification:

```text
select problem -> inspect preset hints -> write buggy code -> submit -> run CodeExecutionTool -> observe failed test -> diagnose error -> update weakness memory -> generate plan
```

## Do Not Do These In MVP

Do not add:

- multi-language judge support
- full LeetCode-scale problem bank
- Docker sandbox implementation
- voice or video interview simulation
- multi-agent collaboration
- enterprise-grade permission system
- complex charting as a blocker
- decorative UI animation as a blocker
- full accepted answer generation as the default AI behavior

## Development Style

Prefer simple, explainable implementations over clever abstractions.

Add abstractions only when they protect a planned extension:

- Piston now, Docker later
- Anthropic-compatible API now, model provider swap later
- list dashboard now, charts later

Keep names aligned with the project docs. If a service or concept appears in `docs/IMPLEMENTATION_PLAN.md`, reuse that naming unless there is a strong reason not to.

When in doubt, protect the demo loop.

## Next-Step Focus

Near-term work should follow `docs/PROJECT_STATUS.md`:

1. Stabilize three demo problems and known bug samples. ✓
2. Write root README and demo startup notes. ✓
3. Capture screenshots for problem page, AI diagnosis, and Dashboard.
4. Frontend SSE step display. ✓
5. Move preset problem hints from frontend static mapping to backend data. ✓
6. Prepare resume descriptions and interview Q&A.
