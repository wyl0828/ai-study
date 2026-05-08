"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import { X, AlertCircle } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO } from "@/lib/types";
import { submissionApi, agentApi } from "@/lib/api";
import {
  clearDraft,
  formatDraftTime,
  loadDraft,
  saveDraft,
  type ProblemDraft,
} from "@/lib/draft";
import CodeEditor from "./CodeEditor";
import ResultPanel from "./ResultPanel";

interface ProblemWorkspaceProps {
  problemId: number;
  defaultCode: string;
}

const DEMO_USER_ID = 1;

export default function ProblemWorkspace({
  problemId,
  defaultCode,
}: ProblemWorkspaceProps) {
  const [code, setCode] = useState(defaultCode);
  const skipNextAutosaveRef = useRef(false);
  const [isDraftReady, setIsDraftReady] = useState(false);
  const [draftSavedAt, setDraftSavedAt] = useState<string | null>(null);
  const [showDraftNotice, setShowDraftNotice] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [submissionResult, setSubmissionResult] =
    useState<ProblemDraft["lastResult"] | null>(null);
  const [diagnosis, setDiagnosis] =
    useState<ProblemDraft["lastDiagnosis"] | null>(null);
  const [activeTab, setActiveTab] = useState<"test" | "diagnosis" | "hint">(
    "test"
  );

  useEffect(() => {
    skipNextAutosaveRef.current = true;
    const draft = loadDraft(DEMO_USER_ID, problemId);

    if (draft) {
      setCode(draft.code);
      setSubmissionResult(draft.lastResult ?? null);
      setDiagnosis(draft.lastDiagnosis ?? null);
      setDraftSavedAt(draft.updatedAt);
      setShowDraftNotice(true);
      setActiveTab(draft.lastDiagnosis ? "diagnosis" : "test");
    } else {
      setCode(defaultCode);
      setSubmissionResult(null);
      setDiagnosis(null);
      setDraftSavedAt(null);
      setShowDraftNotice(false);
      setActiveTab("test");
    }

    setError(null);
    setIsDraftReady(true);
  }, [defaultCode, problemId]);

  useEffect(() => {
    if (!isDraftReady) {
      return;
    }

    if (skipNextAutosaveRef.current) {
      skipNextAutosaveRef.current = false;
      return;
    }

    const timer = window.setTimeout(() => {
      saveDraft(DEMO_USER_ID, problemId, {
        code,
        language: "java",
      });
      setDraftSavedAt(new Date().toISOString());
    }, 1000);

    return () => window.clearTimeout(timer);
  }, [code, isDraftReady, problemId]);

  const handleReset = useCallback(() => {
    const confirmed = window.confirm(
      "确定要重置代码吗？将恢复为默认模板并清除运行结果。"
    );
    if (!confirmed) {
      return;
    }

    clearDraft(DEMO_USER_ID, problemId);
    skipNextAutosaveRef.current = true;
    setCode(defaultCode);
    setSubmissionResult(null);
    setDiagnosis(null);
    setActiveTab("test");
    setDraftSavedAt(null);
    setShowDraftNotice(false);
  }, [defaultCode, problemId]);

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

      const resultWithSnapshot: ProblemDraft["lastResult"] = {
        ...result,
        codeSnapshot: code,
        submittedAt: new Date().toISOString(),
      };

      setSubmissionResult(resultWithSnapshot);
      saveDraft(DEMO_USER_ID, problemId, {
        code,
        language: "java",
        lastResult: resultWithSnapshot,
        lastDiagnosis: undefined,
      });
      setDraftSavedAt(new Date().toISOString());

      if (result.status !== "ACCEPTED") {
        setIsAnalyzing(true);
        try {
          const { data: diag } = await agentApi.analyze(result.submissionId);
          const diagnosisWithSnapshot: ProblemDraft["lastDiagnosis"] = {
            ...diag,
            codeSnapshot: code,
          };
          setDiagnosis(diagnosisWithSnapshot);
          saveDraft(DEMO_USER_ID, problemId, {
            code,
            language: "java",
            lastResult: resultWithSnapshot,
            lastDiagnosis: diagnosisWithSnapshot,
          });
          setDraftSavedAt(new Date().toISOString());
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

  const isCurrentCodeAccepted =
    submissionResult?.status === "ACCEPTED" &&
    submissionResult.codeSnapshot === code;

  const isDiagnosisStale = Boolean(
    diagnosis && diagnosis.codeSnapshot !== code
  );

  const submitLabel = isCurrentCodeAccepted
    ? "已通过"
    : submissionResult
    ? "重新提交"
    : undefined;

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
          draftSavedAt={
            showDraftNotice && draftSavedAt
              ? formatDraftTime(draftSavedAt)
              : null
          }
          onDismissDraftNotice={() => setShowDraftNotice(false)}
          isSubmitting={isSubmitting}
          isAnalyzing={isAnalyzing}
          isCurrentCodeAccepted={isCurrentCodeAccepted}
          submitLabel={submitLabel}
        />
      </section>

      {/* 右栏：结果面板 */}
      <section className="w-[30%] min-w-[320px] max-w-[520px] h-full flex flex-col bg-surface-container-lowest">
        <ResultPanel
          submissionResult={submissionResult as SubmissionResult | null}
          diagnosis={diagnosis as AgentAnalyzeVO | null}
          isAnalyzing={isAnalyzing}
          isDiagnosisStale={isDiagnosisStale}
          isCurrentCodeAccepted={isCurrentCodeAccepted}
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />
      </section>
    </>
  );
}
