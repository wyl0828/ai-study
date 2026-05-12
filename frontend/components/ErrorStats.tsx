"use client";

import { BarChart3, TrendingDown } from "lucide-react";
import type { ErrorStatsVO } from "@/lib/types";
import { errorTypeName, knowledgePoint } from "@/lib/i18n";

interface ErrorStatsProps {
  stats: ErrorStatsVO | null;
  loading: boolean;
}

export default function ErrorStats({ stats, loading }: ErrorStatsProps) {
  if (loading) {
    return (
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
        <div className="flex items-center gap-2 mb-4">
          <BarChart3 className="w-5 h-5 text-primary" />
          <h3 className="text-sm font-semibold text-on-surface">错误模式分析</h3>
        </div>
        <div className="space-y-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-8 bg-surface-container rounded animate-pulse" />
          ))}
        </div>
      </div>
    );
  }

  if (!stats) {
    return null;
  }

  const hasDistribution = stats.errorTypeDistribution.length > 0;
  const hasWeakPoints = stats.topWeakPoints.length > 0;

  if (!hasDistribution && !hasWeakPoints) {
    return (
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
        <div className="flex items-center gap-2 mb-4">
          <BarChart3 className="w-5 h-5 text-primary" />
          <h3 className="text-sm font-semibold text-on-surface">错误模式分析</h3>
        </div>
        <p className="text-xs text-on-surface-variant">暂无错误数据，去提交代码吧</p>
      </div>
    );
  }

  const maxCount = hasDistribution
    ? Math.max(...stats.errorTypeDistribution.map((d) => d.count))
    : 0;

  return (
    <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5 space-y-5">
      <div className="flex items-center gap-2">
        <BarChart3 className="w-5 h-5 text-primary" />
        <h3 className="text-sm font-semibold text-on-surface">错误模式分析</h3>
      </div>

      {/* 错误类型分布 */}
      {hasDistribution && (
        <div className="space-y-2">
          <h4 className="text-xs font-medium text-on-surface-variant uppercase tracking-wide">
            错误类型分布
          </h4>
          <div className="space-y-2">
            {stats.errorTypeDistribution.map((item) => (
              <div key={item.errorType} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-on-surface-variant">
                    {errorTypeName(item.errorType)}
                  </span>
                  <span className="text-on-surface font-medium">{item.count} 次</span>
                </div>
                <div className="h-2 bg-surface-container rounded-full overflow-hidden">
                  <div
                    className="h-full bg-primary/60 rounded-full transition-all"
                    style={{ width: `${(item.count / maxCount) * 100}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 最薄弱知识点 */}
      {hasWeakPoints && (
        <div className="space-y-2">
          <h4 className="text-xs font-medium text-on-surface-variant uppercase tracking-wide flex items-center gap-1">
            <TrendingDown className="w-3.5 h-3.5" />
            最薄弱知识点
          </h4>
          <div className="space-y-2">
            {stats.topWeakPoints.map((point, i) => (
              <div
                key={`${point.knowledgePoint}-${point.errorType}`}
                className="flex items-center gap-3 bg-surface-container rounded-lg p-3"
              >
                <span className="text-xs font-semibold text-primary bg-primary/10 rounded-full w-5 h-5 flex items-center justify-center shrink-0">
                  {i + 1}
                </span>
                <div className="flex-1 min-w-0">
                  <div className="text-xs font-medium text-on-surface truncate">
                    {knowledgePoint(point.knowledgePoint)}
                  </div>
                  <div className="text-[10px] text-on-surface-variant">
                    {errorTypeName(point.errorType)} · {point.wrongCount} 次错误
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-xs font-semibold text-amber-600">
                    {point.weaknessScore.toFixed(1)}
                  </div>
                  <div className="text-[10px] text-on-surface-variant">弱点分</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
