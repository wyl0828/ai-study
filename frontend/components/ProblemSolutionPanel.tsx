"use client";

import { useState } from "react";
import {
  BookOpen,
  Check,
  Clipboard,
  Eye,
  EyeOff,
  FileCode2,
  Info,
} from "lucide-react";
import { parseProblemSolution } from "@/lib/problemSolution";

interface ProblemSolutionPanelProps {
  solutionOutline?: string | null;
}

export default function ProblemSolutionPanel({
  solutionOutline,
}: ProblemSolutionPanelProps) {
  const [showCode, setShowCode] = useState(false);
  const [copied, setCopied] = useState(false);
  const { explanation, javaCode } = parseProblemSolution(solutionOutline);

  const handleCopy = async () => {
    if (!javaCode) {
      return;
    }

    await navigator.clipboard.writeText(javaCode);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  if (!explanation && !javaCode) {
    return (
      <section className="mt-5 rounded-lg border border-outline-variant/40 bg-surface-container p-4">
        <div className="flex items-start gap-2 text-sm text-on-surface-variant">
          <BookOpen className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
          <span>这道题的参考题解正在整理中</span>
        </div>
      </section>
    );
  }

  return (
    <section className="mt-5 space-y-4">
      <div>
        <h3 className="mb-3 flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-on-surface-variant">
          <BookOpen className="h-3.5 w-3.5 text-primary" />
          参考题解
        </h3>
        {explanation && (
          <div className="rounded-lg border border-outline-variant/40 bg-surface-container p-4">
            <p className="whitespace-pre-wrap text-sm leading-relaxed text-on-surface-variant">
              {explanation}
            </p>
          </div>
        )}
      </div>

      {javaCode && (
        <div className="rounded-lg border border-outline-variant/40 bg-surface-container overflow-hidden">
          <div className="border-b border-outline-variant/30 px-3 py-2.5">
            <div className="flex items-start gap-2 text-xs text-outline">
              <Info className="mt-0.5 h-3.5 w-3.5 shrink-0" />
              <span>建议先独立提交一次，再查看参考实现。</span>
            </div>
          </div>

          <div className="flex items-center justify-between gap-3 px-3 py-2.5">
            <div className="flex min-w-0 items-center gap-2">
              <FileCode2 className="h-4 w-4 shrink-0 text-primary" />
              <span className="truncate text-sm font-semibold text-on-surface">
                Java 参考实现
              </span>
            </div>
            <div className="flex shrink-0 items-center gap-2">
              <button
                type="button"
                onClick={handleCopy}
                className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
              >
                {copied ? (
                  <Check className="h-3.5 w-3.5" />
                ) : (
                  <Clipboard className="h-3.5 w-3.5" />
                )}
                {copied ? "已复制" : "复制代码"}
              </button>
              <button
                type="button"
                onClick={() => setShowCode((value) => !value)}
                className="text-xs font-medium text-primary hover:underline flex items-center gap-1"
                aria-expanded={showCode}
              >
                {showCode ? (
                  <EyeOff className="h-3.5 w-3.5" />
                ) : (
                  <Eye className="h-3.5 w-3.5" />
                )}
                {showCode ? "收起参考实现" : "查看 Java 参考实现"}
              </button>
            </div>
          </div>

          {showCode && (
            <div className="border-t border-outline-variant/30 bg-[#0d1117]">
              <pre className="overflow-x-auto p-3 text-xs leading-relaxed text-slate-100">
                <code>{javaCode}</code>
              </pre>
            </div>
          )}
        </div>
      )}
    </section>
  );
}
