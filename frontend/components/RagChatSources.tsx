"use client";

import { BookOpen, FileText, History, NotebookTabs } from "lucide-react";
import type { RagChatSource } from "@/lib/types";

interface RagChatSourcesProps {
  sources: RagChatSource[];
}

const sourceLabels: Record<string, string> = {
  PROBLEM: "题目资料",
  KNOWLEDGE_CARD: "知识卡",
  USER_WEAKNESS: "学习记录",
  WEAKNESS_EVENT: "学习记录",
  AI_DIAGNOSIS: "历史诊断",
  MISTAKE_CARD: "错题卡",
};

function SourceIcon({ sourceType }: { sourceType: string }) {
  if (sourceType === "PROBLEM") {
    return <FileText className="h-4 w-4" />;
  }
  if (sourceType === "KNOWLEDGE_CARD") {
    return <BookOpen className="h-4 w-4" />;
  }
  if (sourceType === "AI_DIAGNOSIS") {
    return <History className="h-4 w-4" />;
  }
  return <NotebookTabs className="h-4 w-4" />;
}

export default function RagChatSources({ sources }: RagChatSourcesProps) {
  return (
    <aside className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-5">
      <div className="mb-4">
        <h2 className="text-sm font-semibold text-on-surface">引用来源</h2>
        <p className="mt-1 text-xs leading-relaxed text-on-surface-variant">
          回答只参考当前知识库命中的题目、知识卡、学习记录、历史诊断和错题卡。
        </p>
      </div>

      {sources.length === 0 ? (
        <div className="rounded-lg border border-dashed border-outline-variant/60 px-4 py-6 text-sm text-on-surface-variant">
          提问后会在这里显示资料来源。
        </div>
      ) : (
        <div className="space-y-3">
          {sources.map((source, index) => (
            <div
              key={`${source.sourceType}-${source.sourceId ?? index}-${index}`}
              className="rounded-lg border border-outline-variant/30 bg-surface px-4 py-3"
            >
              <div className="mb-2 flex items-center justify-between gap-3">
                <div className="flex min-w-0 items-center gap-2 text-xs font-semibold text-primary">
                  <SourceIcon sourceType={source.sourceType} />
                  <span>{sourceLabels[source.sourceType] ?? source.sourceType}</span>
                </div>
                <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-xs text-primary">
                  score {source.score}
                </span>
              </div>
              <div className="text-sm font-medium leading-snug text-on-surface">
                {source.title || `${source.sourceType}#${source.sourceId ?? "-"} `}
              </div>
              <div className="mt-2 text-xs font-medium text-on-surface">
                命中原因：{source.matchReason}
              </div>
              {source.snippet && (
                <p className="mt-2 text-xs leading-relaxed text-on-surface-variant">
                  {source.snippet}
                </p>
              )}
              <div className="mt-2 text-[11px] text-on-surface-variant">
                {source.sourceType}
                {source.sourceId ? ` #${source.sourceId}` : ""}
              </div>
            </div>
          ))}
        </div>
      )}
    </aside>
  );
}
