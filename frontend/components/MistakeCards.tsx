"use client";

import Link from "next/link";
import { FileWarning, ArrowRight } from "lucide-react";
import type { MistakeCard } from "@/lib/types";
import { buildMistakeCardViews } from "@/lib/learningView";

interface MistakeCardsProps {
  mistakes: MistakeCard[];
}

export default function MistakeCards({ mistakes }: MistakeCardsProps) {
  const cards = buildMistakeCardViews(mistakes);

  return (
    <section>
      <div className="flex items-center gap-2 mb-4">
        <FileWarning className="w-5 h-5 text-primary" />
        <h2 className="text-lg font-semibold text-on-surface">错题卡片</h2>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {cards.length === 0 && (
          <div className="md:col-span-2 bg-surface-container-lowest border border-outline-variant/30 rounded-xl px-5 py-8 text-sm text-on-surface-variant">
            还没有学习数据，去做第一道题并触发 AI 诊断吧。
          </div>
        )}
        {cards.map((m) => {
          const isError = m.errorType === "逻辑错误";
          return (
            <div
              key={m.id}
              className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5 hover:shadow-md transition-shadow"
            >
              <div className="flex items-center justify-between mb-3">
                <span
                  className={`text-xs font-semibold px-2 py-0.5 rounded-full ${
                    isError
                      ? "bg-red-50 text-red-700"
                      : "bg-amber-50 text-amber-700"
                  }`}
                >
                  {m.errorType}
                </span>
                <span className="text-xs text-outline">#{m.problemId}</span>
              </div>
              <h3 className="text-sm font-semibold text-on-surface mb-2">
                {m.problemTitle}
              </h3>
              <div className="space-y-2 text-xs text-on-surface-variant leading-relaxed">
                <div>
                  <span className="font-medium text-on-surface">错误原因：</span>
                  {m.errorReason}
                </div>
                <div>
                  <span className="font-medium text-on-surface">错误代码行为：</span>
                  {m.codeBehavior}
                </div>
                <div>
                  <span className="font-medium text-on-surface">AI 诊断结果：</span>
                  {m.aiDiagnosis}
                </div>
                <div>
                  <span className="font-medium text-on-surface">推荐复习知识点：</span>
                  {m.reviewPoint}
                </div>
                <div>
                  <span className="font-medium text-on-surface">下次训练建议：</span>
                  {m.nextTrainingAdvice}
                </div>
              </div>
              <div className="mt-3 pt-3 border-t border-outline-variant/20 flex items-center justify-between">
                <span className="text-xs text-on-surface-variant">
                  {m.count > 1 ? `已合并 ${m.count} 条相似记录` : m.knowledgePoint}
                </span>
                <Link
                  href={`/problem/${m.problemId}`}
                  className="text-xs text-primary font-medium hover:underline flex items-center gap-1"
                >
                  重新练习
                  <ArrowRight className="w-3.5 h-3.5" />
                </Link>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
