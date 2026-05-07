"use client";

import { CheckCircle, XCircle, AlertCircle } from "lucide-react";
import type { SubmissionResult } from "@/lib/types";

interface TestResultProps {
  result: SubmissionResult;
}

export default function TestResult({ result }: TestResultProps) {
  const isPassed = result.status === "ACCEPTED";
  const passRate =
    result.totalCount > 0
      ? Math.round((result.passedCount / result.totalCount) * 100)
      : 0;

  return (
    <div className="p-5 space-y-4">
      {/* 状态总览 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <span
            className={`text-xs font-semibold px-2.5 py-1 rounded-full flex items-center gap-1 ${
              isPassed
                ? "bg-emerald-50 text-emerald-700"
                : "bg-red-100 text-red-700"
            }`}
          >
            {isPassed ? (
              <CheckCircle className="w-3.5 h-3.5" />
            ) : (
              <XCircle className="w-3.5 h-3.5" />
            )}
            {isPassed ? "通过" : "未通过"}
          </span>
          <span className="text-sm text-on-surface-variant">
            通过 {result.passedCount} / {result.totalCount}
          </span>
        </div>
        {(result.runtime != null || result.memory != null) && (
          <div className="text-xs text-outline">
            {result.runtime != null && <span>{result.runtime}ms</span>}
            {result.runtime != null && result.memory != null && (
              <span> &middot; </span>
            )}
            {result.memory != null && <span>{result.memory}MB</span>}
          </div>
        )}
      </div>

      {/* 通过进度条 */}
      <div className="w-full bg-surface-container-high h-2 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full ${
            isPassed ? "bg-emerald-500" : "bg-error"
          }`}
          style={{ width: `${passRate}%` }}
        />
      </div>

      {/* 错误信息 */}
      {result.errorMessage && (
        <div className="bg-surface-container rounded-lg border border-outline-variant/40 p-3">
          <pre className="whitespace-pre-wrap font-mono text-xs text-on-surface-variant">
            {result.errorMessage}
          </pre>
        </div>
      )}

      {/* 失败用例 */}
      {result.failedCases?.length > 0 && (
        <div>
          <h3 className="text-xs font-semibold text-on-surface mb-2 flex items-center gap-1">
            <AlertCircle className="w-[15px] h-[15px] text-error" />
            失败用例
          </h3>
          <div className="space-y-3">
            {result.failedCases.map((fc) => (
              <div
                key={fc.caseId}
                className="bg-surface-container rounded-lg border border-outline-variant/40 overflow-hidden"
              >
                <div className="bg-surface-container-high px-3 py-1.5 text-xs font-medium text-on-surface-variant border-b border-outline-variant/30">
                  用例 {fc.caseId}
                </div>
                <div className="p-3 space-y-2 text-xs font-mono">
                  <div>
                    <span className="text-secondary font-sans font-medium">输入：</span>
                    <span className="text-on-surface">{fc.input}</span>
                  </div>
                  <div>
                    <span className="text-secondary font-sans font-medium">期望：</span>
                    <span className="text-emerald-600">{fc.expectedOutput}</span>
                  </div>
                  <div>
                    <span className="text-secondary font-sans font-medium">实际：</span>
                    <span className="text-error">{fc.actualOutput}</span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
