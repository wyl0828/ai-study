"use client";

import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { Activity, Bot, CheckCircle2, ClipboardList, Loader2, RotateCw, StopCircle } from "lucide-react";
import { formatApiError, mockInterviewApi } from "@/lib/api";
import type { MockInterviewSession, MockInterviewTurn } from "@/lib/types";
import InterviewConversation from "./InterviewConversation";
import InterviewReport from "./InterviewReport";
import InterviewSetupPanel from "./InterviewSetupPanel";

const DEMO_USER_ID = 1;

const categoryLabels: Record<string, string> = {
  JAVA: "Java 基础面试",
  JVM: "JVM 面试",
  SPRING: "Spring 面试",
  MYSQL: "MySQL 面试",
  REDIS: "Redis 面试",
  AI: "系统设计面试",
  PROJECT: "系统设计面试",
};

type DisplayState = "WAITING_FIRST" | "EVALUATING" | "FOLLOW_UP" | "REPORTED";

function deriveDisplayState(session: MockInterviewSession | null, pendingAnswer: string): DisplayState {
  if (!session) return "WAITING_FIRST";
  if (session.status === "REPORTED") return "REPORTED";
  if (pendingAnswer) return "EVALUATING";
  if (session.status === "ASKING_FOLLOW_UP") return "FOLLOW_UP";
  return "WAITING_FIRST";
}

const statusLabels: Record<MockInterviewSession["status"], string> = {
  CREATED: "准备开始",
  ASKING_MAIN: "等待你的回答",
  MAIN_ANSWERED: "正在评估回答",
  ASKING_FOLLOW_UP: "AI 面试官追问中",
  FOLLOW_UP_ANSWERED: "AI 面试官出题中",
  NEXT_QUESTION: "AI 面试官出题中",
  FINISHED: "面试已结束",
  REPORTED: "报告已生成",
};

function formatElapsed(startedAt: string | null, tick: number) {
  if (!startedAt) return "--:--";
  const start = new Date(startedAt).getTime();
  if (Number.isNaN(start)) return "--:--";
  const totalSeconds = Math.max(0, Math.floor((tick - start) / 1000));
  const minutes = Math.floor(totalSeconds / 60).toString().padStart(2, "0");
  const seconds = (totalSeconds % 60).toString().padStart(2, "0");
  return `${minutes}:${seconds}`;
}

function sessionTitle(session: MockInterviewSession | null) {
  if (!session) return "系统设计面试";
  return categoryLabels[session.category] || `${session.category} 面试`;
}

function statusText(session: MockInterviewSession | null, loading: boolean, pendingAnswer: string) {
  if (!session) return "准备开始";
  const ds = deriveDisplayState(session, pendingAnswer);
  switch (ds) {
    case "EVALUATING": return "AI 正在分析...";
    case "FOLLOW_UP": return "AI 面试官追问中";
    case "REPORTED": return "报告已生成";
    default: return loading ? "AI 面试官出题中" : "等待你的回答";
  }
}

function progressText(session: MockInterviewSession | null, questionCount: number) {
  const total = Math.max(1, session?.questionCount || questionCount);
  return `${session?.answeredMainCount || 0}/${total}`;
}

function displayMissingPoints(turn: MockInterviewTurn) {
  if (turn.missingKeyPoints.length > 0) {
    return turn.missingKeyPoints;
  }
  return ["这一轮暂未记录明确缺失点，可以继续补充工程边界和回答顺序。"];
}

function expressionAdvice(turn: MockInterviewTurn) {
  return (
    turn.expressionFeedback ||
    turn.followUpReason ||
    turn.interviewerObservation ||
    "下一轮可以先用 2-3 句话说主线，再补一个你最确定的细节。"
  );
}

function currentMainIssue(turns: MockInterviewTurn[]) {
  const latest = turns[turns.length - 1];
  if (!latest) return "回答后会在这里给出本轮最需要修正的问题。";
  // Prefer feedback (AI-generated summary) over gapSummary (which duplicates missing points)
  const hitCount = latest.hitKeyPoints?.length || 0;
  const missingCount = latest.missingKeyPoints?.length || 0;
  if (hitCount > 0 && missingCount > 0) {
    return `答对了 ${hitCount} 个要点，还缺少 ${missingCount} 个关键点需要补充。`;
  }
  if (hitCount > 0) {
    return "核心内容覆盖较完整，可以继续补充边界和工程细节。";
  }
  return (
    latest.feedback ||
    latest.followUpReason ||
    latest.interviewerObservation ||
    "这轮回答还需要补充主线、关键阶段和具体机制，让表达更接近正式面试。"
  );
}

function ErrorBanner({ message }: { message: string | null }) {
  if (!message) return null;

  return (
    <div className="mb-6 rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
      {message}
    </div>
  );
}

function PageHeader() {
  return (
    <section className="mb-6">
      <h1 className="text-2xl font-bold tracking-tight text-on-surface">模拟面试</h1>
      <p className="mt-1 text-sm text-on-surface-variant">
        一问一答追问式训练，模拟 Java 后端真实面试
      </p>
    </section>
  );
}

interface StatusCardProps {
  session: MockInterviewSession;
  questionCount: number;
  loading: boolean;
  pendingAnswer: string;
  elapsed: string;
}

function StatusCard({ session, questionCount, loading, pendingAnswer, elapsed }: StatusCardProps) {
  const [current, total] = progressText(session, questionCount).split("/");

  return (
    <section className="rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-6">
        <div className="flex flex-wrap items-center gap-7">
          <div>
            <p className="text-xs text-on-surface-variant">当前场次</p>
            <p className="mt-1 text-xl font-bold text-on-surface">{sessionTitle(session)}</p>
          </div>
          <div className="hidden h-10 w-px bg-outline-variant md:block" />
          <div>
            <p className="text-xs text-on-surface-variant">进度</p>
            <p className="mt-1 text-xl font-bold text-on-surface">
              {current}
              <span className="ml-1 text-sm font-medium text-on-surface-variant">/{total}</span>
            </p>
          </div>
          <div className="hidden h-10 w-px bg-outline-variant md:block" />
          <div>
            <p className="text-xs text-on-surface-variant">用时</p>
            <p className="mt-1 font-mono text-xl font-bold text-on-surface">{elapsed}</p>
          </div>
        </div>
        <div className="inline-flex items-center gap-2 rounded-full border border-outline-variant bg-surface-container-low px-4 py-2 text-sm font-bold text-primary">
          <Bot className="h-5 w-5" />
          当前状态：{statusText(session, loading, pendingAnswer)}
        </div>
      </div>
    </section>
  );
}

function InterviewPreviewCard() {
  const flow = [
    "AI 面试官先提出主问题",
    "你用自己的语言回答",
    "系统根据回答继续追问",
    "结束后生成面试报告",
  ];
  const dimensions = ["表达清晰", "边界意识", "核心方案", "工程实践"];

  return (
    <section className="rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-6 shadow-sm">
      <div className="flex items-center gap-2">
        <Activity className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-bold text-on-surface">本场面试预览</h2>
      </div>

      <p className="mt-4 text-sm leading-6 text-on-surface-variant">
        开始后，AI 面试官会围绕你选择的方向提出主问题，并根据你的回答继续追问。
      </p>
      <p className="mt-3 text-sm leading-6 text-on-surface-variant">
        训练重点不是背答案，而是看你能不能把概念说清楚、流程讲完整、边界补到位、实践细节说到位。
      </p>

      <div className="mt-6 space-y-3">
        {flow.map((item, index) => (
          <div key={item} className="flex items-center gap-3 text-sm text-on-surface">
            <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
              {index + 1}
            </span>
            {item}
          </div>
        ))}
      </div>

      <div className="mt-6 rounded-xl bg-surface-container-low p-4">
        <p className="text-sm font-bold text-on-surface">评分维度</p>
        <div className="mt-3 grid grid-cols-2 gap-3">
          {dimensions.map((item) => (
            <div key={item} className="flex items-center gap-2 text-sm text-on-surface-variant">
              <CheckCircle2 className="h-4 w-4 text-primary" />
              {item}
            </div>
          ))}
        </div>
      </div>

      <p className="mt-5 text-sm leading-6 text-on-surface-variant">
        实时反馈会在面试开始后显示。
      </p>
    </section>
  );
}

function InterviewBoundaryCard() {
  return (
    <section className="mt-6 rounded-xl border border-primary/15 bg-primary/5 px-5 py-4 text-sm leading-6 text-on-surface-variant">
      <div className="mb-1 flex items-center gap-2 font-semibold text-primary">
        <ClipboardList className="h-4 w-4" />
        训练边界
      </div>
      本模式重点训练回答思路、追问应对、表达完整度和复盘能力，不会要求你直接写完整 Java AC 代码。
    </section>
  );
}

interface RealtimeFeedbackProps {
  session: MockInterviewSession;
  pendingAnswer: string;
  loading: boolean;
  canFinish: boolean;
  onFinish: () => void;
}

function FinishReportButton({ loading, disabled, onFinish }: {
  loading: boolean;
  disabled?: boolean;
  onFinish: () => void;
}) {
  return (
    <button
      className="flex w-full items-center justify-center gap-2 rounded-xl border border-primary/25 bg-primary/5 px-4 py-3 text-sm font-bold text-primary transition-colors hover:bg-primary hover:text-on-primary disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-primary/5 disabled:hover:text-primary"
      disabled={disabled || loading}
      onClick={onFinish}
      type="button"
    >
      {loading ? <RotateCw className="h-5 w-5 animate-spin" /> : <StopCircle className="h-5 w-5" />}
      提前结束并生成报告
    </button>
  );
}

function FeedbackEmptyState({ session, loading, displayState, onFinish }: RealtimeFeedbackProps & { displayState: DisplayState }) {
  const tags = ["表达清晰", "边界意识", "核心方案", "工程实践"];

  if (displayState === "EVALUATING") {
    return (
      <section className="flex h-full flex-col items-center justify-center rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-5 shadow-sm">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
        <p className="mt-4 text-sm font-semibold text-on-surface">正在分析回答...</p>
        <p className="mt-1 text-xs text-on-surface-variant">分析完成后会显示反馈</p>
      </section>
    );
  }

  return (
    <section className="flex h-full flex-col rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-5 shadow-sm">
      <div className="flex items-center gap-2">
        <Activity className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-bold text-on-surface">实时反馈</h2>
      </div>
      <p className="mt-4 text-sm leading-6 text-on-surface-variant">
        开始面试后，这里会根据你的回答实时评估：表达是否清晰、思路是否完整、边界是否覆盖、方案是否有取舍。
      </p>
      <div className="mt-5 grid grid-cols-2 gap-3">
        {tags.map((tag) => (
          <span
            key={tag}
            className="rounded-full border border-outline-variant/70 bg-surface-container-low px-3 py-2 text-center text-xs font-medium text-on-surface-variant"
          >
            {tag}
          </span>
        ))}
      </div>
      <div className="mt-6 rounded-xl bg-surface-container-low p-4 text-sm text-on-surface-variant">
        <div className="mb-1 font-semibold text-on-surface">本轮建议</div>
        <p>回答后会在这里给出最需要补充的一点。</p>
      </div>
      <div className="mt-auto pt-4">
        <p className="mb-4 text-center text-sm font-medium text-on-surface-variant">等待你的第一轮回答</p>
        <FinishReportButton
          loading={loading}
          disabled
          onFinish={onFinish}
        />
      </div>
    </section>
  );
}

function RealtimeFeedback({ session, pendingAnswer, loading, canFinish, onFinish }: RealtimeFeedbackProps) {
  const displayState = deriveDisplayState(session, pendingAnswer);

  if (displayState === "WAITING_FIRST" || displayState === "EVALUATING") {
    return (
      <FeedbackEmptyState
        session={session}
        pendingAnswer={pendingAnswer}
        loading={loading}
        canFinish={canFinish}
        onFinish={onFinish}
        displayState={displayState}
      />
    );
  }

  const latest = session.turns[session.turns.length - 1];
  const missingPoints = displayMissingPoints(latest);
  const visibleMissingPoints = missingPoints.slice(0, 3);
  const hiddenMissingCount = Math.max(0, missingPoints.length - visibleMissingPoints.length);
  const advice = expressionAdvice(latest);
  const score = latest.score;
  const mainIssue = currentMainIssue(session.turns);

  return (
    <section className="flex h-full flex-col overflow-hidden rounded-xl border border-outline-variant/70 bg-surface-container-lowest shadow-sm">
      <div className="flex shrink-0 items-center gap-2 border-b border-outline-variant/70 p-4">
        <Activity className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-bold text-on-surface">实时反馈</h2>
        <div className="ml-auto rounded-lg bg-primary px-3 py-1.5 text-xs text-on-primary">
          最近一轮表现：
          <span className="mx-1 text-lg font-bold">{score ?? "--"}</span>
          <span className="text-xs opacity-75">/100</span>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 p-4">
        <div className="rounded-xl bg-surface-container-low p-4 text-sm text-on-surface-variant">
          <div className="mb-2 font-semibold text-on-surface">本轮主要问题</div>
          <p className="leading-6">{mainIssue}</p>
        </div>

        <div className="rounded-xl border border-outline-variant/60 bg-surface-container-lowest p-4">
          <div className="mb-2 text-sm font-bold text-on-surface">缺失要点</div>
          <ul className="space-y-2 text-sm leading-6 text-on-surface-variant">
            {visibleMissingPoints.map((point) => (
              <li key={point} className="flex gap-2">
                <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                <span>{point}</span>
              </li>
            ))}
            {hiddenMissingCount > 0 && (
              <li className="pl-3 text-xs font-medium text-on-surface-variant">
                还有 {hiddenMissingCount} 个要点待补齐
              </li>
            )}
          </ul>
        </div>

        <div className="rounded-xl border border-primary/15 bg-primary/5 p-4 text-sm text-on-surface-variant">
          <div className="mb-2 font-semibold text-on-surface">下一轮怎么补答</div>
          <p className="leading-6">{advice}</p>
        </div>
      </div>

      <div className="shrink-0 border-t border-outline-variant/70 p-4">
        <FinishReportButton
          loading={loading}
          disabled={session.status === "REPORTED"}
          onFinish={onFinish}
        />
      </div>
    </section>
  );
}

interface StartViewProps {
  category: string;
  questionCount: number;
  interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN";
  loading: boolean;
  error: string | null;
  onCategoryChange: (category: string) => void;
  onQuestionCountChange: (questionCount: number) => void;
  onInterviewerStyleChange: (interviewerStyle: "GUIDED" | "BIG_TECH" | "FAST_SCREEN") => void;
  onStart: () => void;
}

function MockInterviewStartView({
  category,
  questionCount,
  interviewerStyle,
  loading,
  error,
  onCategoryChange,
  onQuestionCountChange,
  onInterviewerStyleChange,
  onStart,
}: StartViewProps) {
  return (
    <main className="min-h-screen bg-surface">
      <div className="mx-auto w-full max-w-[1280px] px-4 py-8 sm:px-6 lg:px-8">
        <PageHeader />
        <ErrorBanner message={error} />

        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_420px]">
          <InterviewSetupPanel
            category={category}
            questionCount={questionCount}
            interviewerStyle={interviewerStyle}
            loading={loading}
            onCategoryChange={onCategoryChange}
            onQuestionCountChange={onQuestionCountChange}
            onInterviewerStyleChange={onInterviewerStyleChange}
            onStart={onStart}
          />
          <InterviewPreviewCard />
        </div>

        <InterviewBoundaryCard />
      </div>
    </main>
  );
}

interface WorkspaceProps {
  session: MockInterviewSession;
  questionCount: number;
  answer: string;
  pendingAnswer: string;
  loading: boolean;
  error: string | null;
  elapsed: string;
  canFinish: boolean;
  onAnswerChange: (answer: string) => void;
  onSubmitAnswer: () => void;
  onFinish: () => void;
}

function MockInterviewWorkspace({
  session,
  questionCount,
  answer,
  pendingAnswer,
  loading,
  error,
  elapsed,
  canFinish,
  onAnswerChange,
  onSubmitAnswer,
  onFinish,
}: WorkspaceProps) {
  return (
    <main className="flex h-screen flex-col bg-surface">
      <div className="mx-auto w-full max-w-[1500px] shrink-0 px-6 pt-6 xl:px-10">
        <PageHeader />
        <ErrorBanner message={error} />
        <StatusCard
          session={session}
          questionCount={questionCount}
          loading={loading}
          pendingAnswer={pendingAnswer}
          elapsed={elapsed}
        />
      </div>

      <div className="mx-auto grid min-h-0 w-full max-w-[1500px] flex-1 grid-cols-[minmax(0,1fr)_380px] gap-5 px-6 pb-4 xl:px-10">
        <div className="min-h-0 min-w-0">
          <InterviewConversation
            session={session}
            answer={answer}
            pendingAnswer={pendingAnswer}
            loading={loading}
            onAnswerChange={onAnswerChange}
            submitAnswer={onSubmitAnswer}
          />
        </div>

        <aside className="grid min-h-0 min-w-0 grid-rows-[auto_minmax(0,1fr)_auto] overflow-hidden">
          {session.report ? (
            <InterviewReport report={session.report} turns={session.turns} />
          ) : (
            <RealtimeFeedback
              session={session}
              pendingAnswer={pendingAnswer}
              loading={loading}
              canFinish={canFinish}
              onFinish={onFinish}
            />
          )}
        </aside>
      </div>
    </main>
  );
}

export default function MockInterviewPage() {
  const searchParams = useSearchParams();
  const categoryParam = searchParams.get("category");
  const [category, setCategory] = useState(() => categoryParam || "SPRING");
  const [interviewerStyle, setInterviewerStyle] = useState<"GUIDED" | "BIG_TECH" | "FAST_SCREEN">("BIG_TECH");
  const [questionCount, setQuestionCount] = useState(3);
  const [session, setSession] = useState<MockInterviewSession | null>(null);
  const [answer, setAnswer] = useState("");
  const [pendingAnswer, setPendingAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());
  const restoredSessionId = searchParams.get("sessionId");

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (categoryParam && categoryParam !== category && !session) {
      setCategory(categoryParam);
    }
  }, [categoryParam, category, session]);

  useEffect(() => {
    if (!restoredSessionId || session?.sessionId === Number(restoredSessionId)) {
      return;
    }
    const sessionId = Number(restoredSessionId);
    if (!Number.isFinite(sessionId) || sessionId <= 0) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(null);
    mockInterviewApi.get(sessionId)
      .then((response) => {
        if (!cancelled) {
          setSession(response.data);
          setAnswer("");
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(formatApiError(err, "mockInterview"));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [restoredSessionId, session?.sessionId]);

  const elapsed = useMemo(() => formatElapsed(session?.startedAt || null, now), [now, session?.startedAt]);
  const canFinish = session?.status === "FINISHED" || session?.status === "REPORTED";

  const start = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await mockInterviewApi.create({
        userId: DEMO_USER_ID,
        category,
        questionCount,
        interviewerStyle,
      });
      setSession(response.data);
      window.history.replaceState(null, "", `/mock-interview?sessionId=${response.data.sessionId}`);
      setAnswer("");
    } catch (err) {
      setError(formatApiError(err, "mockInterview"));
    } finally {
      setLoading(false);
    }
  };

  const restore = async () => {
    if (!session) return;
    setLoading(true);
    setError(null);
    try {
      const response = await mockInterviewApi.get(session.sessionId);
      setSession(response.data);
    } catch (err) {
      setError(formatApiError(err, "mockInterview"));
    } finally {
      setLoading(false);
    }
  };

  const submitAnswer = async () => {
    if (!session || !answer.trim()) return;
    const submitted = answer.trim();
    setPendingAnswer(submitted);
    setAnswer("");
    setLoading(true);
    setError(null);
    try {
      const response = await mockInterviewApi.answer(session.sessionId, {
        userAnswer: submitted,
      });
      setSession(response.data);
      setPendingAnswer("");
    } catch (err) {
      setAnswer(submitted);
      setPendingAnswer("");
      setError(`刚才这段回答没有提交成功，可以直接重试。${formatApiError(err, "mockInterview")}`);
    } finally {
      setLoading(false);
    }
  };

  const finish = async () => {
    if (!session || session.status === "REPORTED") return;
    setLoading(true);
    setError(null);
    try {
      const response = await mockInterviewApi.finish(session.sessionId);
      setSession(response.data);
    } catch (err) {
      setError(formatApiError(err, "mockInterview"));
    } finally {
      setLoading(false);
    }
  };

  if (!session) {
    return (
      <MockInterviewStartView
        category={category}
        questionCount={questionCount}
        interviewerStyle={interviewerStyle}
        loading={loading}
        error={error}
        onCategoryChange={setCategory}
        onQuestionCountChange={(value) => setQuestionCount(Math.max(1, Math.min(value || 1, 5)))}
        onInterviewerStyleChange={setInterviewerStyle}
        onStart={start}
      />
    );
  }

  return (
    <MockInterviewWorkspace
      session={session}
      questionCount={questionCount}
      answer={answer}
      pendingAnswer={pendingAnswer}
      loading={loading}
      error={error}
      elapsed={elapsed}
      canFinish={Boolean(canFinish)}
      onAnswerChange={setAnswer}
      onSubmitAnswer={submitAnswer}
      onFinish={finish}
    />
  );
}
