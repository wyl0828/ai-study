"use client";

import { useState } from "react";
import { BookOpen, Check, Code2, RefreshCw, Sparkles, X } from "lucide-react";
import Link from "next/link";
import type { TrainingPlan as TrainingPlanType } from "@/lib/types";
import {
  selectTodayTrainingItem,
  trainingPlanItemAction,
  trainingPlanItemPrefix,
  trainingPlanItemTitle,
} from "@/lib/learningView";
import {
  knowledgePoint,
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
  const [expanded, setExpanded] = useState(false);

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

  const todayItem = selectTodayTrainingItem(plan);
  const dayIndexes = Object.keys(grouped).map(Number);
  const firstDay = dayIndexes.length > 0 ? Math.min(...dayIndexes) : 1;
  const collapsedGrouped = plan.items.reduce<Record<number, typeof plan.items>>(
    (acc, item) => {
      const isFirstDay = item.dayIndex === firstDay;
      const isTodayItem = todayItem && item.id === todayItem.id;
      if (!isFirstDay && !isTodayItem) {
        return acc;
      }
      if (!acc[item.dayIndex]) acc[item.dayIndex] = [];
      acc[item.dayIndex].push(item);
      return acc;
    },
    {}
  );
  const visibleGrouped = expanded ? grouped : collapsedGrouped;
  const hiddenItemCount = Math.max(
    plan.items.length -
      Object.values(collapsedGrouped).reduce((total, items) => total + items.length, 0),
    0
  );

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

        {Object.entries(visibleGrouped).map(([day, items]) => {
          const title = items[0]?.knowledgePoint
            ? `${knowledgePoint(items[0].knowledgePoint)}专项`
            : `第 ${day} 天训练`;
          const reviewFocus = items[0]?.reviewFocus;

          return (
            <div key={day} className="mb-5 last:mb-0">
              <div className="flex items-center gap-2 mb-3">
                <span className="w-6 h-6 rounded-full bg-primary text-on-primary flex items-center justify-center text-xs font-bold">
                  {day}
                </span>
                <span className="text-sm font-semibold text-on-surface">
                  第 {day} 天 &mdash; {title}
                </span>
              </div>
              <div className="ml-8 space-y-3">
                {items.map((item, i) => {
                  const action = trainingPlanItemAction(item);
                  const ActionIcon = item.itemType === "KNOWLEDGE_CARD" ? BookOpen : Code2;

                  return (
                    <div
                      key={item.id || `${day}-${i}`}
                      className="bg-surface-container rounded-lg border border-outline-variant/30 p-3"
                    >
                      <div className="flex items-center justify-between gap-2 mb-1">
                        <div className="flex items-center gap-2 min-w-0">
                          {item.itemType === "KNOWLEDGE_CARD" ? (
                            <BookOpen className="w-4 h-4 text-primary shrink-0" />
                          ) : (
                            <Code2 className="w-4 h-4 text-primary shrink-0" />
                          )}
                          <span className="text-sm font-medium text-on-surface truncate">
                            {trainingPlanItemPrefix(item)}：{trainingPlanItemTitle(item)}
                          </span>
                        </div>
                        <span
                          className={`text-xs font-medium ${
                            getStatus(item.status).className
                          }`}
                        >
                          {getStatus(item.status).label}
                        </span>
                      </div>
                      <p className="text-xs text-on-surface-variant">
                        {trainingPlanText(item.reason)}
                      </p>
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
              {reviewFocus && (
                <div className="ml-8 mt-2 p-2 bg-primary/5 rounded-lg border border-primary/15">
                  <p className="text-xs text-on-surface-variant">
                    <span className="font-medium text-primary">复习重点：</span>
                    {trainingPlanText(reviewFocus)}
                  </p>
                </div>
              )}
            </div>
          );
        })}

        {hiddenItemCount > 0 && (
          <button
            type="button"
            onClick={() => setExpanded((current) => !current)}
            className="mt-4 w-full rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/10"
          >
            {expanded
              ? "收起完整 3 天计划"
              : `展开完整 3 天计划（还有 ${hiddenItemCount} 项）`}
          </button>
        )}
      </div>
    </section>
  );
}
