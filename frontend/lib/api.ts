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
} from "./types";

const API_BASE = "http://localhost:8080";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, {
    ...init,
    cache: "no-store",
  });
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
        let buffer = "";

        const processLine = (line: string, event: string): string => {
          if (line.startsWith("event:")) {
            return line.slice(6).trim();
          }
          if (line.startsWith("data:")) {
            const data = line.slice(5).trim();
            if (!data) return "";

            try {
              const parsed = JSON.parse(data);

              if (event === "agent_step") {
                callbacks.onStep(parsed);
              } else if (event === "done") {
                const finalResult = parsed?.data ?? parsed;
                console.log("[SSE done raw]", parsed);
                console.log("[SSE done finalResult]", finalResult);
                if (!finalResult || typeof finalResult !== "object") {
                  callbacks.onError("SSE done 事件缺少诊断数据");
                } else {
                  callbacks.onDone(finalResult as AgentAnalyzeVO);
                }
              } else if (event === "error") {
                callbacks.onError(parsed.message || "Agent 诊断失败");
              }
            } catch {
              // 忽略无法解析的行
            }

            return "";
          }
          return event;
        };

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() || "";

          let currentEvent = "";
          for (const line of lines) {
            currentEvent = processLine(line, currentEvent);
          }
        }

        // 处理流结束后 buffer 中剩余的内容
        buffer += decoder.decode();
        if (buffer.trim()) {
          const lines = buffer.split("\n");
          let currentEvent = "";
          for (const line of lines) {
            currentEvent = processLine(line, currentEvent);
          }
        }

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
  recentSubmissions: (userId: number) =>
    request<ApiResponse<SubmissionHistoryVO[]>>(`/api/users/${userId}/submissions/recent`),
};
