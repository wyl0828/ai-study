import type {
  ApiResponse,
  ProblemListItem,
  ProblemDetail,
  ProblemTemplate,
  SubmitCodeRequest,
  SubmissionResult,
  AgentAnalyzeVO,
  DashboardStatsVO,
  UserWeakness,
  MistakeCard,
  TrainingPlan,
  SubmissionHistoryVO,
} from "./types";

const API_BASE = "http://localhost:8080";

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE}${url}`, init);
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
