"use client";

import { useEffect, useMemo, useState } from "react";
import {
  BookOpen,
  Check,
  ChevronDown,
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

interface SolutionSection {
  title: string;
  content: string;
}

export default function ProblemSolutionPanel({
  solutionOutline,
}: ProblemSolutionPanelProps) {
  const [showCode, setShowCode] = useState(false);
  const [copied, setCopied] = useState(false);
  const { explanation, javaCode } = parseProblemSolution(solutionOutline);
  const solutionSections = useMemo(
    () => buildSolutionSections(explanation),
    [explanation]
  );
  const [openSections, setOpenSections] = useState<Set<string>>(
    () => new Set(solutionSections.slice(0, 1).map((section) => section.title))
  );

  useEffect(() => {
    setOpenSections(
      new Set(solutionSections.slice(0, 1).map((section) => section.title))
    );
  }, [solutionSections]);

  const handleCopy = async () => {
    if (!javaCode) {
      return;
    }

    await navigator.clipboard.writeText(javaCode);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1600);
  };

  const toggleSection = (title: string) => {
    setOpenSections((current) => {
      const next = new Set(current);
      if (next.has(title)) {
        next.delete(title);
      } else {
        next.add(title);
      }
      return next;
    });
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
        {solutionSections.length > 0 && (
          <div className="space-y-3">
            <div className="rounded-lg border border-primary/20 bg-primary/5 p-3">
              <div className="text-xs font-semibold text-primary mb-2">
                先按这个顺序看
              </div>
              <ol className="space-y-1.5 text-xs text-on-surface-variant">
                {solutionSections.slice(0, 5).map((section, index) => (
                  <li key={section.title} className="flex gap-2">
                    <span className="mt-0.5 flex h-4 w-4 shrink-0 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
                      {index + 1}
                    </span>
                    <span>{section.title}</span>
                  </li>
                ))}
              </ol>
            </div>

            {solutionSections.map((section) => {
              const isOpen = openSections.has(section.title);
              return (
                <div
                  key={section.title}
                  className="rounded-lg border border-outline-variant/40 bg-surface-container overflow-hidden"
                >
                  <button
                    type="button"
                    onClick={() => toggleSection(section.title)}
                    className="flex w-full items-center justify-between gap-3 px-3 py-2.5 text-left"
                    aria-expanded={openSections.has(section.title)}
                  >
                    <span className="text-sm font-semibold text-on-surface">
                      {section.title}
                    </span>
                    <ChevronDown
                      className={`h-4 w-4 shrink-0 text-outline transition-transform ${
                        isOpen ? "rotate-180" : ""
                      }`}
                    />
                  </button>

                  {isOpen && (
                    <div className="border-t border-outline-variant/30 px-3 py-3">
                      <SolutionText content={section.content} />
                    </div>
                  )}
                </div>
              );
            })}
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

function buildSolutionSections(explanation: string): SolutionSection[] {
  const lines = explanation
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
  const sections: SolutionSection[] = [];
  let current: SolutionSection | null = null;

  for (const line of lines) {
    if (isSolutionHeading(line)) {
      if (current) {
        sections.push(current);
      }
      current = {
        title: normalizeHeading(line),
        content: "",
      };
      continue;
    }

    if (!current) {
      current = {
        title: "解法总览",
        content: line,
      };
      continue;
    }

    current.content = [current.content, line].filter(Boolean).join("\n");
  }

  if (current) {
    sections.push(current);
  }

  return sections.filter((section) => section.content.trim().length > 0);
}

function isSolutionHeading(line: string): boolean {
  return /^(方法|先从|为什么|关键变量|用样例|伪代码|新手常见错误|复杂度|易错点|解题思路)/.test(
    line
  );
}

function normalizeHeading(line: string): string {
  return line.replace(/[：:]$/, "");
}

function SolutionText({ content }: { content: string }) {
  return (
    <div className="space-y-2 text-sm leading-relaxed text-on-surface-variant">
      {content.split(/\r?\n/).map((line) => {
        const trimmed = line.trim();
        if (!trimmed) {
          return null;
        }
        const ordered = trimmed.match(/^(\d+)[.、]\s*(.+)$/);
        if (ordered) {
          return (
            <div key={trimmed} className="flex gap-2">
              <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-secondary/10 text-[11px] font-bold text-secondary">
                {ordered[1]}
              </span>
              <span>{ordered[2]}</span>
            </div>
          );
        }
        if (trimmed.startsWith("- ")) {
          return (
            <div key={trimmed} className="flex gap-2">
              <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
              <span>{trimmed.slice(2)}</span>
            </div>
          );
        }
        return <p key={trimmed}>{trimmed}</p>;
      })}
    </div>
  );
}
