"use client";

import { useEffect, useMemo, useState } from "react";
import { Lightbulb } from "lucide-react";
import { cacheApi, formatApiError, ragMaintenanceApi, userApi } from "@/lib/api";
import type {
  CacheMaintenanceStatus,
  DashboardStatsVO,
  ErrorStatsVO,
  MistakeCard as MistakeCardType,
  MockInterviewRecent,
  MockInterviewTrace,
  MockInterviewTrend,
  RagHealth,
  SubmissionHistoryVO,
  TrainingPlanActivity,
  TrainingPlanHistory,
  TrainingPlanTrace,
  TrainingPlan as TrainingPlanType,
  UserWeakness,
} from "@/lib/types";
import WeaknessList from "@/components/WeaknessList";
import SubmissionHistory from "@/components/SubmissionHistory";
import MistakeCards from "@/components/MistakeCards";
import TrainingPlan from "@/components/TrainingPlan";
import TodayTrainingFocus from "@/components/TodayTrainingFocus";
import DashboardNextActions from "@/components/DashboardNextActions";
import ErrorStats from "@/components/ErrorStats";
import KnowledgeTrainingEntry from "@/components/KnowledgeTrainingEntry";
import {
  buildDashboardCoachAdvice,
  buildDashboardNextActions,
  groupMistakeCards,
  groupWeaknesses,
  trainingPlanActivitySourceText,
  trainingPlanSourceLabel,
} from "@/lib/learningView";

const DEMO_USER_ID = 1;

const emptyStats: DashboardStatsVO = {
  totalSubmissions: 0,
  passedProblems: 0,
  weakPointCount: 0,
  mistakeCount: 0,
};

const mockInterviewCategoryLabels: Record<string, string> = {
  JAVA: "Java",
  JVM: "JVM",
  SPRING: "Spring",
  MYSQL: "MySQL",
  REDIS: "Redis",
  AI: "系统设计",
  PROJECT: "系统设计",
};

function mockInterviewStatusLabel(status: MockInterviewRecent["status"]) {
  if (status === "REPORTED") return "已生成报告";
  if (status === "FINISHED") return "已完成";
  return "进行中";
}

function trainingPlanStatusLabel(status: string) {
  if (status === "ACTIVE") return "进行中";
  if (status === "COMPLETED") return "已完成";
  if (status === "REGENERATED") return "已重新生成";
  return status;
}

function trainingActivityStatusLabel(status: TrainingPlanActivity["status"]) {
  if (status === "COMPLETED") return "已完成";
  if (status === "SKIPPED") return "已跳过";
  return status;
}

function formatActivityTime(value: string | null) {
  if (!value) return "";
  return value.replace("T", " ").slice(0, 16);
}

function sourceTypeSummary(counts: Record<string, number> | null | undefined) {
  if (!counts) return "暂无来源";
  const entries = Object.entries(counts).filter(([, count]) => count > 0);
  if (entries.length === 0) return "暂无来源";
  return entries
    .slice(0, 3)
    .map(([sourceType, count]) => `${trainingPlanSourceLabel(sourceType)} ${count}`)
    .join(" · ");
}

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStatsVO>(emptyStats);
  const [weaknesses, setWeaknesses] = useState<UserWeakness[]>([]);
  const [mistakes, setMistakes] = useState<MistakeCardType[]>([]);
  const [trainingPlan, setTrainingPlan] = useState<TrainingPlanType | null>(null);
  const [trainingPlanHistory, setTrainingPlanHistory] = useState<TrainingPlanHistory[]>([]);
  const [trainingActivities, setTrainingActivities] = useState<TrainingPlanActivity[]>([]);
  const [trainingPlanTrace, setTrainingPlanTrace] = useState<TrainingPlanTrace | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionHistoryVO[]>([]);
  const [mockInterviews, setMockInterviews] = useState<MockInterviewRecent[]>([]);
  const [mockInterviewTrace, setMockInterviewTrace] = useState<MockInterviewTrace | null>(null);
  const [mockInterviewTrends, setMockInterviewTrends] = useState<MockInterviewTrend[]>([]);
  const [errorStats, setErrorStats] = useState<ErrorStatsVO | null>(null);
  const [cacheStatus, setCacheStatus] = useState<CacheMaintenanceStatus | null>(null);
  const [ragHealth, setRagHealth] = useState<RagHealth | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [regeneratingPlan, setRegeneratingPlan] = useState(false);
  const [trainingPlanRefreshing, setTrainingPlanRefreshing] = useState(false);
  const [mockInterviewRefreshing, setMockInterviewRefreshing] = useState(false);
  const [cacheMaintenanceRunning, setCacheMaintenanceRunning] = useState<string | null>(null);
  const [cacheMaintenanceResult, setCacheMaintenanceResult] = useState<string | null>(null);
  const [ragMaintenanceRunning, setRagMaintenanceRunning] = useState<string | null>(null);
  const [ragMaintenanceResult, setRagMaintenanceResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      setLoading(true);
      setError(null);
      try {
        const [
          statsResponse,
          weaknessesResponse,
          mistakesResponse,
          planResponse,
          planHistoryResponse,
          planActivitiesResponse,
          planTraceResponse,
          submissionsResponse,
          mockInterviewsResponse,
          mockInterviewTraceResponse,
          mockInterviewTrendsResponse,
          errorStatsResponse,
          cacheStatusResponse,
          ragHealthResponse,
        ] = await Promise.all([
          userApi.stats(DEMO_USER_ID),
          userApi.weaknesses(DEMO_USER_ID),
          userApi.mistakes(DEMO_USER_ID),
          userApi.latestPlan(DEMO_USER_ID),
          userApi.trainingPlanHistory(DEMO_USER_ID),
          userApi.trainingPlanActivities(DEMO_USER_ID),
          userApi.trainingPlanTrace(DEMO_USER_ID).catch(() => null),
          userApi.recentSubmissions(DEMO_USER_ID),
          userApi.recentMockInterviews(DEMO_USER_ID).catch(() => null),
          userApi.mockInterviewTrace(DEMO_USER_ID).catch(() => null),
          userApi.mockInterviewTrends(DEMO_USER_ID).catch(() => null),
          userApi.errorStats(DEMO_USER_ID),
          cacheApi.status().catch(() => null),
          ragMaintenanceApi.health().catch(() => null),
        ]);

        if (cancelled) {
          return;
        }

        setStats(statsResponse.data);
        setWeaknesses(weaknessesResponse.data);
        setMistakes(mistakesResponse.data);
        setTrainingPlan(planResponse.data);
        setTrainingPlanHistory(planHistoryResponse.data);
        setTrainingActivities(planActivitiesResponse.data);
        setTrainingPlanTrace(planTraceResponse?.data ?? null);
        setSubmissions(submissionsResponse.data);
        setMockInterviews(mockInterviewsResponse?.data ?? []);
        setMockInterviewTrace(mockInterviewTraceResponse?.data ?? null);
        setMockInterviewTrends(mockInterviewTrendsResponse?.data ?? []);
        setErrorStats(errorStatsResponse.data);
        setCacheStatus(cacheStatusResponse?.data ?? null);
        setRagHealth(ragHealthResponse?.data ?? null);
      } catch (err) {
        if (!cancelled) {
          setError(formatApiError(err, "dashboard"));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadDashboard();

    return () => {
      cancelled = true;
    };
  }, []);

  const aggregatedWeaknesses = useMemo(() => groupWeaknesses(weaknesses), [weaknesses]);
  const aggregatedMistakeCards = useMemo(() => groupMistakeCards(mistakes), [mistakes]);
  const statValue = (value: number) => (loading ? "--" : value);
  const weakPointCount = aggregatedWeaknesses.length;
  const coachAdvice = buildDashboardCoachAdvice({
    weaknesses: aggregatedWeaknesses,
    mistakes: aggregatedMistakeCards,
    trainingPlan,
  });
  const nextActions = buildDashboardNextActions({
    trainingPlan,
    trainingPlanTrace,
    mockInterviewTrace,
    mockInterviewTrends,
  });

  const loadTrainingPlanState = async () => {
    const [planResponse, historyResponse, activitiesResponse, traceResponse] = await Promise.all([
      userApi.latestPlan(DEMO_USER_ID),
      userApi.trainingPlanHistory(DEMO_USER_ID),
      userApi.trainingPlanActivities(DEMO_USER_ID),
      userApi.trainingPlanTrace(DEMO_USER_ID).catch(() => null),
    ]);
    setTrainingPlan(planResponse.data);
    setTrainingPlanHistory(historyResponse.data);
    setTrainingActivities(activitiesResponse.data);
    setTrainingPlanTrace(traceResponse?.data ?? null);
  };

  const refreshPlan = async () => {
    setTrainingPlanRefreshing(true);
    setError(null);
    try {
      await loadTrainingPlanState();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setTrainingPlanRefreshing(false);
    }
  };

  const loadMockInterviewState = async () => {
    const [recentResponse, traceResponse, trendsResponse] = await Promise.all([
      userApi.recentMockInterviews(DEMO_USER_ID).catch(() => null),
      userApi.mockInterviewTrace(DEMO_USER_ID).catch(() => null),
      userApi.mockInterviewTrends(DEMO_USER_ID).catch(() => null),
    ]);
    setMockInterviews(recentResponse?.data ?? []);
    setMockInterviewTrace(traceResponse?.data ?? null);
    setMockInterviewTrends(trendsResponse?.data ?? []);
  };

  const refreshMockInterviewState = async () => {
    setMockInterviewRefreshing(true);
    setError(null);
    try {
      await loadMockInterviewState();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setMockInterviewRefreshing(false);
    }
  };

  const loadCacheStatus = async () => {
    const statusResponse = await cacheApi.status();
    setCacheStatus(statusResponse.data);
  };

  const refreshCacheStatus = async () => {
    setCacheMaintenanceRunning("status");
    setError(null);
    try {
      await loadCacheStatus();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setCacheMaintenanceRunning(null);
    }
  };

  const refreshHotCache = async () => {
    setCacheMaintenanceRunning("refresh");
    setCacheMaintenanceResult(null);
    setError(null);
    try {
      const response = await cacheApi.refresh();
      setCacheMaintenanceResult(
        response.data.warmupResultSummary
          || response.data.summary
          || response.data.message
          || "热点缓存刷新完成。"
      );
      try {
        await loadCacheStatus();
      } catch {
        setError("热点缓存刷新完成，但缓存状态回读失败；可稍后点击“刷新缓存状态”确认最新状态。");
      }
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setCacheMaintenanceRunning(null);
    }
  };

  const loadRagHealth = async () => {
    const healthResponse = await ragMaintenanceApi.health();
    setRagHealth(healthResponse.data);
  };

  const refreshRagHealth = async () => {
    setRagMaintenanceRunning("status");
    setError(null);
    try {
      await loadRagHealth();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setRagMaintenanceRunning(null);
    }
  };

  const rebuildRagSystemIndex = async () => {
    setRagMaintenanceRunning("rebuild");
    setRagMaintenanceResult(null);
    setError(null);
    try {
      const response = await ragMaintenanceApi.rebuildSystemIndex();
      setRagMaintenanceResult(response.data.summary || response.data.message || "系统索引重建完成。");
      try {
        await loadRagHealth();
      } catch {
        setError("RAG 维护已完成，但状态回读失败；可稍后点击“刷新 RAG 状态”确认最新状态。");
      }
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setRagMaintenanceRunning(null);
    }
  };

  const retryRagFailedVectors = async () => {
    setRagMaintenanceRunning("retry");
    setRagMaintenanceResult(null);
    setError(null);
    try {
      const response = await ragMaintenanceApi.retryFailedVectors(50);
      setRagMaintenanceResult(response.data.summary || response.data.message || "失败向量重试完成。");
      try {
        await loadRagHealth();
      } catch {
        setError("RAG 维护已完成，但状态回读失败；可稍后点击“刷新 RAG 状态”确认最新状态。");
      }
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setRagMaintenanceRunning(null);
    }
  };

  const updatePlanItemStatus = async (
    itemId: number,
    status: "PENDING" | "COMPLETED" | "SKIPPED"
  ) => {
    setUpdatingItemId(itemId);
    setError(null);
    try {
      await userApi.updateTrainingPlanItemStatus(DEMO_USER_ID, itemId, status);
      await refreshPlan();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setUpdatingItemId(null);
    }
  };

  const regeneratePlan = async () => {
    setRegeneratingPlan(true);
    setError(null);
    try {
      await userApi.regenerateTrainingPlan(DEMO_USER_ID);
      await refreshPlan();
    } catch (err) {
      setError(formatApiError(err, "dashboard"));
    } finally {
      setRegeneratingPlan(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      {/* 页面标题 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-on-surface tracking-tight">学习中心</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          追踪你的薄弱知识点、查看训练计划、回顾错题
        </p>
      </div>

      {error && (
        <div className="mb-6 rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
          {error}
        </div>
      )}

      {/* 统计概览卡片 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">总提交次数</div>
          <div className="text-2xl font-bold text-on-surface">
            {statValue(stats.totalSubmissions)}
          </div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">通过题目</div>
          <div className="text-2xl font-bold text-emerald-600">
            {statValue(stats.passedProblems)}
          </div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">薄弱知识点</div>
          <div className="text-2xl font-bold text-amber-600">
            {statValue(weakPointCount)}
          </div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">错题数量</div>
          <div className="text-2xl font-bold text-error">
            {statValue(stats.mistakeCount)}
          </div>
        </div>
      </div>

      {/* 今日学习指挥台：主线内容 + sticky 辅助侧栏 */}
      <section className="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(360px,0.95fr)]">
        <div className="space-y-6">
          <TodayTrainingFocus
            plan={trainingPlan}
            updatingItemId={updatingItemId}
            onItemStatusChange={updatePlanItemStatus}
          />
          <DashboardNextActions actions={nextActions} />
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">训练计划追踪</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              汇总最新训练计划的完成率、来源分布和下一步待练任务。
            </p>
            {!trainingPlanTrace || !trainingPlanTrace.planId ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm text-on-surface-variant">
                  暂无可追踪训练计划，完成一次诊断或手动生成后会出现进度摘要。
                </p>
                <button
                  type="button"
                  onClick={refreshPlan}
                  disabled={trainingPlanRefreshing}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {trainingPlanRefreshing ? "刷新中..." : "刷新训练追踪"}
                </button>
              </div>
            ) : (
              <div className="mt-4 grid gap-3 sm:grid-cols-3">
                <div className="rounded-lg border border-outline-variant/30 px-3 py-3">
                  <div className="text-xs text-on-surface-variant">完成率</div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    计划状态：{trainingPlanTrace.statusLabel || trainingPlanTrace.status || "未知"}
                  </div>
                  <div className="mt-1 text-xl font-semibold text-on-surface">
                    {trainingPlanTrace.completionRate}%
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    完成 {trainingPlanTrace.completedCount}/{trainingPlanTrace.itemCount}
                    {trainingPlanTrace.skippedCount > 0 ? ` · 跳过 ${trainingPlanTrace.skippedCount}` : ""}
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    已处理 {trainingPlanTrace.handledCount}/{trainingPlanTrace.itemCount}
                    {" · "}
                    {trainingPlanTrace.handledRate}%
                  </div>
                </div>
                <div className="rounded-lg border border-outline-variant/30 px-3 py-3 sm:col-span-2">
                  <div className="text-xs text-on-surface-variant">推荐来源</div>
                  <div className="mt-1 text-sm font-medium text-on-surface">
                    {trainingPlanTrace.sourceTypeSummary || sourceTypeSummary(trainingPlanTrace.sourceTypeCounts)}
                  </div>
                  {trainingPlanTrace.progressSummary && (
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      {trainingPlanTrace.progressSummary}
                    </div>
                  )}
                  <div
                    className={`mt-1 text-[11px] ${
                      trainingPlanTrace.overdue ? "text-error" : "text-on-surface-variant"
                    }`}
                  >
                    计划时限：
                    {trainingPlanTrace.overdue
                      ? `已逾期 ${Math.abs(trainingPlanTrace.daysRemaining)} 天`
                      : `剩余 ${trainingPlanTrace.daysRemaining} 天`}
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    计划已运行 {trainingPlanTrace.daysSinceCreated} 天
                    {trainingPlanTrace.planCreatedAt
                      ? ` · 创建 ${formatActivityTime(trainingPlanTrace.planCreatedAt)}`
                      : ""}
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    下一步：{trainingPlanTrace.nextAction || trainingPlanTrace.nextItem?.knowledgeCardTitle || trainingPlanTrace.nextItem?.problemTitle || "暂无待练任务"}
                  </div>
                  {trainingPlanTrace.nextActionReason && (
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      原因：{trainingPlanTrace.nextActionReason}
                      {trainingPlanTrace.nextActionPriority ? ` · 优先级 ${trainingPlanTrace.nextActionPriority}` : ""}
                    </div>
                  )}
                  {trainingPlanTrace.nextTargetHref && (
                    <a
                      href={trainingPlanTrace.nextTargetHref}
                      className="mt-2 inline-flex items-center rounded-lg border border-primary/30 px-3 py-1.5 text-xs font-medium text-primary transition hover:border-primary hover:bg-primary/5"
                    >
                      进入下一项训练：{trainingPlanTrace.nextTargetLabel || "查看"}
                    </a>
                  )}
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    {trainingPlanTrace.latestActivitySummary || "暂无完成或跳过记录"}
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    最近推进：{trainingPlanTrace.latestActivityAt ? formatActivityTime(trainingPlanTrace.latestActivityAt) : "暂无"}
                  </div>
                </div>
              </div>
            )}
          </section>
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">最近训练完成</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              记录最近完成或跳过的训练项，帮助判断学习节奏是否持续。
            </p>
            {trainingActivities.length === 0 ? (
              <p className="mt-4 text-sm text-on-surface-variant">
                暂无最近完成的训练项，完成或跳过任务后会出现在这里。
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {trainingActivities.slice(0, 5).map((activity) => (
                  <div
                    key={activity.itemId}
                    className="rounded-lg border border-outline-variant/30 px-4 py-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-medium text-on-surface">
                          {activity.taskTitle}
                        </div>
                        <div className="mt-1 text-xs text-on-surface-variant">
                          {activity.statusLabel || trainingActivityStatusLabel(activity.status)}
                          {activity.statusUpdatedAt
                            ? ` · ${formatActivityTime(activity.statusUpdatedAt)}`
                            : ""}
                          {activity.planTitle ? ` · ${activity.planTitle}` : ""}
                        </div>
                      </div>
                      <span className="rounded-full bg-emerald-50 px-2 py-1 text-[11px] text-emerald-700">
                        {activity.itemType === "KNOWLEDGE_CARD" ? "知识复习" : "算法复盘"}
                      </span>
                    </div>
                    {(activity.sourceSummary || activity.sourceType) && (
                      <p className="mt-2 text-xs text-on-surface-variant">
                        推荐来源：{trainingPlanActivitySourceText(activity)}
                      </p>
                    )}
                    {activity.learningImpactSummary && (
                      <p className="mt-1 text-xs text-on-surface-variant">
                        学习影响：{activity.learningImpactSummary}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
          <WeaknessList weaknesses={aggregatedWeaknesses} />
          <SubmissionHistory submissions={submissions} />
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <div className="flex items-center justify-between gap-3 mb-4">
              <div>
                <h2 className="text-base font-semibold text-on-surface">最近模拟面试</h2>
                <p className="text-xs text-on-surface-variant mt-1">
                  复盘最近的文字一问一答训练，继续把面试结果带回学习闭环。
                </p>
              </div>
              <a
                href="/mock-interview"
                className="text-xs font-medium text-primary hover:underline"
              >
                开始新面试
              </a>
            </div>
            {mockInterviews.length === 0 ? (
              <div className="space-y-3">
                <p className="text-sm text-on-surface-variant">
                  暂无模拟面试记录，完成一场后这里会展示报告和继续入口。
                </p>
                <button
                  type="button"
                  onClick={refreshMockInterviewState}
                  disabled={mockInterviewRefreshing}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {mockInterviewRefreshing ? "刷新中..." : "刷新面试闭环"}
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {mockInterviews.map((item) => (
                  <div
                    key={item.sessionId}
                    className="rounded-lg border border-outline-variant/30 px-4 py-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div>
                        <div className="text-sm font-medium text-on-surface">
                          {mockInterviewCategoryLabels[item.category] || item.category} 面试
                        </div>
                        <div className="mt-1 text-xs text-on-surface-variant">
                          {mockInterviewStatusLabel(item.status)}
                          {item.averageScore != null ? ` · 平均分 ${item.averageScore}` : ""}
                        </div>
                      </div>
                      <a
                        href={`/mock-interview?sessionId=${item.sessionId}`}
                        className="text-xs font-medium text-primary hover:underline"
                      >
                        {item.status === "REPORTED" ? "查看报告" : "继续面试"}
                      </a>
                    </div>
                    {item.weaknessTags.length > 0 && (
                      <div className="mt-3 flex flex-wrap gap-2">
                        {item.weaknessTags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="rounded-full bg-amber-50 px-2 py-1 text-[11px] text-amber-700"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </section>
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">模拟面试闭环追踪</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              检查最近面试是否沉淀为报告、弱点事件和训练计划推荐。
            </p>
            {!mockInterviewTrace || mockInterviewTrace.sessionCount === 0 ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm text-on-surface-variant">
                  {mockInterviewTrace?.closureSummary || mockInterviewTrace?.nextAction || "暂无模拟面试闭环数据，完成一场面试报告后会显示追踪摘要。"}
                </p>
                <button
                  type="button"
                  onClick={refreshMockInterviewState}
                  disabled={mockInterviewRefreshing}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {mockInterviewRefreshing ? "刷新中..." : "刷新面试闭环"}
                </button>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                <div className="rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-on-surface">
                  {mockInterviewTrace.closureStatusLabel && (
                    <div className="mb-2 inline-flex items-center rounded-full bg-primary/10 px-2.5 py-1 text-xs font-medium text-primary">
                      闭环状态：{mockInterviewTrace.closureStatusLabel}
                    </div>
                  )}
                  {mockInterviewTrace.nextAction || "继续跟进最近模拟面试报告，并把推荐知识卡纳入训练计划。"}
                  {mockInterviewTrace.closureStatus && (
                    <div className="mt-1 text-xs text-on-surface-variant">
                      状态码：{mockInterviewTrace.closureStatus}
                    </div>
                  )}
                  <div className="mt-1 text-xs text-on-surface-variant">
                    最近会话：{mockInterviewTrace.latestSessionStatusLabel || mockInterviewTrace.latestSessionStatus || "暂无"}
                  </div>
                  <div className="mt-1 text-xs text-on-surface-variant">
                    最近方向：{mockInterviewTrace.latestCategory ? (mockInterviewCategoryLabels[mockInterviewTrace.latestCategory] || mockInterviewTrace.latestCategory) : "暂无"}
                  </div>
                  <div className="mt-1 text-xs text-on-surface-variant">
                    最近时间：{formatActivityTime(mockInterviewTrace.latestInterviewAt) || "暂无"}
                  </div>
                  {mockInterviewTrace.nextActionReason && (
                    <div className="mt-1 text-xs text-on-surface-variant">
                      原因：{mockInterviewTrace.nextActionReason}
                      {mockInterviewTrace.nextActionPriority ? ` · 优先级 ${mockInterviewTrace.nextActionPriority}` : ""}
                    </div>
                  )}
                  {mockInterviewTrace.closureSummary && (
                    <div className="mt-1 text-xs text-on-surface-variant">
                      {mockInterviewTrace.closureSummary}
                    </div>
                  )}
                  {mockInterviewTrace.reviewPathSummary && (
                    <div className="mt-1 text-xs text-on-surface-variant">
                      复盘链路：{mockInterviewTrace.reviewPathSummary}
                    </div>
                  )}
                  {mockInterviewTrace.nextTargetHref && (
                    <a
                      href={mockInterviewTrace.nextTargetHref}
                      className="mt-2 inline-flex items-center rounded-lg border border-primary/30 px-3 py-1.5 text-xs font-medium text-primary transition hover:border-primary hover:bg-primary/5"
                    >
                      进入面试闭环：{mockInterviewTrace.nextTargetLabel || "查看"}
                    </a>
                  )}
                  {mockInterviewTrace.reportReviewHref && (
                    <a
                      href={mockInterviewTrace.reportReviewHref}
                      className="ml-2 mt-2 inline-flex items-center rounded-lg border border-outline-variant/60 px-3 py-1.5 text-xs font-medium text-on-surface-variant transition hover:border-primary hover:text-primary"
                    >
                      {mockInterviewTrace.reportReviewLabel || "查看报告"}
                    </a>
                  )}
                </div>
                <div className="grid gap-3 sm:grid-cols-3">
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-3">
                    <div className="text-xs text-on-surface-variant">报告</div>
                    <div className="mt-1 text-xl font-semibold text-on-surface">
                      {mockInterviewTrace.reportedSessionCount}/{mockInterviewTrace.sessionCount}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      最近平均分 {mockInterviewTrace.latestAverageScore ?? "--"}
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-3">
                    <div className="text-xs text-on-surface-variant">低分回答</div>
                    <div className="mt-1 text-xl font-semibold text-amber-600">
                      {mockInterviewTrace.lowScoreTurnCount}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      已评分 {mockInterviewTrace.answeredTurnCount} 次
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-3">
                    <div className="text-xs text-on-surface-variant">闭环沉淀</div>
                    <div className="mt-1 text-xl font-semibold text-primary">
                      {mockInterviewTrace.trainingPlanItemCount}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      弱点事件 {mockInterviewTrace.weaknessEventCount}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      {mockInterviewTrace.reportTrainingPlanLinked ? "报告已接入训练计划" : "报告暂未接入训练计划"}
                    </div>
                  </div>
                </div>
              </div>
            )}
          </section>
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">模拟面试趋势</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              同一知识点多次面试后的分数变化，用来判断复盘是否真的变稳。
            </p>
            {mockInterviewTrends.length === 0 ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm text-on-surface-variant">
                  暂无可比较的知识点趋势，完成同一知识点的多次面试后会显示变化。
                </p>
                <button
                  type="button"
                  onClick={refreshMockInterviewState}
                  disabled={mockInterviewRefreshing}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {mockInterviewRefreshing ? "刷新中..." : "刷新面试趋势"}
                </button>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                {mockInterviewTrends.slice(0, 5).map((trend) => (
                  <div
                    key={trend.knowledgeCardId}
                    className="rounded-lg border border-outline-variant/30 px-4 py-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-medium text-on-surface">
                          {trend.knowledgePoint}
                        </div>
                        <div className="mt-1 text-xs text-on-surface-variant">
                          最近 {trend.latestScore} 分
                          {trend.previousScore != null ? ` · 上次 ${trend.previousScore} 分` : ""}
                          {trend.lastInterviewAt ? ` · ${formatActivityTime(trend.lastInterviewAt)}` : ""}
                        </div>
                      </div>
                      <span
                        className={`rounded-full px-2 py-1 text-[11px] ${
                          trend.deltaScore >= 0
                            ? "bg-emerald-50 text-emerald-700"
                            : "bg-amber-50 text-amber-700"
                        }`}
                      >
                        {trend.trendLabel}
                      </span>
                    </div>
                    {trend.latestIssue && (
                      <p className="mt-2 text-xs text-on-surface-variant">
                        最近卡点
                        {trend.latestIssueTypeLabel ? `（${trend.latestIssueTypeLabel}）` : ""}：
                        {trend.latestIssue}
                      </p>
                    )}
                    <div className="mt-2 text-[11px] text-on-surface-variant">
                      已面试 {trend.interviewCount} 次 · {mockInterviewCategoryLabels[trend.category] || trend.category}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        </div>

        <aside className="space-y-6 lg:sticky lg:top-24 xl:sticky xl:top-24 self-start">
          <TrainingPlan
            plan={trainingPlan}
            updatingItemId={updatingItemId}
            regenerating={regeneratingPlan}
            onItemStatusChange={updatePlanItemStatus}
            onRegenerate={regeneratePlan}
          />
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">缓存层状态</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              展示 Redis 热点缓存是否可用，以及缓存层和 MySQL 事实源的边界。
            </p>
            {!cacheStatus ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm text-on-surface-variant">
                  暂无缓存状态，学习数据仍以 MySQL 返回为准。
                </p>
                <button
                  type="button"
                  onClick={refreshCacheStatus}
                  disabled={cacheMaintenanceRunning !== null}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {cacheMaintenanceRunning === "status" ? "检查中..." : "重试缓存状态"}
                </button>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-on-surface-variant">统一状态</div>
                    <div className="mt-1 text-lg font-semibold text-on-surface">
                      {cacheStatus.statusLabel || "UNKNOWN"}
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2 text-right">
                    <div className="text-xs text-on-surface-variant">缓存 Key</div>
                    <div className="mt-1 text-lg font-semibold text-primary">
                      {cacheStatus.cachedKeyCount}
                    </div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                    <div className="text-xs text-on-surface-variant">题目缓存</div>
                    <div className="mt-1 text-sm font-medium text-on-surface">
                      {cacheStatus.problem?.statusLabel || "UNKNOWN"}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      key {cacheStatus.problem?.cachedKeyCount ?? 0} · 命中 {cacheStatus.problem?.hitCount ?? 0}
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                    <div className="text-xs text-on-surface-variant">知识卡缓存</div>
                    <div className="mt-1 text-sm font-medium text-on-surface">
                      {cacheStatus.knowledge?.statusLabel || "UNKNOWN"}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      key {cacheStatus.knowledge?.cachedKeyCount ?? 0} · 命中 {cacheStatus.knowledge?.hitCount ?? 0}
                    </div>
                  </div>
                </div>
                <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                  <div className="grid grid-cols-2 gap-3 text-[11px] text-on-surface-variant">
                    <div>
                      <span className="block">缓存命中率</span>
                      <span className="text-sm font-medium text-on-surface">
                        {cacheStatus.hitRate}%
                      </span>
                    </div>
                    <div>
                      <span className="block">回源次数</span>
                      <span className="text-sm font-medium text-on-surface">
                        {cacheStatus.fallbackCount}
                      </span>
                    </div>
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    命中 {cacheStatus.hitCount} · 未命中 {cacheStatus.missCount}
                  </div>
                  {cacheStatus.lastFallbackReason && (
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      最近降级：{cacheStatus.lastFallbackReason}
                    </div>
                  )}
                  {cacheStatus.probeWarning && (
                    <div className="mt-1 text-[11px] text-warning">
                      状态探测：{cacheStatus.probeWarning}
                    </div>
                  )}
                </div>
                <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                  <div className="grid grid-cols-2 gap-3 text-[11px] text-on-surface-variant">
                    <div>
                      <span className="block">缓存开关</span>
                      <span className="text-sm font-medium text-on-surface">
                        {cacheStatus.allEnabled ? "全部启用" : "部分关闭"}
                      </span>
                    </div>
                    <div>
                      <span className="block">Redis 可用</span>
                      <span className="text-sm font-medium text-on-surface">
                        {cacheStatus.allRedisAvailable ? "全部可用" : "部分不可用"}
                      </span>
                    </div>
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    最近检查：{cacheStatus.checkedAt ? formatActivityTime(cacheStatus.checkedAt) : "暂无"}
                  </div>
                </div>
                {cacheStatus.maintenanceAction && (
                  <p className="text-xs text-on-surface-variant">
                    维护动作：{cacheStatus.maintenanceAction}
                  </p>
                )}
                {cacheStatus.cacheBenefitSummary && (
                  <p className="text-xs text-on-surface-variant">
                    缓存收益：{cacheStatus.cacheBenefitSummary}
                  </p>
                )}
                {cacheStatus.fallbackRiskSummary && (
                  <p className="text-xs text-on-surface-variant">
                    回源风险：{cacheStatus.fallbackRiskSummary}
                  </p>
                )}
                {cacheStatus.protectedDataSummary && (
                  <p className="text-xs text-on-surface-variant">
                    保护数据：{cacheStatus.protectedDataSummary}
                  </p>
                )}
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={refreshHotCache}
                    disabled={cacheMaintenanceRunning !== null}
                    className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {cacheMaintenanceRunning === "refresh" ? "刷新中..." : "刷新热点缓存"}
                  </button>
                  <button
                    type="button"
                    onClick={refreshCacheStatus}
                    disabled={cacheMaintenanceRunning !== null}
                    className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {cacheMaintenanceRunning === "status" ? "检查中..." : "刷新缓存状态"}
                  </button>
                </div>
                {cacheMaintenanceResult && (
                  <p className="text-[11px] text-on-surface-variant">
                    最近刷新：{cacheMaintenanceResult}
                  </p>
                )}
                {cacheStatus.boundary && (
                  <p className="text-[11px] text-on-surface-variant">
                    边界：{cacheStatus.boundary}
                  </p>
                )}
              </div>
            )}
          </section>
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">RAG 索引状态</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              展示内部 RAG 索引健康状态和下一步维护动作，不暴露原始 chunk。
            </p>
            {!ragHealth ? (
              <div className="mt-4 space-y-3">
                <p className="text-sm text-on-surface-variant">
                  暂无 RAG 维护状态，诊断流程仍会按可降级步骤继续运行。
                </p>
                <button
                  type="button"
                  onClick={refreshRagHealth}
                  disabled={ragMaintenanceRunning !== null}
                  className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {ragMaintenanceRunning === "status" ? "检查中..." : "重试 RAG 状态"}
                </button>
              </div>
            ) : (
              <div className="mt-4 space-y-3">
                <div className="flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <div className="text-xs text-on-surface-variant">索引状态</div>
                    <div className="mt-1 text-lg font-semibold text-on-surface">
                      {ragHealth.statusLabel || "UNKNOWN"}
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2 text-right">
                    <div className="text-xs text-on-surface-variant">系统 Chunk</div>
                    <div className="mt-1 text-lg font-semibold text-primary">
                      {ragHealth.systemChunkCount}
                    </div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                    <div className="text-xs text-on-surface-variant">用户记忆 Chunk</div>
                    <div className="mt-1 text-sm font-medium text-on-surface">
                      {ragHealth.userMemoryChunkCount}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      用户记忆仍按 userId 隔离
                    </div>
                  </div>
                  <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                    <div className="text-xs text-on-surface-variant">向量索引</div>
                    <div className="mt-1 text-sm font-medium text-on-surface">
                      {ragHealth.vectorEnabled ? "已启用" : "未启用"}
                    </div>
                    <div className="mt-1 text-[11px] text-on-surface-variant">
                      failed {ragHealth.vectorFailedChunkCount} · pending {ragHealth.vectorPendingChunkCount}
                    </div>
                  </div>
                </div>
                <div className="rounded-lg border border-outline-variant/30 px-3 py-2">
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-xs text-on-surface-variant">维护告警</span>
                    <span className="text-sm font-semibold text-on-surface">
                      {ragHealth.warnings.length}
                    </span>
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    下一维护接口：{ragHealth.nextMaintenanceEndpoint || "暂无"}
                  </div>
                  <div className="mt-1 text-[11px] text-on-surface-variant">
                    优先级：{ragHealth.maintenancePriority || "NONE"}
                  </div>
                </div>
                {ragHealth.maintenanceSummary && (
                  <p className="text-xs text-on-surface-variant">
                    维护摘要：{ragHealth.maintenanceSummary}
                  </p>
                )}
                {ragHealth.maintenanceReason && (
                  <p className="text-xs text-on-surface-variant">
                    维护原因：{ragHealth.maintenanceReason}
                  </p>
                )}
                {ragHealth.preferredMaintenanceAction && (
                  <p className="text-xs text-on-surface-variant">
                    首选动作：{ragHealth.preferredMaintenanceAction}
                  </p>
                )}
                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={rebuildRagSystemIndex}
                    disabled={ragMaintenanceRunning !== null}
                    className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {ragMaintenanceRunning === "rebuild" ? "重建中..." : "重建系统索引"}
                  </button>
                  <button
                    type="button"
                    onClick={retryRagFailedVectors}
                    disabled={ragMaintenanceRunning !== null}
                    className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {ragMaintenanceRunning === "retry" ? "重试中..." : "重试失败向量"}
                  </button>
                  <button
                    type="button"
                    onClick={refreshRagHealth}
                    disabled={ragMaintenanceRunning !== null}
                    className="rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary disabled:cursor-not-allowed disabled:opacity-60"
                  >
                    {ragMaintenanceRunning === "status" ? "检查中..." : "刷新 RAG 状态"}
                  </button>
                </div>
                {ragMaintenanceResult && (
                  <p className="text-[11px] text-on-surface-variant">
                    最近维护：{ragMaintenanceResult}
                  </p>
                )}
              </div>
            )}
          </section>
          <section className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
            <h2 className="text-base font-semibold text-on-surface">训练计划历史</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              查看最近几轮训练计划的状态和完成情况。
            </p>
            {trainingPlanHistory.length === 0 ? (
              <p className="mt-4 text-sm text-on-surface-variant">
                暂无历史训练计划，完成一次诊断或手动生成后会出现在这里。
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {trainingPlanHistory.slice(0, 5).map((plan) => (
                  <div
                    key={plan.id}
                    className="rounded-lg border border-outline-variant/30 px-3 py-2.5"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <div className="min-w-0">
                        <div className="truncate text-sm font-medium text-on-surface">
                          {plan.title}
                        </div>
                        <div className="mt-1 text-xs text-on-surface-variant">
                          {plan.statusLabel || trainingPlanStatusLabel(plan.status)}
                          {" · "}
                          完成 {plan.completedCount}/{plan.itemCount}
                          {plan.skippedCount > 0 ? ` · 跳过 ${plan.skippedCount}` : ""}
                          {plan.pendingCount > 0 ? ` · 待练 ${plan.pendingCount}` : ""}
                        </div>
                        <div className="mt-1 text-[11px] text-on-surface-variant">
                          完成率 {plan.completionRate}% · 已处理 {plan.handledRate}%
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
          <ErrorStats stats={errorStats} loading={loading} />
          <KnowledgeTrainingEntry />

          {/* AI 教练建议 */}
          <div className="bg-primary/5 border border-primary/20 rounded-xl p-5">
            <div className="flex items-center gap-2 mb-3">
              <Lightbulb className="w-5 h-5 text-primary" />
              <h3 className="text-sm font-semibold text-on-surface">AI 教练建议</h3>
            </div>
            <p className="text-xs text-on-surface-variant leading-relaxed">
              {coachAdvice}
            </p>
          </div>
        </aside>
      </section>

      <section className="mt-8">
        <MistakeCards mistakes={aggregatedMistakeCards} />
      </section>
    </div>
  );
}
