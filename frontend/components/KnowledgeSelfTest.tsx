"use client";

import { useState } from "react";
import { Bot, SendHorizontal } from "lucide-react";
import {
  evaluateSelfTest,
  type KnowledgeTopic,
  type SelfTestFeedback,
} from "@/lib/knowledgeData";

interface KnowledgeSelfTestProps {
  topic: KnowledgeTopic;
  onFeedback: (feedback: SelfTestFeedback, answer: string) => void;
  onSkip: () => void;
}

export default function KnowledgeSelfTest({
  topic,
  onFeedback,
  onSkip,
}: KnowledgeSelfTestProps) {
  const [answer, setAnswer] = useState("");
  const [error, setError] = useState("");

  const submitSelfTest = () => {
    if (!answer.trim()) {
      setError("请先输入你的回答");
      return;
    }

    setError("");
    onFeedback(evaluateSelfTest(answer, topic), answer);
  };

  return (
    <section className="rounded-lg border border-primary/15 bg-primary/5 p-4">
      <div className="flex items-center gap-2 mb-3">
        <Bot className="h-4 w-4 text-primary" />
        <h4 className="text-sm font-semibold text-on-surface">模拟自测</h4>
      </div>
      <textarea
        value={answer}
        onChange={(event) => {
          setAnswer(event.target.value);
          if (error) setError("");
        }}
        rows={6}
        placeholder="先像面试现场一样口述或写下你的回答，再查看标杆回答解析。"
        className="w-full resize-y rounded-lg border border-outline-variant/50 bg-surface-container-lowest px-3 py-2 text-sm leading-relaxed text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
      />
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-1">
          <p className="text-xs text-error">{error}</p>
          <p className="text-xs text-on-surface-variant">
            提交自测后，这里会给出评分、点评和核心记忆点。
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          <button
            type="button"
            onClick={onSkip}
            className="text-xs font-semibold text-primary hover:underline"
          >
            跳过自测，直接查看解析
          </button>
          <button
            type="button"
            onClick={submitSelfTest}
            className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-2 text-xs font-semibold text-on-primary shadow-sm transition hover:bg-primary-container"
          >
            <SendHorizontal className="h-3.5 w-3.5" />
            提交自测
          </button>
        </div>
      </div>
    </section>
  );
}
