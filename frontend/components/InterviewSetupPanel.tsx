"use client";

import { Check, Play, SlidersHorizontal } from "lucide-react";

const categories = [
  { value: "SPRING", label: "Spring" },
  { value: "JAVA", label: "Java 集合" },
  { value: "JVM", label: "JVM" },
  { value: "MYSQL", label: "MySQL" },
  { value: "REDIS", label: "Redis" },
  { value: "PROJECT", label: "系统设计" },
];

const styles = [
  { value: "BIG_TECH", label: "大厂追问型", description: "连续追问边界、原理和取舍。" },
  { value: "GUIDED", label: "基础巩固型", description: "先搭框架，再补齐关键概念。" },
  { value: "FAST_SCREEN", label: "快速筛选型", description: "压缩表达，练习高频问题直答。" },
];

interface Props {
  category: string;
  questionCount: number;
  interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN";
  loading: boolean;
  onCategoryChange: (category: string) => void;
  onQuestionCountChange: (questionCount: number) => void;
  onInterviewerStyleChange: (interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN") => void;
  onStart: () => void;
}

const questionCounts = [3, 5];

export default function InterviewSetupPanel({
  category,
  questionCount,
  interviewerStyle,
  loading,
  onCategoryChange,
  onQuestionCountChange,
  onInterviewerStyleChange,
  onStart,
}: Props) {
  return (
    <section className="coach-card p-5">
      <div>
        <div className="flex items-center gap-2 font-semibold text-primary">
          <SlidersHorizontal className="h-4 w-4" />
          面试配置
        </div>
        <p className="mt-2 text-sm leading-6 text-on-surface-variant">
          选择一个 Java 后端训练方向，开始一轮一问一答的追问式面试。
        </p>
      </div>

      <div className="mt-6">
        <p className="text-sm font-bold text-on-surface">选择训练方向</p>
        <div className="mt-3 flex flex-wrap gap-3">
          {categories.map((item) => {
            const active = category === item.value;
            return (
              <button
                key={item.value}
                type="button"
                className={`rounded-full border px-4 py-2 text-sm font-semibold transition-colors ${
                  active
                    ? "border-primary bg-primary text-on-primary shadow-sm"
                    : "border-outline-variant bg-surface-container-lowest text-on-surface-variant hover:border-primary hover:text-primary"
                }`}
                onClick={() => onCategoryChange(item.value)}
              >
                {item.label}
              </button>
            );
          })}
        </div>
      </div>

      <div className="mt-6">
        <p className="text-sm font-bold text-on-surface">面试风格</p>
        <div className="mt-3 grid gap-3 md:grid-cols-3">
          {styles.map((item) => {
            const active = interviewerStyle === item.value;
            return (
              <button
                key={item.value}
                type="button"
                className={`min-h-28 rounded-lg border p-4 text-left transition-colors ${
                  active
                    ? "border-primary bg-primary/5 text-primary shadow-sm"
                    : "border-outline-variant bg-surface-container-lowest text-on-surface hover:border-primary/60"
                }`}
                onClick={() => onInterviewerStyleChange(item.value as "GUIDED" | "BIG_TECH" | "FAST_SCREEN")}
              >
                <span className="flex items-center justify-between gap-3 text-sm font-bold">
                  {item.label}
                  {active && (
                    <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary text-on-primary">
                      <Check className="h-3.5 w-3.5" />
                    </span>
                  )}
                </span>
                <span className="mt-3 block text-xs leading-5 text-on-surface-variant">{item.description}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="mt-6">
        <p className="text-sm font-bold text-on-surface">题数</p>
        <div className="mt-3 flex flex-wrap gap-3">
          {questionCounts.map((count) => {
            const active = questionCount === count;
            return (
              <button
                key={count}
                type="button"
                className={`rounded-full border px-5 py-2 text-sm font-semibold transition-colors ${
                  active
                    ? "border-primary bg-primary text-on-primary shadow-sm"
                    : "border-outline-variant bg-surface-container-lowest text-on-surface-variant hover:border-primary hover:text-primary"
                }`}
                onClick={() => onQuestionCountChange(count)}
              >
                {count} 题
              </button>
            );
          })}
        </div>
      </div>

      <button
        className="coach-primary-button mt-7 w-full py-3 font-bold"
        disabled={loading}
        onClick={onStart}
        type="button"
      >
        <Play className="h-4 w-4" />
        {loading ? "正在开始" : "开始模拟面试"}
      </button>
    </section>
  );
}
