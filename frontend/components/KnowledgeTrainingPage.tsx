"use client";

import { useMemo, useState } from "react";
import { BookOpenCheck, Search, SearchX } from "lucide-react";
import KnowledgeCard from "./KnowledgeCard";
import {
  knowledgeCategories,
  knowledgeDifficulties,
  knowledgeTopics,
  type KnowledgeCategory,
  type KnowledgeDifficulty,
} from "@/lib/knowledgeData";

type CategoryFilter = "全部分类" | KnowledgeCategory;
type DifficultyFilter = "全部" | KnowledgeDifficulty;

export default function KnowledgeTrainingPage() {
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("全部");
  const [category, setCategory] = useState<CategoryFilter>("全部分类");
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [masteredIds, setMasteredIds] = useState<Set<number>>(
    () => new Set(knowledgeTopics.filter((topic) => topic.mastered).map((topic) => topic.id))
  );

  const filteredTopics = useMemo(() => {
    const keyword = search.trim().toLowerCase();

    return knowledgeTopics.filter((topic) => {
      if (difficulty !== "全部" && topic.difficulty !== difficulty) return false;
      if (category !== "全部分类" && topic.category !== category) return false;
      if (!keyword) return true;

      return [
        topic.title,
        topic.question,
        topic.category,
        topic.difficulty,
        ...topic.tags,
        ...topic.keyPoints,
      ]
        .join(" ")
        .toLowerCase()
        .includes(keyword);
    });
  }, [category, difficulty, search]);

  const markMastered = (id: number) => {
    setMasteredIds((current) => {
      const next = new Set(current);
      next.add(id);
      return next;
    });
  };

  return (
    <main className="mx-auto max-w-6xl px-6 py-8">
      <div className="mb-6">
        <div className="mb-2 flex items-center gap-2">
          <BookOpenCheck className="h-6 w-6 text-primary" />
          <h1 className="text-2xl font-bold tracking-tight text-on-surface">
            知识训练
          </h1>
        </div>
        <p className="text-sm text-on-surface-variant">
          Java 后端高频面试知识点训练，通过 AI 自测、AI 点评和标杆回答解析形成复习闭环
        </p>
      </div>

      <section className="mb-6 rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-4 shadow-sm">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center">
          <div className="relative lg:w-72">
            <Search className="absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-outline" />
            <input
              type="text"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="搜索知识点、问题或标签..."
              className="w-full rounded-lg border border-outline-variant/40 bg-surface px-9 py-2 text-sm text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {knowledgeDifficulties.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setDifficulty(item)}
                className={`rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
                  difficulty === item
                    ? "bg-primary text-on-primary"
                    : "border border-outline-variant/40 bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
                }`}
              >
                {item}
              </button>
            ))}
          </div>

          <div className="hidden h-5 w-px bg-outline-variant/40 lg:block" />

          <div className="flex flex-wrap items-center gap-2">
            {knowledgeCategories.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => setCategory(item)}
                className={`rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
                  category === item
                    ? "bg-primary text-on-primary"
                    : "border border-outline-variant/40 bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
                }`}
              >
                {item}
              </button>
            ))}
          </div>
        </div>

        <div className="mt-4 flex flex-wrap items-center gap-4 border-t border-outline-variant/25 pt-3 text-xs text-on-surface-variant">
          <span>总知识点 {knowledgeTopics.length}</span>
          <span className="flex items-center gap-1">
            <span className="h-2 w-2 rounded-full bg-emerald-500" />
            已掌握 {masteredIds.size}
          </span>
          <span>当前显示 {filteredTopics.length}</span>
        </div>
      </section>

      {filteredTopics.length === 0 ? (
        <div className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest py-16 text-center text-on-surface-variant">
          <SearchX className="mx-auto mb-2 h-10 w-10" />
          <p className="text-sm">没有匹配的知识点</p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredTopics.map((topic) => (
            <KnowledgeCard
              key={topic.id}
              topic={topic}
              expanded={expandedId === topic.id}
              mastered={masteredIds.has(topic.id)}
              onToggle={() =>
                setExpandedId((current) => (current === topic.id ? null : topic.id))
              }
              onMarkMastered={() => markMastered(topic.id)}
            />
          ))}
        </div>
      )}
    </main>
  );
}
