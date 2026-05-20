"use client";

import { useState } from "react";
import { BookOpen, Check, Code2, RefreshCw, Sparkles, X } from "lucide-react";
import Link from "next/link";
import type { TrainingPlan as TrainingPlanType } from "@/lib/types";
import {
  trainingPlanItemAction,
  trainingPlanItemPrefix,
  trainingPlanItemTitle,
} from "@/lib/learningView";
import {
  trainingPlanText,
  trainingPlanTitle,
} from "@/lib/i18n";

interface TrainingPlanProps {
  plan: TrainingPlanType | null;
  updatingItemId?: number | null;
  regenerating?: boolean;
  onItemStatusChange?: (
    itemId: number,
    status: "PENDING" | "COMPLETED" | "SKIPPED"
  ) => void;
  onRegenerate?: () => void;
}

export default function TrainingPlan({
  plan,
  updatingItemId,
  regenerating,
  onItemStatusChange,
  onRegenerate,
}: TrainingPlanProps) {
  const [activeDay, setActiveDay] = useState(1);

  if (!plan) {
    return (
      <section>
        <div className="flex items-center gap-2 mb-4">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">完整训练计划</h2>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl px-5 py-8 text-sm text-on-surface-variant">
          还没有学习数据，去做第一道题并触发 AI 诊断吧。
        </div>
      </section>
    );
  }

  const grouped = plan.items.reduce<Record<number, typeof plan.items>>(
    (acc, item) => {
      if (!acc[item.dayIndex]) acc[item.dayIndex] = [];
      acc[item.dayIndex].push(item);
      return acc;
    },
    {}
  );

  const getStatus = (status: string) => {
    const normalized = status.toUpperCase();
    if (normalized === "COMPLETED") {
      return { label: "已完成", className: "text-emerald-600" };
    }
    if (normalized === "SKIPPED") {
      return { label: "已跳过", className: "text-on-surface-variant" };
    }
    if (normalized === "RETRY" || normalized === "NEEDS_REVIEW") {
      return { label: "需要重做", className: "text-amber-600" };
    }
      return { label: "待完成", className: "text-primary" };
  };

  const dayIndexes = Object.keys(grouped).map(Number);
  const firstDay = dayIndexes.length > 0 ? Math.min(...dayIndexes) : 1;
  const orderedDayIndexes = dayIndexes.sort((a, b) => a - b);
  const selectedDay = orderedDayIndexes.includes(activeDay) ? activeDay : firstDay;
  const selectedItems = grouped[selectedDay] || [];

  return (
    <section>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">完整训练计划</h2>
        </div>
        <button
          type="button"
          onClick={onRegenerate}
          disabled={!onRegenerate || regenerating}
          className="text-xs bg-primary/10 text-primary px-3 py-1.5 rounded-lg font-medium flex items-center gap-1 disabled:opacity-60"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          {regenerating ? "生成中" : "重新生成"}
        </button>
      </div>

      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
        <h3 className="text-base font-semibold text-on-surface mb-1">
          {trainingPlanTitle(plan.title)}
        </h3>
        <p className="text-xs text-on-surface-variant mb-5 leading-relaxed">
          {trainingPlanText(plan.summary)}
        </p>

        <div className="mb-5 grid grid-cols-3 gap-2">
          {orderedDayIndexes.map((day) => {
            const items = grouped[day] || [];
            const completedCount = items.filter((item) =>
              ["COMPLETED", "SKIPPED"].includes(item.status.toUpperCase())
            ).length;
            const active = day === selectedDay;

            return (
              <button
                key={day}
                type="button"
                onClick={() => setActiveDay(day)}
                className={`rounded-lg border px-2 py-2 text-left transition ${
                  active
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-outline-variant/30 bg-surface-container text-on-surface-variant hover:border-primary/30"
                }`}
              >
                <span className="block text-xs font-bold">第 {day} 天</span>
                <span className="mt-0.5 block text-[11px]">
                  {completedCount}/{items.length} 项
                </span>
              </button>
            );
          })}
        </div>

        <div className="rounded-xl border border-primary/15 bg-primary/5 p-3">
          <div className="mb-3 flex items-center gap-2">
            <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary text-xs font-bold text-on-primary">
              {selectedDay}
            </span>
            <span className="text-sm font-semibold text-on-surface">
              第 {selectedDay} 天
            </span>
          </div>
          <div className="space-y-3">
            {selectedItems.map((item, i) => {
              const action = trainingPlanItemAction(item);
              const ActionIcon = item.itemType === "KNOWLEDGE_CARD" ? BookOpen : Code2;

              return (
                <div
                  key={item.id || `${selectedDay}-${i}`}
                  className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest p-3"
                >
                  <div className="mb-1 flex items-center justify-between gap-2">
                    <div className="flex min-w-0 items-center gap-2">
                      {item.itemType === "KNOWLEDGE_CARD" ? (
                        <BookOpen className="h-4 w-4 shrink-0 text-primary" />
                      ) : (
                        <Code2 className="h-4 w-4 shrink-0 text-primary" />
                      )}
                      <span className="truncate text-sm font-medium text-on-surface">
                        {trainingPlanItemPrefix(item)}：{trainingPlanItemTitle(item)}
                      </span>
                    </div>
                    <span className={`text-xs font-medium ${getStatus(item.status).className}`}>
                      {getStatus(item.status).label}
                    </span>
                  </div>
                  <p className="text-xs text-on-surface-variant">
                    {trainingPlanText(item.reason)}
                  </p>
                  {item.reviewFocus && (
                    <p className="mt-2 rounded-md border border-primary/15 bg-primary/5 px-2 py-1.5 text-xs text-on-surface-variant">
                      <span className="font-medium text-primary">复习重点：</span>
                      {trainingPlanText(item.reviewFocus)}
                    </p>
                  )}
                  <div className="mt-3 flex flex-wrap gap-2">
                    <Link
                      href={action.href}
                      className="inline-flex items-center gap-1 rounded-md border border-primary/20 bg-primary/10 px-2.5 py-1.5 text-xs font-medium text-primary transition hover:bg-primary/15"
                    >
                      <ActionIcon className="h-3.5 w-3.5" />
                      {action.label}
                    </Link>
                    {onItemStatusChange && item.id && (
                      <>
                        <button
                          type="button"
                          disabled={updatingItemId === item.id || item.status === "COMPLETED"}
                          onClick={() => onItemStatusChange(item.id, "COMPLETED")}
                          className="inline-flex items-center gap-1 rounded-md border border-emerald-200 bg-emerald-50 px-2.5 py-1.5 text-xs font-medium text-emerald-700 disabled:opacity-60"
                        >
                          <Check className="h-3.5 w-3.5" />
                          完成
                        </button>
                        <button
                          type="button"
                          disabled={updatingItemId === item.id || item.status === "SKIPPED"}
                          onClick={() => onItemStatusChange(item.id, "SKIPPED")}
                          className="inline-flex items-center gap-1 rounded-md border border-outline-variant/40 bg-surface-container-lowest px-2.5 py-1.5 text-xs font-medium text-on-surface-variant disabled:opacity-60"
                        >
                          <X className="h-3.5 w-3.5" />
                          跳过
                        </button>
                      </>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
