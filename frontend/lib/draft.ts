import type { AgentAnalyzeVO, SubmissionResult } from "./types";

export interface ProblemDraft {
  userId: number;
  problemId: number;
  code: string;
  language: "java";
  updatedAt: string;
  lastResult?: SubmissionResult & {
    codeSnapshot: string;
    submittedAt: string;
  };
  lastDiagnosis?: AgentAnalyzeVO & {
    codeSnapshot: string;
  };
}

const DRAFT_KEY_PREFIX = "interview_coach_draft";

function getDraftKey(userId: number, problemId: number): string {
  return `${DRAFT_KEY_PREFIX}_${userId}_${problemId}`;
}

function getStorage(): Storage | null {
  if (typeof globalThis.localStorage === "undefined") {
    return null;
  }
  return globalThis.localStorage;
}

function isProblemDraft(value: unknown): value is ProblemDraft {
  if (!value || typeof value !== "object") {
    return false;
  }

  const draft = value as Partial<ProblemDraft>;
  return (
    typeof draft.userId === "number" &&
    typeof draft.problemId === "number" &&
    typeof draft.code === "string" &&
    draft.language === "java" &&
    typeof draft.updatedAt === "string"
  );
}

export function loadDraft(
  userId: number,
  problemId: number
): ProblemDraft | null {
  try {
    const storage = getStorage();
    const raw = storage?.getItem(getDraftKey(userId, problemId));
    if (!raw) {
      return null;
    }

    const parsed = JSON.parse(raw);
    if (!isProblemDraft(parsed)) {
      return null;
    }

    if (parsed.userId !== userId || parsed.problemId !== problemId) {
      return null;
    }

    return parsed;
  } catch {
    return null;
  }
}

export function saveDraft(
  userId: number,
  problemId: number,
  draft: Partial<ProblemDraft>
): void {
  try {
    const storage = getStorage();
    if (!storage) {
      return;
    }

    const existing = loadDraft(userId, problemId);
    const hasLastResult = Object.prototype.hasOwnProperty.call(
      draft,
      "lastResult"
    );
    const hasLastDiagnosis = Object.prototype.hasOwnProperty.call(
      draft,
      "lastDiagnosis"
    );

    const next: ProblemDraft = {
      userId,
      problemId,
      code: draft.code ?? existing?.code ?? "",
      language: "java",
      updatedAt: new Date().toISOString(),
      lastResult: hasLastResult ? draft.lastResult : existing?.lastResult,
      lastDiagnosis: hasLastDiagnosis
        ? draft.lastDiagnosis
        : existing?.lastDiagnosis,
    };

    storage.setItem(getDraftKey(userId, problemId), JSON.stringify(next));
  } catch {
    // Draft persistence must never block the coding or submission flow.
  }
}

export function clearDraft(userId: number, problemId: number): void {
  try {
    getStorage()?.removeItem(getDraftKey(userId, problemId));
  } catch {
    // Ignore storage failures so reset remains a UI-only fallback.
  }
}

export function formatDraftTime(isoString: string): string {
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) {
    return "";
  }

  const pad = (value: number) => String(value).padStart(2, "0");
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(
    date.getHours()
  )}:${pad(date.getMinutes())}`;
}
