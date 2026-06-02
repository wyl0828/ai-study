"use client";

import { useState, useMemo } from "react";
import type { ReactNode } from "react";
import {
  BarChart3,
  BookOpenCheck,
  Filter,
  Layers3,
  Search,
  SearchX,
  Target,
} from "lucide-react";
import type { HomeProblem } from "@/lib/types";
import { categoryName, problemDescription, problemTitle } from "@/lib/i18n";
import ProblemCard from "./ProblemCard";
import ProblemTrainingSidebar from "./ProblemTrainingSidebar";
import { getAuthToken, getStoredUser } from "@/lib/auth";

interface HomeClientProps {
  problems: HomeProblem[];
}

const difficulties = [
  { key: "ALL", label: "全部", color: "bg-primary text-on-primary" },
  { key: "EASY", label: "简单", color: "bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100" },
  { key: "MEDIUM", label: "中等", color: "bg-amber-50 text-amber-700 border border-amber-200 hover:bg-amber-100" },
  { key: "HARD", label: "困难", color: "bg-red-50 text-red-700 border border-red-200 hover:bg-red-100" },
] as const;

const categoryLabels: Record<string, string> = {
  ALL: "全部分类",
};

export default function HomeClient({ problems }: HomeClientProps) {
  const [difficulty, setDifficulty] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("ALL");

  const categories = useMemo(() => {
    const set = new Set(problems.map((p) => p.category));
    return ["ALL", ...Array.from(set)];
  }, [problems]);

  const difficultyCounts = useMemo(() => {
    return problems.reduce<Record<string, number>>(
      (counts, problem) => {
        counts[problem.difficulty] = (counts[problem.difficulty] ?? 0) + 1;
        return counts;
      },
      { EASY: 0, MEDIUM: 0, HARD: 0 }
    );
  }, [problems]);

  const filtered = useMemo(() => {
    return problems.filter((p) => {
      if (difficulty !== "ALL" && p.difficulty !== difficulty) return false;
      if (category !== "ALL" && p.category !== category) return false;
      const keyword = search.trim().toLowerCase();
      if (
        keyword &&
        ![
          p.title,
          problemTitle(p.title),
          p.description ? problemDescription(p.description) : "",
          p.category,
          categoryName(p.category),
        ]
          .join(" ")
          .toLowerCase()
          .includes(keyword)
      )
        return false;
      return true;
    });
  }, [problems, difficulty, category, search]);

  const passedCount = 0;
  const attemptedCount = 0;
  const currentUser = getAuthToken() ? getStoredUser() : null;
  const activeFilterSummary = [
    difficulty === "ALL" ? "全部难度" : difficulties.find((d) => d.key === difficulty)?.label,
    category === "ALL" ? "全部专题" : categoryName(category),
    search.trim() ? `关键词：${search.trim()}` : null,
  ].filter(Boolean);

  return (
    <div className="coach-shell max-w-[1440px] py-8">
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <section className="min-w-0">
          <div className="mb-5 grid gap-4 lg:grid-cols-[minmax(0,1fr)_360px] lg:items-end">
            <div>
              <div className="coach-pill mb-3 w-fit border-primary/20 bg-primary/5 text-primary">
                <BookOpenCheck className="h-3.5 w-3.5" />
                Hot100 Java Solution 模式
              </div>
              <h1 className="text-2xl font-bold tracking-tight text-on-surface">
                题库训练台
              </h1>
              <p className="mt-1 max-w-2xl text-sm leading-6 text-on-surface-variant">
                从高频算法题进入提交、诊断、记忆和训练计划闭环；优先练习能暴露 Java 后端面试表达的核心题型。
              </p>
            </div>

            <div className="grid grid-cols-3 gap-2">
              <TrainingMetric
                icon={<Target className="h-4 w-4" />}
                label="题目总数"
                value={problems.length}
              />
              <TrainingMetric
                icon={<BarChart3 className="h-4 w-4" />}
                label="当前筛选"
                value={filtered.length}
              />
              <TrainingMetric
                icon={<Layers3 className="h-4 w-4" />}
                label="知识专题"
                value={Math.max(categories.length - 1, 0)}
              />
            </div>
          </div>

          <div className="coach-card mb-5 p-4">
            <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Filter className="h-4 w-4 text-primary" />
                <h2 className="text-sm font-semibold text-on-surface">
                  筛选与搜索
                </h2>
              </div>
              <div className="flex flex-wrap items-center gap-2 text-[11px] text-on-surface-variant">
                <span className="coach-pill px-2 py-0.5 text-[11px]">
                  简单 {difficultyCounts.EASY}
                </span>
                <span className="coach-pill px-2 py-0.5 text-[11px]">
                  中等 {difficultyCounts.MEDIUM}
                </span>
                <span className="coach-pill px-2 py-0.5 text-[11px]">
                  困难 {difficultyCounts.HARD}
                </span>
              </div>
            </div>

            <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_280px] lg:items-start">
            <div className="flex min-w-0 flex-wrap items-center gap-2">
              <div className="flex items-center gap-1.5">
                {difficulties.map((d) => (
                  <button
                    key={d.key}
                    onClick={() => setDifficulty(d.key)}
                    className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                      difficulty === d.key
                        ? d.color
                        : "bg-surface-container text-on-surface-variant border border-outline-variant/40 hover:bg-surface-container-high"
                    }`}
                  >
                    {d.label}
                  </button>
                ))}
              </div>

              <div className="h-5 w-px bg-outline-variant/40" />

              {/* 分类筛选 */}
              <div className="flex min-w-0 flex-wrap items-center gap-1.5">
                {categories.map((c) => (
                  <button
                    key={c}
                    onClick={() => setCategory(c)}
                    className={`px-3 py-1.5 rounded-full text-xs font-medium transition-colors ${
                      category === c
                        ? "bg-primary text-on-primary"
                        : "bg-surface-container text-on-surface-variant border border-outline-variant/40 hover:bg-surface-container-high"
                    }`}
                  >
                    {categoryLabels[c] || categoryName(c)}
                  </button>
                ))}
              </div>
            </div>

            <div className="w-full lg:w-[280px]">
              <div className="relative">
                <Search className="w-[18px] h-[18px] text-outline absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="搜索算法题..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="w-full pl-9 pr-4 py-1.5 text-sm border border-outline-variant/40 rounded-lg bg-surface-container-lowest focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary/30"
                />
              </div>
            </div>
            </div>
          </div>

          <div className="mb-4 flex flex-wrap items-center justify-between gap-3 text-xs text-on-surface-variant">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-medium text-on-surface">
                当前筛选：{activeFilterSummary.join(" / ")}
              </span>
              <span>共 {filtered.length} 道题</span>
            </div>
            <div className="flex items-center gap-4">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              已通过 {passedCount}
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber-500" />
              尝试过 {attemptedCount}
            </span>
            </div>
          </div>

          {filtered.length === 0 ? (
            <div className="coach-empty-state">
              <SearchX className="w-10 h-10 mb-2 text-outline" />
              <p className="text-sm font-semibold text-on-surface">没有匹配的题目</p>
              <p className="mt-1 text-xs">换一个知识专题或清空搜索关键词再试。</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-4">
              {filtered.map((problem) => (
                <ProblemCard key={problem.id} problem={problem} />
              ))}
            </div>
          )}
        </section>

        <ProblemTrainingSidebar userId={currentUser?.id} />
      </div>
    </div>
  );
}

function TrainingMetric({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: number;
}) {
  return (
    <div className="coach-card p-3">
      <div className="mb-2 flex items-center gap-1.5 text-primary">
        {icon}
        <span className="text-[11px] font-semibold text-on-surface-variant">
          {label}
        </span>
      </div>
      <div className="text-xl font-bold text-on-surface">{value}</div>
    </div>
  );
}
