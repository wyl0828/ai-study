"use client";

import { useState } from "react";
import Link from "next/link";
import { FileWarning, ArrowRight } from "lucide-react";
import type { AggregatedMistakeCard } from "@/lib/learningView";

interface MistakeCardsProps {
  mistakes: AggregatedMistakeCard[];
}

export default function MistakeCards({ mistakes }: MistakeCardsProps) {
  const [showAll, setShowAll] = useState(false);
  const [expandedGroupKeys, setExpandedGroupKeys] = useState<Set<string>>(new Set());
  const cards = mistakes;
  const visibleCards = showAll ? cards : cards.slice(0, 6);
  const hiddenCount = Math.max(cards.length - visibleCards.length, 0);

  const toggleGroup = (groupKey: string) => {
    setExpandedGroupKeys((current) => {
      const next = new Set(current);
      if (next.has(groupKey)) {
        next.delete(groupKey);
      } else {
        next.add(groupKey);
      }
      return next;
    });
  };

  return (
    <section>
      <div className="flex items-center gap-2 mb-4">
        <FileWarning className="w-5 h-5 text-primary" />
        <h2 className="text-lg font-semibold text-on-surface">错题卡片</h2>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
        {cards.length === 0 && (
          <div className="md:col-span-2 xl:col-span-3 bg-surface-container-lowest border border-outline-variant/30 rounded-xl px-5 py-8 text-sm text-on-surface-variant">
            还没有学习数据，去做第一道题并触发 AI 诊断吧。
          </div>
        )}
        {visibleCards.map((m) => {
          const isReturnOrStateIssue =
            m.userFacingErrorTag.includes("返回") || m.userFacingErrorTag.includes("状态");
          const expanded = expandedGroupKeys.has(m.groupKey);
          return (
            <div
              key={m.groupKey}
              className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5 hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between mb-3">
                <span
                  className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                    isReturnOrStateIssue
                      ? "bg-red-50 text-red-700"
                      : "bg-amber-50 text-amber-700"
                  }`}
                >
                  {m.userFacingErrorTag}
                </span>
                <span className="text-xs text-outline whitespace-nowrap">
                  出现 {m.totalOccurrences} 次
                </span>
              </div>
              <h3 className="text-sm font-semibold text-on-surface mb-2">
                {m.patternTitle}
              </h3>
              <div className="space-y-2 text-xs text-on-surface-variant leading-relaxed">
                <div>
                  <span className="font-medium text-on-surface">本质问题：</span>
                  {m.rootCause}
                </div>
                <div>
                  <span className="font-medium text-on-surface">修复动作：</span>
                  {m.fixAction}
                </div>
                <div>
                  <span className="font-medium text-on-surface">复盘口令：</span>
                  {m.reviewScript}
                </div>
                <div>
                  <span className="font-medium text-on-surface">知识点：</span>
                  {m.knowledgePoint}
                </div>
              </div>

              {m.rawRecords.length > 1 && (
                <div className="mt-3">
                  <button
                    type="button"
                    onClick={() => toggleGroup(m.groupKey)}
                    className="ml-auto flex items-center text-xs font-medium text-primary/80 hover:text-primary hover:underline"
                  >
                    {expanded
                      ? "收起复盘记录"
                      : `查看最近 ${m.rawRecords.length} 条复盘记录`}
                  </button>
                  {expanded && (
                    <div className="mt-2 space-y-2 rounded-lg bg-surface-container/50 px-3 py-2 text-xs text-on-surface-variant">
                      <div className="font-medium text-on-surface">最近复盘记录</div>
                      {m.rawRecords.slice(0, 4).map((record) => (
                        <div key={record.id} className="rounded-md bg-surface-container-lowest/80 px-2 py-1.5">
                          <div>{record.summary}</div>
                          {record.correctIdea && record.correctIdea !== record.summary && (
                            <div className="mt-1 text-outline">修正：{record.correctIdea}</div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div className="mt-3 pt-3 border-t border-outline-variant/20 flex items-center justify-between gap-3">
                <span className="text-xs text-on-surface-variant">
                  {m.status === "RESOLVED"
                    ? "已解决"
                    : `最近一次：${m.recentLabel}`}
                </span>
                {m.problemId ? (
                  <Link
                    href={`/problem/${m.problemId}`}
                    className="text-xs text-primary font-medium hover:underline flex items-center gap-1 whitespace-nowrap"
                  >
                    重新练习
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                ) : (
                  <span className="text-xs text-on-surface-variant whitespace-nowrap">
                    原题暂无入口
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </div>
      {cards.length > 6 && (
        <button
          type="button"
          onClick={() => setShowAll((current) => !current)}
          className="mt-4 w-full rounded-lg border border-primary/20 bg-primary/5 px-3 py-2 text-xs font-medium text-primary transition hover:bg-primary/10"
        >
          {showAll ? "收起错题" : `查看全部错题（还有 ${hiddenCount} 张）`}
        </button>
      )}
    </section>
  );
}
