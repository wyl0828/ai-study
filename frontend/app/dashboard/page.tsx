"use client";

import { useEffect, useMemo, useState } from "react";
import { Lightbulb } from "lucide-react";
import { formatApiError, userApi } from "@/lib/api";
import type {
  DashboardStatsVO,
  ErrorStatsVO,
  MistakeCard as MistakeCardType,
  MockInterviewRecent,
  MockInterviewTrend,
  SubmissionHistoryVO,
  TrainingPlanActivity,
  TrainingPlanHistory,
  TrainingPlan as TrainingPlanType,
  UserWeakness,
} from "@/lib/types";
import WeaknessList from "@/components/WeaknessList";
import SubmissionHistory from "@/components/SubmissionHistory";
import MistakeCards from "@/components/MistakeCards";
import TrainingPlan from "@/components/TrainingPlan";
import TodayTrainingFocus from "@/components/TodayTrainingFocus";
import ErrorStats from "@/components/ErrorStats";
import KnowledgeTrainingEntry from "@/components/KnowledgeTrainingEntry";
import {
  buildDashboardCoachAdvice,
  groupMistakeCards,
  groupWeaknesses,
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

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStatsVO>(emptyStats);
  const [weaknesses, setWeaknesses] = useState<UserWeakness[]>([]);
  const [mistakes, setMistakes] = useState<MistakeCardType[]>([]);
  const [trainingPlan, setTrainingPlan] = useState<TrainingPlanType | null>(null);
  const [trainingPlanHistory, setTrainingPlanHistory] = useState<TrainingPlanHistory[]>([]);
  const [trainingActivities, setTrainingActivities] = useState<TrainingPlanActivity[]>([]);
  const [submissions, setSubmissions] = useState<SubmissionHistoryVO[]>([]);
  const [mockInterviews, setMockInterviews] = useState<MockInterviewRecent[]>([]);
  const [mockInterviewTrends, setMockInterviewTrends] = useState<MockInterviewTrend[]>([]);
  const [errorStats, setErrorStats] = useState<ErrorStatsVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [updatingItemId, setUpdatingItemId] = useState<number | null>(null);
  const [regeneratingPlan, setRegeneratingPlan] = useState(false);
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
          submissionsResponse,
          mockInterviewsResponse,
          mockInterviewTrendsResponse,
          errorStatsResponse,
        ] = await Promise.all([
          userApi.stats(DEMO_USER_ID),
          userApi.weaknesses(DEMO_USER_ID),
          userApi.mistakes(DEMO_USER_ID),
          userApi.latestPlan(DEMO_USER_ID),
          userApi.trainingPlanHistory(DEMO_USER_ID),
          userApi.trainingPlanActivities(DEMO_USER_ID),
          userApi.recentSubmissions(DEMO_USER_ID),
          userApi.recentMockInterviews(DEMO_USER_ID),
          userApi.mockInterviewTrends(DEMO_USER_ID),
          userApi.errorStats(DEMO_USER_ID),
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
        setSubmissions(submissionsResponse.data);
        setMockInterviews(mockInterviewsResponse.data);
        setMockInterviewTrends(mockInterviewTrendsResponse.data);
        setErrorStats(errorStatsResponse.data);
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

  const refreshPlan = async () => {
    const [planResponse, historyResponse, activitiesResponse] = await Promise.all([
      userApi.latestPlan(DEMO_USER_ID),
      userApi.trainingPlanHistory(DEMO_USER_ID),
      userApi.trainingPlanActivities(DEMO_USER_ID),
    ]);
    setTrainingPlan(planResponse.data);
    setTrainingPlanHistory(historyResponse.data);
    setTrainingActivities(activitiesResponse.data);
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
                          {trainingActivityStatusLabel(activity.status)}
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
                    {activity.sourceSummary && (
                      <p className="mt-2 text-xs text-on-surface-variant">
                        {activity.sourceSummary}
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
              <p className="text-sm text-on-surface-variant">
                暂无模拟面试记录，完成一场后这里会展示报告和继续入口。
              </p>
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
            <h2 className="text-base font-semibold text-on-surface">模拟面试趋势</h2>
            <p className="mt-1 text-xs text-on-surface-variant">
              同一知识点多次面试后的分数变化，用来判断复盘是否真的变稳。
            </p>
            {mockInterviewTrends.length === 0 ? (
              <p className="mt-4 text-sm text-on-surface-variant">
                暂无可比较的知识点趋势，完成同一知识点的多次面试后会显示变化。
              </p>
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
                        最近卡点：{trend.latestIssue}
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
                          {trainingPlanStatusLabel(plan.status)}
                          {" · "}
                          完成 {plan.completedCount}/{plan.itemCount}
                          {plan.skippedCount > 0 ? ` · 跳过 ${plan.skippedCount}` : ""}
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
