"use client";

import type { MockInterviewTurn } from "@/lib/types";

interface Props {
  turn: MockInterviewTurn;
}

export default function InterviewTurnFeedback({ turn }: Props) {
  const isFollowUp = turn.turnType === "FOLLOW_UP";
  return (
    <div className={isFollowUp
      ? "mt-3 rounded-lg border border-outline-variant/60 bg-surface-variant/20 p-4"
      : "mt-3 rounded-lg border border-outline-variant/60 bg-surface-variant/30 p-4"}>
      <div className="flex flex-wrap items-center gap-2">
        <span className="rounded-full bg-primary/10 px-2.5 py-1 text-xs font-semibold text-primary">
          {isFollowUp ? "追问反馈" : "面试官反馈"}
        </span>
        <span className="text-sm font-semibold text-on-surface">{turn.performanceLevel}</span>
      </div>
      <p className="mt-2 text-sm leading-6 text-on-surface-variant">{turn.feedback}</p>
      {!isFollowUp && (
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          <div>
            <p className="text-xs font-semibold text-on-surface-variant">回答优点</p>
            <p className="mt-1 text-sm text-on-surface">{turn.strengthSummary}</p>
          </div>
          <div>
            <p className="text-xs font-semibold text-on-surface-variant">需要加强</p>
            <p className="mt-1 text-sm text-on-surface">{turn.gapSummary}</p>
          </div>
        </div>
      )}
      {turn.expressionFeedback && (
        <p className="mt-3 text-sm text-on-surface-variant">
          表达建议：{turn.expressionFeedback}
        </p>
      )}
      {turn.interviewerObservation && (
        <div className="mt-3 rounded-md bg-white/70 p-3 text-sm leading-6 text-on-surface-variant">
          <p className="font-semibold text-on-surface">AI 面试官观察</p>
          <p className="mt-1">{turn.interviewerObservation}</p>
          {turn.followUpReason && <p className="mt-1">{turn.followUpReason}</p>}
        </div>
      )}
    </div>
  );
}
