"use client";

import Editor from "@monaco-editor/react";
import { Code, RefreshCw, Play, Loader2 } from "lucide-react";

interface CodeEditorProps {
  code: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  onReset: () => void;
  isSubmitting: boolean;
  isAnalyzing: boolean;
}

export default function CodeEditor({
  code,
  onChange,
  onSubmit,
  onReset,
  isSubmitting,
  isAnalyzing,
}: CodeEditorProps) {
  const disabled = isSubmitting || isAnalyzing;

  const buttonLabel = isSubmitting
    ? "判题中..."
    : isAnalyzing
    ? "AI 分析中..."
    : "提交代码";

  return (
    <div className="flex flex-col h-full">
      {/* 顶栏 - GitHub 暗色风格 */}
      <div className="flex justify-between items-center bg-[#010409] text-gray-400 border-b border-[#30363d] shrink-0">
        <div className="flex items-center">
          <div className="flex items-center gap-2 text-gray-200 px-4 py-2 border-r border-[#30363d] border-t-2 border-t-blue-500 bg-[#0d1117]">
            <Code className="w-4 h-4 text-blue-400" />
            <span className="font-mono text-xs tracking-wide">解题代码.java</span>
          </div>
        </div>
        <div className="flex gap-2 mr-4">
          <button
            onClick={onReset}
            disabled={disabled}
            className="text-xs px-3 py-1.5 rounded text-gray-400 hover:text-white hover:bg-[#21262d] transition-all flex items-center gap-1 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <RefreshCw className="w-4 h-4" />
            重置
          </button>
          <button
            onClick={onSubmit}
            disabled={disabled}
            className="bg-[#238636] hover:bg-[#2ea043] text-white text-xs px-4 py-1.5 rounded font-medium transition-all flex items-center gap-1 shadow-sm disabled:opacity-60 disabled:cursor-not-allowed"
          >
            {isSubmitting || isAnalyzing ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Play className="w-4 h-4" />
            )}
            {buttonLabel}
          </button>
        </div>
      </div>

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
