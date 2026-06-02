"use client";

import { useEffect, useMemo, useState } from "react";
import type { ReactNode } from "react";
import Link from "next/link";
import { AlertCircle, ArrowRight, BarChart3, CheckCircle2, Code2, Loader2, Sparkles } from "lucide-react";
import { formatApiError, userApi } from "@/lib/api";
import type { SubmissionHistoryVO, TrainingPlan, TrainingPlanItem, UserWeakness } from "@/lib/types";
import {
  selectTodayTrainingItem,
  trainingPlanItemPrefix,
  trainingPlanItemTitle,
  trainingPlanSourceText,
} from "@/lib/learningView";
import { problemTitle } from "@/lib/i18n";

interface ProblemTrainingSidebarProps {
  userId?: number;
}

interface CardState<T> {
  loading: boolean;
  error: string | null;
  data: T;
}

function itemAction(item: TrainingPlanItem) {
  const targetHref = item.targetHref;
  const targetLabel = item.targetLabel;
  const href = targetHref || (item.problemId ? `/problem/${item.problemId}` : "/dashboard");
  const label = targetLabel || (item.problemId ? "去做题" : "查看学习中心");
  return { href, label };
}

function statusLabel(status: string) {
  if (status === "ACCEPTED") return "已通过";
  if (status === "RETRY" || status === "NEEDS_REVIEW") return "待重试";
  return "未通过";
}

function formatCreatedAt(createdAt: string | null) {
  if (!createdAt) return "未知时间";
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) return createdAt;
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function ProblemTrainingSidebar({ userId }: ProblemTrainingSidebarProps) {
  const [planState, setPlanState] = useState<CardState<TrainingPlan | null>>({
    loading: Boolean(userId),
    error: null,
    data: null,
  });
  const [submissionState, setSubmissionState] = useState<CardState<SubmissionHistoryVO[]>>({
    loading: Boolean(userId),
    error: null,
    data: [],
  });
  const [weaknessState, setWeaknessState] = useState<CardState<UserWeakness[]>>({
    loading: Boolean(userId),
    error: null,
    data: [],
  });

  useEffect(() => {
    if (!userId) {
      setPlanState({ loading: false, error: null, data: null });
      setSubmissionState({ loading: false, error: null, data: [] });
      setWeaknessState({ loading: false, error: null, data: [] });
      return;
    }
    let cancelled = false;

    setPlanState({ loading: true, error: null, data: null });
    setSubmissionState({ loading: true, error: null, data: [] });
    setWeaknessState({ loading: true, error: null, data: [] });

    userApi.latestPlan(userId)
      .then((response) => {
        if (!cancelled) {
          setPlanState({ loading: false, error: null, data: response.data });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setPlanState({ loading: false, error: formatApiError(err, "dashboard"), data: null });
        }
      });

    userApi.recentSubmissions(userId)
      .then((response) => {
        if (!cancelled) {
          setSubmissionState({ loading: false, error: null, data: response.data });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setSubmissionState({ loading: false, error: formatApiError(err, "dashboard"), data: [] });
        }
      });

    userApi.weaknesses(userId)
      .then((response) => {
        if (!cancelled) {
          setWeaknessState({ loading: false, error: null, data: response.data });
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setWeaknessState({ loading: false, error: formatApiError(err, "dashboard"), data: [] });
        }
      });

    return () => {
      cancelled = true;
    };
  }, [userId]);

  const todayItem = useMemo(() => selectTodayTrainingItem(planState.data), [planState.data]);
  const recentSubmission = submissionState.data[0] ?? null;
  const topWeaknesses = useMemo(
    () => [...weaknessState.data].sort((a, b) => b.weaknessScore - a.weaknessScore).slice(0, 3),
    [weaknessState.data]
  );
  const planLoading = planState.loading;
  const submissionsLoading = submissionState.loading;
  const weaknessLoading = weaknessState.loading;
  const planError = planState.error;
  const submissionsError = submissionState.error;
  const weaknessError = weaknessState.error;

  return (
    <aside className="hidden xl:block">
      <div className="sticky top-20 space-y-4">
        <SidebarCard
          icon={<Sparkles className="h-4 w-4 text-primary" />}
          title="今日优先训练"
          loading={planLoading}
          error={planError}
          empty={!planState.data || !todayItem}
          emptyText={
            !userId
              ? "登录后会显示今日训练建议。"
              : planState.data
              ? "今日训练暂无待办，可以去学习中心复盘最近错题。"
              : "完成一次诊断后会生成今日训练建议。"
          }
        >
          {todayItem && <TodayPlanContent item={todayItem} />}
        </SidebarCard>

        <SidebarCard
          icon={<AlertCircle className="h-4 w-4 text-primary" />}
          title="最近一次诊断"
          loading={submissionsLoading}
          error={submissionsError}
          empty={!recentSubmission}
          emptyText={
            userId
              ? "提交一次代码后，这里会显示最近诊断摘要。"
              : "登录后会显示最近诊断摘要。"
          }
        >
          {recentSubmission && <RecentSubmissionContent submission={recentSubmission} />}
        </SidebarCard>

        <SidebarCard
          icon={<BarChart3 className="h-4 w-4 text-primary" />}
          title="薄弱知识点"
          loading={weaknessLoading}
          error={weaknessError}
          empty={topWeaknesses.length === 0}
          emptyText={
            userId
              ? "触发 AI 诊断或知识自测后会沉淀薄弱知识点。"
              : "登录后会显示薄弱知识点。"
          }
        >
          <div className="space-y-3">
            {topWeaknesses.map((weakness) => (
              <div key={weakness.id} className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest px-3 py-2.5">
                <div className="text-sm font-semibold text-on-surface">{weakness.knowledgePoint}</div>
                <div className="mt-1 text-xs text-on-surface-variant">
                  {weakness.errorType} · 薄弱分 {weakness.weaknessScore}
                </div>
                <div className="mt-1 text-[11px] text-on-surface-variant">错误 {weakness.wrongCount} 次</div>
              </div>
            ))}
          </div>
          <SidebarLink href="/dashboard" label="查看薄弱点" />
        </SidebarCard>
      </div>
    </aside>
  );
}

function TodayPlanContent({ item }: { item: TrainingPlanItem }) {
  const action = itemAction(item);

  return (
    <div>
      <div className="mb-2 inline-flex rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
        {trainingPlanItemPrefix(item)}
      </div>
      <h3 className="text-sm font-semibold leading-5 text-on-surface">{trainingPlanItemTitle(item)}</h3>
      <p className="mt-2 text-xs leading-5 text-on-surface-variant">
        {item.reviewFocus || item.reason || trainingPlanSourceText(item)}
      </p>
      <SidebarLink href={action.href} label={action.label} />
    </div>
  );
}

function RecentSubmissionContent({ submission }: { submission: SubmissionHistoryVO }) {
  const accepted = submission.status === "ACCEPTED";

  return (
    <div>
      <div className="flex items-start gap-2">
        {accepted ? (
          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-emerald-600" />
        ) : (
          <Code2 className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
        )}
        <div className="min-w-0">
          <h3 className="truncate text-sm font-semibold text-on-surface">
            #{submission.problemId} {problemTitle(submission.problemTitle)}
          </h3>
          <p className="mt-1 text-xs leading-5 text-on-surface-variant">
            {statusLabel(submission.status)} · 通过 {submission.passedCount}/{submission.totalCount}
          </p>
          <p className="mt-1 text-[11px] text-on-surface-variant">{formatCreatedAt(submission.createdAt)}</p>
        </div>
      </div>
      <p className="mt-3 text-xs leading-5 text-on-surface-variant">
        {accepted ? "最近提交已通过，可以继续复盘代码表达和复杂度。" : "最近提交未通过，建议回到题目页查看测试结果和 AI 诊断。"}
      </p>
      <SidebarLink href={`/problem/${submission.problemId}`} label={accepted ? "复盘题目" : "查看诊断"} />
    </div>
  );
}

function SidebarCard({
  icon,
  title,
  loading,
  error,
  empty,
  emptyText,
  children,
}: {
  icon: ReactNode;
  title: string;
  loading: boolean;
  error: string | null;
  empty: boolean;
  emptyText: string;
  children: ReactNode;
}) {
  return (
    <section className="coach-card p-4">
      <div className="mb-3 flex items-center gap-2">
        {icon}
        <h2 className="text-sm font-semibold text-on-surface">{title}</h2>
      </div>
      {loading ? (
        <div className="flex items-center gap-2 rounded-lg bg-surface-container px-3 py-3 text-xs text-on-surface-variant">
          <Loader2 className="h-3.5 w-3.5 animate-spin" />
          加载中...
        </div>
      ) : error ? (
        <p className="rounded-lg border border-outline-variant/30 bg-surface-container px-3 py-3 text-xs leading-5 text-on-surface-variant">
          暂时无法加载，主列表不受影响。
        </p>
      ) : empty ? (
        <p className="rounded-lg border border-outline-variant/30 bg-surface-container px-3 py-3 text-xs leading-5 text-on-surface-variant">
          {emptyText}
        </p>
      ) : (
        children
      )}
    </section>
  );
}

function SidebarLink({ href, label }: { href: string; label: string }) {
  return (
    <Link
      href={href}
      className="coach-primary-button mt-3 w-full py-2 text-xs"
    >
      {label}
      <ArrowRight className="h-3.5 w-3.5" />
    </Link>
  );
}
