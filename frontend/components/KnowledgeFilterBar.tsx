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
    <section className="coach-card mb-5 p-4">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <SlidersHorizontal className="h-4 w-4 text-primary" />
          <h2 className="text-sm font-semibold text-on-surface">
            筛选与搜索
          </h2>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-xs text-on-surface-variant">
          <span className="coach-pill px-2 py-0.5 text-[11px]">
            共 {total} 张卡
          </span>
          {loading && <span>加载中...</span>}
        </div>
      </div>

      <div className="grid gap-3 xl:grid-cols-[minmax(0,1fr)_320px] xl:items-start">
        <div className="flex min-w-0 flex-wrap items-center gap-3 text-sm">
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold text-on-surface-variant">
              难度
            </span>
            <div className="flex flex-wrap gap-1.5">
              {knowledgeDifficulties.map((item) => (
                <button
                  key={item}
                  type="button"
                  onClick={() => onDifficultyChange(item)}
                  className={`rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
                    difficulty === item
                      ? "bg-primary text-on-primary"
                      : "bg-surface-container text-on-surface-variant hover:bg-surface-container-high"
                  }`}
                >
                  {item}
                </button>
              ))}
            </div>
          </div>

          <div className="hidden h-5 w-px bg-outline-variant/40 sm:block" />

          <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-semibold text-on-surface-variant">
              状态
            </span>
            <div className="flex flex-wrap gap-1.5">
              {statusFilters.map((item) => {
                const label =
                  item === "全部" ? item : `${item} (${statusCounts[item]})`;
                return (
                  <button
                    key={item}
                    type="button"
                    onClick={() => onStatusChange(item)}
                    className={`rounded-full px-3 py-1.5 text-xs font-semibold transition-colors ${
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
          </div>
        </div>

        <div className="relative w-full">
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
