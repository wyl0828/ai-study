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
  Code,
  Zap,
  MessageSquare,
  Target,
} from "lucide-react";
import type { AgentAnalyzeVO, AgentStepVO, CodeReviewResult } from "@/lib/types";
import {
  agentStepName,
  diagnosisDisplay,
  errorTypeName,
  knowledgePoint,
  trainingPlanTitle,
} from "@/lib/i18n";
import AgentTimeline from "./AgentTimeline";

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
  console.log("[AiDiagnosis] isAccepted:", isAccepted, "isAnalyzing:", isAnalyzing, "diagnosis:", diagnosis);

  // 兼容 ApiResponse 包裹和裸 AgentAnalyzeVO
  const unwrap = (d: AgentAnalyzeVO | null): AgentAnalyzeVO | null => {
    if (!d) return null;
    const raw = d as unknown as Record<string, unknown>;
    return (raw.data ?? d) as AgentAnalyzeVO;
  };

  if (isAccepted) {
    const du = unwrap(diagnosis);
    if (du?.codeReview) {
      return <CodeReviewPanel review={du.codeReview} steps={du.steps ?? []} />;
    }
    if (isAnalyzing) {
      return (
        <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
          <CheckCircle className="w-12 h-12 text-emerald-500 mb-3" />
          <p className="text-lg font-medium mb-1">本次提交已通过</p>
          <p className="text-sm">AI 正在生成面试点评...</p>
        </div>
      );
    }
    // diagnosis 已到达但没有 codeReview（兜底）
    if (du) {
      return (
        <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
          <CheckCircle className="w-12 h-12 text-emerald-500 mb-3" />
          <p className="text-lg font-medium mb-1">本次提交已通过</p>
          <p className="text-sm">面试点评生成完成</p>
        </div>
      );
    }
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
          <AgentTimeline steps={agentSteps} />
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

  const d = unwrap(diagnosis) ?? diagnosis;
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
      {(d.errorType || d.knowledgePoint) && (
        <div className="flex flex-wrap gap-2">
          {d.errorType && (
            <span className="bg-red-50 text-red-700 border border-red-200 text-xs font-semibold px-2.5 py-1 rounded-full">
              {errorTypeName(d.errorType)}
            </span>
          )}
          {d.knowledgePoint && (
            <span className="bg-blue-50 text-blue-700 border border-blue-200 text-xs font-semibold px-2.5 py-1 rounded-full">
              {knowledgePoint(d.knowledgePoint)}
            </span>
          )}
        </div>
      )}

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
          <div className="mt-2">
            <AgentTimeline steps={d.steps} />
          </div>
        </details>
      )}
    </div>
  );
}

function CodeReviewPanel({
  review,
  steps,
}: {
  review: CodeReviewResult;
  steps: AgentStepVO[];
}) {
  return (
    <div className="p-5 space-y-4">
      {/* 通过标签 */}
      <div className="flex items-center gap-2">
        <span className="bg-emerald-50 text-emerald-700 border border-emerald-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          已通过
        </span>
        <span className="bg-blue-50 text-blue-700 border border-blue-200 text-xs font-semibold px-2.5 py-1 rounded-full">
          面试点评
        </span>
      </div>

      {/* 复杂度分析 */}
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
          <Zap className="w-[18px] h-[18px] text-primary" />
          复杂度分析
        </h3>
        <div className="bg-surface-container rounded-lg border border-outline-variant/40 p-4">
          <p className="text-sm text-on-surface-variant leading-relaxed">
            {review.complexity}
          </p>
        </div>
      </div>

      {/* 代码风格 */}
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
          <Code className="w-[18px] h-[18px] text-primary" />
          代码风格
        </h3>
        <div className="bg-surface-container rounded-lg border border-outline-variant/40 p-4">
          <p className="text-sm text-on-surface-variant leading-relaxed">
            {review.codeStyle}
          </p>
        </div>
      </div>

      {/* 面试建议 */}
      <div className="space-y-2">
        <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
          <MessageSquare className="w-[18px] h-[18px] text-primary" />
          面试建议
        </h3>
        <div className="bg-primary/5 rounded-lg border border-primary/20 p-4">
          <p className="text-sm text-on-surface-variant leading-relaxed">
            {review.interviewSuggestion}
          </p>
        </div>
      </div>

      {/* 优化点 */}
      {review.optimizationPoints?.length > 0 && (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
            <Target className="w-[18px] h-[18px] text-primary" />
            优化建议
          </h3>
          <div className="space-y-2">
            {review.optimizationPoints.map((point, i) => (
              <div
                key={i}
                className="flex items-start gap-2 bg-surface-container rounded-lg border border-outline-variant/40 p-3"
              >
                <span className="text-xs font-semibold text-primary bg-primary/10 rounded-full w-5 h-5 flex items-center justify-center shrink-0 mt-0.5">
                  {i + 1}
                </span>
                <p className="text-sm text-on-surface-variant leading-relaxed">
                  {point}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 点评完成状态 */}
      <div className="flex items-center gap-2 text-xs text-on-surface-variant">
        <div className="w-2 h-2 rounded-full bg-emerald-500" />
        点评完成
      </div>

      {/* Agent 步骤（可折叠） */}
      {steps?.length > 0 && (
        <details className="group">
          <summary className="text-xs font-semibold text-on-surface-variant uppercase tracking-wide cursor-pointer hover:text-on-surface transition-colors">
            执行步骤 ({steps.length})
          </summary>
          <div className="mt-2">
            <AgentTimeline steps={steps} />
          </div>
        </details>
      )}
    </div>
  );
}
