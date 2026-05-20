"use client";

import { useState } from "react";
import { BarChart3 } from "lucide-react";
import type { AggregatedWeakness } from "@/lib/learningView";

interface WeaknessListProps {
  weaknesses: AggregatedWeakness[];
}

export default function WeaknessList({ weaknesses }: WeaknessListProps) {
  const [showAll, setShowAll] = useState(false);
  const sorted = weaknesses;
  const visibleWeaknesses = showAll ? sorted : sorted.slice(0, 5);
  const hiddenCount = Math.max(sorted.length - visibleWeaknesses.length, 0);

  const rankColors = [
    { bg: "bg-red-100 text-red-600", error: "text-error" },
    { bg: "bg-amber-100 text-amber-600", error: "text-amber-600" },
    { bg: "bg-blue-100 text-blue-600", error: "text-primary" },
  ];

  return (
    <section>
      <div className="flex items-center gap-2 mb-4">
        <BarChart3 className="w-5 h-5 text-primary" />
        <h2 className="text-lg font-semibold text-on-surface">薄弱知识点排行</h2>
      </div>
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl overflow-hidden">
        {sorted.length === 0 && (
          <div className="px-5 py-8 text-sm text-on-surface-variant">
            还没有学习数据，去做第一道题并触发 AI 诊断吧。
          </div>
        )}
        {visibleWeaknesses.map((w, i) => {
          const colors = rankColors[i] || rankColors[2];
          return (
            <div
              key={w.canonicalKey}
              className={`flex items-center justify-between px-5 py-4 hover:bg-surface-container-low transition-colors ${
                i < visibleWeaknesses.length - 1 ? "border-b border-outline-variant/20" : ""
              }`}
            >
              <div className="flex items-center gap-3">
                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold ${colors.bg}`}
                >
                  {i + 1}
                </div>
                <div>
                  <div className="text-sm font-semibold text-on-surface">
                    {w.canonicalName}
                  </div>
                  <div className="text-xs text-on-surface-variant">
                    {w.errorType} · 薄弱分 {w.weaknessScore}
                  </div>
                  {i < 2 && w.trendLabel && (
                    <div className="mt-1 inline-flex rounded-md bg-primary/10 px-2 py-0.5 text-[11px] font-medium text-primary">
                      {w.trendLabel}
                    </div>
                  )}
                </div>
              </div>
              <div className="text-right">
                <div className={`text-sm font-semibold ${colors.error}`}>
                  错误 {w.wrongCount} 次
                </div>
                <div className="text-xs text-on-surface-variant">
                  合并 {w.sourceCount} 条
                </div>
                {typeof w.lastDeltaScore === "number" && (
                  <div className="text-[11px] text-on-surface-variant">
                    最近变化 +{w.lastDeltaScore}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
      {sorted.length > 5 && (
        <button
          type="button"
          onClick={() => setShowAll((current) => !current)}
          className="mt-4 w-full rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/10"
        >
          {showAll ? "收起薄弱点" : `查看全部薄弱点（还有 ${hiddenCount} 条）`}
        </button>
      )}
    </section>
  );
}
