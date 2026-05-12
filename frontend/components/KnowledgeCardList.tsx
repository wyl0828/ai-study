"use client";

import { BookOpen, SearchX } from "lucide-react";
import type { KnowledgeCardListItem } from "@/lib/types";
import DifficultyBadge from "./DifficultyBadge";

interface KnowledgeCardListProps {
  cards: KnowledgeCardListItem[];
  activeId: number | null;
  loading: boolean;
  onSelect: (id: number) => void;
}

export default function KnowledgeCardList({
  cards,
  activeId,
  loading,
  onSelect,
}: KnowledgeCardListProps) {
  if (loading) {
    return (
      <div className="space-y-3">
        {[1, 2, 3, 4].map((i) => (
          <div
            key={i}
            className="h-28 bg-surface-container-lowest border border-outline-variant/30 rounded-xl animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (cards.length === 0) {
    return (
      <div className="text-center py-16 text-on-surface-variant bg-surface-container-lowest border border-outline-variant/30 rounded-xl">
        <SearchX className="w-10 h-10 mx-auto mb-2" />
        没有匹配的知识卡片
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {cards.map((card) => {
        const active = activeId === card.id;
        return (
          <button
            key={card.id}
            type="button"
            onClick={() => onSelect(card.id)}
            className={`w-full text-left bg-surface-container-lowest border rounded-xl p-4 transition-all ${
              active
                ? "border-primary/50 shadow-sm ring-1 ring-primary/20"
                : "border-outline-variant/30 hover:border-primary/30 hover:shadow-sm"
            }`}
          >
            <div className="flex items-start justify-between gap-3 mb-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2 mb-2">
                  <BookOpen className="w-4 h-4 text-primary shrink-0" />
                  <span className="text-xs font-medium text-primary">{card.label}</span>
                </div>
                <h3 className="text-sm font-semibold text-on-surface leading-snug">
                  {card.title}
                </h3>
              </div>
              <DifficultyBadge difficulty={card.difficulty} />
            </div>
            <p className="text-xs text-on-surface-variant leading-relaxed mb-3 line-clamp-2">
              {card.question}
            </p>
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
          </button>
        );
      })}
    </div>
  );
}
