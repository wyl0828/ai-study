"use client";

import { useState } from "react";
import {
  Lightbulb,
  Loader2,
  Trophy,
  Eye,
  EyeOff,
  Info,
  AlertTriangle,
} from "lucide-react";
import type { AgentAnalyzeVO } from "@/lib/types";

interface HintPanelProps {
  diagnosis: AgentAnalyzeVO | null;
  isAnalyzing: boolean;
  isAccepted: boolean;
  isDiagnosisStale: boolean;
}

const levels = [
  {
    key: "hintLevel1",
    label: "方向提示",
    num: 1,
    numBg: "bg-emerald-100 text-emerald-700",
    contentBg: "bg-emerald-50/50 border-emerald-200/50",
    defaultOpen: true,
  },
  {
    key: "hintLevel2",
    label: "知识点提示",
    num: 2,
    numBg: "bg-amber-100 text-amber-700",
    contentBg: "bg-amber-50/50 border-amber-200/50",
    defaultOpen: false,
  },
  {
    key: "hintLevel3",
    label: "伪代码提示",
    num: 3,
    numBg: "bg-red-100 text-red-700",
    contentBg: "bg-red-50/30 border-red-200/50",
    defaultOpen: false,
  },
] as const;

export default function HintPanel({
  diagnosis,
  isAnalyzing,
  isAccepted,
  isDiagnosisStale,
}: HintPanelProps) {
  const [expanded, setExpanded] = useState<Record<string, boolean>>(() => {
    const init: Record<string, boolean> = {};
    levels.forEach((l) => {
      init[l.key] = l.defaultOpen;
    });
    return init;
  });

  if (isAccepted) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Trophy className="w-12 h-12 text-emerald-500 mb-3" />
        <p className="text-lg font-medium mb-1">全部通过！</p>
        <p className="text-sm">不需要提示，尝试优化你的解法吧</p>
      </div>
    );
  }

  if (isAnalyzing) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Loader2 className="w-10 h-10 text-primary mb-3 animate-spin" />
        <p className="text-sm">AI 正在生成提示...</p>
      </div>
    );
  }

  if (!diagnosis) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Lightbulb className="w-10 h-10 mb-3" />
        <p className="text-sm">提交代码后，AI 将为你生成分层提示</p>
      </div>
    );
  }

  return (
    <div className="p-5 space-y-4">
      {isDiagnosisStale && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>该诊断基于上次提交，当前代码已修改，仅供参考。</span>
        </div>
      )}

      {levels.map(({ key, label, num, numBg, contentBg, defaultOpen }) => {
        const hint = diagnosis[key as keyof AgentAnalyzeVO] as string;
        if (!hint) return null;

        const isOpen = expanded[key];

        return (
          <div
            key={key}
            className="bg-surface-container rounded-lg border border-outline-variant/40 overflow-hidden"
          >
            <div className="flex items-center justify-between px-4 py-3 cursor-pointer">
              <div className="flex items-center gap-2">
                <span
                  className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-bold ${numBg}`}
                >
                  {num}
                </span>
                <span className="text-sm font-semibold text-on-surface">
                  {label}
                </span>
              </div>
              {defaultOpen ? (
                <span className="text-xs text-primary font-medium">默认展开</span>
              ) : (
                <button
                  onClick={() =>
                    setExpanded((prev) => ({ ...prev, [key]: !prev[key] }))
                  }
                  className="text-xs text-primary font-medium hover:underline flex items-center gap-1"
                >
                  {isOpen ? (
                    <EyeOff className="w-3.5 h-3.5" />
                  ) : (
                    <Eye className="w-3.5 h-3.5" />
                  )}
                  {isOpen ? "收起提示" : "查看提示"}
                </button>
              )}
            </div>
            {isOpen && (
              <div className="px-4 pb-4 pt-0">
                <div className={`rounded-lg p-3 border ${contentBg}`}>
                  <p className="text-sm text-on-surface-variant leading-relaxed whitespace-pre-wrap">
                    {hint}
                  </p>
                </div>
              </div>
            )}
          </div>
        );
      })}

      <div className="text-xs text-outline text-center pt-2 flex items-center justify-center gap-1">
        <Info className="w-3 h-3" />
        提示不会直接给出完整答案，帮助你独立思考
      </div>
    </div>
  );
}
