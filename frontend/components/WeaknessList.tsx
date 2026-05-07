"use client";

import { BarChart3 } from "lucide-react";
import type { UserWeakness } from "@/lib/types";
import { categoryName, knowledgePoint } from "@/lib/i18n";

interface WeaknessListProps {
  weaknesses: UserWeakness[];
}

export default function WeaknessList({ weaknesses }: WeaknessListProps) {
  const sorted = [...weaknesses].sort(
    (a, b) => b.weaknessScore - a.weaknessScore
  );

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
        {sorted.map((w, i) => {
          const colors = rankColors[i] || rankColors[2];
          return (
            <div
              key={w.id}
              className={`flex items-center justify-between px-5 py-4 hover:bg-surface-container-low transition-colors ${
                i < sorted.length - 1 ? "border-b border-outline-variant/20" : ""
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
                    {knowledgePoint(w.knowledgePoint)}
                  </div>
                  <div className="text-xs text-on-surface-variant">
                    {categoryName(w.category)} &middot; 关联题目 {w.relatedProblemCount} 道
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className={`text-sm font-semibold ${colors.error}`}>
                  错误 {w.errorCount} 次
                </div>
                <div className="text-xs text-on-surface-variant">
                  薄弱分数 {w.weaknessScore}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
