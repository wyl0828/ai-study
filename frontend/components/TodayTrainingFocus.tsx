"use client";

import { BookOpen, Check, Code2, Sparkles, X } from "lucide-react";
import Link from "next/link";
import type { TrainingPlan as TrainingPlanType } from "@/lib/types";
import {
  selectTodayTrainingItem,
  trainingPlanItemAction,
  trainingPlanItemPrefix,
  trainingPlanItemTitle,
} from "@/lib/learningView";
import { trainingPlanText } from "@/lib/i18n";

interface TodayTrainingFocusProps {
  plan: TrainingPlanType | null;
  updatingItemId?: number | null;
  onItemStatusChange?: (
    itemId: number,
    status: "PENDING" | "COMPLETED" | "SKIPPED"
  ) => void;
}

export default function TodayTrainingFocus({
  plan,
  updatingItemId,
  onItemStatusChange,
}: TodayTrainingFocusProps) {
  const item = selectTodayTrainingItem(plan);

  if (!plan) {
    return (
      <section className="rounded-xl border border-primary/20 bg-primary/5 p-5">
        <Header />
        <p className="text-sm text-on-surface-variant">
          还没有学习数据，去做第一道题并触发 AI 诊断吧。
        </p>
      </section>
    );
  }

  if (!item) {
    return (
      <section className="rounded-xl border border-emerald-200 bg-emerald-50 p-5">
        <Header />
        <p className="text-sm font-medium text-emerald-700">
          今日训练已完成，可以复盘最近错题。
        </p>
        <p className="mt-2 text-xs text-emerald-700/80">
          如果还想继续，可以查看完整训练计划或重新生成下一轮安排。
        </p>
      </section>
    );
  }

  const action = trainingPlanItemAction(item);
  const ActionIcon = item.itemType === "KNOWLEDGE_CARD" ? BookOpen : Code2;

  return (
    <section className="rounded-xl border border-primary/20 bg-primary/5 p-5">
      <Header />
      <div className="mt-4 flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div className="min-w-0">
          <div className="mb-2 inline-flex rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
            {trainingPlanItemPrefix(item)}
          </div>
          <h2 className="text-xl font-semibold text-on-surface">
            {trainingPlanItemTitle(item)}
          </h2>
          <p className="mt-2 text-sm text-on-surface-variant leading-relaxed">
            {trainingPlanText(item.reason)}
          </p>
          {item.reviewFocus && (
            <p className="mt-2 text-sm text-on-surface-variant leading-relaxed">
              <span className="font-medium text-primary">复习重点：</span>
              {trainingPlanText(item.reviewFocus)}
            </p>
          )}
        </div>

        <div className="flex shrink-0 flex-wrap gap-2">
          <Link
            href={action.href}
            className="inline-flex items-center gap-1 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-on-primary transition hover:bg-primary/90"
          >
            <ActionIcon className="h-4 w-4" />
            {action.label}
          </Link>
          {onItemStatusChange && item.id && (
            <>
              <button
                type="button"
                disabled={updatingItemId === item.id || item.status === "COMPLETED"}
                onClick={() => onItemStatusChange(item.id, "COMPLETED")}
                className="inline-flex items-center gap-1 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm font-medium text-emerald-700 disabled:opacity-60"
              >
                <Check className="h-4 w-4" />
                完成
              </button>
              <button
                type="button"
                disabled={updatingItemId === item.id || item.status === "SKIPPED"}
                onClick={() => onItemStatusChange(item.id, "SKIPPED")}
                className="inline-flex items-center gap-1 rounded-lg border border-outline-variant/40 bg-surface-container-lowest px-3 py-2 text-sm font-medium text-on-surface-variant disabled:opacity-60"
              >
                <X className="h-4 w-4" />
                跳过
              </button>
            </>
          )}
        </div>
      </div>
    </section>
  );
}

function Header() {
  return (
    <div className="flex items-center gap-2">
      <Sparkles className="h-5 w-5 text-primary" />
      <div>
        <p className="text-xs font-semibold uppercase tracking-wide text-primary">
          今日建议
        </p>
        <h2 className="text-lg font-semibold text-on-surface">今日优先训练</h2>
      </div>
    </div>
  );
}
