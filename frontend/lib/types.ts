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
  userId?: number;
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

export interface CacheChildStatus {
  enabled: boolean;
  provider: string | null;
  redisAvailable: boolean;
  statusLabel: string | null;
  summary: string | null;
  checkedAt: string | null;
  maintenanceAction: string | null;
  cachedKeyCount: number;
  hitCount: number;
  missCount: number;
  fallbackCount: number;
  hitRate: number;
  lastFallbackReason: string | null;
  probeWarning: string | null;
  fallback: string | null;
}

export interface CacheMaintenanceStatus {
  provider: string;
  problem: CacheChildStatus | null;
  knowledge: CacheChildStatus | null;
  allEnabled: boolean;
  allRedisAvailable: boolean;
  cachedKeyCount: number;
  hitCount: number;
  missCount: number;
  fallbackCount: number;
  hitRate: number;
  lastFallbackReason: string | null;
  probeWarning: string | null;
  statusLabel: string | null;
  summary: string | null;
  cacheBenefitSummary: string | null;
  fallbackRiskSummary: string | null;
  protectedDataSummary: string | null;
  checkedAt: string | null;
  maintenanceAction: string | null;
  boundary: string | null;
}

export interface CacheRefreshChildResult {
  enabled: boolean;
  redisAvailable: boolean;
  listWarmAttempted?: boolean;
  categoryWarmAttempted?: boolean;
  listWarmAttemptedCount?: number;
  detailWarmAttemptedCount?: number;
  templateWarmAttemptedCount?: number;
  totalWarmAttemptedCount: number;
  failedCount: number;
  message: string | null;
  summary: string | null;
  statusLabel: string | null;
  maintenanceAction: string | null;
  refreshedAt: string | null;
}

export interface CacheMaintenanceRefreshResult {
  problem: CacheRefreshChildResult | null;
  knowledge: CacheRefreshChildResult | null;
  totalWarmAttemptedCount: number;
  failedCount: number;
  statusLabel: string | null;
  maintenanceAction: string | null;
  message: string | null;
  summary: string | null;
  refreshScopeSummary: string | null;
  warmupResultSummary: string | null;
  protectedDataSummary: string | null;
  refreshedAt: string | null;
  boundary: string | null;
}

export interface RagHealth {
  healthy: boolean;
  tablesAvailable: boolean;
  statusLabel: string | null;
  maintenanceSummary: string | null;
  preferredMaintenanceAction: string | null;
  nextMaintenanceEndpoint: string | null;
  maintenancePriority: string | null;
  maintenanceReason: string | null;
  systemDocumentCount: number;
  systemChunkCount: number;
  enabledProblemCount: number;
  enabledKnowledgeCardCount: number;
  missingSystemProblemDocumentCount: number;
  missingSystemKnowledgeCardDocumentCount: number;
  userMemoryDocumentCount: number;
  userMemoryChunkCount: number;
  duplicateSystemDocumentCount: number;
  staleProblemDocumentCount: number;
  staleKnowledgeCardDocumentCount: number;
  documentSourceTypeCounts: Record<string, number>;
  chunkSourceTypeCounts: Record<string, number>;
  vectorEnabled: boolean;
  vectorIndexedChunkCount: number;
  vectorFailedChunkCount: number;
  vectorPendingChunkCount: number;
  warnings: string[];
  maintenanceActions: string[];
  checkedAt: string | null;
}

export interface RagSystemRebuildResult {
  attempted: boolean;
  success: boolean;
  vectorEnabled: boolean;
  indexedProblemCount: number;
  indexedKnowledgeCardCount: number;
  beforeSystemDocumentCount: number;
  afterSystemDocumentCount: number;
  beforeSystemChunkCount: number;
  afterSystemChunkCount: number;
  beforeUserMemoryDocumentCount: number;
  afterUserMemoryDocumentCount: number;
  beforeUserMemoryChunkCount: number;
  afterUserMemoryChunkCount: number;
  warnings: string[];
  rebuiltAt: string | null;
  statusLabel: string | null;
  maintenanceAction: string | null;
  message: string | null;
  summary: string | null;
}

export interface RagVectorRetryResult {
  enabled: boolean;
  requestedLimit: number;
  effectiveLimit: number;
  attemptedCount: number;
  matchedRetryableCount: number;
  indexedCount: number;
  failedCount: number;
  skippedCount: number;
  retriedAt: string | null;
  statusLabel: string | null;
  maintenanceAction: string | null;
  message: string | null;
  summary: string | null;
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
  statusLabel: string | null;
  items: TrainingPlanItem[];
}

export interface TrainingPlanHistory {
  id: number;
  title: string;
  summary: string | null;
  status: string;
  statusLabel: string | null;
  startDate: string | null;
  endDate: string | null;
  itemCount: number;
  completedCount: number;
  skippedCount: number;
  pendingCount: number;
  handledCount: number;
  completionRate: number;
  handledRate: number;
  createdAt: string | null;
}

export interface TrainingPlanActivity {
  itemId: number;
  planId: number;
  planTitle: string | null;
  itemType: "PROBLEM" | "KNOWLEDGE_CARD";
  taskTitle: string;
  knowledgePoint: string;
  sourceType?: string | null;
  sourceSummary?: string | null;
  learningImpactSummary?: string | null;
  status: "COMPLETED" | "SKIPPED";
  statusLabel: string | null;
  statusUpdatedAt: string | null;
}

export interface TrainingPlanTrace {
  planId: number | null;
  title: string | null;
  summary: string | null;
  status: string | null;
  statusLabel: string | null;
  startDate: string | null;
  endDate: string | null;
  planCreatedAt: string | null;
  daysSinceCreated: number;
  daysRemaining: number;
  overdue: boolean;
  itemCount: number;
  pendingCount: number;
  completedCount: number;
  skippedCount: number;
  handledCount: number;
  completionRate: number;
  handledRate: number;
  progressSummary: string | null;
  sourceTypeCounts: Record<string, number>;
  sourceTypeSummary: string | null;
  nextItem: TrainingPlanItem | null;
  nextAction: string | null;
  nextActionReason: string | null;
  nextActionPriority: string | null;
  nextTargetHref: string | null;
  nextTargetLabel: string | null;
  recentActivities: TrainingPlanActivity[];
  latestActivitySummary: string | null;
  latestActivityAt: string | null;
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
  sourceType?: string | null;
  sourceId?: number | null;
  sourceSummary?: string | null;
  targetHref?: string | null;
  targetLabel?: string | null;
  status: string;
  statusUpdatedAt?: string | null;
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

// 知识库问答
export interface RagChatRequest {
  userId: number;
  question: string;
}

export interface RagChatSource {
  sourceType: string;
  sourceId: number | null;
  title: string | null;
  score: number;
  snippet: string;
  matchReason: string;
}

export interface RagChatResponse {
  answer: string;
  sources: RagChatSource[];
}

// 模拟面试
export interface MockInterviewCreateRequest {
  userId: number;
  category: string;
  questionCount: number;
  interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN";
}

export interface MockInterviewAnswerRequest {
  userAnswer: string;
}

export interface MockInterviewTurn {
  id: number;
  knowledgeCardId: number;
  turnOrder: number;
  turnType: "MAIN" | "FOLLOW_UP";
  parentTurnId: number | null;
  question: string;
  userAnswer: string;
  score: number;
  feedback: string;
  performanceLevel: string;
  strengthSummary: string;
  gapSummary: string;
  expressionFeedback: string;
  interviewerObservation: string;
  followUpReason: string;
  hitKeyPoints: string[];
  missingKeyPoints: string[];
  expressionIssue: string | null;
  createdAt: string | null;
}

export interface MockInterviewReport {
  id: number;
  averageScore: number;
  summary: string;
  strengths: string;
  weaknesses: string;
  expressionAdvice: string;
  recommendedCardIds: number[];
  weaknessTags: string[];
  trainingPlanLinked: boolean;
  trainingPlanItemCount: number;
  reviewPathSummary: string;
  createdAt: string | null;
}

export interface MockInterviewRecent {
  sessionId: number;
  category: string;
  status:
    | "CREATED"
    | "ASKING_MAIN"
    | "MAIN_ANSWERED"
    | "ASKING_FOLLOW_UP"
    | "FOLLOW_UP_ANSWERED"
    | "NEXT_QUESTION"
    | "FINISHED"
    | "REPORTED";
  interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN";
  questionCount: number;
  answeredMainCount: number;
  averageScore: number | null;
  weaknessTags: string[];
  startedAt: string | null;
  finishedAt: string | null;
  createdAt: string | null;
}

export interface MockInterviewTrend {
  knowledgeCardId: number;
  knowledgePoint: string;
  category: string;
  latestSessionId: number;
  latestScore: number;
  previousScore: number | null;
  deltaScore: number;
  trendLabel: string;
  interviewCount: number;
  latestIssue: string | null;
  latestIssueType: string | null;
  latestIssueTypeLabel: string | null;
  lastInterviewAt: string | null;
}

export interface MockInterviewTrace {
  sessionCount: number;
  reportedSessionCount: number;
  latestSessionId: number | null;
  latestSessionStatus: string | null;
  latestSessionStatusLabel: string | null;
  latestCategory: string | null;
  latestReportId: number | null;
  latestAverageScore: number | null;
  latestWeaknessTags: string[];
  recommendedCardIds: number[];
  answeredTurnCount: number;
  lowScoreTurnCount: number;
  weaknessEventCount: number;
  trainingPlanItemCount: number;
  reportTrainingPlanLinked: boolean;
  latestInterviewAt: string | null;
  closureStatus: string | null;
  closureStatusLabel: string | null;
  nextAction: string | null;
  nextActionReason: string | null;
  nextActionPriority: string | null;
  nextTargetHref: string | null;
  nextTargetLabel: string | null;
  reportReviewHref: string | null;
  reportReviewLabel: string | null;
  closureSummary: string | null;
  reviewPathSummary: string | null;
}

export interface MockInterviewSession {
  sessionId: number;
  status:
    | "CREATED"
    | "ASKING_MAIN"
    | "MAIN_ANSWERED"
    | "ASKING_FOLLOW_UP"
    | "FOLLOW_UP_ANSWERED"
    | "NEXT_QUESTION"
    | "FINISHED"
    | "REPORTED";
  category: string;
  interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN";
  questionCount: number;
  answeredMainCount: number;
  currentKnowledgeCardId: number | null;
  currentQuestion: string | null;
  currentTurnType: "MAIN" | "FOLLOW_UP" | null;
  turns: MockInterviewTurn[];
  report: MockInterviewReport | null;
  startedAt: string | null;
  finishedAt: string | null;
}

// Auth
export interface AuthUser {
  id: number;
  username: string;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}
