"use client";

import { Bot, UserRound } from "lucide-react";
import type { RagChatSource } from "@/lib/types";

interface RagChatMessageProps {
  role: "user" | "assistant";
  content: string;
  sources?: RagChatSource[];
  selected?: boolean;
  onSelect?: () => void;
}

const sourceLabels: Record<string, string> = {
  PROBLEM: "题目资料",
  KNOWLEDGE_CARD: "知识卡",
  USER_WEAKNESS: "学习记录",
  WEAKNESS_EVENT: "学习记录",
  MISTAKE_CARD: "错题卡",
  AI_DIAGNOSIS: "历史诊断",
};

export default function RagChatMessage({
  role,
  content,
  sources = [],
  selected = false,
  onSelect,
}: RagChatMessageProps) {
  const isUser = role === "user";
  const clickable = !isUser && sources.length > 0 && onSelect;
  const bubbleClassName = isUser
    ? "max-w-[82%] rounded-xl bg-primary px-4 py-3 text-left text-sm leading-relaxed text-on-primary"
    : selected
    ? "max-w-[82%] cursor-pointer rounded-xl border border-primary/40 bg-primary/5 px-4 py-3 text-left text-sm leading-relaxed text-on-surface"
    : "max-w-[82%] rounded-xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 text-left text-sm leading-relaxed text-on-surface";
  const bubbleContent = (
    <>
      <p>{content}</p>
      {!isUser && sources.length > 0 && (
        <div className="mt-3 space-y-2 border-t border-outline-variant/30 pt-3">
          {sources.slice(0, 3).map((source, index) => (
            <div
              key={`${source.sourceType}-${source.sourceId ?? index}-${index}`}
              className="rounded-lg bg-surface px-3 py-2 text-xs text-on-surface-variant"
            >
              <div className="font-semibold text-primary">
                {sourceLabels[source.sourceType] ?? source.sourceType}
                {source.title ? `｜${source.title}` : ""}
              </div>
              <div className="mt-1">命中原因：{source.matchReason}</div>
            </div>
          ))}
        </div>
      )}
    </>
  );

  return (
    <div className={`flex gap-3 ${isUser ? "justify-end" : "justify-start"}`}>
      {!isUser && (
        <div className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary">
          <Bot className="h-4 w-4" />
        </div>
      )}
      {clickable ? (
        <button type="button" onClick={onSelect} className={bubbleClassName}>
          {bubbleContent}
        </button>
      ) : (
        <div className={bubbleClassName}>{bubbleContent}</div>
      )}
      {isUser && (
        <div className="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-surface-variant text-on-surface-variant">
          <UserRound className="h-4 w-4" />
        </div>
      )}
    </div>
  );
}
