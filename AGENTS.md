# AGENTS.md

## Project Identity

This repository is for **AI Interview Coach Agent**, a Java code diagnosis and interview-training system built around an Agent Workflow.

The project must not drift into a generic LeetCode clone, a generic AI chatbot, a broad education platform, or a Spring Boot wrapper around an LLM API. Its core value is the Agent-driven interview-training loop:

```text
agent task -> planner -> tool call -> observation -> error diagnosis / code review -> weakness memory -> training plan
```

Current product distinction:

- Problem-level layered hints are preset problem content shown on the left problem panel.
- RAG V1 is an internal Agent Tool after code execution observation; it retrieves problem knowledge, backend knowledge cards, and this user's historical learning memory from MySQL before AI diagnosis or AC code review.
- AI diagnosis is generated only after a failed submission and explains this user's current error on the right result panel.
- Accepted submissions may receive lightweight code review through the Agent flow; this is not a full accepted answer generator.
- `hint_record` and legacy hint fields are retained for schema compatibility and future expansion, but the current Agent flow no longer writes new AI hint records and the frontend no longer shows a separate right-side "layered hints" tab.

Primary audience:

- Java backend job seekers
- Student developers preparing for backend interviews
- Interviewers reviewing this as a resume project

Primary resume focus:

- Spring Boot backend design
- Code execution service integration
- Agent Workflow, Tool Calling, Observation, RAG Retrieval, and Memory design
- MySQL data modeling
- MyBatis-Plus mapper and SQL-layer design
- Redis problem hot-cache design with MySQL fallback
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

As of Phase 5 product polish, RAG V1, and learning-memory continuity, the project has a demoable end-to-end Agent workflow, real Dashboard data, a unified Hot100 Solution-mode Java submission model, SSE frontend diagnosis, AC code review branch, clearer frontend separation between preset hints and AI diagnosis, MySQL-backed RAG retrieval, and persistent knowledge self-test / mastery records:

```text
POST /api/submissions
  -> persist original Java submission
  -> wrap current Hot100 Solution-mode problems into Main.java for Piston through CodeWrapper adapters
  -> judge Java submission through Piston

POST /api/agent/analyze
  -> run the Agent workflow synchronously as fallback and API-level demo
  -> failed submissions return error type, knowledge point, diagnosis, improvement suggestion, training plan title, and trace steps
  -> accepted submissions return a lightweight codeReview object and trace steps
  -> frontend does not show hintLevel1/2/3 as a separate right-side tab

GET /api/submissions/{submissionId}/diagnosis/stream
  -> create AgentRun
  -> emit AgentStep events through SSE
  -> rejudge submission through CodeExecutionTool
  -> run RagRetrieveTool after Observation to retrieve problem / knowledge-card / user-memory evidence
  -> for failed submissions: classify error through AI with RAG evidence as supporting context
  -> persist diagnosis, weakness memory, and mistake card
  -> index diagnosis and mistake card into RAG memory after learning records are persisted
  -> create deterministic 3-day training plan with optional RAG-preferred knowledge-card items (optional, failure does not block)
  -> for accepted submissions: run CodeReviewTool with RAG evidence and skip weakness/mistake/training-plan writes
  -> emit final AgentAnalyzeVO

Dashboard and knowledge learning:
  -> Dashboard reads MySQL-backed stats, weaknesses, weakness events, mistake cards, recent submissions, error distribution, and latest training plan
  -> training plan items can be marked PENDING / COMPLETED / SKIPPED
  -> manual training-plan regeneration is exposed through UserController and uses deterministic rules, not LLM generation
  -> knowledge-card self-tests persist self_test_record and update user_knowledge_card_mastery
  -> low-score self-tests may write user_weakness_event with sourceType=SELF_TEST

Frontend SSE integration:
  -> fetch + ReadableStream (not EventSource, for future Authorization header)
  -> real-time step display during analysis, including optional RAG_RETRIEVAL
  -> done event shows final diagnosis result
  -> onEnd fallback ensures loading state is cleared
  -> streamId protection prevents old requests from overwriting new results
```

Current implemented controllers:

- `ProblemController`
- `SubmissionController`
- `AgentController`
- `UserController`
- `KnowledgeController`
- `RagChatController`
- `MockInterviewController`

Not yet exposed as REST controllers:

- single-hint lookup
- standalone accepted-code review endpoint (current accepted review is returned by `/api/agent/analyze` and SSE through `codeReview`)
- public raw RAG retrieval endpoint; `/api/rag/chat` is a controlled learning-material QA entry, not a general chat product

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
- Redis (problem list, problem detail, and problem template hot cache)
- Server-Sent Events

AI:

- Anthropic-compatible API
- Structured JSON outputs whenever AI results are persisted or consumed by business logic
- AI is used for error classification and code review; current training plans use deterministic fallback; AI hint generation is disabled
- Current frontend treats problem-level layered hints as preset content, not as an on-click AI generation flow

RAG:

- V1 uses MySQL structured retrieval. Optional Qdrant vector retrieval can be enabled for enhanced recall, while MySQL remains the source of truth.
- RAG sources are `problem`, `knowledge_card`, `ai_diagnosis`, and `mistake_card`.
- `RagRetrieveTool` is an internal Agent Tool. `/api/rag/chat` is a controlled learning-material QA entry that reuses MySQL RAG and existing learning memory without becoming a general chat product.
- Knowledge QA V1 only answers questions about problems, knowledge cards, AI diagnoses, mistake cards, and the current user's learning records. It does not use web search, upload documents, generate complete Java AC code, or replace the code-submission diagnosis flow.
- Mock Interview V1 is the preferred interview-training expansion path over generic chat: `/mock-interview` uses knowledge cards as interview questions, stores session / turn / report records, evaluates user answers, asks one follow-up, writes low-score or missing-point weakness events, and still does not generate complete Java AC code.
- RAG retrieval failure is non-blocking and should only record a failed Agent step / warning.
- User memory chunks must be isolated by `user_id`; never leak one user's mistake cards or AI diagnoses to another user.

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

1. Protect the core loop: submit Java code, run tests, diagnose failed submissions, show preset hints, record weakness, generate plan.
2. AI diagnosis quality and AC code review quality.
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
- `RagService` for MySQL structured retrieval, indexing, and system index rebuild
- `LearningTracker` for weakness and mistake tracking
- `TrainingPlanService` for training plan generation

Use Java-backend-style package responsibilities:

- `controller`: receive HTTP requests and return VO responses
- `service`: business interfaces
- `service/impl`: business implementations and orchestration
- `agent`: Agent orchestration classes such as `InterviewCoachAgent`, `AgentState`, `AgentContext`, and `AgentStep`
- `agent/tool`: Tool implementations such as `CodeExecutionTool`, `RagRetrieveTool`, `ErrorClassifierTool`, `CodeReviewTool`, `WeaknessTrackerTool`, and `TrainingPlannerTool`
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
- RAG retrieval scoring and user-memory isolation
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
- Keep RAG retrieval and index rebuild behind `RagService`; do not add public RAG REST endpoints unless product direction explicitly changes.

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
Planner -> CodeExecutionTool -> Observation -> RagRetrieveTool -> ErrorClassifierTool / CodeReviewTool -> WeaknessTrackerTool -> TrainingPlannerTool
```

`RagRetrieveTool`, `WeaknessTrackerTool`, and `TrainingPlannerTool` are optional steps — their failure does not block the final diagnosis result. `CodeReviewTool` is also optional for accepted submissions and may fail without turning an accepted submission into a failed Agent run.

Agent concepts:

- `AgentContext` carries submission, problem, execution result, diagnosis or code review, optional legacy hints, weakness update, and training plan data.
- `AgentStep` records each step name, tool name, status, input summary, output summary, duration, and error message.
- `Tool` implementations must have clear inputs and outputs and should call service-layer abstractions instead of controllers.
- `CodeExecutionTool` must call a service-layer execution abstraction such as `SubmissionService.rejudge(...)`, which in turn uses `JudgeService`; it must not call Piston directly.
- `RagRetrieveTool` must call `RagService.retrieveForDiagnosis(...)`; it must not query mappers from controllers and must not call AI directly.
- `AgentContext` carries `ragRetrieveResult`; AI tools may use it as evidence, but execution results and failed cases remain the source of truth.
- LLM calls belong only in tools that need semantic judgment, such as error classification or code review; hint generation and AI training-plan generation are not active in the current MVP.
- Tool outputs that affect business state must be converted into structured data before persistence.

AI must not directly provide full accepted Java solutions by default.

RAG evidence rules:

- Use retrieved evidence only as supporting context for `ErrorClassifierTool` and `CodeReviewTool`.
- If RAG evidence conflicts with Piston execution results, trust the execution result.
- Do not copy retrieved text verbatim when a short diagnosis or review is enough.
- Do not use RAG to generate full Java accepted solutions.
- Prefer RAG-hit `KNOWLEDGE_CARD` items in `TrainingPlannerTool`; if no knowledge-card hit exists, keep the current generic knowledge-card fallback.

Use layered hints:

- Level 1: direction only
- Level 2: related knowledge point and likely issue
- Level 3: pseudocode or key idea, not full Java answer
- For the current MVP UI, problem-level hints are preset and shown on the left; expanding them must not call AI. AI-generated hint fields are legacy-compatible response fields and should remain null unless the product direction changes.

Persist AI outputs only after converting them to structured data. Prefer JSON fields such as:

- `errorType`
- `knowledgePoint`
- `specificError`
- `diagnosis`
- `confidence`
- `weaknessScoreDelta`
- `complexity`
- `codeStyle`
- `interviewSuggestion`
- `optimizationPoints`

AI output should support the learning loop:

```text
failed submission -> tool observation -> RAG retrieval -> diagnosis -> weakness update -> mistake card -> RAG memory index -> training plan
```

Frontend display should present this as:

```text
problem preset hints -> failed submission -> test result -> RAG retrieval step -> AI diagnosis -> weakness memory -> training plan
```

## Code Execution Rules

Use Piston API for the MVP.

The first version supports Java only. Do not add Python, JavaScript, C++, or multi-language execution unless the user explicitly changes scope.

Current Java submission mode:

- Current seed problems are Hot100 selected Java problems in Solution mode.
- Users submit non-public `class Solution` code and do not handle stdin/stdout.
- The current demo problem IDs are `1` Two Sum, `206` Reverse Linked List, and `121` Best Time to Buy and Sell Stock.
- `CodeWrapper` owns the supported-problem adapter registry for the current Hot100 selected problems.
- `SubmissionServiceImpl` must save the original user code to `submission.code`.
- Only code sent to `JudgeService/Piston` should be wrapped by `CodeWrapper`.
- `problem.code_mode` is an internal backend DB configuration field for judge wrapping selection, not a REST request parameter.
- Do not add a REST `code_mode` parameter unless the user explicitly asks for a broader schema migration.

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
- user weakness events
- training plans
- training plan items
- mistake cards
- knowledge cards
- self-test records
- user knowledge-card mastery
- RAG documents
- RAG chunks

RAG persistence rules:

- `rag_document` and `rag_chunk` are MySQL-backed V1 retrieval indexes.
- System chunks have `user_id = NULL` and may come from `problem` and `knowledge_card`.
- User-memory chunks have `user_id` and may come from `ai_diagnosis` and `mistake_card`.
- Retrieval must enforce `(user_id IS NULL OR user_id = current userId)` in query logic and in service-level filtering.
- `RagService.rebuildSystemIndex()` may rebuild system problem / knowledge-card chunks, but must not delete user-memory chunks.
- Old databases need `data/rag_mysql_migration.sql`; new databases get RAG tables through `data/schema.sql`.

Redis is currently wired for read-only problem hot-cache data:

- hot problem list cache
- problem detail cache
- problem template cache

Future Redis wiring may be used for temporary Agent context or short-lived UI state, but do not cache durable learning state there.

Do not rely on Redis as the only storage for anything needed in the resume demo.

## SSE Rules

Use SSE for AI diagnosis streaming.

SSE is for server-to-browser updates such as:

- "planning agent steps"
- "running code execution tool"
- "observing failed test cases"
- "retrieving related knowledge and historical mistakes"
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

Current notable Agent step names include:

- `PLANNING`
- `CODE_EXECUTION`
- `OBSERVATION`
- `RAG_RETRIEVAL`
- `ERROR_CLASSIFICATION`
- `CODE_REVIEW`
- `MEMORY_UPDATE`
- `TRAINING_PLAN`
- `COMPLETED`
- `FAILED`

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
- RAG evidence can enter diagnosis / code-review prompts without overriding execution results

RAG verification:

- same problem and same knowledge point rank above unrelated chunks
- user A's mistake-card / AI-diagnosis chunks are not returned for user B
- empty RAG index returns an empty result without throwing
- `RAG_RETRIEVAL` appears after `OBSERVATION` and before `ERROR_CLASSIFICATION` / `CODE_REVIEW`
- RAG retrieval failure does not block failed-submission diagnosis or accepted-submission code review
- training plans prefer retrieved `KNOWLEDGE_CARD` hits and fallback to generic review cards when none are retrieved

Frontend verification:

- problem page loads
- editor accepts code
- submit button calls backend
- test result is displayed
- preset layered hints are visible on the left problem panel and all levels are collapsed by default
- SSE is the primary frontend diagnosis path; synchronous analyze remains as fallback
- RAG retrieval appears only as an Agent timeline step
- AI diagnosis is displayed after a failed submission; AC submissions can display lightweight code review
- the right result panel has no separate layered hints tab
- preset layered hints can be expanded manually without calling AI
- draft code, last result, and last diagnosis can be restored after refresh
- stale diagnosis warning appears after editing code that differs from the diagnosis snapshot

Demo verification:

```text
select problem -> inspect preset hints -> write buggy code -> submit -> run CodeExecutionTool -> observe failed test -> run RagRetrieveTool -> diagnose error -> update weakness memory -> generate plan
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
- general-purpose RAG chat / public raw RAG retrieval REST endpoint
- broad retrieval-stack expansion beyond the optional Qdrant enhancement already scoped for RAG; do not add Elasticsearch, pgvector, Milvus, or multiple vector stores without explicit direction

## Development Style

Prefer simple, explainable implementations over clever abstractions.

Add abstractions only when they protect a planned extension:

- Piston now, Docker later
- Anthropic-compatible API now, model provider swap later
- MySQL structured RAG as source of truth, optional Qdrant vector retrieval as enhancement
- list dashboard now, charts later

Keep names aligned with the project docs. If a service or concept appears in `docs/IMPLEMENTATION_PLAN.md`, reuse that naming unless there is a strong reason not to.

When in doubt, protect the demo loop.

## Next-Step Focus

Near-term work should follow `docs/PROJECT_STATUS.md`:

1. Stabilize three demo problems and known bug samples. ✓
2. Write root README and demo startup notes. ✓
3. Frontend SSE step display. ✓
4. Move preset problem hints from frontend static mapping to backend data. ✓
5. Knowledge training page V1. ✓
6. First priority: small-scope product polish. ✓
   - Improve knowledge training feedback with missing key points and more interviewer-like low-score comments.
   - Improve AC code review display by reusing the existing `CodeReviewTool` / `codeReview` branch; do not add a standalone accepted-code review REST endpoint.
   - Improve error states so backend, Piston, AI, and SSE failures point to concrete local troubleshooting steps instead of only "request failed".
7. Second priority: stabilize the core loop. ✓
   - Keep problem page boundaries: left side preset hints only; right side test result and AI diagnosis / AC review only.
   - Keep SSE as the primary frontend path, with `POST /api/agent/analyze` only as fallback; protect repeated submissions, stale streams, user interruption, and unmount through stream id checks and abort.
   - Keep Dashboard data sourced from MySQL for stats, weaknesses, mistake cards, error distribution, and training plans; do not return to mock data.
   - Keep `frontend/lib/core-loop-stability.node-test.cjs` as the regression guard for page boundaries, SSE fallback conditions, stale stream blocking, abort behavior, Dashboard no mock, and empty states.
8. Learning-memory continuity. ✓
   - Training plan items can be completed or skipped, and manual regeneration is available through UserController.
   - Knowledge-card self-tests persist records, update mastery, and low-score self-tests can add weakness events.
   - Algorithm diagnosis and backend knowledge-card training stay source-separated, even when shown in one training plan.
9. RAG V1 internal retrieval. ✓
   - `RagRetrieveTool` runs after `OBSERVATION` and before AI diagnosis / AC code review.
   - `RagService` indexes problem, knowledge-card, AI-diagnosis, and mistake-card chunks in MySQL.
   - RAG retrieval is optional and user-memory chunks are isolated by `user_id`.
   - Training plans prefer RAG-hit knowledge cards before generic fallback cards.
10. Knowledge QA V1. ✓
   - `/rag-chat` and `POST /api/rag/chat` provide a controlled learning-material QA entry.
   - It answers only problem, knowledge-card, historical diagnosis, mistake-card, and current-user learning-record questions.
   - Learning-record questions query `UserLearningService` by `userId`; ordinary knowledge questions use MySQL RAG and chat-only rerank.
   - It does not expose raw retrieval, web search, document upload, or complete AC code generation.
11. Mock Interview V1. ✓
   - `/mock-interview` and `/api/mock-interviews` provide controlled interviewer-style training.
   - Session state is explicit: CREATED -> ASKING_MAIN -> MAIN_ANSWERED -> ASKING_FOLLOW_UP -> FOLLOW_UP_ANSWERED -> NEXT_QUESTION / FINISHED -> REPORTED.
   - Turns persist MAIN / FOLLOW_UP questions, user answers, scores, hit / missing key points, expression issues, and AI raw JSON.
   - Low-score or missing-point answers write `user_weakness_event` with `sourceType=MOCK_INTERVIEW`.
12. Third priority: keep but do not implement yet.
   - Single-hint lookup endpoint.
   - Standalone accepted-code review REST endpoint.
   - General-purpose RAG chat / public raw RAG retrieval REST endpoint.
   - Additional vector-store alternatives beyond the current optional Qdrant path.
   - Redis caching beyond read-only problem hot data.

Final-stage-only work: full `1` / `206` / `121` demo replay, screenshots or recording, broad document polish, `hint_record` final strategy, and interview Q&A.
