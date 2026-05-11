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
  AlertTriangle,
} from "lucide-react";
import type { AgentAnalyzeVO, AgentStepVO } from "@/lib/types";
import {
  agentStepName,
  diagnosisDisplay,
  errorTypeName,
  knowledgePoint,
  trainingPlanTitle,
} from "@/lib/i18n";

interface AiDiagnosisProps {
  diagnosis: AgentAnalyzeVO | null;
  isAnalyzing: boolean;
  agentSteps: AgentStepVO[];
  isAccepted: boolean;
  isDiagnosisStale: boolean;
}

export default function AiDiagnosis({
  diagnosis,
  isAnalyzing,
  agentSteps,
  isAccepted,
  isDiagnosisStale,
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
      <div className="p-5 space-y-3">
        <div className="flex items-center gap-2 text-sm font-medium text-primary">
          <Loader2 className="w-4 h-4 animate-spin" />
          AI 正在诊断你的代码...
        </div>
        {agentSteps.length > 0 && (
          <div className="space-y-1">
            {agentSteps.map((step) => (
              <div
                key={step.stepName}
                className="flex items-center gap-2 text-xs py-1.5"
              >
                {step.status === "RUNNING" ? (
                  <Loader2 className="w-3.5 h-3.5 text-primary animate-spin" />
                ) : step.status === "SUCCESS" ? (
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                ) : step.status === "FAILED" ? (
                  <XCircle className="w-3.5 h-3.5 text-error" />
                ) : (
                  <Circle className="w-3.5 h-3.5 text-on-surface-variant" />
                )}
                <span
                  className={
                    step.status === "RUNNING"
                      ? "text-primary font-medium"
                      : step.status === "SUCCESS"
                      ? "text-on-surface-variant"
                      : step.status === "FAILED"
                      ? "text-error"
                      : "text-on-surface-variant/60"
                  }
                >
                  {agentStepName(step.stepName)}
                </span>
                {step.status === "SUCCESS" && step.durationMs != null && (
                  <span className="text-outline ml-auto">
                    {step.durationMs}ms
                  </span>
                )}
              </div>
            ))}
          </div>
        )}
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

  // 兼容 ApiResponse 包裹和裸 AgentAnalyzeVO
  const raw = diagnosis as unknown as Record<string, unknown>;
  const d = (raw.data ?? diagnosis) as AgentAnalyzeVO;
  const display = diagnosisDisplay(d.diagnosis, d.specificError);

  return (
    <div className="p-5 space-y-4">
      {isDiagnosisStale && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>该诊断基于上次提交，当前代码已修改，仅供参考。</span>
        </div>
      )}

      {/* 错误类型标签 */}
      <div className="flex flex-wrap gap-2">
        <span className="bg-red-50 text-red-700 border border-red-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          {errorTypeName(d.errorType)}
        </span>
        <span className="bg-blue-50 text-blue-700 border border-blue-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          {knowledgePoint(d.knowledgePoint)}
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
            {display.diagnosisText}
          </p>
        </div>
      </div>

      {/* 具体错误 */}
      {display.suggestionText && (
        <div className="space-y-3">
          <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
            <Lightbulb className="w-[18px] h-[18px] text-primary" />
            改进建议
          </h3>
          <div className="bg-primary/5 rounded-lg border border-primary/20 p-4">
            <p className="text-sm text-on-surface-variant leading-relaxed">
              {display.suggestionText}
            </p>
          </div>
        </div>
      )}

      {/* 训练计划标题 */}
      {d.trainingPlanTitle && (
        <div className="flex items-center gap-2 bg-surface-container rounded-lg border border-outline-variant/40 p-3">
          <BookOpen className="w-4 h-4 text-primary" />
          <span className="text-sm text-on-surface">
            推荐训练：{trainingPlanTitle(d.trainingPlanTitle)}
          </span>
        </div>
      )}

      {/* 诊断完成状态 */}
      <div className="flex items-center gap-2 text-xs text-on-surface-variant">
        <div className="w-2 h-2 rounded-full bg-emerald-500" />
        诊断完成
      </div>

      {/* Agent 步骤（可折叠） */}
      {d.steps?.length > 0 && (
        <details className="group">
          <summary className="text-xs font-semibold text-on-surface-variant uppercase tracking-wide cursor-pointer hover:text-on-surface transition-colors">
            诊断执行步骤 ({d.steps.length})
          </summary>
          <div className="mt-2 space-y-1">
            {d.steps.map((step: AgentStepVO, i: number) => (
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
                <span className="font-medium">{agentStepName(step.stepName)}</span>
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
