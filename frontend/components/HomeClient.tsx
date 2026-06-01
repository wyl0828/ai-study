"use client";

import { useState, useMemo } from "react";
import { Search, SearchX } from "lucide-react";
import type { HomeProblem } from "@/lib/types";
import { categoryName, problemDescription, problemTitle } from "@/lib/i18n";
import ProblemCard from "./ProblemCard";
import ProblemTrainingSidebar from "./ProblemTrainingSidebar";
import { getStoredUser } from "@/lib/auth";

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

  return (
    <div className="max-w-[1440px] mx-auto px-6 py-8">
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_320px]">
        <section className="min-w-0">
          {/* 页面标题 */}
          <div className="mb-6">
            <h1 className="text-2xl font-bold text-on-surface tracking-tight">算法题库</h1>
            <p className="text-sm text-on-surface-variant mt-1">
              Java 后端面试高频题目，AI 教练实时诊断你的代码
            </p>
          </div>

          {/* 筛选栏 */}
          <div className="mb-6 grid gap-3 lg:grid-cols-[minmax(0,1fr)_280px] lg:items-start">
            <div className="flex min-w-0 flex-wrap items-center gap-2">
              {/* 难度筛选 */}
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

            {/* 搜索 */}
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

          {/* 统计信息 */}
          <div className="flex items-center gap-4 mb-4 text-xs text-on-surface-variant">
            <span>共 {filtered.length} 道题</span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500" />
              已通过 {passedCount}
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber-500" />
              尝试过 {attemptedCount}
            </span>
          </div>

          {/* 卡片网格 */}
          {filtered.length === 0 ? (
            <div className="text-center py-20 text-on-surface-variant">
              <SearchX className="w-10 h-10 mx-auto mb-2" />
              没有匹配的题目
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 2xl:grid-cols-3 gap-4">
              {filtered.map((problem) => (
                <ProblemCard key={problem.id} problem={problem} />
              ))}
            </div>
          )}
        </section>

        <ProblemTrainingSidebar userId={getStoredUser()?.id} />
      </div>
    </div>
  );
}
