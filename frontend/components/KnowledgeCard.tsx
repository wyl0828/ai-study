"use client";

import { useEffect, useState } from "react";
import {
  BookOpenText,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  HelpCircle,
  ListChecks,
  MessagesSquare,
} from "lucide-react";
import type { KnowledgeTopic, SelfTestFeedback } from "@/lib/knowledgeData";
import { formatApiError, userApi } from "@/lib/api";
import type { SelfTestRecord } from "@/lib/types";
import KnowledgeFeedback from "./KnowledgeFeedback";
import KnowledgeSelfTest from "./KnowledgeSelfTest";

interface KnowledgeCardProps {
  topic: KnowledgeTopic;
  expanded: boolean;
  mastered: boolean;
  userId: number;
  onToggle: () => void;
  onMarkMastered: () => void;
  onRecentScoreChange: (id: number, score: number | null) => void;
}

const difficultyStyle: Record<KnowledgeTopic["difficulty"], string> = {
  简单: "bg-emerald-50 text-emerald-700 border-emerald-100",
  中等: "bg-amber-50 text-amber-700 border-amber-100",
  困难: "bg-red-50 text-red-700 border-red-100",
};

const categoryStyle: Record<KnowledgeTopic["category"], string> = {
  Java: "bg-blue-50 text-blue-700 border-blue-100",
  MySQL: "bg-violet-50 text-violet-700 border-violet-100",
  Redis: "bg-rose-50 text-rose-700 border-rose-100",
  Spring: "bg-emerald-50 text-emerald-700 border-emerald-100",
  JVM: "bg-slate-50 text-slate-700 border-slate-200",
  AI: "bg-cyan-50 text-cyan-700 border-cyan-100",
};

export default function KnowledgeCard({
  topic,
  expanded,
  mastered,
  userId,
  onToggle,
  onMarkMastered,
  onRecentScoreChange,
}: KnowledgeCardProps) {
  const [feedback, setFeedback] = useState<SelfTestFeedback | null>(null);
  const [showAnalysis, setShowAnalysis] = useState(false);
  const [recentRecords, setRecentRecords] = useState<SelfTestRecord[]>([]);
  const [latestScore, setLatestScore] = useState<number | null>(null);
  const [saveNotice, setSaveNotice] = useState("");

  useEffect(() => {
    let cancelled = false;

    if (!expanded) {
      return () => {
        cancelled = true;
      };
    }

    userApi
      .recentSelfTests(userId, topic.id)
      .then((response) => {
        if (!cancelled) {
          setRecentRecords(response.data);
          const score = response.data[0]?.score ?? null;
          setLatestScore(score);
          onRecentScoreChange(topic.id, score);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRecentRecords([]);
          setLatestScore(null);
          onRecentScoreChange(topic.id, null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [expanded, onRecentScoreChange, topic.id, userId]);

  const handleFeedback = async (nextFeedback: SelfTestFeedback, answer: string) => {
    setFeedback(nextFeedback);
    setShowAnalysis(true);
    setLatestScore(nextFeedback.score);
    onRecentScoreChange(topic.id, nextFeedback.score);
    setSaveNotice("正在保存自测记录...");
    try {
      const response = await userApi.submitSelfTest(userId, topic.id, {
        userAnswer: answer,
        score: nextFeedback.score,
        feedback: nextFeedback.comment,
        missingKeyPoints: nextFeedback.missingKeyPoints,
      });
      setRecentRecords((current) => [response.data, ...current].slice(0, 5));
      setLatestScore(response.data.score);
      onRecentScoreChange(topic.id, response.data.score);
      setSaveNotice("自测记录已保存");
    } catch (err) {
      setSaveNotice(formatApiError(err, "knowledge"));
    }
  };

  const skipSelfTest = () => {
    setFeedback(null);
    setShowAnalysis(true);
  };

  const recentScore = latestScore;
  const trainingStatus = mastered
    ? "已掌握"
    : typeof recentScore === "number" && recentScore < 80
    ? "需复习"
    : "未练";
  const actionLabel = expanded
    ? "收起"
    : mastered
    ? "重新练习"
    : showAnalysis
    ? "查看解析"
    : recentScore === null
    ? "开始自测"
    : "继续作答";

  return (
    <article
      className={`coach-card coach-card-hover overflow-hidden transition-all ${
        expanded
          ? "border-primary/25 border-l-4 border-l-primary shadow-md ring-1 ring-primary/10"
          : ""
      }`}
    >
      <div className="p-5">
          <div className="mb-3 flex flex-wrap items-start justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <span className="coach-pill border-primary/20 bg-primary/5 text-primary">
                <BookOpenText className="h-3.5 w-3.5" />
                面试题卡
              </span>
              <span
                className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${
                  difficultyStyle[topic.difficulty]
                }`}
              >
                {topic.difficulty}
              </span>
              <span
                className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${
                  categoryStyle[topic.category]
                }`}
              >
                {topic.category}
              </span>
              <span className="inline-flex items-center gap-1 rounded-full border border-outline-variant/30 bg-surface-container px-2.5 py-1 text-xs font-semibold text-on-surface-variant">
                状态：{trainingStatus}
              </span>
            </div>
            <button
              type="button"
              onClick={onToggle}
              className="rounded-full p-1 text-outline transition hover:bg-surface-container hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/20"
              aria-label={expanded ? "收起知识卡" : "展开知识卡"}
              aria-expanded={expanded}
            >
              {expanded ? (
                <ChevronUp className="h-5 w-5 shrink-0" />
              ) : (
                <ChevronDown className="h-5 w-5 shrink-0" />
              )}
            </button>
          </div>

          <button
            type="button"
            onClick={onToggle}
            className="block w-full text-left outline-none focus-visible:ring-2 focus-visible:ring-primary/20"
            aria-expanded={expanded}
          >
            <h2 className="mb-2 text-base font-bold leading-snug text-on-surface">
              {topic.title}
            </h2>
            <p className="mb-4 text-sm leading-relaxed text-on-surface-variant">
              {topic.question}
            </p>
          </button>

          <div className="mb-2 text-xs font-semibold text-on-surface-variant">
            核心标签
          </div>
          <div className="mb-4 flex flex-wrap gap-1.5">
            {topic.tags.map((tag) => (
              <span
                key={tag}
                className="rounded-full border border-outline-variant/30 bg-surface-container px-2 py-0.5 text-[11px] text-on-surface-variant"
              >
                {tag}
              </span>
            ))}
          </div>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-outline-variant/20 pt-3">
            <div className="flex flex-wrap items-center gap-3 text-xs text-on-surface-variant">
              <span>最近得分：{recentScore === null ? "未自测" : recentScore}</span>
              <span className="inline-flex items-center gap-1.5 font-semibold text-primary">
                <BookOpenText className="h-3.5 w-3.5" />
                训练任务
              </span>
            </div>
            <button
              type="button"
              onClick={onToggle}
              className="coach-primary-button px-3 py-2 text-xs"
              aria-expanded={expanded}
            >
              {actionLabel}
            </button>
          </div>
        </div>

      {expanded && (
        <div className="border-t border-primary/10 px-5 pb-5 pt-4">
          <div className="space-y-5">
            <KnowledgeSelfTest
              topic={topic}
              onFeedback={handleFeedback}
              onSkip={skipSelfTest}
            />

            {showAnalysis && (
              <div className="space-y-4">
                {feedback && <KnowledgeFeedback feedback={feedback} />}
                {saveNotice && (
                  <div className="rounded-lg border border-outline-variant/30 bg-surface-container-low px-3 py-2 text-xs text-on-surface-variant">
                    {saveNotice}
                  </div>
                )}
                {recentRecords.length > 0 && (
                  <section className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest p-4">
                    <div className="mb-2 text-xs font-semibold text-on-surface">
                      最近自测记录
                    </div>
                    <div className="space-y-2">
                      {recentRecords.slice(0, 3).map((record) => (
                        <div
                          key={record.id}
                          className="flex items-center justify-between gap-3 text-xs text-on-surface-variant"
                        >
                          <span>得分 {record.score}</span>
                          <span className="truncate">{record.feedback}</span>
                        </div>
                      ))}
                    </div>
                  </section>
                )}

                <section className="rounded-lg border border-outline-variant/30 bg-surface-container-lowest p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <ListChecks className="h-4 w-4 text-primary" />
                    <h3 className="text-sm font-semibold text-on-surface">
                      标杆回答解析
                    </h3>
                  </div>
                  <p className="mb-4 whitespace-pre-line text-sm leading-relaxed text-on-surface-variant">
                    {topic.referenceAnswer}
                  </p>
                  <div className="mb-2 text-xs font-semibold text-on-surface">
                    核心记忆要点
                  </div>
                  <div className="space-y-2">
                    {topic.keyPoints.length > 0 ? (
                      topic.keyPoints.map((point) => (
                        <div key={point} className="flex gap-2 text-sm text-on-surface-variant">
                          <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-primary" />
                          <span>{point}</span>
                        </div>
                      ))
                    ) : (
                      <p className="text-sm text-on-surface-variant">
                        暂无核心记忆点，建议先围绕题目整理定义、机制、场景和风险。
                      </p>
                    )}
                  </div>
                  {topic.sourceName && (
                    <div className="mt-4 border-t border-outline-variant/20 pt-3 text-xs text-on-surface-variant">
                      选题参考：
                      {topic.sourceUrl ? (
                        <a
                          href={topic.sourceUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="font-medium text-primary hover:underline"
                        >
                          {topic.sourceName}
                        </a>
                      ) : (
                        <span>{topic.sourceName}</span>
                      )}
                    </div>
                  )}
                </section>

                <section className="rounded-lg border border-outline-variant/30 bg-surface-container-low p-4">
                  <div className="mb-3 flex items-center gap-2">
                    <MessagesSquare className="h-4 w-4 text-primary" />
                    <h3 className="text-sm font-semibold text-on-surface">
                      面试官高频追问
                    </h3>
                  </div>
                  <ul className="space-y-2">
                    {topic.followUpQuestions.length > 0 ? (
                      topic.followUpQuestions.map((question) => (
                        <li key={question} className="flex gap-2 text-sm text-on-surface-variant">
                          <HelpCircle className="mt-0.5 h-4 w-4 shrink-0 text-primary/70" />
                          <span>{question}</span>
                        </li>
                      ))
                    ) : (
                      <li className="text-sm text-on-surface-variant">
                        暂无追问，建议从机制、边界和项目落地三个方向继续追问自己。
                      </li>
                    )}
                  </ul>
                </section>

                <div className="flex flex-wrap justify-end gap-3 pt-1">
                  <button
                    type="button"
                    onClick={onMarkMastered}
                    disabled={mastered}
                    className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-2 text-xs font-semibold transition ${
                      mastered
                        ? "cursor-default border-emerald-100 bg-emerald-50 text-emerald-700"
                        : "border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
                    }`}
                  >
                    <CheckCircle2 className="h-3.5 w-3.5" />
                    {mastered ? "已掌握" : "标记已掌握"}
                  </button>
                  <button
                    type="button"
                    onClick={onToggle}
                    className="rounded-lg border border-outline-variant/40 bg-surface-container-lowest px-3 py-2 text-xs font-semibold text-on-surface-variant transition hover:bg-surface-container"
                  >
                    收起解析
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </article>
  );
}
