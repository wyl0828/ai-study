"use client";

import { useEffect, useRef } from "react";
import type { MockInterviewSession } from "@/lib/types";
import { Bot, ClipboardList, Loader2, Send } from "lucide-react";

interface Props {
  session: MockInterviewSession;
  answer: string;
  pendingAnswer: string;
  loading: boolean;
  onAnswerChange: (answer: string) => void;
  submitAnswer: () => void;
}

type DisplayState = "WAITING_FIRST" | "EVALUATING" | "FOLLOW_UP" | "REPORTED";

function deriveDisplayState(session: MockInterviewSession, pendingAnswer: string): DisplayState {
  if (session.status === "REPORTED") return "REPORTED";
  if (pendingAnswer) return "EVALUATING";
  if (session.status === "ASKING_FOLLOW_UP") return "FOLLOW_UP";
  return "WAITING_FIRST";
}

function isAsking(status: MockInterviewSession["status"]) {
  return status === "ASKING_MAIN" || status === "ASKING_FOLLOW_UP";
}

function currentQuestionType(session: MockInterviewSession) {
  if (session.currentTurnType === "FOLLOW_UP" || session.status === "ASKING_FOLLOW_UP") {
    return "面试官追问";
  }
  return "面试官主问题";
}

function currentQuestionGuidance(session: MockInterviewSession) {
  if (session.currentTurnType === "FOLLOW_UP" || session.status === "ASKING_FOLLOW_UP") {
    return "先回应追问点，再补充机制、边界和一个你最确定的例子。";
  }
  return "先给出核心定义，再按流程、边界和常见误区展开。";
}

function CurrentQuestionCard({ session, displayState }: {
  session: MockInterviewSession;
  displayState: DisplayState;
}) {
  if (displayState === "EVALUATING") {
    return (
      <div className="rounded-lg border border-primary/20 bg-primary/5 p-5">
        <div className="flex items-center gap-2 text-sm font-bold text-primary">
          <Loader2 className="h-5 w-5 animate-spin" />
          面试官正在分析你的回答
        </div>
        <p className="mt-3 text-sm leading-6 text-on-surface-variant">
          分析完成后会给出追问或进入下一题。
        </p>
      </div>
    );
  }

  if (displayState === "REPORTED") {
    return (
      <div className="rounded-lg border border-outline-variant/50 bg-surface-container-low p-5">
        <div className="flex items-center gap-2 text-sm font-bold text-on-surface-variant">
          <Bot className="h-5 w-5" />
          面试已结束
        </div>
        <p className="mt-3 text-sm leading-6 text-on-surface-variant">
          当前没有待回答的问题，报告已在右侧展示。
        </p>
      </div>
    );
  }

  const question = session.currentQuestion;

  return (
    <div className="rounded-lg border border-primary/20 bg-primary/5 p-5">
      <div className="flex items-center gap-2 text-sm font-bold text-primary">
        <Bot className="h-5 w-5" />
        当前问题
      </div>
      {question ? (
        <>
          <p className="mt-4 text-xs font-semibold text-on-surface-variant">{currentQuestionType(session)}</p>
          <h2 className="mt-2 text-xl font-bold leading-8 text-on-surface">{question}</h2>
          <div className="mt-4 rounded-lg border border-primary/15 bg-surface-container-lowest px-4 py-3 text-sm leading-6 text-on-surface-variant">
            <span className="font-semibold text-on-surface">回答建议：</span>
            {currentQuestionGuidance(session)}
          </div>
        </>
      ) : (
        <p className="mt-3 text-sm leading-6 text-on-surface-variant">当前没有待回答的问题。</p>
      )}
    </div>
  );
}

function AnswerComposer({
  answer,
  displayState,
  loading,
  onAnswerChange,
  submitAnswer,
}: {
  answer: string;
  displayState: DisplayState;
  loading: boolean;
  onAnswerChange: (answer: string) => void;
  submitAnswer: () => void;
}) {
  const disabled = displayState === "EVALUATING" || displayState === "REPORTED" || loading;

  return (
    <div className="shrink-0 border-t border-outline-variant/70 bg-surface-container-lowest p-4">
      <div className="mb-2 text-sm font-bold text-on-surface">你的回答</div>
      <div className="relative rounded-lg border border-outline-variant bg-surface-container-lowest transition focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
        <textarea
          className="min-h-[88px] w-full resize-none rounded-lg border-0 bg-transparent px-4 py-3 pr-16 text-sm leading-6 text-on-surface outline-none placeholder:text-on-surface-variant focus:ring-0"
          placeholder={
            displayState === "EVALUATING" ? "面试官正在分析..." :
            displayState === "REPORTED" ? "面试已结束" :
            "输入你的回答..."
          }
          value={answer}
          disabled={disabled}
          onChange={(event) => onAnswerChange(event.target.value)}
        />
        <button
          className="absolute bottom-3 right-3 flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-on-primary shadow-sm transition-colors hover:bg-primary-container disabled:cursor-not-allowed disabled:bg-primary/45"
          disabled={disabled || !answer.trim()}
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
  );
}

function ReviewTurnCard({
  question,
  answer,
  note,
}: {
  question: string;
  answer: string;
  note?: string;
}) {
  return (
    <div className="coach-card p-4">
      <div className="text-xs font-semibold text-on-surface-variant">面试官</div>
      <p className="mt-1 text-sm leading-6 text-on-surface">{question}</p>

      <div className="mt-3 text-xs font-semibold text-on-surface-variant">你的回答</div>
      <p className="mt-1 rounded-lg bg-surface-container-low px-3 py-2 text-sm leading-6 text-on-surface-variant">
        {answer}
      </p>

      {note && (
        <>
          <div className="mt-3 text-xs font-semibold text-on-surface-variant">AI 追问</div>
          <p className="mt-1 text-sm leading-6 text-on-surface-variant">{note}</p>
        </>
      )}
    </div>
  );
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
  const displayState = deriveDisplayState(session, pendingAnswer);
  const asking = isAsking(session.status);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [session.turns.length, session.currentQuestion, pendingAnswer]);

  return (
    <section className="coach-card grid h-[calc(100dvh-300px)] min-h-0 grid-rows-[auto_minmax(0,1fr)_auto] overflow-hidden">
      {/* 当前问题 — 固定顶部 */}
      <div className="shrink-0 bg-surface-container-lowest p-4 pb-3 sm:p-5 sm:pb-3">
        <CurrentQuestionCard session={session} displayState={displayState} />
      </div>

      {/* 最近问答 — 可滚动 */}
      <div className="min-h-0 overflow-y-auto border-t border-outline-variant/70 bg-surface px-4 py-3 sm:px-5">
        <div className="mb-2 flex items-center gap-2 text-xs font-semibold text-on-surface-variant">
          <ClipboardList className="h-4 w-4" />
          最近问答记录
        </div>

        {session.turns.length === 0 && !pendingAnswer ? (
          <p className="rounded-lg border border-dashed border-outline-variant/70 bg-surface-container-lowest px-4 py-3 text-xs text-on-surface-variant">
            回答后，这里会保留问答记录，方便你复盘表达。
          </p>
        ) : (
          <div className="space-y-3">
            {session.turns.map((turn) => (
              <ReviewTurnCard
                key={turn.id}
                question={turn.question}
                answer={turn.userAnswer}
                note={turn.gapSummary || turn.followUpReason || undefined}
              />
            ))}

            {pendingAnswer && displayState === "EVALUATING" && (
              <div className="rounded-lg border border-primary/20 bg-primary/5 p-3 text-sm">
                <p className="line-clamp-2 text-xs font-semibold text-primary">
                  A：{pendingAnswer}
                </p>
                <div className="mt-2 flex items-center gap-2 text-[11px] text-primary/70">
                  <Loader2 className="h-3 w-3 animate-spin" />
                  面试官正在分析回答...
                </div>
              </div>
            )}
          </div>
        )}

        {displayState === "REPORTED" && (
          <div className="mt-3 rounded-lg border border-outline-variant/60 bg-surface-container-low p-3 text-xs leading-5 text-on-surface-variant">
            面试已结束，报告已在右侧展示。
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* 输入区 — 固定底部 */}
      <AnswerComposer
        answer={answer}
        displayState={displayState}
        loading={loading}
        onAnswerChange={onAnswerChange}
        submitAnswer={submitAnswer}
      />
    </section>
  );
}
