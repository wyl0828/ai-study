"use client";

import { Sparkles, RefreshCw } from "lucide-react";
import type { TrainingPlan as TrainingPlanType } from "@/lib/types";
import { knowledgePoint, problemTitle, trainingPlanTitle } from "@/lib/i18n";

interface TrainingPlanProps {
  plan: TrainingPlanType | null;
}

export default function TrainingPlan({ plan }: TrainingPlanProps) {
  if (!plan) {
    return (
      <section>
        <div className="flex items-center gap-2 mb-4">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">训练计划</h2>
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
    if (status === "COMPLETED") {
      return { label: "已通过", className: "text-emerald-600" };
    }
    if (status === "RETRY" || status === "NEEDS_REVIEW") {
      return { label: "需要重做", className: "text-amber-600" };
    }
    return { label: "待完成", className: "text-primary" };
  };

  return (
    <section>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">训练计划</h2>
        </div>
        <span className="text-xs bg-primary/10 text-primary px-3 py-1.5 rounded-lg font-medium flex items-center gap-1">
          <RefreshCw className="w-3.5 h-3.5" />
          Agent 生成
        </span>
      </div>

      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
        <h3 className="text-base font-semibold text-on-surface mb-1">
          {trainingPlanTitle(plan.title)}
        </h3>
        <p className="text-xs text-on-surface-variant mb-5 leading-relaxed">
          {plan.summary}
        </p>

        {Object.entries(grouped).map(([day, items]) => {
          const dayNum = Number(day);
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
                {items.map((item, i) => (
                  <div
                    key={i}
                    className="bg-surface-container rounded-lg border border-outline-variant/30 p-3"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-sm font-medium text-on-surface">
                        {problemTitle(item.problemTitle)}
                      </span>
                      <span
                        className={`text-xs font-medium ${
                          getStatus(item.status).className
                        }`}
                      >
                        {getStatus(item.status).label}
                      </span>
                    </div>
                    <p className="text-xs text-on-surface-variant">
                      {item.reason}
                    </p>
                  </div>
                ))}
              </div>
              {reviewFocus && (
                <div className="ml-8 mt-2 p-2 bg-primary/5 rounded-lg border border-primary/15">
                  <p className="text-xs text-on-surface-variant">
                    <span className="font-medium text-primary">复习重点：</span>
                    {reviewFocus}
                  </p>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}
