"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import { X, AlertCircle } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO } from "@/lib/types";
import { submissionApi, agentApi, problemApi } from "@/lib/api";
import {
  clearDraft,
  formatDraftTime,
  loadDraft,
  saveDraft,
  shouldUseDraftForTemplate,
  type ProblemDraft,
} from "@/lib/draft";
import CodeEditor from "./CodeEditor";
import ResultPanel from "./ResultPanel";

interface ProblemWorkspaceProps {
  problemId: number;
}

const DEMO_USER_ID = 1;

export default function ProblemWorkspace({
  problemId,
}: ProblemWorkspaceProps) {
  const [code, setCode] = useState("");
  const [templateCode, setTemplateCode] = useState("");
  const skipNextAutosaveRef = useRef(false);
  const [isDraftReady, setIsDraftReady] = useState(false);
  const [isTemplateLoading, setIsTemplateLoading] = useState(true);
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

  const loadProblemTemplate = useCallback(
    async (preferDraft: boolean) => {
      setIsTemplateLoading(true);
      setError(null);
      skipNextAutosaveRef.current = true;

      try {
        const { data: template } = await problemApi.template(problemId);
        const nextTemplateCode = template.templateCode;
        const draft = preferDraft ? loadDraft(DEMO_USER_ID, problemId) : null;

        setTemplateCode(nextTemplateCode);

        if (draft && shouldUseDraftForTemplate(draft, nextTemplateCode)) {
          setCode(draft.code);
          setSubmissionResult(draft.lastResult ?? null);
          setDiagnosis(draft.lastDiagnosis ?? null);
          setDraftSavedAt(draft.updatedAt);
          setShowDraftNotice(true);
          setActiveTab(draft.lastDiagnosis ? "diagnosis" : "test");
        } else {
          if (draft) {
            clearDraft(DEMO_USER_ID, problemId);
          }
          setCode(nextTemplateCode);
          setSubmissionResult(null);
          setDiagnosis(null);
          setDraftSavedAt(null);
          setShowDraftNotice(false);
          setActiveTab("test");
        }

        setIsDraftReady(true);
      } catch (err) {
        setIsDraftReady(false);
        setError(err instanceof Error ? err.message : "代码模板加载失败，请刷新重试");
      } finally {
        setIsTemplateLoading(false);
      }
    },
    [problemId]
  );

  useEffect(() => {
    setIsDraftReady(false);
    setTemplateCode("");
    setCode("");
    setSubmissionResult(null);
    setDiagnosis(null);
    setDraftSavedAt(null);
    setShowDraftNotice(false);
    setActiveTab("test");
    void loadProblemTemplate(true);
  }, [loadProblemTemplate]);

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
      "确定要重置代码吗？将重新读取后端模板并清除运行结果。"
    );
    if (!confirmed) {
      return;
    }

    clearDraft(DEMO_USER_ID, problemId);
    void loadProblemTemplate(false);
  }, [loadProblemTemplate, problemId]);

  const handleSubmit = useCallback(async () => {
    if (!templateCode) {
      setError("代码模板仍在加载，请稍后再提交");
      return;
    }

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
  }, [problemId, code, templateCode]);

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
          isTemplateLoading={isTemplateLoading}
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
