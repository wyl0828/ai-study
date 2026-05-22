"use client";

import type { MockInterviewReport } from "@/lib/types";
import { BarChart3, BookOpenCheck } from "lucide-react";

interface Props {
  report: MockInterviewReport;
}

export default function InterviewReport({ report }: Props) {
  return (
    <section className="border border-outline-variant/50 bg-white rounded-lg p-5">
      <div className="flex items-center gap-2 text-primary font-semibold">
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
    </section>
  );
}
