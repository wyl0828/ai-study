"use client";

import { AlertTriangle, CheckCircle2, Lightbulb } from "lucide-react";
import type { SelfTestFeedback } from "@/lib/knowledgeData";

interface KnowledgeFeedbackProps {
  feedback: SelfTestFeedback | null;
}

const scoreColorMap: Record<SelfTestFeedback["level"], string> = {
  low: "text-red-600 bg-red-50 border-red-100",
  medium: "text-amber-700 bg-amber-50 border-amber-100",
  high: "text-emerald-700 bg-emerald-50 border-emerald-100",
};

export default function KnowledgeFeedback({ feedback }: KnowledgeFeedbackProps) {
  if (!feedback) {
    return (
      <div className="rounded-lg border border-outline-variant/25 bg-surface-container-lowest px-3 py-2">
        <div className="flex items-center gap-2">
          <Lightbulb className="h-3.5 w-3.5 text-outline" />
          <h4 className="text-sm font-semibold text-on-surface">点评反馈</h4>
          <p className="text-xs text-on-surface-variant">
            提交自测后，这里会给出评分、点评和核心记忆点。
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-amber-100 bg-amber-50/70 p-4">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-3">
        <div className="flex items-center gap-2">
          <Lightbulb className="h-4 w-4 text-amber-600" />
          <h4 className="text-sm font-semibold text-on-surface">点评反馈</h4>
        </div>
        <span
          className={`rounded-full border px-2.5 py-1 text-xs font-bold ${
            scoreColorMap[feedback.level]
          }`}
        >
          {feedback.score}/100
        </span>
      </div>

      <p className="text-sm text-on-surface leading-relaxed mb-3">
        {feedback.comment}
      </p>

      <div className="space-y-4">
        <div>
        <div className="mb-2 text-xs font-semibold text-on-surface-variant">
          命中的核心记忆点
        </div>
        {feedback.matchedKeyPoints.length > 0 ? (
          <ul className="space-y-2">
            {feedback.matchedKeyPoints.map((point) => (
              <li key={point} className="flex gap-2 text-xs text-on-surface-variant">
                <CheckCircle2 className="mt-0.5 h-3.5 w-3.5 shrink-0 text-emerald-600" />
                <span>{point}</span>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-xs text-on-surface-variant">
            暂未命中核心记忆点，建议先围绕题目中的机制、触发条件和优化目的补全回答。
          </p>
        )}
        </div>

        {feedback.missingKeyPoints.length > 0 && (
          <div>
            <div className="mb-2 text-xs font-semibold text-on-surface-variant">
              缺失要点
            </div>
            <ul className="space-y-2">
              {feedback.missingKeyPoints.map((point) => (
                <li
                  key={point}
                  className="flex gap-2 text-xs text-on-surface-variant"
                >
                  <AlertTriangle className="mt-0.5 h-3.5 w-3.5 shrink-0 text-amber-600" />
                  <span>{point}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  );
}
