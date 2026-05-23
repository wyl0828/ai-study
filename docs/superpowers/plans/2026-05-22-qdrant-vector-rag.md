# Qdrant Vector RAG Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the existing MySQL structured RAG into a hybrid MySQL + Qdrant vector RAG while preserving the Agent workflow, user-memory isolation, and non-blocking RAG failure behavior.

**Architecture:** MySQL remains the source of truth for `rag_document`, `rag_chunk`, problems, knowledge cards, diagnoses, and mistake cards. Qdrant stores only vector points for `rag_chunk` rows, with payload metadata used for filtering by `userId`, `sourceType`, `problemId`, `knowledgePoint`, and `errorType`. `RagService` keeps the same public contract and combines deterministic MySQL scores with vector similarity.

**Tech Stack:** Spring Boot 3.5, Java 17, MyBatis-Plus, MySQL 8, Qdrant Docker service, official Qdrant Java client `io.qdrant:client:1.18.1`, Anthropic-compatible chat API kept for diagnosis/review, separate embedding client abstraction for vector generation.

---

## File Map

- Modify: `docker-compose.yml` or create it if absent, to add a local Qdrant service.
- Modify: `backend/pom.xml`, to add Qdrant client dependency.
- Modify: `backend/src/main/resources/application.yml`, to add `coach.rag.vector` and `coach.ai.embedding` properties.
- Create: `backend/src/main/java/com/interview/coach/config/RagVectorProperties.java`, vector feature flag and Qdrant settings.
- Create: `backend/src/main/java/com/interview/coach/config/EmbeddingProperties.java`, embedding endpoint and model settings.
- Create: `backend/src/main/java/com/interview/coach/integration/ai/EmbeddingClient.java`, embedding abstraction.
- Create: `backend/src/main/java/com/interview/coach/integration/ai/OpenAiCompatibleEmbeddingClient.java`, HTTP embedding implementation.
- Create: `backend/src/main/java/com/interview/coach/integration/vector/RagVectorStore.java`, vector-store abstraction.
- Create: `backend/src/main/java/com/interview/coach/integration/vector/QdrantRagVectorStore.java`, Qdrant implementation.
- Create: `backend/src/main/java/com/interview/coach/dto/RagVectorHit.java`, vector search hit DTO.
- Modify: `backend/src/main/java/com/interview/coach/entity/RagChunk.java`, add optional vector metadata fields.
- Modify: `data/schema.sql` and create `data/rag_vector_migration.sql`, add vector metadata columns.
- Modify: `backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`, write vector points during indexing and use hybrid retrieval.
- Modify: `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`, cover fallback, user isolation, and vector-score fusion.
- Create: `backend/src/test/java/com/interview/coach/integration/vector/QdrantRagVectorStoreTest.java`, unit-test payload/filter construction with mocked Qdrant client.
- Modify: `docs/AI-Interview-Coach.md`, `docs/API.md`, `docs/PROJECT_STATUS.md`, document hybrid RAG as optional future-ready enhancement after implementation.

---

### Task 1: Add Local Qdrant Runtime

**Files:**
- Create or modify: `docker-compose.yml`
- Modify: `docs/PROJECT_STATUS.md`

- [ ] **Step 1: Add Qdrant service**

If `docker-compose.yml` does not exist, create it with:

```yaml
services:
  qdrant:
    image: qdrant/qdrant:latest
    container_name: ai-study-qdrant
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_storage:/qdrant/storage

volumes:
  qdrant_storage:
```

If it exists, add only the `qdrant` service and `qdrant_storage` volume without changing MySQL, Redis, Piston, or frontend services.

- [ ] **Step 2: Start Qdrant**

Run:

```bash
docker compose up -d qdrant
```

Expected: container `ai-study-qdrant` is running and port `6334` is available for the Java client.

- [ ] **Step 3: Smoke-test health**

Run:

```bash
curl http://localhost:6333/healthz
```

Expected: HTTP 200 or a Qdrant health response.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml docs/PROJECT_STATUS.md
git commit -m "chore: add local qdrant service"
```

---

### Task 2: Add Configuration And Dependencies

**Files:**
- Modify: `backend/pom.xml`
- Modify: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/interview/coach/config/RagVectorProperties.java`
- Create: `backend/src/main/java/com/interview/coach/config/EmbeddingProperties.java`
- Modify: `backend/src/main/java/com/interview/coach/CoachApplication.java` if configuration properties scanning is not already enabled.

- [ ] **Step 1: Add Qdrant dependency**

Add to `backend/pom.xml` dependencies:

```xml
<dependency>
    <groupId>io.qdrant</groupId>
    <artifactId>client</artifactId>
    <version>1.18.1</version>
</dependency>
```

- [ ] **Step 2: Add properties**

Append to `application.yml`:

```yaml
coach:
  rag:
    vector:
      enabled: ${RAG_VECTOR_ENABLED:false}
      collection-name: ${QDRANT_COLLECTION_NAME:ai_study_rag_chunks}
      host: ${QDRANT_HOST:localhost}
      port: ${QDRANT_PORT:6334}
      use-tls: ${QDRANT_USE_TLS:false}
      vector-size: ${RAG_VECTOR_SIZE:1536}
      hybrid-vector-weight: ${RAG_HYBRID_VECTOR_WEIGHT:0.60}
      hybrid-rule-weight: ${RAG_HYBRID_RULE_WEIGHT:0.40}
      vector-candidate-limit: ${RAG_VECTOR_CANDIDATE_LIMIT:30}
  ai:
    embedding:
      base-url: ${EMBEDDING_BASE_URL:${AI_BASE_URL:https://api.openai.com}}
      api-key: ${EMBEDDING_API_KEY:${AI_API_KEY:}}
      model: ${EMBEDDING_MODEL:text-embedding-3-small}
      dimensions: ${EMBEDDING_DIMENSIONS:1536}
```

If the existing `coach.ai` block conflicts structurally, merge the `embedding` child into the existing `coach.ai` section instead of duplicating top-level keys.

- [ ] **Step 3: Create `RagVectorProperties`**

```java
package com.interview.coach.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "coach.rag.vector")
public class RagVectorProperties {
    private boolean enabled = false;
    private String collectionName = "ai_study_rag_chunks";
    private String host = "localhost";
    private int port = 6334;
    private boolean useTls = false;
    private int vectorSize = 1536;
    private BigDecimal hybridVectorWeight = new BigDecimal("0.60");
    private BigDecimal hybridRuleWeight = new BigDecimal("0.40");
    private int vectorCandidateLimit = 30;
}
```

- [ ] **Step 4: Create `EmbeddingProperties`**

```java
package com.interview.coach.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "coach.ai.embedding")
public class EmbeddingProperties {
    private String baseUrl;
    private String apiKey;
    private String model = "text-embedding-3-small";
    private int dimensions = 1536;
}
```

- [ ] **Step 5: Compile**

Run:

```bash
cd backend
./mvnw -q -DskipTests compile
```

Expected: build succeeds.

- [ ] **Step 6: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/interview/coach/config
git commit -m "chore: configure vector rag settings"
```

---

### Task 3: Add Embedding Client

**Files:**
- Create: `backend/src/main/java/com/interview/coach/integration/ai/EmbeddingClient.java`
- Create: `backend/src/main/java/com/interview/coach/integration/ai/OpenAiCompatibleEmbeddingClient.java`
- Create: `backend/src/test/java/com/interview/coach/integration/ai/OpenAiCompatibleEmbeddingClientTest.java`

- [ ] **Step 1: Create embedding abstraction**

```java
package com.interview.coach.integration.ai;

public interface EmbeddingClient {
    float[] embed(String text);
}
```

- [ ] **Step 2: Implement OpenAI-compatible client**

Use `RestClient`, `EmbeddingProperties`, and `ObjectMapper`. POST to `/v1/embeddings` with body:

```json
{
  "model": "text-embedding-3-small",
  "input": "chunk text",
  "dimensions": 1536
}
```

Parse `data[0].embedding` into `float[]`. If API key is missing, throw `AiClientException("EMBEDDING_API_KEY is required")`.

- [ ] **Step 3: Test response parsing**

Mock the HTTP response body:

```json
{"data":[{"embedding":[0.1,0.2,0.3]}]}
```

Expected: `embed("abc")` returns a `float[]` of length 3.

- [ ] **Step 4: Run test**

```bash
cd backend
./mvnw -q -Dtest=OpenAiCompatibleEmbeddingClientTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interview/coach/integration/ai backend/src/test/java/com/interview/coach/integration/ai
git commit -m "feat: add embedding client"
```

---

### Task 4: Add Vector Store Abstraction

**Files:**
- Create: `backend/src/main/java/com/interview/coach/dto/RagVectorHit.java`
- Create: `backend/src/main/java/com/interview/coach/integration/vector/RagVectorStore.java`
- Create: `backend/src/main/java/com/interview/coach/integration/vector/QdrantRagVectorStore.java`
- Create: `backend/src/test/java/com/interview/coach/integration/vector/QdrantRagVectorStoreTest.java`

- [ ] **Step 1: Create vector hit DTO**

```java
package com.interview.coach.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RagVectorHit {
    private Long chunkId;
    private BigDecimal similarity;
}
```

- [ ] **Step 2: Create vector store interface**

```java
package com.interview.coach.integration.vector;

import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagVectorHit;
import com.interview.coach.entity.RagChunk;
import java.util.List;

public interface RagVectorStore {
    void ensureCollection();
    void upsertChunk(RagChunk chunk, float[] vector);
    void deleteSystemChunks();
    List<RagVectorHit> search(float[] queryVector, RagRetrieveQuery query, int limit);
}
```

- [ ] **Step 3: Implement Qdrant store**

Implementation rules:

- Point id: use `chunk.id`.
- Vector: embedding from `EmbeddingClient`.
- Payload keys: `chunkId`, `documentId`, `sourceType`, `sourceId`, `userId`, `problemId`, `knowledgePoint`, `errorType`, `tags`.
- Search filter: allow `userId` missing/null system chunks and current user's chunks only.
- If Qdrant throws, let the exception bubble to `RagServiceImpl`, where vector failure will be downgraded to MySQL-only retrieval.

- [ ] **Step 4: Test filter construction**

Mock the Qdrant client and assert search for `userId=1` includes the equivalent of:

```text
userId is empty OR userId = 1
```

Expected: no vector search can request all user-memory points without the current user filter.

- [ ] **Step 5: Run test**

```bash
cd backend
./mvnw -q -Dtest=QdrantRagVectorStoreTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interview/coach/dto/RagVectorHit.java backend/src/main/java/com/interview/coach/integration/vector backend/src/test/java/com/interview/coach/integration/vector
git commit -m "feat: add qdrant vector store"
```

---

### Task 5: Add Vector Metadata To MySQL

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/entity/RagChunk.java`
- Modify: `data/schema.sql`
- Create: `data/rag_vector_migration.sql`

- [ ] **Step 1: Add entity fields**

Add to `RagChunk`:

```java
private String vectorPointId;

private String embeddingModel;

private Integer embeddingDim;

private String vectorStatus;
```

- [ ] **Step 2: Add schema columns**

Add to `rag_chunk` in `data/schema.sql`:

```sql
    vector_point_id VARCHAR(64),
    embedding_model VARCHAR(128),
    embedding_dim INT,
    vector_status VARCHAR(32),
    INDEX idx_rag_chunk_vector_status (vector_status)
```

- [ ] **Step 3: Create migration**

```sql
ALTER TABLE rag_chunk
    ADD COLUMN vector_point_id VARCHAR(64) NULL AFTER metadata_json,
    ADD COLUMN embedding_model VARCHAR(128) NULL AFTER vector_point_id,
    ADD COLUMN embedding_dim INT NULL AFTER embedding_model,
    ADD COLUMN vector_status VARCHAR(32) NULL AFTER embedding_dim,
    ADD INDEX idx_rag_chunk_vector_status (vector_status);
```

- [ ] **Step 4: Compile**

```bash
cd backend
./mvnw -q -DskipTests compile
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/interview/coach/entity/RagChunk.java data/schema.sql data/rag_vector_migration.sql
git commit -m "feat: add rag vector metadata"
```

---

### Task 6: Index Chunks Into Qdrant

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- Modify: `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **Step 1: Inject vector dependencies**

Add optional dependencies to `RagServiceImpl`:

```java
private final RagVectorProperties ragVectorProperties;
private final EmbeddingProperties embeddingProperties;
private final EmbeddingClient embeddingClient;
private final RagVectorStore ragVectorStore;
```

- [ ] **Step 2: Update `insertChunk`**

After `ragChunkMapper.insert(chunk)`, call a new helper:

```java
indexVectorIfEnabled(chunk);
```

Helper behavior:

- if `ragVectorProperties.isEnabled()` is false, do nothing.
- call `embeddingClient.embed(chunk.getChunkText())`.
- call `ragVectorStore.ensureCollection()`.
- call `ragVectorStore.upsertChunk(chunk, vector)`.
- set `vectorPointId = String.valueOf(chunk.getId())`.
- set `embeddingModel` and `embeddingDim`.
- set `vectorStatus = "INDEXED"`.
- update chunk by id.
- catch runtime exceptions, set `vectorStatus = "FAILED"`, update chunk by id, log warning, do not throw.

- [ ] **Step 3: Test disabled mode**

In `RagServiceImplTest`, when `RAG_VECTOR_ENABLED=false`, verify `indexProblem(problem)` inserts MySQL chunks and never calls `EmbeddingClient`.

- [ ] **Step 4: Test vector failure downgrade**

Mock `EmbeddingClient.embed(...)` to throw. Verify `indexKnowledgeCard(card)` still inserts `rag_chunk` and sets `vectorStatus = "FAILED"` or leaves MySQL retrieval usable.

- [ ] **Step 5: Run tests**

```bash
cd backend
./mvnw -q -Dtest=RagServiceImplTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java
git commit -m "feat: index rag chunks into qdrant"
```

---

### Task 7: Add Hybrid Retrieval

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- Modify: `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **Step 1: Keep MySQL retrieval as baseline**

Refactor current candidate loading and deterministic scoring into a helper:

```java
private List<RagChunkHit> mysqlRuleHits(RagRetrieveQuery query, int limit)
```

This helper must preserve current user isolation and active-document filtering.

- [ ] **Step 2: Add vector search helper**

```java
private List<RagVectorHit> vectorHits(RagRetrieveQuery query) {
    if (!ragVectorProperties.isEnabled()) {
        return List.of();
    }
    try {
        float[] queryVector = embeddingClient.embed(String.join(" ", queryKeywords(query)));
        return ragVectorStore.search(queryVector, query, ragVectorProperties.getVectorCandidateLimit());
    } catch (RuntimeException ex) {
        log.warn("Vector RAG search failed, fallback to MySQL-only retrieval: {}", ex.getMessage());
        return List.of();
    }
}
```

- [ ] **Step 3: Fuse scores**

For chunks appearing in vector hits:

```text
finalScore = round(ruleScore * ruleWeight + similarity * 100 * vectorWeight)
```

For chunks missing vector hits:

```text
finalScore = ruleScore
```

Sort by `finalScore DESC`, then `chunkId ASC`.

- [ ] **Step 4: Test vector score promotion**

Create two chunks:

- chunk A has higher MySQL score but no vector hit.
- chunk B has lower MySQL score but vector similarity `0.95`.

With `vectorWeight=0.60`, assert chunk B ranks above chunk A.

- [ ] **Step 5: Test vector search failure**

Mock `ragVectorStore.search(...)` to throw. Assert `retrieve(query)` still returns MySQL hits and does not throw.

- [ ] **Step 6: Run tests**

```bash
cd backend
./mvnw -q -Dtest=RagServiceImplTest test
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java
git commit -m "feat: add hybrid rag retrieval"
```

---

### Task 8: Rebuild System Index With Vector Cleanup

**Files:**
- Modify: `backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- Modify: `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **Step 1: Update `rebuildSystemIndex()`**

Before deleting system MySQL chunks, call:

```java
if (ragVectorProperties.isEnabled()) {
    try {
        ragVectorStore.deleteSystemChunks();
    } catch (RuntimeException ex) {
        log.warn("Failed to delete system vector chunks, continue rebuilding MySQL RAG index: {}", ex.getMessage());
    }
}
```

Then keep existing behavior:

```text
delete user_id IS NULL rag_chunk
delete user_id IS NULL rag_document
index enabled problems
index enabled knowledge cards
do not delete user-memory chunks
```

- [ ] **Step 2: Test user memory preservation**

Create one system chunk and one `userId=1` chunk. Run `rebuildSystemIndex()`. Assert the user chunk remains in MySQL and vector cleanup was called only for system chunks.

- [ ] **Step 3: Run tests**

```bash
cd backend
./mvnw -q -Dtest=RagServiceImplTest test
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java
git commit -m "feat: rebuild qdrant system rag index"
```

---

### Task 9: End-To-End Verification

**Files:**
- No production file changes unless failures reveal bugs.

- [ ] **Step 1: Start local dependencies**

```bash
docker compose up -d qdrant
```

Also start MySQL, Redis, Piston, backend, and frontend using the existing project startup notes.

- [ ] **Step 2: Apply database migration**

Run `data/rag_vector_migration.sql` against the local MySQL database.

Expected: `rag_chunk` has vector metadata columns.

- [ ] **Step 3: Enable vector mode**

Start backend with:

```bash
RAG_VECTOR_ENABLED=true
QDRANT_HOST=localhost
QDRANT_PORT=6334
EMBEDDING_API_KEY=<local-key>
EMBEDDING_BASE_URL=<embedding-provider-url>
EMBEDDING_MODEL=text-embedding-3-small
```

- [ ] **Step 4: Rebuild system index**

Run the existing local maintenance path that calls:

```java
ragService.rebuildSystemIndex();
```

Expected: MySQL `rag_chunk` contains `PROBLEM` and `KNOWLEDGE_CARD` rows with `vector_status='INDEXED'` when embedding is configured.

- [ ] **Step 5: Verify failed-submission flow**

Use problem `1` Two Sum and submit a known wrong Java `class Solution`. Confirm SSE step order includes:

```text
PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> ERROR_CLASSIFICATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED
```

Expected: diagnosis still trusts Piston execution result and uses retrieved evidence only as support.

- [ ] **Step 6: Verify accepted-submission flow**

Submit accepted Java `class Solution`. Confirm SSE includes:

```text
PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> CODE_REVIEW -> COMPLETED
```

Expected: AC code review appears; no weakness or mistake card is written.

- [ ] **Step 7: Verify Qdrant failure fallback**

Stop Qdrant:

```bash
docker compose stop qdrant
```

Submit the same wrong solution.

Expected: `RAG_RETRIEVAL` may be empty or downgraded, but final AI diagnosis still appears.

- [ ] **Step 8: Run regression tests**

```bash
cd backend
./mvnw test -Dtest=RagServiceImplTest,InterviewCoachAgentTest,RagRetrieveToolTest
cd ../frontend
node lib/core-loop-stability.node-test.cjs
```

Expected: all pass.

- [ ] **Step 9: Commit fixes if needed**

```bash
git add backend frontend data docs
git commit -m "test: verify hybrid vector rag flow"
```

---

### Task 10: Documentation And Interview Narrative

**Files:**
- Modify: `docs/AI-Interview-Coach.md`
- Modify: `docs/API.md`
- Modify: `docs/PROJECT_STATUS.md`
- Modify: `README.md` if it contains startup notes.

- [ ] **Step 1: Update architecture docs**

Add this summary:

```text
RAG V2 uses hybrid retrieval. MySQL remains the durable source of truth and deterministic metadata filter. Qdrant stores chunk vectors and supports semantic recall. RagService merges rule score and vector similarity, while Agent tools still consume RagRetrieveResult.
```

- [ ] **Step 2: Update safety notes**

Document:

```text
User memory chunks must be isolated in both MySQL and Qdrant payload filters. RAG retrieval remains optional; vector service failure must not block code execution, diagnosis, AC review, weakness tracking, or training-plan generation.
```

- [ ] **Step 3: Update startup notes**

Document:

```bash
docker compose up -d qdrant
RAG_VECTOR_ENABLED=true
QDRANT_HOST=localhost
QDRANT_PORT=6334
```

- [ ] **Step 4: Commit**

```bash
git add docs README.md
git commit -m "docs: describe hybrid qdrant rag"
```

---

## Completion Criteria

- Qdrant can run locally through Docker.
- Backend starts with `RAG_VECTOR_ENABLED=false` and behaves exactly like current MySQL RAG.
- Backend starts with `RAG_VECTOR_ENABLED=true` and indexes `rag_chunk` vectors into Qdrant.
- `RagService.retrieve(...)` returns hybrid-ranked hits.
- User memory retrieval is isolated by `userId` in both MySQL and Qdrant.
- Qdrant or embedding failure downgrades to MySQL-only retrieval and does not block Agent diagnosis.
- SSE still shows `RAG_RETRIEVAL` after `OBSERVATION`.
- Failed submissions still write diagnosis, weakness memory, mistake card, and training plan.
- Accepted submissions still run AC code review without writing weakness memory.

## Self-Review

- Spec coverage: Covers local Qdrant setup, embedding, vector indexing, hybrid retrieval, rebuild, user isolation, fallback behavior, tests, and docs.
- Placeholder scan: No TBD or open-ended implementation placeholders; each task identifies exact files and expected behavior.
- Type consistency: `RagVectorHit`, `RagVectorStore`, `EmbeddingClient`, and property class names are introduced before use.
