"use client";

import { useEffect, useRef } from "react";
import type { MockInterviewSession } from "@/lib/types";
import { Bot, Send, UserRound } from "lucide-react";

interface Props {
  session: MockInterviewSession;
  answer: string;
  pendingAnswer: string;
  loading: boolean;
  onAnswerChange: (answer: string) => void;
  submitAnswer: () => void;
}

function isAsking(status: MockInterviewSession["status"]) {
  return status === "ASKING_MAIN" || status === "ASKING_FOLLOW_UP";
}

export default function InterviewConversation({
  session,
  answer,
  pendingAnswer,
  loading,
  onAnswerChange,
  submitAnswer,
}: Props) {
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const asking = isAsking(session.status);
  const showCurrentQuestion = asking && Boolean(session.currentQuestion) && !pendingAnswer;

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [session.turns.length, session.currentQuestion, pendingAnswer]);

  const interviewerQuestion = (question: string, muted = false) => (
    <div className={`flex max-w-[92%] items-start gap-4 ${muted ? "opacity-75" : ""}`}>
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-outline-variant bg-surface-variant text-primary">
        <Bot className="h-5 w-5" />
      </div>
      <div className="rounded-2xl rounded-tl-sm border border-outline-variant/70 bg-surface-container-lowest px-5 py-4 text-sm leading-7 text-on-surface shadow-sm">
        {question}
      </div>
    </div>
  );

  const userAnswer = (content: string) => (
    <div className="flex max-w-[92%] flex-row-reverse items-start gap-4 self-end">
      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-primary bg-primary-container text-on-primary">
        <UserRound className="h-5 w-5" />
      </div>
      <div className="rounded-2xl rounded-tr-sm bg-primary px-5 py-4 text-sm leading-7 text-on-primary shadow-sm">
        {content}
      </div>
    </div>
  );

  return (
    <section className="flex min-h-[620px] flex-col overflow-hidden rounded-xl border border-outline-variant/70 bg-surface-container-lowest shadow-sm">
      <div className="flex-1 space-y-6 overflow-y-auto bg-surface px-5 py-6 sm:px-6">
        {session.turns.map((turn) => (
          <div key={turn.id} className="flex flex-col gap-5">
            {interviewerQuestion(turn.question)}
            {userAnswer(turn.userAnswer)}
          </div>
        ))}

        {pendingAnswer && (
          <div className="flex flex-col gap-5">
            {session.currentQuestion && interviewerQuestion(session.currentQuestion)}
            {userAnswer(pendingAnswer)}
            <div className="ml-14 w-fit rounded-2xl rounded-tl-sm border border-outline-variant/70 bg-surface-container-lowest px-5 py-4 text-sm text-on-surface-variant shadow-sm">
              <p>面试官正在评估你的回答...</p>
              <p className="mt-1">面试官在判断是否继续追问...</p>
            </div>
          </div>
        )}

        {showCurrentQuestion && session.currentQuestion && interviewerQuestion(session.currentQuestion)}

        <p className="rounded-lg border border-dashed border-outline-variant/70 bg-surface-container-lowest px-4 py-3 text-xs text-on-surface-variant">
          这里会保留本轮问答记录，方便你复盘表达。
        </p>

        {!asking && !pendingAnswer && (
          <div className="rounded-lg border border-outline-variant/60 bg-surface-container-low p-4 text-sm leading-6 text-on-surface-variant">
            {session.status === "FINISHED"
              ? "你已经完成本轮问题，可以在右侧提前结束并生成报告。"
              : "当前会话没有待回答问题，报告生成后会展示完整复盘。"}
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="border-t border-outline-variant/70 bg-surface-container-lowest p-4">
        <div className="relative rounded-xl border border-outline-variant bg-surface-container-lowest transition focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
          <textarea
            className="min-h-28 w-full resize-none rounded-xl border-0 bg-transparent px-4 py-4 pr-16 text-sm leading-6 text-on-surface outline-none placeholder:text-on-surface-variant focus:ring-0"
            placeholder="输入你的回答..."
            value={answer}
            disabled={loading || !asking}
            onChange={(event) => onAnswerChange(event.target.value)}
          />
          <button
            className="absolute bottom-3 right-3 flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-on-primary shadow-sm transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:bg-primary/45"
            disabled={loading || !asking || !answer.trim()}
            onClick={submitAnswer}
            type="button"
            aria-label={loading ? "评估中" : "发送回答"}
          >
            <Send className="h-5 w-5" />
          </button>
        </div>

        <p className="mt-2 text-xs text-on-surface-variant">
          这里只训练思路、追问、表达和复盘，不直接给完整 Java AC 代码。
        </p>

      </div>
    </section>
  );
}
