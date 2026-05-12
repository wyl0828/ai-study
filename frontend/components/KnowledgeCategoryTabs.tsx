"use client";

import type { KnowledgeCategory } from "@/lib/types";

interface KnowledgeCategoryTabsProps {
  categories: KnowledgeCategory[];
  activeCategory: string;
  onChange: (category: string) => void;
}

const categoryLabels: Record<string, string> = {
  ALL: "全部",
  JAVA: "Java",
  JVM: "JVM",
  SPRING: "Spring",
  MYSQL: "MySQL",
  REDIS: "Redis",
};

export default function KnowledgeCategoryTabs({
  categories,
  activeCategory,
  onChange,
}: KnowledgeCategoryTabsProps) {
  const total = categories.reduce((sum, category) => sum + category.count, 0);
  const items = [{ category: "ALL", label: "全部", count: total }, ...categories];

  return (
    <div className="flex flex-wrap items-center gap-2">
      {items.map((item) => {
        const active = activeCategory === item.category;
        return (
          <button
            key={item.category}
            type="button"
            onClick={() => onChange(item.category)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              active
                ? "bg-primary text-on-primary"
                : "bg-surface-container text-on-surface-variant border border-outline-variant/40 hover:bg-surface-container-high"
            }`}
          >
            {categoryLabels[item.category] || item.label}
            <span className={active ? "ml-1 text-on-primary/80" : "ml-1 text-outline"}>
              {item.count}
            </span>
          </button>
        );
      })}
    </div>
  );
}
