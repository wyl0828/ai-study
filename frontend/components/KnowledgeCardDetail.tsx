"use client";

import { ExternalLink, HelpCircle, ListChecks, MessageSquareText } from "lucide-react";
import type { KnowledgeCardDetail as KnowledgeCardDetailType } from "@/lib/types";
import DifficultyBadge from "./DifficultyBadge";

interface KnowledgeCardDetailProps {
  card: KnowledgeCardDetailType | null;
  loading: boolean;
}

export default function KnowledgeCardDetail({ card, loading }: KnowledgeCardDetailProps) {
  if (loading) {
    return (
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5 space-y-4">
        <div className="h-6 w-2/3 bg-surface-container rounded animate-pulse" />
        <div className="h-20 bg-surface-container rounded animate-pulse" />
        <div className="h-24 bg-surface-container rounded animate-pulse" />
      </div>
    );
  }

  if (!card) {
    return (
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-8 text-sm text-on-surface-variant">
        选择一张知识卡片查看答案、追问和记忆要点。
      </div>
    );
  }

  return (
    <article className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
      <div className="flex items-start justify-between gap-3 mb-4">
        <div>
          <div className="text-xs text-primary font-semibold mb-2">{card.label}</div>
          <h2 className="text-lg font-semibold text-on-surface leading-snug">{card.title}</h2>
        </div>
        <DifficultyBadge difficulty={card.difficulty} />
      </div>

      <div className="space-y-5">
        <section>
          <div className="flex items-center gap-2 mb-2">
            <HelpCircle className="w-4 h-4 text-primary" />
            <h3 className="text-sm font-semibold text-on-surface">问题</h3>
          </div>
          <p className="text-sm text-on-surface-variant leading-relaxed">{card.question}</p>
        </section>

        <section>
          <div className="flex items-center gap-2 mb-2">
            <MessageSquareText className="w-4 h-4 text-primary" />
            <h3 className="text-sm font-semibold text-on-surface">标准回答</h3>
          </div>
          <p className="text-sm text-on-surface-variant leading-relaxed whitespace-pre-line">
            {card.answer}
          </p>
        </section>

        {card.followUp && (
          <section className="bg-primary/5 border border-primary/15 rounded-lg p-3">
            <h3 className="text-sm font-semibold text-on-surface mb-1">面试追问</h3>
            <p className="text-sm text-on-surface-variant leading-relaxed">{card.followUp}</p>
          </section>
        )}

        {card.keyPoints.length > 0 && (
          <section>
            <div className="flex items-center gap-2 mb-2">
              <ListChecks className="w-4 h-4 text-primary" />
              <h3 className="text-sm font-semibold text-on-surface">记忆要点</h3>
            </div>
            <ul className="space-y-2">
              {card.keyPoints.map((point) => (
                <li key={point} className="flex gap-2 text-sm text-on-surface-variant">
                  <span className="mt-2 h-1.5 w-1.5 rounded-full bg-primary shrink-0" />
                  <span>{point}</span>
                </li>
              ))}
            </ul>
          </section>
        )}

        <div className="pt-3 border-t border-outline-variant/30 flex flex-wrap gap-2 items-center justify-between">
          <div className="flex flex-wrap gap-1.5">
            {card.tags.map((tag) => (
              <span
                key={tag}
                className="bg-surface-container text-on-surface-variant text-[11px] px-2 py-0.5 rounded-full border border-outline-variant/30"
              >
                {tag}
              </span>
            ))}
          </div>
          {card.sourceUrl && (
            <a
              href={card.sourceUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-xs text-primary hover:underline"
            >
              {card.sourceName || "参考来源"}
              <ExternalLink className="w-3.5 h-3.5" />
            </a>
          )}
        </div>
      </div>
    </article>
  );
}
