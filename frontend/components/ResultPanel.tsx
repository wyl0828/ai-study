"use client";

import { ClipboardCheck, Brain } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO, AgentStepVO } from "@/lib/types";
import TestResult from "./TestResult";
import AiDiagnosis from "./AiDiagnosis";

interface ResultPanelProps {
  submissionResult: SubmissionResult | null;
  diagnosis: AgentAnalyzeVO | null;
  isAnalyzing: boolean;
  agentSteps: AgentStepVO[];
  isDiagnosisStale: boolean;
  isAcceptedSubmission: boolean;
  activeTab: "test" | "diagnosis";
  onTabChange: (tab: "test" | "diagnosis") => void;
}

const tabs = [
  { key: "test" as const, label: "测试结果", Icon: ClipboardCheck },
  { key: "diagnosis" as const, label: "AI 诊断", Icon: Brain },
];

export default function ResultPanel({
  submissionResult,
  diagnosis,
  isAnalyzing,
  agentSteps,
  isDiagnosisStale,
  isAcceptedSubmission,
  activeTab,
  onTabChange,
}: ResultPanelProps) {
  return (
    <div className="coach-panel flex h-full flex-col border-t md:border-l md:border-t-0">
      <div className="shrink-0 border-b border-outline-variant/60 bg-surface-container-lowest px-4 py-3">
        <div className="text-xs font-semibold uppercase tracking-wide text-outline">
          执行观察
        </div>
        <div className="mt-1 text-sm font-semibold text-on-surface">
          Agent 教练反馈
        </div>
        <div className="mt-1 text-xs text-on-surface-variant">
          测试反馈与教练诊断
        </div>
      </div>

      <div className="grid shrink-0 grid-cols-2 border-b border-outline-variant/60 bg-surface-container-low">
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
            agentSteps={agentSteps}
            isAccepted={isAcceptedSubmission}
            isDiagnosisStale={isDiagnosisStale}
          />
        )}

      </div>
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <div className="coach-empty-state m-4">
      <ClipboardCheck className="w-10 h-10 mb-3 text-outline" />
      <p className="text-sm">{text}</p>
    </div>
  );
}
