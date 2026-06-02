"use client";

import {
  CheckCircle,
  Loader2,
  Stethoscope,
  Brain,
  Lightbulb,
  BookOpen,
  AlertTriangle,
  Code,
  Zap,
  MessageSquare,
  Target,
} from "lucide-react";
import type { AgentAnalyzeVO, AgentStepVO, CodeReviewResult } from "@/lib/types";
import type { ReactNode } from "react";
import {
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
  // 兼容 ApiResponse 包裹和裸 AgentAnalyzeVO
  const unwrap = (d: AgentAnalyzeVO | null): AgentAnalyzeVO | null => {
    if (!d) return null;
    const raw = d as unknown as Record<string, unknown>;
    return (raw.data ?? d) as AgentAnalyzeVO;
  };

  const d = unwrap(diagnosis) ?? diagnosis;
  const hasCodeReview = Boolean(d?.codeReview);

  if (hasCodeReview || isAccepted) {
    if (d?.codeReview) {
      return (
        <CodeReviewPanel
          review={d.codeReview}
          steps={d.steps ?? []}
          isDiagnosisStale={isDiagnosisStale}
        />
      );
    }
    if (isAnalyzing) {
      return (
        <div className="p-5 space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <Loader2 className="w-4 h-4 animate-spin" />
            AI 正在生成面试点评...
          </div>
          {agentSteps.length > 0 && (
            <AgentProcessCard steps={agentSteps} />
          )}
        </div>
      );
    }
    // diagnosis 已到达但没有 codeReview（兜底）
    if (d) {
      return (
        <CodeReviewPanel
          review={{}}
          steps={d.steps ?? []}
          isDiagnosisStale={isDiagnosisStale}
        />
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
          AI 正在诊断错误原因并生成训练建议...
        </div>
        {agentSteps.length > 0 && (
          <AgentProcessCard steps={agentSteps} />
        )}
      </div>
    );
  }

  if (!d) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-on-surface-variant p-6">
        <Stethoscope className="w-10 h-10 mb-3" />
        <p className="text-sm">提交代码后，AI 将自动诊断</p>
      </div>
    );
  }

  const report = {
    failurePhenomenon:
      formatFailurePhenomenon(d.failurePhenomenon ?? d.specificError) ??
      "本次提交未返回明确失败现象，请结合左侧测试结果复盘。",
    rootCause: d.rootCause ?? d.diagnosis ?? d.specificError ?? "暂未定位到明确根因，请先复盘失败用例和核心分支。",
    repairDirection: d.repairDirection ?? d.suggestion ?? d.diagnosis ?? "先根据失败用例修正核心逻辑，再补充边界测试。",
    interviewReminder:
      d.interviewReminder ?? interviewReminderFallback(d.knowledgePoint),
  };

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

      <div className="space-y-3">
        <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
          <Brain className="w-[18px] h-[18px] text-primary" />
          教练报告
        </h3>
        <CoachReportSection
          title="失败现象"
          icon={<Stethoscope className="w-[18px] h-[18px] text-primary" />}
          text={report.failurePhenomenon}
        />
        <CoachReportSection
          title="根本原因"
          icon={<Brain className="w-[18px] h-[18px] text-primary" />}
          text={report.rootCause}
        />
        <CoachReportSection
          title="修改方向"
          icon={<Lightbulb className="w-[18px] h-[18px] text-primary" />}
          text={report.repairDirection}
          accent
        />
        <CoachReportSection
          title="面试提醒"
          icon={<MessageSquare className="w-[18px] h-[18px] text-primary" />}
          text={report.interviewReminder}
        />
      </div>

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
            Agent 诊断过程 ({d.steps.length})
          </summary>
          <div className="mt-2">
            <AgentTimeline steps={d.steps} />
          </div>
        </details>
      )}
    </div>
  );
}

function CoachReportSection({
  title,
  icon,
  text,
  accent = false,
}: {
  title: string;
  icon: ReactNode;
  text: string;
  accent?: boolean;
}) {
  return (
    <div
      className={`rounded-lg border p-4 ${
        accent
          ? "border-primary/20 bg-primary/5"
          : "border-outline-variant/40 bg-surface-container"
      }`}
    >
      <h4 className="mb-2 flex items-center gap-1.5 text-sm font-semibold text-on-surface">
        {icon}
        {title}
      </h4>
      <p className="whitespace-pre-wrap text-sm leading-relaxed text-on-surface-variant">
        {text}
      </p>
    </div>
  );
}

function CodeReviewPanel({
  review,
  steps,
  isDiagnosisStale,
}: {
  review: Partial<CodeReviewResult>;
  steps: AgentStepVO[];
  isDiagnosisStale: boolean;
}) {
  const optimizationPoints = review.optimizationPoints?.filter(Boolean) ?? [];

  return (
    <div className="p-5 space-y-4">
      {isDiagnosisStale && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" />
          <span>该点评基于上次提交，当前代码已修改，仅供参考。</span>
        </div>
      )}

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
            {formatReviewField(
              review.complexity,
              "暂未返回复杂度分析。面试讲解时可以先说明时间复杂度、空间复杂度，以及主要循环或数据结构的成本。"
            )}
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
            {formatReviewField(
              review.codeStyle,
              "暂未返回代码风格点评。建议检查变量命名、边界处理、重复逻辑和是否便于面试官快速读懂。"
            )}
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
            {formatReviewField(
              review.interviewSuggestion,
              "暂未返回面试建议。复盘时可以主动说明解题思路、关键判断和可优化空间。"
            )}
          </p>
        </div>
      </div>

      {/* 优化点 */}
      {optimizationPoints.length > 0 ? (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
            <Target className="w-[18px] h-[18px] text-primary" />
            优化建议
          </h3>
          <div className="space-y-2">
            {optimizationPoints.map((point, i) => (
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
      ) : (
        <div className="space-y-2">
          <h3 className="text-sm font-semibold text-on-surface flex items-center gap-1.5">
            <Target className="w-[18px] h-[18px] text-primary" />
            优化建议
          </h3>
          <div className="bg-surface-container rounded-lg border border-outline-variant/40 p-4">
            <p className="text-sm text-on-surface-variant leading-relaxed">
              暂未返回具体优化点。本次提交已通过，可以优先复盘边界条件、复杂度表达和是否有更清晰的写法。
            </p>
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
            Agent 诊断过程 ({steps.length})
          </summary>
          <div className="mt-2">
            <AgentTimeline steps={steps} />
          </div>
        </details>
      )}
    </div>
  );
}

function AgentProcessCard({ steps }: { steps: AgentStepVO[] }) {
  return (
    <div className="rounded-lg border border-primary/20 bg-primary/5 p-4">
      <h3 className="mb-3 text-sm font-semibold text-on-surface">
        Agent 诊断过程
      </h3>
      <AgentTimeline steps={steps} showSummary />
    </div>
  );
}

function formatReviewField(value: string | null | undefined, fallback: string) {
  const text = value?.trim();
  return text || fallback;
}

function interviewReminderFallback(rawKnowledgePoint: string | null | undefined) {
  const point = rawKnowledgePoint?.trim();
  if (point) {
    return `面试中要主动说明「${knowledgePoint(point)}」的关键边界、失败用例和修正思路。`;
  }
  return "面试中要主动说明失败用例暴露的边界条件，以及你如何一步步修正。";
}

function formatFailurePhenomenon(value: string | null | undefined) {
  const text = value?.trim();
  if (!text) {
    return null;
  }
  const firstLine = text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find(Boolean);
  if (!firstLine) {
    return null;
  }
  const compactLine = firstLine.replace(/^Exception in thread "[^"]+"\s+/, "");
  if (compactLine.includes("OutOfMemoryError")) {
    return `运行时异常：${compactLine}。程序在遍历或输出时没有正常终止，常见原因是链表成环、递归未收敛或循环条件错误。`;
  }
  if (/(Exception|Error)(:|$)/.test(compactLine)) {
    return `运行时异常：${compactLine}。请结合测试结果中的堆栈定位触发位置。`;
  }
  return text;
}
