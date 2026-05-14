# RAG 功能实施计划

> **给执行 Agent 的要求：** 实施本计划时必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans`，按任务逐项执行。任务步骤使用复选框语法，方便跟踪进度。

**目标：** 为 AI Interview Coach Agent 增加一个小而清晰的 V1 RAG 层，让 Agent 在诊断和生成训练计划前，可以检索题目知识、项目知识卡和用户历史学习记忆。

**架构：** V1 使用 MySQL 结构化检索，不引入向量数据库。Agent 在 `OBSERVATION` 之后执行 `RagRetrieveTool`；RAG 检索失败只记录日志，不阻塞代码诊断、弱点记录和训练计划。

**技术栈：** Spring Boot 3、Java 17、MyBatis-Plus、MySQL 8、现有 SSE AgentStep 流、现有 Anthropic-compatible AI 客户端。

---

## 1. 产品边界

V1 RAG 不是通用知识库聊天，也不是“问我任何 Java 问题”的开放入口。它只是现有 Agent Workflow 里的一个 Tool：

```text
Planner
 -> CodeExecutionTool
 -> Observation
 -> RagRetrieveTool
 -> ErrorClassifierTool / CodeReviewTool
 -> WeaknessTrackerTool
 -> TrainingPlannerTool
```

V1 要做：

- 从 `knowledge_card`、`problem`、`mistake_card`、`ai_diagnosis` 检索上下文。
- 使用 MySQL 字段、标签和关键词做确定性打分。
- 把检索结果作为证据注入 AI 诊断和 AC 代码点评 prompt。
- 在 Agent Trace / SSE 中新增 `RAG_RETRIEVAL` 步骤。
- 训练计划优先使用检索到的知识卡。
- 增加检索排序、用户隔离、Agent 步骤顺序、prompt 注入相关测试。

V1 不做：

- 不生成 embedding。
- 不接 Qdrant、Milvus、Elasticsearch、pgvector 等向量库。
- 不新增独立 RAG 聊天页面。
- 不让 AI 生成完整 Java 标准答案。
- 不重新引入右侧“分层提示”tab。

---

## 2. 内容来源与整理规范

RAG 知识内容可以参考以下两个站点的选题范围和知识组织方式：

- 小林 coding：`https://xiaolincoding.com/`
- JavaGuide：`https://javaguide.net/` 或 `https://javaguide.cn/javaguide/`

使用方式：

- 只参考“知识点选题、常见面试问题、知识结构、表达角度”。
- 入库内容必须重新整理成项目自己的原创表述，不能整段复制文章。
- 每张 `knowledge_card` 必须适合面试训练场景，突出“定义 -> 机制 -> 使用场景 -> 常见追问 -> 面试表达”。
- 内容写入 `knowledge_card.source_name` 和 `knowledge_card.source_url`，方便后续 RAG 返回来源。
- 如果同一个知识点同时参考了小林 coding 和 JavaGuide，优先用自己的综合总结，不把两个网站原文拼接在一起。

第一批建议整理的 RAG 知识方向：

```text
Java 基础：
- HashMap 底层结构、扩容、hash 冲突、线程安全
- ArrayList 扩容机制、LinkedList 对比
- equals / hashCode 契约
- final / static / volatile 基础语义

JVM：
- 类加载过程
- 双亲委派模型
- 运行时数据区
- GC Roots 与可达性分析
- 常见垃圾收集器和 STW

并发：
- synchronized 与 ReentrantLock
- volatile 可见性和禁止重排序
- ThreadLocal 原理和内存泄漏
- 线程池核心参数和拒绝策略
- AQS 基本思想

MySQL：
- B+ 树索引
- 聚簇索引和非聚簇索引
- 覆盖索引、最左前缀、索引失效
- 事务 ACID
- MVCC 和隔离级别
- explain 常见字段

Redis：
- 常见数据结构
- 缓存穿透、击穿、雪崩
- 过期策略和淘汰策略
- 分布式锁基本风险

Spring / Spring Boot：
- IoC 和 AOP
- Bean 生命周期
- 事务传播行为
- Spring MVC 请求流程
- Spring Boot 自动配置
```

内容颗粒度：

- 一张知识卡只讲一个核心知识点。
- 单张卡片 `answer` 建议控制在 300-600 中文字。
- `key_points` 用 4-6 个短点，便于自测评分和 RAG chunk 检索。
- `follow_up` 放 2-4 个面试官追问。
- 不要把完整长文塞进一张卡片，RAG V1 检索的是短 chunk，不是文章库。

RAG 内容与当前算法诊断的边界：

- 算法题失败时，RAG 可以检索相似错题、题目提示、HashMap / 链表 / DP 等相关知识。
- 不要因为 Two Sum 错误就强行推荐 MySQL、Redis、Spring。
- 后端知识卡可以作为训练计划的“穿插复习”，但不能替代算法错因诊断。

---

## 3. 文件清单

新增文件：

- `data/rag_mysql_migration.sql`：RAG 表结构迁移。
- `backend/src/main/java/com/interview/coach/entity/RagDocument.java`：RAG 文档实体。
- `backend/src/main/java/com/interview/coach/entity/RagChunk.java`：RAG 文本块实体。
- `backend/src/main/java/com/interview/coach/enums/RagSourceTypeEnum.java`：RAG 来源类型枚举。
- `backend/src/main/java/com/interview/coach/mapper/RagDocumentMapper.java`：RAG 文档 Mapper。
- `backend/src/main/java/com/interview/coach/mapper/RagChunkMapper.java`：RAG 文本块 Mapper。
- `backend/src/main/java/com/interview/coach/dto/RagRetrieveQuery.java`：内部检索请求 DTO。
- `backend/src/main/java/com/interview/coach/dto/RagChunkHit.java`：单条检索命中结果。
- `backend/src/main/java/com/interview/coach/dto/RagRetrieveResult.java`：检索结果集合。
- `backend/src/main/java/com/interview/coach/service/RagService.java`：RAG 服务接口。
- `backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`：MySQL 检索和索引实现。
- `backend/src/main/java/com/interview/coach/agent/tool/RagRetrieveTool.java`：Agent Tool。
- `backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`：检索服务测试。
- `backend/src/test/java/com/interview/coach/agent/tool/RagRetrieveToolTest.java`：Tool 测试。

修改文件：

- `data/schema.sql`：追加 RAG 表，保证新库初始化可用。
- `backend/src/main/java/com/interview/coach/agent/AgentContext.java`：增加 `ragRetrieveResult`。
- `backend/src/main/java/com/interview/coach/enums/AgentState.java`：增加 `RAG_RETRIEVAL`。
- `backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`：接入 `RagRetrieveTool`。
- `backend/src/main/java/com/interview/coach/agent/tool/ErrorClassifierTool.java`：把 RAG 证据加入诊断 prompt。
- `backend/src/main/java/com/interview/coach/agent/tool/CodeReviewTool.java`：把 RAG 证据加入 AC 代码点评 prompt。
- `backend/src/main/java/com/interview/coach/agent/tool/TrainingPlannerTool.java`：训练计划优先使用 RAG 检索到的知识卡。
- `backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java`：诊断和错题卡落库后同步写入 RAG 索引。
- `frontend/lib/i18n.ts`：新增 `RAG_RETRIEVAL` 中文显示。
- `frontend/lib/core-loop-stability.node-test.cjs`：确认 RAG 不改变右侧结果面板边界。
- `backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java`：补充 Agent 步骤顺序和可选失败测试。

---

## 4. 任务一：新增 MySQL RAG 表

**文件：**

- 新增：`data/rag_mysql_migration.sql`
- 修改：`data/schema.sql`

- [ ] **步骤 1：新增迁移 SQL**

创建 `data/rag_mysql_migration.sql`：

```sql
CREATE TABLE rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    title VARCHAR(255) NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_rag_document_source (source_type, source_id, user_id),
    INDEX idx_rag_document_problem (problem_id, status),
    INDEX idx_rag_document_user (user_id, status),
    INDEX idx_rag_document_knowledge (knowledge_point, status),
    INDEX idx_rag_document_error (error_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rag_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    user_id BIGINT,
    problem_id BIGINT,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    knowledge_point VARCHAR(128),
    error_type VARCHAR(64),
    tags VARCHAR(512),
    metadata_json TEXT,
    created_at DATETIME NOT NULL,
    INDEX idx_rag_chunk_document (document_id, chunk_index),
    INDEX idx_rag_chunk_problem (problem_id, source_type),
    INDEX idx_rag_chunk_user (user_id, source_type),
    INDEX idx_rag_chunk_knowledge (knowledge_point, source_type),
    INDEX idx_rag_chunk_error (error_type, source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

- [ ] **步骤 2：把同样表结构追加到 `data/schema.sql`**

建议放在学习记忆相关表之后，例如 `mistake_card` 后面。这样新环境只执行 `schema.sql` 也能得到完整结构。

- [ ] **步骤 3：本地执行迁移**

先确认 `application.yml` 里的数据库名，然后执行：

```powershell
mysql -u root -p ai_interview_coach < data/rag_mysql_migration.sql
```

预期结果：SQL 无报错执行完成。

- [ ] **步骤 4：提交**

```powershell
git add data/rag_mysql_migration.sql data/schema.sql
git commit -m "feat: add mysql rag schema"
```

---

## 5. 任务二：新增 RAG 实体、DTO 和 Mapper

**文件：**

- 新增：`backend/src/main/java/com/interview/coach/entity/RagDocument.java`
- 新增：`backend/src/main/java/com/interview/coach/entity/RagChunk.java`
- 新增：`backend/src/main/java/com/interview/coach/enums/RagSourceTypeEnum.java`
- 新增：`backend/src/main/java/com/interview/coach/mapper/RagDocumentMapper.java`
- 新增：`backend/src/main/java/com/interview/coach/mapper/RagChunkMapper.java`
- 新增：`backend/src/main/java/com/interview/coach/dto/RagRetrieveQuery.java`
- 新增：`backend/src/main/java/com/interview/coach/dto/RagChunkHit.java`
- 新增：`backend/src/main/java/com/interview/coach/dto/RagRetrieveResult.java`

- [ ] **步骤 1：新增来源类型枚举**

```java
package com.interview.coach.enums;

public enum RagSourceTypeEnum {
    KNOWLEDGE_CARD,
    PROBLEM,
    MISTAKE_CARD,
    AI_DIAGNOSIS
}
```

- [ ] **步骤 2：新增实体**

`RagDocument.java`：

```java
package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rag_document")
public class RagDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sourceType;
    private Long sourceId;
    private Long userId;
    private Long problemId;
    private String title;
    private String knowledgePoint;
    private String errorType;
    private String tags;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

`RagChunk.java`：

```java
package com.interview.coach.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("rag_chunk")
public class RagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;
    private String sourceType;
    private Long sourceId;
    private Long userId;
    private Long problemId;
    private Integer chunkIndex;
    private String chunkText;
    private String knowledgePoint;
    private String errorType;
    private String tags;
    private String metadataJson;
    private LocalDateTime createdAt;
}
```

- [ ] **步骤 3：新增 Mapper**

`RagDocumentMapper.java`：

```java
package com.interview.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.coach.entity.RagDocument;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagDocumentMapper extends BaseMapper<RagDocument> {
}
```

`RagChunkMapper.java`：

```java
package com.interview.coach.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.interview.coach.entity.RagChunk;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RagChunkMapper extends BaseMapper<RagChunk> {
}
```

- [ ] **步骤 4：新增 DTO**

`RagRetrieveQuery.java`：

```java
package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagRetrieveQuery {

    private Long userId;
    private Long problemId;
    private String problemTitle;
    private String problemCategory;
    private String errorType;
    private String knowledgePoint;
    private String executionStatus;
    private String errorMessage;
    private List<String> keywords = new ArrayList<>();
    private int limit = 5;
}
```

`RagChunkHit.java`：

```java
package com.interview.coach.dto;

import lombok.Data;

@Data
public class RagChunkHit {

    private Long chunkId;
    private Long documentId;
    private String sourceType;
    private Long sourceId;
    private Long userId;
    private Long problemId;
    private String title;
    private String knowledgePoint;
    private String errorType;
    private String chunkText;
    private int score;

    public String toPromptLine(int index) {
        return "%d. [%s#%s score=%d] %s".formatted(index, sourceType, sourceId, score, compact(chunkText));
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180) + "...";
    }
}
```

`RagRetrieveResult.java`：

```java
package com.interview.coach.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RagRetrieveResult {

    private List<RagChunkHit> hits = new ArrayList<>();

    public boolean hasHits() {
        return hits != null && !hits.isEmpty();
    }

    public String summary() {
        if (!hasHits()) {
            return "未检索到 RAG 证据";
        }
        return "已检索到 " + hits.size() + " 条 RAG 证据";
    }

    public String toPromptBlock() {
        if (!hasHits()) {
            return "没有检索到可用证据。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            builder.append(hits.get(i).toPromptLine(i + 1)).append("\\n");
        }
        return builder.toString();
    }
}
```

- [ ] **步骤 5：编译后端**

```powershell
cd backend
mvn -q -DskipTests compile
```

预期结果：`BUILD SUCCESS`。

- [ ] **步骤 6：提交**

```powershell
git add backend/src/main/java/com/interview/coach/entity/RagDocument.java backend/src/main/java/com/interview/coach/entity/RagChunk.java backend/src/main/java/com/interview/coach/enums/RagSourceTypeEnum.java backend/src/main/java/com/interview/coach/mapper/RagDocumentMapper.java backend/src/main/java/com/interview/coach/mapper/RagChunkMapper.java backend/src/main/java/com/interview/coach/dto/RagRetrieveQuery.java backend/src/main/java/com/interview/coach/dto/RagChunkHit.java backend/src/main/java/com/interview/coach/dto/RagRetrieveResult.java
git commit -m "feat: add rag domain model"
```

---

## 6. 任务三：实现 MySQL 结构化检索

**文件：**

- 新增：`backend/src/main/java/com/interview/coach/service/RagService.java`
- 新增：`backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- 新增：`backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **步骤 1：新增服务接口**

```java
package com.interview.coach.service;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagRetrieveQuery;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.entity.AiDiagnosis;
import com.interview.coach.entity.KnowledgeCard;
import com.interview.coach.entity.MistakeCard;
import com.interview.coach.entity.Problem;

public interface RagService {

    RagRetrieveResult retrieveForDiagnosis(AgentContext context, int limit);

    RagRetrieveResult retrieve(RagRetrieveQuery query);

    void indexProblem(Problem problem);

    void indexKnowledgeCard(KnowledgeCard card);

    void indexLearningMemory(AgentContext context, AiDiagnosis diagnosis, MistakeCard mistakeCard);
}
```

- [ ] **步骤 2：实现确定性打分**

`RagServiceImpl` 使用以下规则：

```text
+60 同一用户的学习记忆
+50 同一题目
+40 同一知识点
+30 同一错误类型
+20 标题 / 标签 / 分类命中关键词
+10 chunk 文本命中关键词
```

检索用户历史记忆时必须隔离用户：

```text
(user_id IS NULL OR user_id = 当前 userId)
```

这样可以避免 A 用户的错题卡被 B 用户检索到。

- [ ] **步骤 3：新增检索测试**

`RagServiceImplTest` 至少覆盖：

- 同题目 + 同知识点的 chunk 排名高于无关知识卡。
- user A 的 `mistake_card` 不会出现在 user B 的检索结果中。
- 没有 chunk 时返回空 `RagRetrieveResult`，不抛异常。

建议测试方法名：

```java
retrieve_prefersSameProblemAndKnowledgePoint()
retrieve_doesNotLeakOtherUsersMistakeMemory()
retrieve_returnsEmptyResultWhenNoChunksExist()
```

- [ ] **步骤 4：运行测试**

```powershell
cd backend
mvn -q -Dtest=RagServiceImplTest test
```

预期结果：RAG 服务测试全部通过。

- [ ] **步骤 5：提交**

```powershell
git add backend/src/main/java/com/interview/coach/service/RagService.java backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java
git commit -m "feat: implement mysql rag retrieval"
```

---

## 7. 任务四：索引现有项目知识

**文件：**

- 修改：`backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- 修改：`backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java`
- 测试：`backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`
- 测试：`backend/src/test/java/com/interview/coach/service/LearningTrackerImplTest.java`

- [ ] **步骤 1：题目切分规则**

`indexProblem(Problem problem)` 创建这些 chunk：

```text
chunk 0: title + category + description
chunk 1: hint_level1 + hint_level2 + hint_level3
chunk 2: solution_outline
```

空内容直接跳过。字段设置：

```text
source_type = PROBLEM
source_id = problem.id
problem_id = problem.id
user_id = NULL
```

- [ ] **步骤 2：知识卡切分规则**

`indexKnowledgeCard(KnowledgeCard card)` 创建这些 chunk：

```text
chunk 0: question
chunk 1: answer
chunk 2: key_points
chunk 3: follow_up
```

空内容直接跳过。字段设置：

```text
source_type = KNOWLEDGE_CARD
source_id = card.id
tags = card.tags
user_id = NULL
```

- [ ] **步骤 3：学习记忆切分规则**

`indexLearningMemory(AgentContext context, AiDiagnosis diagnosis, MistakeCard mistakeCard)` 创建：

```text
AI_DIAGNOSIS chunk: diagnosis.specificError + diagnosis.diagnosis + diagnosis.suggestion
MISTAKE_CARD chunk: mistakeCard.mistakeSummary + mistakeCard.correctIdea
```

字段设置：

```text
user_id = context.userId
problem_id = context.problemId
knowledge_point = diagnosis.knowledgePoint
error_type = diagnosis.errorType
```

- [ ] **步骤 4：让学习记录落库后同步进入 RAG 索引**

调整 `LearningTrackerImpl`：

```java
private AiDiagnosis insertDiagnosis(AgentContext context, LocalDateTime now) {
    AiDiagnosis diagnosis = new AiDiagnosis();
    // 保留原有字段赋值
    aiDiagnosisMapper.insert(diagnosis);
    return diagnosis;
}

private MistakeCard insertMistakeCard(AgentContext context, LocalDateTime now) {
    // 保留原有 upsert 逻辑
    // 返回插入或更新后的 mistakeCard
}
```

在 `recordDiagnosis` 末尾调用：

```java
AiDiagnosis diagnosis = insertDiagnosis(context, now);
insertHints(context, now);
upsertWeakness(context, now);
MistakeCard mistakeCard = insertMistakeCard(context, now);
ragService.indexLearningMemory(context, diagnosis, mistakeCard);
```

如果 RAG 索引失败，只记录 warn 日志，不影响诊断、弱点和错题卡保存。

- [ ] **步骤 5：运行测试**

```powershell
cd backend
mvn -q -Dtest=LearningTrackerImplTest,RagServiceImplTest test
```

预期结果：原学习记录测试继续通过，新 RAG 索引断言通过。

- [ ] **步骤 6：提交**

```powershell
git add backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/main/java/com/interview/coach/service/impl/LearningTrackerImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java backend/src/test/java/com/interview/coach/service/LearningTrackerImplTest.java
git commit -m "feat: index learning memory for rag"
```

---

## 8. 任务五：把 `RagRetrieveTool` 接入 Agent Workflow

**文件：**

- 新增：`backend/src/main/java/com/interview/coach/agent/tool/RagRetrieveTool.java`
- 修改：`backend/src/main/java/com/interview/coach/agent/AgentContext.java`
- 修改：`backend/src/main/java/com/interview/coach/enums/AgentState.java`
- 修改：`backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java`
- 测试：`backend/src/test/java/com/interview/coach/agent/tool/RagRetrieveToolTest.java`
- 测试：`backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java`

- [ ] **步骤 1：扩展 `AgentContext`**

新增字段：

```java
private RagRetrieveResult ragRetrieveResult;
```

新增 import：

```java
import com.interview.coach.dto.RagRetrieveResult;
```

- [ ] **步骤 2：扩展 `AgentState`**

在 `OBSERVATION` 后增加 `RAG_RETRIEVAL`：

```java
PLANNING,
CODE_EXECUTION,
OBSERVATION,
RAG_RETRIEVAL,
ERROR_CLASSIFICATION,
CODE_REVIEW,
HINT_GENERATION,
MEMORY_UPDATE,
TRAINING_PLAN,
COMPLETED,
FAILED
```

- [ ] **步骤 3：新增 Tool**

```java
package com.interview.coach.agent.tool;

import com.interview.coach.agent.AgentContext;
import com.interview.coach.dto.RagRetrieveResult;
import com.interview.coach.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RagRetrieveTool implements Tool<AgentContext, RagRetrieveResult> {

    private static final int DEFAULT_LIMIT = 5;

    private final RagService ragService;

    @Override
    public String name() {
        return "RagRetrieveTool";
    }

    @Override
    public RagRetrieveResult execute(AgentContext input, AgentContext context) {
        RagRetrieveResult result = ragService.retrieveForDiagnosis(context, DEFAULT_LIMIT);
        context.setRagRetrieveResult(result);
        return result;
    }
}
```

- [ ] **步骤 4：在 Agent 中接入可选步骤**

在 `InterviewCoachAgent` 注入：

```java
private final RagRetrieveTool ragRetrieveTool;
```

在 `OBSERVATION` 后、AC/失败分支前增加：

```java
runOptionalStep(context, AgentState.RAG_RETRIEVAL, ragRetrieveTool.name(),
        "Retrieve problem knowledge and user learning memory",
        "RAG evidence ready", sink,
        () -> ragRetrieveTool.execute(context, context));
```

- [ ] **步骤 5：补充测试**

`RagRetrieveToolTest`：

- 验证会调用 `ragService.retrieveForDiagnosis`。
- 验证结果会写回 `AgentContext`。

`InterviewCoachAgentTest`：

- 验证 `RAG_RETRIEVAL` 出现在 `OBSERVATION` 之后。
- 验证 `RagRetrieveTool` 抛异常时，不影响 `ERROR_CLASSIFICATION` 或 `CODE_REVIEW` 后续步骤。

- [ ] **步骤 6：运行测试**

```powershell
cd backend
mvn -q -Dtest=RagRetrieveToolTest,InterviewCoachAgentTest test
```

预期结果：相关测试全部通过。

- [ ] **步骤 7：提交**

```powershell
git add backend/src/main/java/com/interview/coach/agent/tool/RagRetrieveTool.java backend/src/main/java/com/interview/coach/agent/AgentContext.java backend/src/main/java/com/interview/coach/enums/AgentState.java backend/src/main/java/com/interview/coach/agent/InterviewCoachAgent.java backend/src/test/java/com/interview/coach/agent/tool/RagRetrieveToolTest.java backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java
git commit -m "feat: add rag retrieval agent step"
```

---

## 9. 任务六：把 RAG 证据用于 AI Prompt

**文件：**

- 修改：`backend/src/main/java/com/interview/coach/agent/tool/ErrorClassifierTool.java`
- 修改：`backend/src/main/java/com/interview/coach/agent/tool/CodeReviewTool.java`
- 测试：可新增 `ErrorClassifierToolTest`，也可以在 `InterviewCoachAgentTest` 里用 fake AI client 捕获 prompt。

- [ ] **步骤 1：在 `ErrorClassifierTool` 中增加证据 helper**

```java
private String ragEvidence(AgentContext context) {
    if (context.getRagRetrieveResult() == null || !context.getRagRetrieveResult().hasHits()) {
        return "没有检索到可用证据。";
    }
    return context.getRagRetrieveResult().toPromptBlock();
}
```

- [ ] **步骤 2：把证据加入 user prompt**

在最终诊断指令前增加：

```text
Retrieved evidence:
%s
```

格式化参数中传入：

```java
ragEvidence(context)
```

- [ ] **步骤 3：强化 system prompt 边界**

在 `systemPrompt()` 加入：

```text
Use retrieved evidence only as supporting context.
Do not copy retrieved text verbatim when a short diagnosis is enough.
If retrieved evidence conflicts with execution result, trust execution result.
Do not provide a full accepted Java solution.
```

- [ ] **步骤 4：同样处理 `CodeReviewTool`**

AC 代码点评可以参考题目和知识卡证据，但仍然只做轻量代码点评，不生成完整标准答案。

- [ ] **步骤 5：运行 prompt 相关测试**

```powershell
cd backend
mvn -q -Dtest=InterviewCoachAgentTest test
```

预期结果：测试能确认有 RAG 命中时，证据文本会进入 AI prompt。

- [ ] **步骤 6：提交**

```powershell
git add backend/src/main/java/com/interview/coach/agent/tool/ErrorClassifierTool.java backend/src/main/java/com/interview/coach/agent/tool/CodeReviewTool.java backend/src/test/java/com/interview/coach/agent/InterviewCoachAgentTest.java
git commit -m "feat: ground ai prompts with rag evidence"
```

---

## 10. 任务七：训练计划优先使用 RAG 检索知识卡

**文件：**

- 修改：`backend/src/main/java/com/interview/coach/agent/tool/TrainingPlannerTool.java`
- 测试：`backend/src/test/java/com/interview/coach/agent/tool/TrainingPlannerToolTest.java`

- [ ] **步骤 1：提取 RAG 命中的知识卡**

在 `TrainingPlannerTool` 增加：

```java
private List<RagChunkHit> retrievedKnowledgeCards(AgentContext context) {
    if (context.getRagRetrieveResult() == null || !context.getRagRetrieveResult().hasHits()) {
        return List.of();
    }
    return context.getRagRetrieveResult().getHits().stream()
            .filter(hit -> "KNOWLEDGE_CARD".equals(hit.getSourceType()))
            .limit(KNOWLEDGE_CARD_LIMIT)
            .toList();
}
```

- [ ] **步骤 2：优先使用检索到的知识卡**

在 `addKnowledgeCards` 中先读取 RAG 命中的 `KNOWLEDGE_CARD`。如果没有命中，再保持当前 `knowledgeCardService.listReviewCards(KNOWLEDGE_CARD_LIMIT)` 兜底逻辑。

RAG 命中的知识卡训练项 reason 使用：

```text
结合本次错误知识点检索到的后端知识卡片，补充面试表达训练。
```

- [ ] **步骤 3：补充测试**

扩展 `TrainingPlannerToolTest`：

- 有 `KNOWLEDGE_CARD` RAG 命中时，训练计划包含该知识卡 id。
- 没有 RAG 命中时，仍然使用原有 fallback 知识卡。

- [ ] **步骤 4：运行测试**

```powershell
cd backend
mvn -q -Dtest=TrainingPlannerToolTest test
```

预期结果：训练计划测试通过。

- [ ] **步骤 5：提交**

```powershell
git add backend/src/main/java/com/interview/coach/agent/tool/TrainingPlannerTool.java backend/src/test/java/com/interview/coach/agent/tool/TrainingPlannerToolTest.java
git commit -m "feat: use rag evidence in training plans"
```

---

## 11. 任务八：前端显示 RAG 步骤，并保护页面边界

**文件：**

- 修改：`frontend/lib/i18n.ts`
- 修改：`frontend/lib/core-loop-stability.node-test.cjs`

- [ ] **步骤 1：新增 Agent 步骤中文名**

在 `agentStepMap` 中增加：

```ts
RAG_RETRIEVAL: "检索相关知识和历史错题",
```

- [ ] **步骤 2：确认右侧结果面板不变**

扩展 `frontend/lib/core-loop-stability.node-test.cjs`，继续断言：

```text
右侧结果面板只有“测试结果”和“AI 诊断”
没有单独的右侧“分层提示”tab
题目预设提示仍然在左侧题目面板
```

RAG 只作为 Agent 时间线步骤出现，不新增产品 tab。

- [ ] **步骤 3：运行前端回归测试**

```powershell
cd frontend
node lib/core-loop-stability.node-test.cjs
```

预期结果：回归测试通过。

- [ ] **步骤 4：提交**

```powershell
git add frontend/lib/i18n.ts frontend/lib/core-loop-stability.node-test.cjs
git commit -m "feat: show rag retrieval in agent timeline"
```

---

## 12. 任务九：增加索引重建入口

**文件：**

- 修改：`backend/src/main/java/com/interview/coach/service/RagService.java`
- 修改：`backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java`
- 测试：`backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java`

- [ ] **步骤 1：增加重建方法**

在 `RagService` 增加：

```java
void rebuildSystemIndex();
```

实现逻辑：

- 删除 `user_id IS NULL` 的 `rag_chunk` 和 `rag_document`。
- 读取启用状态的 `knowledge_card`。
- 读取可用的 `problem`。
- 调用 `indexKnowledgeCard` 和 `indexProblem`。
- 不删除用户自己的 `mistake_card` 和 `ai_diagnosis` 相关 chunk。

- [ ] **步骤 2：选择启动方式**

MVP 推荐做法：

```text
演示前通过后端测试或本地命令触发 rebuildSystemIndex。
```

暂时不要暴露公开 REST 接口。当前项目没有完整权限系统，公开索引重建接口在面试讲解中反而不好解释。

- [ ] **步骤 3：新增测试**

`rebuildSystemIndex_preservesUserMemoryChunks()`：

- 插入一条 `user_id=1` 的 `rag_chunk`。
- 执行 `rebuildSystemIndex()`。
- 断言用户 chunk 仍然存在。
- 断言系统题目 / 知识卡 chunk 被重新创建。

- [ ] **步骤 4：运行测试**

```powershell
cd backend
mvn -q -Dtest=RagServiceImplTest test
```

预期结果：索引重建测试通过。

- [ ] **步骤 5：提交**

```powershell
git add backend/src/main/java/com/interview/coach/service/RagService.java backend/src/main/java/com/interview/coach/service/impl/RagServiceImpl.java backend/src/test/java/com/interview/coach/service/impl/RagServiceImplTest.java
git commit -m "feat: add rag index rebuild"
```

---

## 13. 任务十：端到端验证

**文件：**

- 默认不需要继续改源码。
- 如果实际行为和本文档不一致，再同步更新项目文档。

- [ ] **步骤 1：运行后端测试**

```powershell
cd backend
mvn test
```

预期结果：后端测试全部通过。

- [ ] **步骤 2：运行前端回归测试**

```powershell
cd frontend
node lib/core-loop-stability.node-test.cjs
node lib/problem-hints-ui.node-test.cjs
```

预期结果：两个脚本都通过。

- [ ] **步骤 3：手动跑 demo**

启动本地依赖：

```text
MySQL
Piston
Spring Boot backend
Next.js frontend
```

验证流程：

```text
1. 打开 /problem/1。
2. 提交一个已知错误的 Two Sum 代码。
3. 确认 SSE 时间线包含：
   PLANNING -> CODE_EXECUTION -> OBSERVATION -> RAG_RETRIEVAL -> ERROR_CLASSIFICATION -> MEMORY_UPDATE -> TRAINING_PLAN -> COMPLETED
4. 确认 AI 诊断引用当前失败用例，不暴露完整 Java 答案。
5. 确认 Dashboard 仍然读取 MySQL 中的弱点、错题卡和训练计划。
6. 提交 AC 代码。
7. 确认 AC 代码点评仍然工作，并且不会写入弱点或错题卡。
```

- [ ] **步骤 4：检查数据库证据**

执行：

```sql
SELECT source_type, COUNT(*) FROM rag_chunk GROUP BY source_type;
SELECT step_name, status FROM agent_step WHERE step_name = 'RAG_RETRIEVAL' ORDER BY id DESC LIMIT 5;
```

预期结果：

- `rag_chunk` 中有 `PROBLEM` 和 `KNOWLEDGE_CARD` 数据。
- 失败提交后会产生用户维度的 `AI_DIAGNOSIS` 和 `MISTAKE_CARD` 数据。
- `agent_step` 中有 `RAG_RETRIEVAL` 步骤，状态为成功或可解释的失败。

- [ ] **步骤 5：如有文档变更则提交**

```powershell
git status --short
```

如果更新了文档：

```powershell
git add docs/API.md docs/PROJECT_STATUS.md docs/AI-Interview-Coach.md
git commit -m "docs: describe rag agent retrieval flow"
```

---

## 14. 面试讲解口径

可以这样介绍：

```text
我没有把 RAG 做成一个通用聊天页面，而是把它设计成 Agent Workflow 中的一个 Tool。
代码执行完成后，Agent 会先检索题目上下文、知识卡片和用户历史错题，
再把这些证据传给错误诊断或代码点评节点。
RAG 检索是可选步骤，即使索引或检索失败，也不会影响核心判题和诊断闭环。
```

重点设计：

- 第一版先用 MySQL 结构化检索，后续再扩展向量库。
- RAG 检索结果通过 `AgentStep` 可追踪。
- 用户历史记忆用 `user_id` 隔离。
- AI 诊断继续使用结构化 JSON。
- 题目预设提示继续留在左侧题目区域。
- 训练计划可以使用检索到的知识卡，但不把算法错误强行映射成无关八股。

---

## 15. V2 升级路线

V1 稳定后，再加 embedding 和向量检索：

```text
RagRetrieveTool
 -> RagService
 -> 先按 user/problem/error/knowledge 做结构化过滤
 -> 在候选集内做向量相似度排序
 -> 仍然返回同一个 RagRetrieveResult
```

V2 推荐新增：

- `rag_embedding` 表，或外部向量库 id。
- embedding 模型配置，放在 `integration/ai` 或独立 config 中。
- 向量库适配接口，例如 `RagVectorStore`。
- 向量相似度分数与 V1 确定性分数融合。

不要在 V1 demo 没稳定前启动 V2。

---

## 16. 自检结果

- 覆盖范围：已覆盖 V1 检索、索引、Agent 接入、prompt 注入、训练计划使用、前端时间线、测试和手动 demo。
- 范围控制：向量数据库、独立聊天、公有管理接口均排除在 V1 之外。
- 类型一致性：`RagRetrieveResult`、`RagChunkHit`、`RagService`、`RagRetrieveTool` 命名保持一致。
- MVP 纪律：实施路线保护现有“提交代码 -> 判题 -> 诊断 -> 记忆 -> 训练计划”的核心闭环。

