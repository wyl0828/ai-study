"use client";

import { useState } from "react";
import {
  BookOpenText,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  HelpCircle,
  ListChecks,
  MessagesSquare,
} from "lucide-react";
import type { KnowledgeTopic, SelfTestFeedback } from "@/lib/knowledgeData";
import KnowledgeFeedback from "./KnowledgeFeedback";
import KnowledgeSelfTest from "./KnowledgeSelfTest";

interface KnowledgeCardProps {
  topic: KnowledgeTopic;
  expanded: boolean;
  mastered: boolean;
  onToggle: () => void;
  onMarkMastered: () => void;
}

const difficultyStyle: Record<KnowledgeTopic["difficulty"], string> = {
  简单: "bg-emerald-50 text-emerald-700 border-emerald-100",
  中等: "bg-amber-50 text-amber-700 border-amber-100",
  困难: "bg-red-50 text-red-700 border-red-100",
};

const categoryStyle: Record<KnowledgeTopic["category"], string> = {
  Java: "bg-blue-50 text-blue-700 border-blue-100",
  MySQL: "bg-violet-50 text-violet-700 border-violet-100",
  Redis: "bg-rose-50 text-rose-700 border-rose-100",
  Spring: "bg-emerald-50 text-emerald-700 border-emerald-100",
  JVM: "bg-slate-50 text-slate-700 border-slate-200",
};

export default function KnowledgeCard({
  topic,
  expanded,
  mastered,
  onToggle,
  onMarkMastered,
}: KnowledgeCardProps) {
  const [feedback, setFeedback] = useState<SelfTestFeedback | null>(null);
  const [showAnalysis, setShowAnalysis] = useState(false);

  const handleFeedback = (nextFeedback: SelfTestFeedback) => {
    setFeedback(nextFeedback);
    setShowAnalysis(true);
  };

  const skipSelfTest = () => {
    setFeedback(null);
    setShowAnalysis(true);
  };

  return (
    <article
      className={`overflow-hidden rounded-xl border bg-surface-container-lowest shadow-sm transition-all ${
        expanded
          ? "border-primary/25 border-l-4 border-l-primary shadow-md ring-1 ring-primary/10"
          : "border-outline-variant/30 hover:border-primary/25 hover:shadow-md"
      }`}
    >
      <button
        type="button"
        onClick={onToggle}
        className="w-full text-left outline-none focus-visible:ring-2 focus-visible:ring-primary/20"
        aria-expanded={expanded}
      >
        <div className="p-5">
          <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <span
                className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${
                  difficultyStyle[topic.difficulty]
                }`}
              >
                {topic.difficulty}
              </span>
              <span
                className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${
                  categoryStyle[topic.category]
                }`}
              >
                {topic.category}
              </span>
              {mastered && (
                <span className="inline-flex items-center gap-1 rounded-full border border-emerald-100 bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  已掌握
                </span>
              )}
            </div>
            {expanded ? (
              <ChevronUp className="h-5 w-5 shrink-0 text-outline" />
            ) : (
              <ChevronDown className="h-5 w-5 shrink-0 text-outline" />
            )}
          </div>

          <div className="mb-3 flex flex-wrap gap-1.5">
            {topic.tags.map((tag) => (
              <span
                key={tag}
                className="rounded-full border border-outline-variant/30 bg-surface-container px-2 py-0.5 text-[11px] text-on-surface-variant"
              >
                {tag}
              </span>
            ))}
          </div>

          <h2 className="mb-2 text-base font-bold leading-snug text-on-surface">
            {topic.title}
          </h2>
          <p className="mb-4 text-sm leading-relaxed text-on-surface-variant">
            {topic.question}
          </p>
          <div className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary">
            <BookOpenText className="h-3.5 w-3.5" />
            查看解析，或在此之前使用 AI 自测
          </div>
        </div>
      </button>

      {expanded && (
        <div className="border-t border-primary/10 px-5 pb-5 pt-4">
          <div className="space-y-5">
            <KnowledgeSelfTest
              topic={topic}
              onFeedback={handleFeedback}
              onSkip={skipSelfTest}
            />

            {showAnalysis && (
              <div className="space-y-4">
                {feedback && <KnowledgeFeedback feedback={feedback} />}

                <section className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <ListChecks className="h-4 w-4 text-primary" />
                    <h3 className="text-sm font-semibold text-on-surface">
                      标杆回答解析
                    </h3>
                  </div>
                  <p className="mb-4 text-sm leading-relaxed text-on-surface-variant">
                    {topic.referenceAnswer}
                  </p>
                  <div className="mb-2 text-xs font-semibold text-on-surface">
                    核心记忆要点
                  </div>
                  <div className="space-y-2">
                    {topic.keyPoints.map((point) => (
                      <div key={point} className="flex gap-2 text-sm text-on-surface-variant">
                        <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                        <span>{point}</span>
                      </div>
                    ))}
                  </div>
                  {topic.sourceName && (
                    <div className="mt-4 border-t border-outline-variant/20 pt-3 text-xs text-on-surface-variant">
                      选题参考：
                      {topic.sourceUrl ? (
                        <a
                          href={topic.sourceUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="font-medium text-primary hover:underline"
                        >
                          {topic.sourceName}
                        </a>
                      ) : (
                        <span>{topic.sourceName}</span>
                      )}
                    </div>
                  )}
                </section>

                <section className="rounded-lg border border-outline-variant/30 bg-surface-container-low p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <MessagesSquare className="h-4 w-4 text-primary" />
                    <h3 className="text-sm font-semibold text-on-surface">
                      面试官高频追问
                    </h3>
                  </div>
                  <ul className="space-y-2">
                    {topic.followUpQuestions.map((question) => (
                      <li key={question} className="flex gap-2 text-sm text-on-surface-variant">
                        <HelpCircle className="mt-0.5 h-4 w-4 shrink-0 text-primary/70" />
                        <span>{question}</span>
                      </li>
                    ))}
                  </ul>
                </section>

                <div className="flex flex-wrap justify-end gap-3 pt-1">
                  <button
                    type="button"
                    onClick={onMarkMastered}
                    disabled={mastered}
                    className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-semibold transition ${
                      mastered
                        ? "cursor-default border-emerald-100 bg-emerald-50 text-emerald-700"
                        : "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                    }`}
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {mastered ? "已掌握" : "标记已掌握"}
                  </button>
                  <button
                    type="button"
                    onClick={onToggle}
                    className="rounded-lg border border-outline-variant/40 bg-surface-container-lowest px-3 py-2 text-xs font-semibold text-on-surface-variant transition hover:bg-surface-container"
                  >
                    收起解析
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </article>
  );
}
