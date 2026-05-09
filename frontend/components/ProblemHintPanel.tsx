"use client";

import { useState } from "react";
import { Eye, EyeOff, Info, Lightbulb } from "lucide-react";
import type { ProblemPresetHints } from "@/lib/problemHints";

interface ProblemHintPanelProps {
  hints: ProblemPresetHints | null;
}

const levels = [
  {
    key: "level1",
    label: "方向提示",
    num: 1,
    numBg: "bg-emerald-100 text-emerald-700",
    contentBg: "bg-emerald-50/50 border-emerald-200/50",
    defaultOpen: false,
  },
  {
    key: "level2",
    label: "知识点提示",
    num: 2,
    numBg: "bg-amber-100 text-amber-700",
    contentBg: "bg-amber-50/50 border-amber-200/50",
    defaultOpen: false,
  },
  {
    key: "level3",
    label: "伪代码提示",
    num: 3,
    numBg: "bg-red-100 text-red-700",
    contentBg: "bg-red-50/30 border-red-200/50",
    defaultOpen: false,
  },
] as const;

export default function ProblemHintPanel({ hints }: ProblemHintPanelProps) {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => {
    const init: Record<string, boolean> = {};
    levels.forEach((level) => {
      init[level.key] = level.defaultOpen;
    });
    return init;
  });

  if (!hints) {
    return null;
  }

  return (
    <section className="mt-5 pt-4 border-t border-outline-variant/30">
      <h3 className="text-xs font-semibold text-on-surface-variant uppercase tracking-wider mb-3 flex items-center gap-1.5">
        <Lightbulb className="h-3.5 w-3.5 text-primary" />
        分层提示
      </h3>

      <div className="space-y-3">
        {levels.map(({ key, label, num, numBg, contentBg }) => {
          const content = hints[key];
          const isOpen = expanded[key];

          return (
            <div
              key={key}
              className="bg-surface-container rounded-lg border border-outline-variant/40 overflow-hidden"
            >
              <div className="flex items-center justify-between gap-2 px-3 py-2.5">
                <div className="flex min-w-0 items-center gap-2">
                  <span
                    className={`h-6 w-6 shrink-0 rounded-full flex items-center justify-center text-xs font-bold ${numBg}`}
                  >
                    {num}
                  </span>
                  <span className="text-sm font-semibold text-on-surface truncate">
                    {label}
                  </span>
                </div>

                <button
                  type="button"
                  onClick={() =>
                    setExpanded((prev) => ({ ...prev, [key]: !prev[key] }))
                  }
                  className="shrink-0 text-xs text-primary font-medium hover:underline flex items-center gap-1"
                  aria-expanded={isOpen}
                >
                  {isOpen ? (
                    <EyeOff className="h-3.5 w-3.5" />
                  ) : (
                    <Eye className="h-3.5 w-3.5" />
                  )}
                  {isOpen ? "收起" : "查看"}
                </button>
              </div>

              {isOpen && (
                <div className="px-3 pb-3">
                  <div className={`rounded-lg border p-3 ${contentBg}`}>
                    <p className="text-sm text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                      {content}
                    </p>
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      <div className="text-xs text-outline mt-3 flex items-center gap-1">
        <Info className="h-3 w-3 shrink-0" />
        提示不会直接给出完整答案，帮助你独立思考
      </div>
    </section>
  );
}
