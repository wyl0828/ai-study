"use client";

import { useState, useCallback, useEffect, useRef } from "react";
import { X, AlertCircle } from "lucide-react";
import type { SubmissionResult, AgentAnalyzeVO, AgentStepVO } from "@/lib/types";

const STEP_ORDER = [
  "PLANNING",
  "CODE_EXECUTION",
  "OBSERVATION",
  "ERROR_CLASSIFICATION",
  "MEMORY_UPDATE",
  "TRAINING_PLAN",
  "COMPLETED",
  "FAILED",
];

const getStepRank = (stepName: string) => {
  const index = STEP_ORDER.indexOf(stepName);
  return index === -1 ? Number.MAX_SAFE_INTEGER : index;
};
import { problemApi, submissionApi, agentApi } from "@/lib/api";
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
}

const DEMO_USER_ID = 1;

export default function ProblemWorkspace({
  problemId,
}: ProblemWorkspaceProps) {
  const [code, setCode] = useState("");
  const skipNextAutosaveRef = useRef(false);
  const templateRequestIdRef = useRef(0);
  const [isDraftReady, setIsDraftReady] = useState(false);
  const [isTemplateLoading, setIsTemplateLoading] = useState(true);
  const [draftSavedAt, setDraftSavedAt] = useState<string | null>(null);
  const [showDraftNotice, setShowDraftNotice] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [agentSteps, setAgentSteps] = useState<AgentStepVO[]>([]);
  const streamControllerRef = useRef<AbortController | null>(null);
  const currentStreamIdRef = useRef(0);
  const diagnosisCodeSnapshotRef = useRef<string | null>(null);
  const [submissionResult, setSubmissionResult] =
    useState<ProblemDraft["lastResult"] | null>(null);
  const [diagnosis, setDiagnosis] = useState<AgentAnalyzeVO | null>(null);
  const [activeTab, setActiveTab] = useState<"test" | "diagnosis">(
    "test"
  );

  const loadTemplate = useCallback(
    async (restoreDraft: boolean) => {
      const requestId = templateRequestIdRef.current + 1;
      templateRequestIdRef.current = requestId;
      skipNextAutosaveRef.current = true;
      setIsDraftReady(false);
      setIsTemplateLoading(true);
      setError(null);

      try {
        const { data: template } = await problemApi.template(problemId);
        if (requestId !== templateRequestIdRef.current) {
          return;
        }
        const draft = restoreDraft ? loadDraft(DEMO_USER_ID, problemId) : null;

        if (draft) {
          setCode(draft.code);
          setSubmissionResult(draft.lastResult ?? null);
          if (draft.lastDiagnosis) {
            const { codeSnapshot, ...diagData } = draft.lastDiagnosis;
            setDiagnosis(diagData);
            diagnosisCodeSnapshotRef.current = codeSnapshot;
          } else {
            setDiagnosis(null);
            diagnosisCodeSnapshotRef.current = null;
          }
          setDraftSavedAt(draft.updatedAt);
          setShowDraftNotice(true);
          setActiveTab(draft.lastDiagnosis ? "diagnosis" : "test");
        } else {
          setCode(template.templateCode);
          setSubmissionResult(null);
          setDiagnosis(null);
          setDraftSavedAt(null);
          setShowDraftNotice(false);
          setActiveTab("test");
        }
      } catch (err) {
        if (requestId !== templateRequestIdRef.current) {
          return;
        }
        const draft = restoreDraft ? loadDraft(DEMO_USER_ID, problemId) : null;
        if (draft) {
          setCode(draft.code);
          setSubmissionResult(draft.lastResult ?? null);
          if (draft.lastDiagnosis) {
            const { codeSnapshot, ...diagData } = draft.lastDiagnosis;
            setDiagnosis(diagData);
            diagnosisCodeSnapshotRef.current = codeSnapshot;
          } else {
            setDiagnosis(null);
            diagnosisCodeSnapshotRef.current = null;
          }
          setDraftSavedAt(draft.updatedAt);
          setShowDraftNotice(true);
          setActiveTab(draft.lastDiagnosis ? "diagnosis" : "test");
        } else {
          setCode("");
          setSubmissionResult(null);
          setDiagnosis(null);
          setDraftSavedAt(null);
          setShowDraftNotice(false);
          setActiveTab("test");
        }
        setError(
          err instanceof Error ? err.message : "代码模板加载失败，请稍后重试"
        );
      } finally {
        if (requestId === templateRequestIdRef.current) {
          setIsDraftReady(true);
          setIsTemplateLoading(false);
        }
      }
    },
    [problemId]
  );

  useEffect(() => {
    void loadTemplate(true);
  }, [loadTemplate]);

  useEffect(() => {
    return () => {
      streamControllerRef.current?.abort();
    };
  }, []);

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
    void loadTemplate(false);
  }, [loadTemplate, problemId]);

  const startAiAnalysis = useCallback(
    (
      resultWithSnapshot: NonNullable<ProblemDraft["lastResult"]>,
      codeSnapshot: string
    ) => {
      streamControllerRef.current?.abort();
      currentStreamIdRef.current += 1;
      const streamId = currentStreamIdRef.current;
      let doneReceived = false;

      // 清空旧诊断状态，只在新一次诊断开始前执行
      setIsAnalyzing(true);
      setDiagnosis(null);
      diagnosisCodeSnapshotRef.current = null;
      setAgentSteps([]);
      setError(null);
      setActiveTab("test");

      streamControllerRef.current = agentApi.streamDiagnosis(
        resultWithSnapshot.submissionId,
        {
          onStep: (step) => {
            if (streamId !== currentStreamIdRef.current) return;
            setAgentSteps((prev) => {
              const idx = prev.findIndex(
                (s) => s.stepName === step.stepName
              );
              const next =
                idx >= 0
                  ? prev.map((s, i) => (i === idx ? step : s))
                  : [...prev, step];
              return next.sort(
                (a, b) => getStepRank(a.stepName) - getStepRank(b.stepName)
              );
            });
          },
          onDone: (result) => {
            if (streamId !== currentStreamIdRef.current) return;
            doneReceived = true;
            console.log("[ProblemWorkspace onDone]", result);
            setDiagnosis(result);
            diagnosisCodeSnapshotRef.current = codeSnapshot;
            setIsAnalyzing(false);
            setActiveTab("diagnosis");

            const diagnosisWithSnapshot: ProblemDraft["lastDiagnosis"] = {
              ...result,
              codeSnapshot,
            };
            saveDraft(DEMO_USER_ID, problemId, {
              code: codeSnapshot,
              language: "java",
              lastResult: resultWithSnapshot,
              lastDiagnosis: diagnosisWithSnapshot,
            });
            setDraftSavedAt(new Date().toISOString());
          },
          onError: (message) => {
            if (streamId !== currentStreamIdRef.current) return;
            console.error("[ProblemWorkspace onError]", message);
            setError("AI 诊断失败，请稍后重试");
            setIsAnalyzing(false);
          },
          onEnd: () => {
            if (streamId !== currentStreamIdRef.current) return;
            if (!doneReceived) {
              console.warn("[SSE ended without done event]");
            }
            // onEnd 只能关闭 loading，不能清空诊断内容
            setIsAnalyzing(false);
          },
        }
      );
    },
    [problemId]
  );

  const handleSubmit = useCallback(async () => {
    streamControllerRef.current?.abort();
    currentStreamIdRef.current += 1;

    setIsSubmitting(true);
    setIsAnalyzing(false);
    setError(null);
    setSubmissionResult(null);
    setDiagnosis(null);
    setAgentSteps([]);
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
        startAiAnalysis(resultWithSnapshot, code);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "提交失败，请稍后重试");
    } finally {
      setIsSubmitting(false);
    }
  }, [problemId, code, startAiAnalysis]);

  const isCurrentCodeAccepted =
    submissionResult?.status === "ACCEPTED" &&
    submissionResult.codeSnapshot === code;

  const isDiagnosisStale = Boolean(
    diagnosis && diagnosisCodeSnapshotRef.current !== code
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
          agentSteps={agentSteps}
          isDiagnosisStale={isDiagnosisStale}
          isCurrentCodeAccepted={isCurrentCodeAccepted}
          activeTab={activeTab}
          onTabChange={setActiveTab}
        />
      </section>
    </>
  );
}
