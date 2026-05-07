"use client";

import { ClipboardCheck, Brain, Lightbulb } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO } from "@/lib/types";
import TestResult from "./TestResult";
import AiDiagnosis from "./AiDiagnosis";
import HintPanel from "./HintPanel";

interface ResultPanelProps {
  submissionResult: SubmissionResult | null;
  diagnosis: AgentAnalyzeVO | null;
  isAnalyzing: boolean;
  activeTab: "test" | "diagnosis" | "hint";
  onTabChange: (tab: "test" | "diagnosis" | "hint") => void;
}

const tabs = [
  { key: "test" as const, label: "测试结果", Icon: ClipboardCheck },
  { key: "diagnosis" as const, label: "AI 诊断", Icon: Brain },
  { key: "hint" as const, label: "分层提示", Icon: Lightbulb },
];

export default function ResultPanel({
  submissionResult,
  diagnosis,
  isAnalyzing,
  activeTab,
  onTabChange,
}: ResultPanelProps) {
  const isAccepted = submissionResult?.status === "ACCEPTED";

  return (
    <div className="flex flex-col h-full bg-surface-container-lowest border-l border-outline-variant/30">
      {/* Tab 栏 */}
      <div className="flex border-b border-outline-variant/30 bg-surface-container-low shrink-0">
        {tabs.map(({ key, label, Icon }) => (
          <button
            key={key}
            onClick={() => onTabChange(key)}
            className={`flex-1 px-4 py-2.5 text-xs transition-colors border-b-2 flex items-center justify-center gap-1 ${
              activeTab === key
                ? "border-primary text-primary font-semibold bg-surface-container-lowest"
                : "border-transparent text-on-surface-variant font-medium hover:text-on-surface"
            }`}
          >
            <Icon className="w-[15px] h-[15px]" />
            {label}
          </button>
        ))}
      </div>

      {/* Tab 内容 */}
      <div className="flex-1 min-h-0 overflow-y-auto">
        {activeTab === "test" &&
          (submissionResult ? (
            <TestResult result={submissionResult} />
          ) : (
            <EmptyState
              text="提交代码后，测试结果将显示在这里"
            />
          ))}

        {activeTab === "diagnosis" && (
          <AiDiagnosis
            diagnosis={diagnosis}
            isAnalyzing={isAnalyzing}
            isAccepted={isAccepted}
          />
        )}

        {activeTab === "hint" && (
          <HintPanel
            diagnosis={diagnosis}
            isAnalyzing={isAnalyzing}
            isAccepted={isAccepted}
          />
        )}
      </div>
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
      <ClipboardCheck className="w-10 h-10 mb-3" />
      <p className="text-sm">{text}</p>
    </div>
  );
}
