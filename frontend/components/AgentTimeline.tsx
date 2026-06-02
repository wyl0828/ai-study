"use client";

import { CheckCircle, XCircle, Loader2, Circle } from "lucide-react";
import type { AgentStepVO } from "@/lib/types";
import { agentStepName } from "@/lib/i18n";

interface AgentTimelineProps {
  steps: AgentStepVO[];
  showSummary?: boolean;
}

export default function AgentTimeline({
  steps,
  showSummary = false,
}: AgentTimelineProps) {
  if (steps.length === 0) {
    return null;
  }

  const totalDuration = steps.reduce(
    (sum, step) => sum + (step.durationMs ?? 0),
    0
  );

  return (
    <div className="space-y-0">
      {steps.map((step, i) => {
        const isLast = i === steps.length - 1;
        const isRunning = step.status === "RUNNING";
        const isSuccess = step.status === "SUCCESS";
        const isFailed = step.status === "FAILED";
        const statusLabel = agentStepStatusLabel(step.status);

        return (
          <div key={step.stepName} className="relative flex gap-3">
            {/* 左侧连接线 */}
            <div className="flex flex-col items-center">
              <div
                className={`w-6 h-6 rounded-full flex items-center justify-center shrink-0 ${
                  isRunning
                    ? "bg-primary/10"
                    : isSuccess
                    ? "bg-emerald-50"
                    : isFailed
                    ? "bg-red-50"
                    : "bg-surface-container"
                }`}
              >
                {isRunning ? (
                  <Loader2 className="w-3.5 h-3.5 text-primary animate-spin" />
                ) : isSuccess ? (
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                ) : isFailed ? (
                  <XCircle className="w-3.5 h-3.5 text-error" />
                ) : (
                  <Circle className="w-3.5 h-3.5 text-on-surface-variant/40" />
                )}
              </div>
              {!isLast && (
                <div
                  className={`w-px flex-1 min-h-[24px] ${
                    isSuccess
                      ? "bg-emerald-200"
                      : isFailed
                      ? "bg-red-200"
                      : "bg-outline-variant/30"
                  }`}
                />
              )}
            </div>

            {/* 右侧内容 */}
            <div
              className={`flex-1 pb-4 ${
                isRunning ? "animate-pulse" : ""
              }`}
            >
              <div className="flex items-center gap-2">
                <span
                  className={`text-xs font-medium ${
                    isRunning
                      ? "text-primary"
                      : isSuccess
                      ? "text-on-surface"
                      : isFailed
                      ? "text-error"
                      : "text-on-surface-variant/60"
                  }`}
                >
                  {agentStepName(step.stepName)}
                </span>
                {step.toolName && (
                  <span className="text-[10px] text-on-surface-variant/50 bg-surface-container px-1.5 py-0.5 rounded">
                    {step.toolName}
                  </span>
                )}
                <span
                  className={`rounded-full px-1.5 py-0.5 text-[10px] font-medium ${
                    isRunning
                      ? "bg-primary/10 text-primary"
                      : isSuccess
                      ? "bg-emerald-50 text-emerald-700"
                      : isFailed
                      ? "bg-red-50 text-error"
                      : "bg-surface-container text-on-surface-variant"
                  }`}
                >
                  {statusLabel}
                </span>
                {step.durationMs != null && (
                  <span className="text-[10px] text-on-surface-variant/60 ml-auto">
                    {formatDuration(step.durationMs)}
                  </span>
                )}
              </div>
              {showSummary && step.outputSummary && (
                <p className="text-[11px] text-on-surface-variant/70 mt-1 leading-relaxed">
                  {step.outputSummary}
                </p>
              )}
              {step.errorMessage && (
                <p className="text-[11px] text-error/80 mt-1">
                  {step.errorMessage}
                </p>
              )}
            </div>
          </div>
        );
      })}

      {/* 总耗时 */}
      {totalDuration > 0 && (
        <div className="flex items-center gap-2 text-[10px] text-on-surface-variant/50 pt-1">
          <div className="w-6" />
          <span>总耗时 {formatDuration(totalDuration)}</span>
        </div>
      )}
    </div>
  );
}

function agentStepStatusLabel(status: string) {
  if (status === "RUNNING") return "执行中";
  if (status === "SUCCESS") return "完成";
  if (status === "FAILED") return "失败";
  return "等待";
}

function formatDuration(durationMs: number) {
  if (durationMs < 1000) {
    return `${durationMs}ms`;
  }
  return `${(durationMs / 1000).toFixed(1)}s`;
}
