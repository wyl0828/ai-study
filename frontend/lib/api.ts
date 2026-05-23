import type {
  ApiResponse,
  ProblemListItem,
  ProblemDetail,
  ProblemTemplate,
  SubmitCodeRequest,
  SubmissionResult,
  AgentAnalyzeVO,
  AgentStepVO,
  DashboardStatsVO,
  UserWeakness,
  MistakeCard,
  TrainingPlan,
  SubmissionHistoryVO,
  ErrorStatsVO,
  KnowledgeCategory,
  KnowledgeCardDetail,
  KnowledgeCardListItem,
  RagChatRequest,
  RagChatResponse,
  MockInterviewAnswerRequest,
  MockInterviewCreateRequest,
  MockInterviewRecent,
  MockInterviewSession,
  SelfTestRecord,
  SelfTestSubmitRequest,
} from "./types";

const API_BASE = "http://localhost:8080";

type ApiErrorContext =
  | "backend"
  | "template"
  | "knowledge"
  | "sse"
  | "submit"
  | "dashboard"
  | "rag"
  | "mockInterview";

export function formatApiError(
  error: unknown,
  context: ApiErrorContext = "backend"
): string {
  const raw =
    error instanceof Error
      ? error.message
      : typeof error === "string"
      ? error
      : "";
  const detail = raw ? `（${raw}）` : "";
  const backendUnreachable =
    /failed to fetch|fetch failed|networkerror|load failed|econnrefused|后端服务不可达/i.test(
      raw
    );
  const pistonFailure = /piston|code execution|代码执行|execute/i.test(raw);
  const aiFailure = /ai request|anthropic|api[_ -]?key|ai_base|ai_model|模型|AI 调用/i.test(raw);

  if (backendUnreachable) {
    return "后端服务不可达，请检查 Spring Boot 是否已启动并监听 localhost:8080。";
  }

  if (context === "template") {
    return `代码模板接口加载失败，已尝试保留本地草稿；请检查题目模板接口是否正常。${detail}`;
  }

  if (context === "knowledge") {
    return `知识卡接口暂不可用，当前使用本地示例或已有列表兜底。${detail}`;
  }

  if (context === "sse") {
    if (aiFailure) {
      return `AI 诊断调用失败，请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL；同步 fallback 会尝试补救，并可查看后端 Agent 日志。${detail}`;
    }
    return `AI 诊断 SSE/AI 调用失败，可稍后重试；同步 fallback 会尝试补救，请查看后端 Agent 日志定位失败步骤。${detail}`;
  }

  if (context === "submit") {
    if (pistonFailure) {
      return `提交接口请求失败，请检查 Piston 服务地址、端口和 PISTON_BASE_URL 配置是否正常。${detail}`;
    }
    return `提交接口请求失败，请检查后端服务和 Piston 代码执行服务是否正常。${detail}`;
  }

  if (context === "dashboard") {
    return `仪表盘接口加载失败，请检查后端服务是否正常。${detail}`;
  }

  if (context === "rag") {
    if (aiFailure) {
      return `知识库问答调用 AI 失败，请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL 配置。${detail}`;
    }
    return `知识库问答接口暂不可用，请检查 Spring Boot 服务、RAG 表迁移和 AI 配置是否正常。${detail}`;
  }

  if (context === "mockInterview") {
    if (aiFailure) {
      return `模拟面试 AI 评分暂不可用，后端会尝试使用 keyPoints fallback；请检查 AI_BASE_URL、AI_API_KEY 和 AI_MODEL。${detail}`;
    }
    return `模拟面试接口暂不可用，请检查 Spring Boot 服务和 mock_interview 表迁移是否正常。${detail}`;
  }

  return raw || "接口请求失败，请稍后重试。";
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${url}`, {
      ...init,
      cache: "no-store",
    });
  } catch (err) {
    throw new Error(formatApiError(err));
  }
  if (!res.ok) {
    throw new Error(`请求失败：${res.status}`);
  }
  const json = await res.json();
  if (typeof json.code !== "undefined" && json.code !== 0) {
    throw new Error(json.message || "接口返回异常");
  }
  return json;
}

export const problemApi = {
  list: () =>
    request<ApiResponse<ProblemListItem[]>>("/api/problems"),
  detail: (id: number) =>
    request<ApiResponse<ProblemDetail>>(`/api/problems/${id}`),
  template: (id: number) =>
    request<ApiResponse<ProblemTemplate>>(`/api/problems/${id}/template`),
};

export const submissionApi = {
  submit: (body: SubmitCodeRequest) =>
    request<ApiResponse<SubmissionResult>>("/api/submissions", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
};

export const agentApi = {
  analyze: (submissionId: number) =>
    request<ApiResponse<AgentAnalyzeVO>>("/api/agent/analyze", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ submissionId }),
    }),

  streamDiagnosis: (
    submissionId: number,
    callbacks: {
      onStep: (step: AgentStepVO) => void;
      onDone: (result: AgentAnalyzeVO) => void;
      onError: (message: string) => void;
      onEnd: () => void;
    },
    options?: { token?: string }
  ): AbortController => {
    const controller = new AbortController();

    fetch(`${API_BASE}/api/submissions/${submissionId}/diagnosis/stream`, {
      method: "GET",
      headers: {
        Accept: "text/event-stream",
        ...(options?.token
          ? { Authorization: `Bearer ${options.token}` }
          : {}),
      },
      signal: controller.signal,
    })
      .then(async (res) => {
        if (!res.ok) {
          throw new Error(`SSE 请求失败：${res.status}`);
        }

        const reader = res.body?.getReader();
        if (!reader) {
          throw new Error("无法读取 SSE 流");
        }

        const decoder = new TextDecoder();
        let raw = "";

        const dispatch = (block: string) => {
          const evMatch = block.match(/^event:\s*(.+)$/m);
          const dataMatch = block.match(/^data:\s*(.+)$/m);
          const event = evMatch ? evMatch[1].trim() : "";
          const data = dataMatch ? dataMatch[1].trim() : "";
          if (!data) return;

          try {
            const parsed = JSON.parse(data);
            if (event === "agent_step") {
              callbacks.onStep(parsed);
            } else if (event === "done") {
              const finalResult = parsed?.data ?? parsed;
              if (!finalResult || typeof finalResult !== "object") {
                callbacks.onError("SSE done 事件缺少诊断数据");
              } else {
                callbacks.onDone(finalResult as AgentAnalyzeVO);
              }
            } else if (event === "error") {
              callbacks.onError(parsed.message || "Agent 诊断失败");
            }
          } catch {
            // ignore unparseable blocks
          }
        };

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;
          raw += decoder.decode(value, { stream: true });

          // 按空行分割完整 event block
          const parts = raw.split(/\n\n+/);
          raw = parts.pop() || ""; // 最后一个可能是不完整的 block

          for (const block of parts) {
            if (block.trim()) dispatch(block);
          }
        }

        // 流结束后处理残余
        raw += decoder.decode();
        if (raw.trim()) dispatch(raw);

        // 流正常结束，通知调用方
        callbacks.onEnd();
      })
      .catch((err) => {
        if (err.name === "AbortError") return;
        callbacks.onError(err.message || "SSE 连接失败");
      });

    return controller;
  },
};

export const userApi = {
  stats: (userId: number) =>
    request<ApiResponse<DashboardStatsVO>>(`/api/users/${userId}/dashboard/stats`),
  weaknesses: (userId: number) =>
    request<ApiResponse<UserWeakness[]>>(`/api/users/${userId}/weaknesses`),
  mistakes: (userId: number) =>
    request<ApiResponse<MistakeCard[]>>(`/api/users/${userId}/mistakes`),
  latestPlan: (userId: number) =>
    request<ApiResponse<TrainingPlan | null>>(`/api/users/${userId}/training-plans/latest`),
  updateTrainingPlanItemStatus: (
    userId: number,
    itemId: number,
    status: "PENDING" | "COMPLETED" | "SKIPPED"
  ) =>
    request<ApiResponse<null>>(`/api/users/${userId}/training-plans/items/${itemId}/status`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ status }),
    }),
  regenerateTrainingPlan: (userId: number) =>
    request<ApiResponse<null>>(`/api/users/${userId}/training-plans/regenerate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ replaceCurrentPlan: true, reason: "USER_REQUEST" }),
    }),
  submitSelfTest: (userId: number, cardId: number, body: SelfTestSubmitRequest) =>
    request<ApiResponse<SelfTestRecord>>(`/api/users/${userId}/knowledge/cards/${cardId}/self-tests`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  recentSelfTests: (userId: number, cardId: number) =>
    request<ApiResponse<SelfTestRecord[]>>(
      `/api/users/${userId}/knowledge/cards/${cardId}/self-tests/recent`
    ),
  recentSubmissions: (userId: number) =>
    request<ApiResponse<SubmissionHistoryVO[]>>(`/api/users/${userId}/submissions/recent`),
  recentMockInterviews: (userId: number) =>
    request<ApiResponse<MockInterviewRecent[]>>(`/api/users/${userId}/mock-interviews/recent`),
  errorStats: (userId: number) =>
    request<ApiResponse<ErrorStatsVO>>(`/api/users/${userId}/dashboard/error-stats`),
};

export const knowledgeApi = {
  categories: () =>
    request<ApiResponse<KnowledgeCategory[]>>("/api/knowledge/categories"),
  cards: (category?: string) =>
    request<ApiResponse<KnowledgeCardListItem[]>>(
      category && category !== "ALL"
        ? `/api/knowledge/cards?category=${encodeURIComponent(category)}`
        : "/api/knowledge/cards"
    ),
  detail: (id: number) =>
    request<ApiResponse<KnowledgeCardDetail>>(`/api/knowledge/cards/${id}`),
};

export const ragChatApi = {
  ask: (body: RagChatRequest) =>
    request<ApiResponse<RagChatResponse>>("/api/rag/chat", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
};

export const mockInterviewApi = {
  create: (body: MockInterviewCreateRequest) =>
    request<ApiResponse<MockInterviewSession>>("/api/mock-interviews", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  get: (sessionId: number) =>
    request<ApiResponse<MockInterviewSession>>(`/api/mock-interviews/${sessionId}`),
  answer: (sessionId: number, body: MockInterviewAnswerRequest) =>
    request<ApiResponse<MockInterviewSession>>(`/api/mock-interviews/${sessionId}/answers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),
  finish: (sessionId: number) =>
    request<ApiResponse<MockInterviewSession>>(`/api/mock-interviews/${sessionId}/finish`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
    }),
};
