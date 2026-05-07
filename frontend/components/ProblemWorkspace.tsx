"use client";

import { useState, useCallback } from "react";
import { X, AlertCircle } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO } from "@/lib/types";
import { submissionApi, agentApi } from "@/lib/api";
import CodeEditor from "./CodeEditor";
import ResultPanel from "./ResultPanel";

interface ProblemWorkspaceProps {
  problemId: number;
  defaultCode: string;
}

export default function ProblemWorkspace({
  problemId,
  defaultCode,
}: ProblemWorkspaceProps) {
  const [code, setCode] = useState(defaultCode);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submissionResult, setSubmissionResult] =
    useState<SubmissionResult | null>(null);
  const [diagnosis, setDiagnosis] = useState<AgentAnalyzeVO | null>(null);
  const [activeTab, setActiveTab] = useState<"test" | "diagnosis" | "hint">(
    "test"
  );

  const handleReset = useCallback(() => {
    setCode(defaultCode);
  }, [defaultCode]);

  const handleSubmit = useCallback(async () => {
    setIsSubmitting(true);
    setError(null);
    setSubmissionResult(null);
    setDiagnosis(null);
    setActiveTab("test");

    try {
      const { data: result } = await submissionApi.submit({
        userId: 1,
        problemId,
        language: "java",
        code,
      });

      setSubmissionResult(result);

      if (result.status !== "ACCEPTED") {
        setIsAnalyzing(true);
        try {
          const { data: diag } = await agentApi.analyze(result.submissionId);
          setDiagnosis(diag);
          setActiveTab("diagnosis");
        } catch (diagErr) {
          console.error("AI 诊断失败:", diagErr);
          setError("AI 诊断失败，请稍后重试");
        } finally {
          setIsAnalyzing(false);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "提交失败，请稍后重试");
    } finally {
      setIsSubmitting(false);
    }
  }, [problemId, code]);

  return (
    <>
      {/* 全局错误提示 */}
      {error && (
        <div className="absolute top-0 left-0 right-0 z-10 bg-error-container text-on-error-container px-4 py-2 text-sm flex items-center gap-2">
          <AlertCircle className="w-4 h-4" />
          {error}
          <button
            onClick={() => setError(null)}
            className="ml-auto hover:opacity-70"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 中栏：代码编辑器 */}
      <section className="flex-1 h-full flex flex-col bg-[#0d1117] relative min-w-0">
        <CodeEditor
          code={code}
          onChange={setCode}
          onSubmit={handleSubmit}
          onReset={handleReset}
          isSubmitting={isSubmitting}
          isAnalyzing={isAnalyzing}
        />
      </section>

      {/* 右栏：结果面板 */}
      <section className="w-[30%] min-w-[320px] max-w-[520px] h-full flex flex-col bg-surface-container-lowest">
        <ResultPanel
          submissionResult={submissionResult}
          diagnosis={diagnosis}
          isAnalyzing={isAnalyzing}
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />
      </section>
    </>
  );
}
