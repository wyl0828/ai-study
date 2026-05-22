"use client";

import { useEffect, useMemo, useState } from "react";
import { Activity, Bot, CheckCircle2, ClipboardList, RotateCw, StopCircle } from "lucide-react";
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
  if (loading && pendingAnswer) return "正在评估回答";
  if (loading) return "AI 面试官出题中";
  return statusLabels[session.status] || "进行中";
}

function progressText(session: MockInterviewSession | null, questionCount: number) {
  const total = Math.max(1, session?.questionCount || questionCount);
  return `${session?.answeredMainCount || 0}/${total}`;
}

function scoreLabel(score: number) {
  if (score >= 85) return "优秀";
  if (score >= 70) return "良好";
  if (score > 0) return "待提升";
  return "待评估";
}

function averageScore(turns: MockInterviewTurn[]) {
  if (turns.length === 0) return 0;
  return Math.round(turns.reduce((sum, turn) => sum + (turn.score || 0), 0) / turns.length);
}

function containsAny(value: string, words: string[]) {
  return words.some((word) => value.includes(word));
}

function buildFeedbackDimensions(turns: MockInterviewTurn[]) {
  const latest = turns[turns.length - 1];
  const allAnswers = turns.map((turn) => turn.userAnswer).join(" ");
  const score = latest?.score || 0;
  const hasCapacity = containsAny(allAnswers, ["容量", "流量", "QPS", "存储", "估算"]);
  const hasTradeoff = containsAny(allAnswers, ["权衡", "取舍", "优缺点", "一致性", "代价"]);

  return [
    {
      name: "需求澄清",
      label: latest ? scoreLabel(Math.min(95, score + 8)) : "待评估",
      value: latest ? Math.min(92, Math.max(50, score + 8)) : 0,
      note: latest?.strengthSummary || "开始回答后会实时评估你对问题边界的澄清程度。",
    },
    {
      name: "容量估算",
      label: hasCapacity ? "良好" : "待评估",
      value: hasCapacity ? 68 : 12,
      note: hasCapacity ? "已经开始涉及流量、存储或容量估算。" : "尚未进入流量和存储容量的估算环节。",
    },
    {
      name: "核心方案",
      label: latest ? scoreLabel(score) : "待评估",
      value: latest ? Math.max(35, score) : 0,
      note: latest?.gapSummary || "回答主流程后，这里会提示核心方案是否完整。",
    },
    {
      name: "技术权衡",
      label: hasTradeoff ? "良好" : "待评估",
      value: hasTradeoff ? 72 : 0,
      note: hasTradeoff ? "已经开始说明方案收益、代价和边界。" : "尚未涉及方案的优缺点对比及取舍。",
    },
  ];
}

function currentSuggestion(turns: MockInterviewTurn[]) {
  const latest = turns[turns.length - 1];
  if (!latest) return "回答后会在这里给出最需要补充的一点。";
  return (
    latest.gapSummary ||
    latest.followUpReason ||
    latest.interviewerObservation ||
    "你可以继续补充核心机制、边界情况和方案取舍，让回答更接近正式面试表达。"
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
  const dimensions = ["表达清晰", "边界意识", "核心方案", "技术权衡"];

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
        训练重点不是背答案，而是看你能不能把概念说清楚、流程讲完整、边界补到位、方案有取舍。
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
  loading: boolean;
  canFinish: boolean;
  onFinish: () => void;
}

function FinishReportButton({ loading, canFinish, disabled, onFinish }: {
  loading: boolean;
  canFinish: boolean;
  disabled?: boolean;
  onFinish: () => void;
}) {
  return (
    <button
      className="flex w-full items-center justify-center gap-2 rounded-2xl border border-primary/25 bg-primary/5 px-4 py-4 text-sm font-bold text-primary transition-colors hover:bg-primary hover:text-on-primary disabled:cursor-not-allowed disabled:opacity-50 disabled:hover:bg-primary/5 disabled:hover:text-primary"
      disabled={disabled || !canFinish || loading}
      onClick={onFinish}
      type="button"
      title={canFinish ? "生成模拟面试报告" : "完成当前面试后可生成报告"}
    >
      {loading && canFinish ? <RotateCw className="h-5 w-5 animate-spin" /> : <StopCircle className="h-5 w-5" />}
      提前结束并生成报告
    </button>
  );
}

function FeedbackEmptyState({ loading, canFinish, onFinish }: RealtimeFeedbackProps) {
  const tags = ["表达清晰", "边界意识", "核心方案", "技术权衡"];

  return (
    <section className="flex min-h-[500px] flex-col rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-6 shadow-sm">
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
      <div className="mt-auto pt-6">
        <p className="mb-4 text-center text-sm font-medium text-on-surface-variant">等待你的第一轮回答</p>
        <FinishReportButton
          loading={loading}
          canFinish={canFinish}
          disabled
          onFinish={onFinish}
        />
      </div>
    </section>
  );
}

function RealtimeFeedback({ session, loading, canFinish, onFinish }: RealtimeFeedbackProps) {
  if (session.turns.length === 0) {
    return (
      <FeedbackEmptyState
        session={session}
        loading={loading}
        canFinish={canFinish}
        onFinish={onFinish}
      />
    );
  }

  const dimensions = buildFeedbackDimensions(session?.turns || []);
  const score = session?.report?.averageScore
    ? Math.round(session.report.averageScore)
    : averageScore(session?.turns || []);
  const suggestion = currentSuggestion(session?.turns || []);

  return (
    <section className="flex min-h-[500px] flex-col rounded-xl border border-outline-variant/70 bg-surface-container-lowest p-6 shadow-sm">
      <div className="flex items-center gap-2 border-b border-outline-variant/70 pb-4">
        <Activity className="h-5 w-5 text-primary" />
        <h2 className="text-lg font-bold text-on-surface">实时反馈</h2>
        <div className="ml-auto rounded-lg bg-primary px-3 py-2 text-sm text-on-primary">
          本轮表现：
          <span className="mx-1 text-lg font-bold">{score || "--"}</span>
          <span className="text-xs opacity-75">/100</span>
        </div>
      </div>

      <div className="mt-5 rounded-xl bg-surface-container-low p-4 text-sm text-on-surface-variant">
        <div className="mb-1 font-semibold text-on-surface">本轮建议</div>
        <p className="leading-6">{suggestion}</p>
      </div>

      <div className="mt-6 flex-1 space-y-5">
        {dimensions.map((item) => (
          <div key={item.name}>
            <div className="flex items-center justify-between gap-3">
              <p className="text-sm font-bold text-on-surface">{item.name}</p>
              <p className={item.value > 0 ? "text-xs text-primary" : "text-xs text-on-surface-variant"}>
                {item.label}
              </p>
            </div>
            <div className="mt-2 h-2 rounded-full bg-surface-container-high">
              <div
                className={item.value > 0 ? "h-2 rounded-full bg-primary" : "h-2 rounded-full bg-outline-variant"}
                style={{ width: `${Math.max(item.value, item.value > 0 ? 20 : 0)}%` }}
              />
            </div>
            <p className="mt-2 text-xs leading-5 text-on-surface-variant">{item.note}</p>
          </div>
        ))}
      </div>

      <div className="mt-8 rounded-lg border border-outline-variant bg-surface-container-low p-4 text-center text-xs text-on-surface-variant">
        保持冷静，结构化表达能有效提升面试表现。
      </div>

      <div className="mt-6 border-t border-outline-variant/70 pt-4">
        <FinishReportButton
          loading={loading}
          canFinish={canFinish}
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
    <main className="min-h-screen bg-surface">
      <div className="mx-auto w-full max-w-[1440px] px-4 py-6 sm:px-6 lg:px-8">
        <PageHeader />
        <ErrorBanner message={error} />

        <div className="space-y-6">
          <StatusCard
            session={session}
            questionCount={questionCount}
            loading={loading}
            pendingAnswer={pendingAnswer}
            elapsed={elapsed}
          />

          <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_400px] xl:grid-cols-[minmax(0,1fr)_420px]">
            <div className="min-w-0">
              <InterviewConversation
                session={session}
                answer={answer}
                pendingAnswer={pendingAnswer}
                loading={loading}
                onAnswerChange={onAnswerChange}
                submitAnswer={onSubmitAnswer}
              />
            </div>

            <aside className="min-w-0">
              {session.report ? (
                <InterviewReport report={session.report} />
              ) : (
                <RealtimeFeedback
                  session={session}
                  loading={loading}
                  canFinish={canFinish}
                  onFinish={onFinish}
                />
              )}
            </aside>
          </div>
        </div>
      </div>
    </main>
  );
}

export default function MockInterviewPage() {
  const [category, setCategory] = useState("SPRING");
  const [interviewerStyle, setInterviewerStyle] = useState<"GUIDED" | "BIG_TECH" | "FAST_SCREEN">("BIG_TECH");
  const [questionCount, setQuestionCount] = useState(3);
  const [session, setSession] = useState<MockInterviewSession | null>(null);
  const [answer, setAnswer] = useState("");
  const [pendingAnswer, setPendingAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

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
    if (!session || !canFinish || session.status === "REPORTED") return;
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
