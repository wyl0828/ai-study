"use client";

import { Search, SlidersHorizontal } from "lucide-react";
import {
  knowledgeDifficulties,
  type KnowledgeDifficulty,
} from "@/lib/knowledgeData";

export type KnowledgeStatusFilter = "全部" | "未练" | "已掌握" | "需复习";
export type KnowledgeDifficultyFilter = "全部" | KnowledgeDifficulty;

interface KnowledgeFilterBarProps {
  search: string;
  difficulty: KnowledgeDifficultyFilter;
  status: KnowledgeStatusFilter;
  total: number;
  statusCounts: Record<Exclude<KnowledgeStatusFilter, "全部">, number>;
  loading: boolean;
  onSearchChange: (value: string) => void;
  onDifficultyChange: (value: KnowledgeDifficultyFilter) => void;
  onStatusChange: (value: KnowledgeStatusFilter) => void;
}

const statusFilters: KnowledgeStatusFilter[] = ["全部", "未练", "已掌握", "需复习"];

export default function KnowledgeFilterBar({
  search,
  difficulty,
  status,
  total,
  statusCounts,
  loading,
  onSearchChange,
  onDifficultyChange,
  onStatusChange,
}: KnowledgeFilterBarProps) {
  return (
    <section className="mb-5 rounded-xl border border-outline-variant/30 bg-surface-container-lowest px-4 py-3 shadow-sm">
      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <span className="inline-flex items-center gap-1.5 font-medium text-on-surface-variant">
            <SlidersHorizontal className="h-4 w-4" />
            难度：
          </span>
          <div className="flex flex-wrap gap-1.5">
            {knowledgeDifficulties.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => onDifficultyChange(item)}
                className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                  difficulty === item
                    ? "bg-on-surface text-surface"
                    : "bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
                }`}
              >
                {item}
              </button>
            ))}
          </div>

          <div className="hidden h-5 w-px bg-outline-variant/40 sm:block" />

          <span className="font-medium text-on-surface-variant">状态：</span>
          <div className="flex flex-wrap gap-1.5">
            {statusFilters.map((item) => {
              const label =
                item === "全部" ? item : `${item} (${statusCounts[item]})`;
              return (
                <button
                  key={item}
                  type="button"
                  onClick={() => onStatusChange(item)}
                  className={`rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors ${
                    status === item
                      ? "bg-primary text-on-primary"
                      : "bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>

          <span className="rounded-lg bg-surface-container px-3 py-1.5 text-xs font-semibold text-on-surface-variant">
            共 {total} 题
          </span>
          {loading && <span className="text-xs text-on-surface-variant">加载中...</span>}
        </div>

        <div className="relative w-full xl:w-80">
          <Search className="absolute left-3 top-1/2 h-[18px] w-[18px] -translate-y-1/2 text-outline" />
          <input
            type="text"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="搜索题目、解析或标签..."
            className="w-full rounded-lg border border-outline-variant/40 bg-surface px-9 py-2 text-sm text-on-surface outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/15"
          />
        </div>
      </div>
    </section>
  );
}
