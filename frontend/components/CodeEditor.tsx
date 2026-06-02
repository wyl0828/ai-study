"use client";

import Editor from "@monaco-editor/react";
import {
  CheckCircle,
  Code,
  Info,
  Loader2,
  Play,
  RefreshCw,
  X,
} from "lucide-react";

interface CodeEditorProps {
  code: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onReset: () => void;
  draftSavedAt: string | null;
  onDismissDraftNotice: () => void;
  isSubmitting: boolean;
  isTemplateLoading: boolean;
  isCurrentCodeAccepted: boolean;
  submitLabel?: string;
}

export default function CodeEditor({
  code,
  onChange,
  onSubmit,
  onReset,
  draftSavedAt,
  onDismissDraftNotice,
  isSubmitting,
  isTemplateLoading,
  isCurrentCodeAccepted,
  submitLabel,
}: CodeEditorProps) {
  const disabled = isTemplateLoading || isSubmitting;

  const buttonLabel = isTemplateLoading
    ? "加载模板..."
    : isSubmitting
    ? "判题中..."
    : submitLabel ?? "提交代码";

  return (
    <div className="flex flex-col h-full">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[#243047] bg-[#0b1020] px-3 py-2 text-slate-300 shrink-0">
        <div className="flex min-w-0 flex-wrap items-center gap-2">
          <div className="flex min-w-0 items-center gap-2 rounded-md border border-[#334155] bg-[#111827] px-3 py-1.5 text-slate-100">
            <Code className="h-4 w-4 shrink-0 text-sky-400" />
            <span className="truncate font-mono text-xs tracking-wide">Solution.java</span>
          </div>
          <span className="rounded-full border border-sky-500/30 bg-sky-500/10 px-2.5 py-1 text-[11px] font-semibold text-sky-200">
            Java 17
          </span>
          <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[11px] font-semibold text-emerald-200">
            自动保存
          </span>
        </div>
        <div className="flex flex-wrap justify-end gap-2">
          <button
            onClick={onReset}
            disabled={disabled}
            className="flex items-center gap-1 rounded-md border border-[#334155] bg-[#111827] px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:border-sky-500/50 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
            type="button"
            title="重置代码"
          >
            <RefreshCw className="w-4 h-4" />
            重置代码
          </button>
          <button
            onClick={onSubmit}
            disabled={disabled}
            className="flex items-center gap-1 rounded-md bg-emerald-600 px-4 py-1.5 text-xs font-semibold text-white shadow-sm transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-60"
            type="button"
            title={buttonLabel}
          >
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : isCurrentCodeAccepted ? (
              <CheckCircle className="w-4 h-4" />
            ) : (
              <Play className="w-4 h-4" />
            )}
            {buttonLabel}
          </button>
        </div>
      </div>

      {draftSavedAt && (
        <div className="flex items-center gap-2 bg-[#161b22] text-[#c9d1d9] border-b border-[#30363d] px-4 py-2 text-xs shrink-0">
          <Info className="w-4 h-4 text-blue-400" />
          <span>草稿已自动保存于 {draftSavedAt}</span>
          <button
            onClick={onDismissDraftNotice}
            className="ml-auto rounded p-0.5 text-[#8b949e] hover:bg-[#21262d] hover:text-white transition-colors"
            aria-label="关闭草稿提示"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 编辑器 - 铺满剩余空间 */}
      <div className="flex-1 min-h-0 bg-[#0d1117]">
        <Editor
          language="java"
          theme="vs-dark"
          height="100%"
          value={code}
          loading={
            <div className="h-full w-full bg-[#0d1117] text-[#8b949e] font-mono text-xs flex items-center justify-center">
              正在加载代码编辑器...
            </div>
          }
          onChange={(v) => onChange(v || "")}
          options={{
            fontSize: 14,
            fontFamily: "'JetBrains Mono', monospace",
            minimap: { enabled: false },
            scrollBeyondLastLine: false,
            lineNumbers: "on",
            automaticLayout: true,
            tabSize: 2,
            padding: { top: 12 },
          }}
        />
      </div>
    </div>
  );
}
