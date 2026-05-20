// 通用响应
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// 题目
export interface ProblemListItem {
  id: number;
  title: string;
  difficulty: string;
  category: string;
}

export interface HomeProblem extends ProblemListItem {
  description?: string;
  knowledgePoints?: string[];
}

export interface ProblemDetail {
  id: number;
  title: string;
  description: string;
  difficulty: string;
  category: string;
  inputFormat: string;
  outputFormat: string;
  knowledgePoints: string[];
  sampleCases: TestCase[];
  solutionOutline?: string | null;
  presetHints?: {
    level1: string;
    level2: string;
    level3: string;
  };
}

export interface TestCase {
  id: number;
  input: string;
  expectedOutput: string;
  sample: boolean;
}

export interface ProblemTemplate {
  problemId: number;
  language: string;
  templateCode: string;
}

// 提交
export interface SubmitCodeRequest {
  userId: number;
  problemId: number;
  language: string;
  code: string;
}

export interface SubmissionResult {
  submissionId: number;
  status: string;
  passedCount: number;
  totalCount: number;
  runtime: number | null;
  memory: number | null;
  errorMessage: string | null;
  failedCases: FailedCase[];
}

export interface FailedCase {
  caseId: number;
  input: string;
  expectedOutput: string;
  actualOutput: string;
}

// Agent 诊断
export interface AgentAnalyzeRequest {
  submissionId: number;
}

export interface CodeReviewResult {
  complexity: string;
  codeStyle: string;
  interviewSuggestion: string;
  optimizationPoints: string[];
}

export interface AgentAnalyzeVO {
  agentRunId: number;
  submissionId: number;
  errorType: string;
  knowledgePoint: string;
  specificError: string;
  diagnosis: string;
  suggestion?: string | null;
  failurePhenomenon?: string | null;
  rootCause?: string | null;
  repairDirection?: string | null;
  interviewReminder?: string | null;
  codeReview?: CodeReviewResult;
  hintLevel1: string;
  hintLevel2: string;
  hintLevel3: string;
  trainingPlanTitle: string;
  steps: AgentStepVO[];
}

export interface AgentStepVO {
  stepName: string;
  toolName: string | null;
  status: string;
  inputSummary: string;
  outputSummary: string | null;
  durationMs: number | null;
  errorMessage: string | null;
}

// Dashboard
export interface DashboardStatsVO {
  totalSubmissions: number;
  passedProblems: number;
  weakPointCount: number;
  mistakeCount: number;
}

export interface UserWeakness {
  id: number;
  knowledgePoint: string;
  errorType: string;
  wrongCount: number;
  weaknessScore: number;
  trendLabel?: string | null;
  lastDeltaScore?: number | null;
  lastEventAt?: string | null;
}

export interface MistakeCard {
  id: number;
  problemId: number;
  problemTitle: string;
  errorType: string;
  knowledgePoint: string;
  mistakeSummary: string;
  correctIdea: string;
  repeatCount?: number | null;
  lastSeenAt?: string | null;
  status?: string | null;
}

export interface TrainingPlan {
  id: number;
  title: string;
  summary: string;
  status: string;
  items: TrainingPlanItem[];
}

export interface TrainingPlanItem {
  id: number;
  itemType: "PROBLEM" | "KNOWLEDGE_CARD";
  problemId?: number | null;
  knowledgeCardId?: number | null;
  dayIndex: number;
  knowledgePoint: string;
  problemTitle: string | null;
  knowledgeCardTitle?: string | null;
  reason: string;
  reviewFocus: string;
  status: string;
}

export interface SelfTestRecord {
  id: number;
  knowledgeCardId: number;
  score: number;
  feedback: string | null;
  missingKeyPoints: string[];
  createdAt: string | null;
}

export interface SelfTestSubmitRequest {
  userAnswer: string;
  score: number;
  feedback: string;
  missingKeyPoints: string[];
}

export interface SubmissionHistoryVO {
  problemId: number;
  problemTitle: string;
  status: string;
  passedCount: number;
  totalCount: number;
  createdAt: string | null;
}

// 错误统计
export interface ErrorTypeCount {
  errorType: string;
  count: number;
}

export interface KnowledgeWeakness {
  knowledgePoint: string;
  errorType: string;
  wrongCount: number;
  weaknessScore: number;
}

export interface ErrorStatsVO {
  errorTypeDistribution: ErrorTypeCount[];
  topWeakPoints: KnowledgeWeakness[];
}

// 后端知识训练
export interface KnowledgeCategory {
  category: string;
  label: string;
  count: number;
}

export interface KnowledgeCardListItem {
  id: number;
  category: string;
  label: string;
  title: string;
  question: string;
  answer?: string | null;
  followUp?: string | null;
  difficulty: string;
  keyPoints: string[];
  tags: string[];
  sourceName: string | null;
  sourceUrl: string | null;
}

export interface KnowledgeCardDetail extends KnowledgeCardListItem {
  answer: string | null;
  followUp: string | null;
  keyPoints: string[];
}
