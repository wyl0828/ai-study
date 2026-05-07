"use client";

import {
  CheckCircle,
  XCircle,
  Loader2,
  Stethoscope,
  Brain,
  Lightbulb,
  BookOpen,
  Circle,
} from "lucide-react";
import type { AgentAnalyzeVO } from "@/lib/types";

interface AiDiagnosisProps {
  diagnosis: AgentAnalyzeVO | null;
  isAnalyzing: boolean;
  isAccepted: boolean;
}

export default function AiDiagnosis({
  diagnosis,
  isAnalyzing,
  isAccepted,
}: AiDiagnosisProps) {
  if (isAccepted) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <CheckCircle className="w-12 h-12 text-emerald-500 mb-3" />
        <p className="text-lg font-medium mb-1">本次提交已通过</p>
        <p className="text-sm">暂无错误诊断</p>
      </div>
    );
  }

  if (isAnalyzing) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Loader2 className="w-10 h-10 text-primary mb-3 animate-spin" />
        <p className="text-sm font-medium">AI 正在分析你的代码...</p>
        <p className="text-xs mt-1">正在诊断错误原因并生成提示</p>
      </div>
    );
  }

  if (!diagnosis) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Stethoscope className="w-10 h-10 mb-3" />
        <p className="text-sm">提交代码后，AI 将自动诊断</p>
      </div>
    );
  }

  return (
    <div className="p-5 space-y-4">
      {/* 错误类型标签 */}
      <div className="flex flex-wrap gap-2">
        <span className="bg-red-50 text-red-700 border border-red-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          {diagnosis.errorType}
        </span>
        <span className="bg-blue-50 text-blue-700 border border-blue-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          {diagnosis.knowledgePoint}
        </span>
      </div>

      {/* 诊断内容 */}
      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
          <Brain className="w-[18px] h-[18px] text-primary" />
          错误诊断
        </h3>
        <div className="bg-surface-container rounded-lg border border-outline-variant/40 p-4">
          <p className="text-sm text-on-surface-variant leading-relaxed whitespace-pre-wrap">
            {diagnosis.diagnosis}
          </p>
        </div>
      </div>

      {/* 具体错误 */}
      {diagnosis.specificError && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
            <Lightbulb className="w-[18px] h-[18px] text-primary" />
            改进建议
          </h3>
          <div className="bg-primary/5 rounded-lg border border-primary/20 p-4">
            <p className="text-sm text-on-surface-variant leading-relaxed">
              {diagnosis.specificError}
            </p>
          </div>
        </div>
      )}

      {/* 训练计划标题 */}
      {diagnosis.trainingPlanTitle && (
        <div className="flex items-center gap-2 bg-surface-container rounded-lg border border-outline-variant/40 p-3">
          <BookOpen className="w-4 h-4 text-primary" />
          <span className="text-sm text-on-surface">
            推荐训练：{diagnosis.trainingPlanTitle}
          </span>
        </div>
      )}

      {/* 诊断完成状态 */}
      <div className="flex items-center gap-2 text-xs text-on-surface-variant">
        <div className="w-2 h-2 rounded-full bg-emerald-500" />
        诊断完成
      </div>

      {/* Agent 步骤（可折叠） */}
      {diagnosis.steps?.length > 0 && (
        <details className="group">
          <summary className="text-xs font-semibold text-on-surface-variant uppercase tracking-wide cursor-pointer hover:text-on-surface transition-colors">
            Agent 执行步骤 ({diagnosis.steps.length})
          </summary>
          <div className="mt-2 space-y-1">
            {diagnosis.steps.map((step, i) => (
              <div
                key={i}
                className="flex items-center gap-2 text-xs text-on-surface-variant py-1"
              >
                {step.status === "SUCCESS" ? (
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                ) : step.status === "FAILED" ? (
                  <XCircle className="w-3.5 h-3.5 text-error" />
                ) : (
                  <Circle className="w-3.5 h-3.5 text-on-surface-variant" />
                )}
                <span className="font-medium">{step.stepName}</span>
                {step.durationMs != null && (
                  <span className="text-outline">{step.durationMs}ms</span>
                )}
              </div>
            ))}
          </div>
        </details>
      )}
    </div>
  );
}
