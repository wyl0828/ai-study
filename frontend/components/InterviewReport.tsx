"use client";

import type { MockInterviewReport, MockInterviewTurn } from "@/lib/types";
import { BarChart3, BookOpenCheck, MessageSquareText } from "lucide-react";

interface Props {
  report: MockInterviewReport;
  turns: MockInterviewTurn[];
}

function pointList(points: string[], emptyText: string) {
  if (!points.length) {
    return <p className="text-xs leading-5 text-on-surface-variant">{emptyText}</p>;
  }
  return (
    <ul className="space-y-1 text-xs leading-5 text-on-surface-variant">
      {points.slice(0, 4).map((point) => (
        <li key={point} className="flex gap-2">
          <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
          <span>{point}</span>
        </li>
      ))}
    </ul>
  );
}

function turnTypeLabel(turn: MockInterviewTurn) {
  return turn.turnType === "FOLLOW_UP" ? "追问" : "主问题";
}

export default function InterviewReport({ report, turns }: Props) {
  return (
    <section className="h-full overflow-y-auto rounded-lg border border-outline-variant/50 bg-white p-5">
      <div className="flex items-center gap-2 font-semibold text-primary">
        <BarChart3 className="h-4 w-4" />
        面试报告
      </div>
      <div className="mt-4 rounded-lg bg-primary/5 p-4">
        <p className="text-sm text-on-surface-variant">总体表现</p>
        <p className="mt-1 text-3xl font-bold text-primary">{report.averageScore}</p>
      </div>
      <div className="mt-4 space-y-3 text-sm leading-6 text-on-surface">
        <p>{report.summary}</p>
        <p>表现较好：{report.strengths}</p>
        <p>需要加强：{report.weaknesses || "暂无明显集中薄弱点"}</p>
        <p>追问表现：{report.weaknessTags.length ? `追问中主要卡在 ${report.weaknessTags.join("、")}` : "追问回答整体比较稳定"}</p>
        <p>表达建议：{report.expressionAdvice}</p>
      </div>
      <div className="mt-4 border-t border-outline-variant/50 pt-4">
        <div className="flex items-center gap-2 text-sm font-semibold text-on-surface">
          <BookOpenCheck className="h-4 w-4 text-primary" />
          推荐复习卡片
        </div>
        <p className="mt-2 text-sm text-on-surface-variant">
          {report.recommendedCardIds.length
            ? report.recommendedCardIds.map((id) => `知识卡 #${id}`).join("、")
            : "暂无明确卡片推荐"}
        </p>
      </div>

      <div className="mt-5 border-t border-outline-variant/50 pt-4">
        <div className="flex items-center gap-2 text-sm font-semibold text-on-surface">
          <MessageSquareText className="h-4 w-4 text-primary" />
          答题复盘
        </div>
        {turns.length === 0 ? (
          <p className="mt-2 text-sm text-on-surface-variant">
            暂无逐轮问答记录。
          </p>
        ) : (
          <div className="mt-3 space-y-3">
            {turns.map((turn) => (
              <article
                key={turn.id}
                className="rounded-lg border border-outline-variant/50 bg-surface-container-lowest p-3"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="text-xs font-semibold text-primary">
                    第 {turn.turnOrder} 轮 · {turnTypeLabel(turn)}
                  </span>
                  <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-medium text-primary">
                    {turn.score ?? "--"}/100
                  </span>
                </div>
                <div className="mt-3 text-xs font-semibold text-on-surface-variant">面试官问题</div>
                <p className="mt-1 text-sm leading-6 text-on-surface">{turn.question}</p>
                <div className="mt-3 text-xs font-semibold text-on-surface-variant">你的回答</div>
                <p className="mt-1 rounded-md bg-surface-container-low px-3 py-2 text-sm leading-6 text-on-surface-variant">
                  {turn.userAnswer || "未记录回答"}
                </p>
                <div className="mt-3 grid gap-3">
                  <div>
                    <div className="mb-1 text-xs font-semibold text-on-surface">命中要点</div>
                    {pointList(turn.hitKeyPoints, "这一轮暂未记录明确命中点。")}
                  </div>
                  <div>
                    <div className="mb-1 text-xs font-semibold text-on-surface">缺失要点</div>
                    {pointList(turn.missingKeyPoints, "这一轮暂未记录明确缺失点。")}
                  </div>
                </div>
                {(turn.expressionIssue || turn.expressionFeedback) && (
                  <p className="mt-3 rounded-md bg-primary/5 px-3 py-2 text-xs leading-5 text-on-surface-variant">
                    表达问题：{turn.expressionIssue || turn.expressionFeedback}
                  </p>
                )}
              </article>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}
