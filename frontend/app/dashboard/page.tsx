"use client";

import { useEffect, useMemo, useState } from "react";
import { Lightbulb } from "lucide-react";
import { formatApiError, userApi } from "@/lib/api";
import type {
  DashboardStatsVO,
  ErrorStatsVO,
  MistakeCard as MistakeCardType,
  MockInterviewRecent,
  SubmissionHistoryVO,
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

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStatsVO>(emptyStats);
  const [weaknesses, setWeaknesses] = useState<UserWeakness[]>([]);
  const [mistakes, setMistakes] = useState<MistakeCardType[]>([]);
  const [trainingPlan, setTrainingPlan] = useState<TrainingPlanType | null>(null);
  const [submissions, setSubmissions] = useState<SubmissionHistoryVO[]>([]);
  const [mockInterviews, setMockInterviews] = useState<MockInterviewRecent[]>([]);
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
          submissionsResponse,
          mockInterviewsResponse,
          errorStatsResponse,
        ] = await Promise.all([
          userApi.stats(DEMO_USER_ID),
          userApi.weaknesses(DEMO_USER_ID),
          userApi.mistakes(DEMO_USER_ID),
          userApi.latestPlan(DEMO_USER_ID),
          userApi.recentSubmissions(DEMO_USER_ID),
          userApi.recentMockInterviews(DEMO_USER_ID),
          userApi.errorStats(DEMO_USER_ID),
        ]);

        if (cancelled) {
          return;
        }

        setStats(statsResponse.data);
        setWeaknesses(weaknessesResponse.data);
        setMistakes(mistakesResponse.data);
        setTrainingPlan(planResponse.data);
        setSubmissions(submissionsResponse.data);
        setMockInterviews(mockInterviewsResponse.data);
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
    const response = await userApi.latestPlan(DEMO_USER_ID);
    setTrainingPlan(response.data);
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
        </div>

        <aside className="space-y-6 lg:sticky lg:top-24 xl:sticky xl:top-24 self-start">
          <TrainingPlan
            plan={trainingPlan}
            updatingItemId={updatingItemId}
            regenerating={regeneratingPlan}
            onItemStatusChange={updatePlanItemStatus}
            onRegenerate={regeneratePlan}
          />
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
