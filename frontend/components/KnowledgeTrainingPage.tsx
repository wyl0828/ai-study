"use client";

import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import type { ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import { AlertCircle, BookOpenCheck, Brain, SearchX, Target } from "lucide-react";
import KnowledgeCard from "./KnowledgeCard";
import KnowledgeFilterBar, {
  type KnowledgeDifficultyFilter,
  type KnowledgeStatusFilter,
} from "./KnowledgeFilterBar";
import KnowledgeSidebar from "./KnowledgeSidebar";
import KnowledgeTopicHeader from "./KnowledgeTopicHeader";
import { formatApiError, knowledgeApi } from "@/lib/api";
import {
  defaultKnowledgeSelection,
  inferKnowledgeSelection,
  knowledgeTopics,
  matchKnowledgeTopic,
  toKnowledgeTopic,
  type KnowledgeCategory,
  type KnowledgeSelection as SharedKnowledgeSelection,
  type KnowledgeTopic,
} from "@/lib/knowledgeData";

type KnowledgeSelection = SharedKnowledgeSelection;

type TopicTrainingStatus = "未练" | "已掌握" | "需复习";

const categoryLabels: KnowledgeCategory[] = ["Java", "MySQL", "Redis", "Spring", "JVM", "AI"];

function isKnowledgeCategory(value: string): value is KnowledgeCategory {
  return categoryLabels.includes(value as KnowledgeCategory);
}

function getTrainingStatus(
  topicId: number,
  masteredIds: Set<number>,
  recentScores: Record<number, number | null>
): TopicTrainingStatus {
  if (masteredIds.has(topicId)) return "已掌握";
  const score = recentScores[topicId];
  if (typeof score === "number" && score < 80) return "需复习";
  return "未练";
}

function matchesKeyword(topic: KnowledgeTopic, keyword: string): boolean {
  if (!keyword) return true;

  return [
    topic.title,
    topic.question,
    topic.category,
    topic.difficulty,
    ...topic.tags,
    ...topic.keyPoints,
  ]
    .join(" ")
    .toLowerCase()
    .includes(keyword);
}

export default function KnowledgeTrainingPage({ userId }: { userId: number }) {
  const searchParams = useSearchParams();
  const cardIdParam = searchParams.get("cardId");
  const linkedCardId = Number(cardIdParam);
  const targetCardId = Number.isFinite(linkedCardId) && linkedCardId > 0
    ? linkedCardId
    : null;

  const [selection, setSelection] =
    useState<KnowledgeSelection>(defaultKnowledgeSelection);
  const [search, setSearch] = useState("");
  const [difficulty, setDifficulty] = useState<KnowledgeDifficultyFilter>("全部");
  const [status, setStatus] = useState<KnowledgeStatusFilter>("全部");
  const [expandedId, setExpandedId] = useState<number | null>(targetCardId);
  const [activeCardId, setActiveCardId] = useState<number | null>(targetCardId);
  const [pendingScrollCardId, setPendingScrollCardId] = useState<number | null>(targetCardId);
  const [topics, setTopics] = useState<KnowledgeTopic[]>(knowledgeTopics);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [detailIds, setDetailIds] = useState<Set<number>>(() => new Set());
  const [recentScores, setRecentScores] = useState<Record<number, number | null>>({});
  const [masteredIds, setMasteredIds] = useState<Set<number>>(
    () => new Set(knowledgeTopics.filter((topic) => topic.mastered).map((topic) => topic.id))
  );
  const topicRefs = useRef<Record<number, HTMLDivElement | null>>({});
  const pendingAnchorAdjustmentRef = useRef<{ cardId: number; top: number } | null>(null);

  useEffect(() => {
    let cancelled = false;

    const syncTargetSelection = (targetTopic: KnowledgeTopic | undefined) => {
      if (!targetTopic) return;
      const inferred = inferKnowledgeSelection(targetTopic);
      if (inferred) {
        setSelection(inferred);
      }
      setActiveCardId(targetTopic.id);
      setPendingScrollCardId(targetTopic.id);
    };

    async function loadKnowledgeCards() {
      setLoading(true);
      setNotice("");
      try {
        const [, cardResponse] = await Promise.all([
          knowledgeApi.categories(),
          knowledgeApi.cards(),
        ]);

        if (cancelled) return;

        const nextTopics = cardResponse.data.map(toKnowledgeTopic);

        if (nextTopics.length === 0) {
          throw new Error("后端知识卡为空");
        }

        setTopics(nextTopics);
        setMasteredIds(new Set());
        setDetailIds(new Set());
        syncTargetSelection(nextTopics.find((topic) => topic.id === targetCardId));

        if (targetCardId && nextTopics.some((topic) => topic.id === targetCardId)) {
          setExpandedId(targetCardId);

          try {
            const detailResponse = await knowledgeApi.detail(targetCardId);
            if (cancelled) return;

            const detailTopic = toKnowledgeTopic(detailResponse.data);
            syncTargetSelection(detailTopic);
            setTopics((current) =>
              current.map((item) => (item.id === detailTopic.id ? detailTopic : item))
            );
            setDetailIds(new Set([targetCardId]));
          } catch (err) {
            if (!cancelled) {
              setNotice(formatApiError(err, "knowledge"));
            }
          }
        }
      } catch (err) {
        if (!cancelled) {
          setTopics(knowledgeTopics);
          setMasteredIds(
            new Set(knowledgeTopics.filter((topic) => topic.mastered).map((topic) => topic.id))
          );
          setDetailIds(new Set(knowledgeTopics.map((topic) => topic.id)));
          syncTargetSelection(knowledgeTopics.find((topic) => topic.id === targetCardId));
          if (targetCardId && knowledgeTopics.some((topic) => topic.id === targetCardId)) {
            setExpandedId(targetCardId);
          }
          setNotice(formatApiError(err, "knowledge"));
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }

    loadKnowledgeCards();

    return () => {
      cancelled = true;
    };
  }, [targetCardId]);

  const keyword = search.trim().toLowerCase();

  const topicScopedTopics = useMemo(
    () => topics.filter((topic) => matchKnowledgeTopic(topic, selection)),
    [selection, topics]
  );

  const scopedStatusCounts = useMemo(() => {
    return topicScopedTopics.reduce(
      (counts, topic) => {
        const nextStatus = getTrainingStatus(topic.id, masteredIds, recentScores);
        counts[nextStatus] += 1;
        return counts;
      },
      { 未练: 0, 已掌握: 0, 需复习: 0 }
    );
  }, [masteredIds, recentScores, topicScopedTopics]);

  const filterBaseTopics = useMemo(
    () =>
      topicScopedTopics.filter((topic) => {
        if (difficulty !== "全部" && topic.difficulty !== difficulty) return false;
        return matchesKeyword(topic, keyword);
      }),
    [difficulty, keyword, topicScopedTopics]
  );

  const statusCounts = useMemo(() => {
    return filterBaseTopics.reduce(
      (counts, topic) => {
        const nextStatus = getTrainingStatus(topic.id, masteredIds, recentScores);
        counts[nextStatus] += 1;
        return counts;
      },
      { 未练: 0, 已掌握: 0, 需复习: 0 }
    );
  }, [filterBaseTopics, masteredIds, recentScores]);

  const filteredTopics = useMemo(() => {
    return filterBaseTopics.filter((topic) => {
      if (status === "全部") return true;
      return getTrainingStatus(topic.id, masteredIds, recentScores) === status;
    });
  }, [filterBaseTopics, masteredIds, recentScores, status]);

  useEffect(() => {
    if (!pendingScrollCardId) return;

    const timer = window.setTimeout(() => {
      const target = topicRefs.current[pendingScrollCardId];
      if (target) {
        target.scrollIntoView({ behavior: "smooth", block: "start" });
        setPendingScrollCardId(null);
      }
    }, 50);

    return () => window.clearTimeout(timer);
  }, [filteredTopics.length, pendingScrollCardId]);

  useLayoutEffect(() => {
    const pending = pendingAnchorAdjustmentRef.current;
    if (!pending) return;

    pendingAnchorAdjustmentRef.current = null;
    const target = topicRefs.current[pending.cardId];
    if (!target) return;

    const nextTop = target.getBoundingClientRect().top;
    const topDelta = nextTop - pending.top;
    if (Math.abs(topDelta) > 1) {
      window.scrollBy({ top: topDelta, behavior: "auto" });
    }
  }, [expandedId, filteredTopics.length]);

  const markMastered = (id: number) => {
    setMasteredIds((current) => {
      const next = new Set(current);
      next.add(id);
      return next;
    });
  };

  const updateRecentScore = useCallback((id: number, score: number | null) => {
    setRecentScores((current) => ({ ...current, [id]: score }));
  }, []);

  const loadTopicDetail = useCallback((topicId: number) => {
    knowledgeApi
      .detail(topicId)
      .then((response) => {
        const detailTopic = toKnowledgeTopic(response.data);
        setTopics((current) =>
          current.map((item) => (item.id === detailTopic.id ? detailTopic : item))
        );
        setDetailIds((current) => {
          const next = new Set(current);
          next.add(topicId);
          return next;
        });
      })
      .catch((err) => {
        setNotice(formatApiError(err, "knowledge"));
      });
  }, []);

  const selectOutlineItem = (nextSelection: KnowledgeSelection) => {
    if (nextSelection.cardId) {
      setSelection({
        domain: nextSelection.domain,
        section: nextSelection.section,
        topic: nextSelection.topic,
      });
      setActiveCardId(nextSelection.cardId);
      setExpandedId(nextSelection.cardId);
      setSearch("");
      setDifficulty("全部");
      setStatus("全部");
      setPendingScrollCardId(nextSelection.cardId);
      if (!detailIds.has(nextSelection.cardId)) {
        loadTopicDetail(nextSelection.cardId);
      }
      return;
    }

    setSelection(nextSelection);
    setActiveCardId(null);
    setExpandedId(null);
  };

  const showAllKnowledge = () => {
    setSelection({ domain: "Java 核心" });
    setSearch("");
    setDifficulty("全部");
    setStatus("全部");
  };

  const toggleTopic = (topic: KnowledgeTopic) => {
    const opening = expandedId !== topic.id;
    if (opening) {
      const target = topicRefs.current[topic.id];
      if (target) {
        pendingAnchorAdjustmentRef.current = {
          cardId: topic.id,
          top: target.getBoundingClientRect().top,
        };
      }
      setActiveCardId(topic.id);
    }
    setExpandedId((current) => (current === topic.id ? null : topic.id));

    if (!opening || detailIds.has(topic.id)) {
      return;
    }

    loadTopicDetail(topic.id);
  };

  return (
    <main className="min-h-screen bg-surface">
      <div className="coach-shell max-w-[1680px] py-6">
        <div className="grid grid-cols-1 gap-5 lg:grid-cols-[320px_minmax(0,1fr)] xl:grid-cols-[340px_minmax(0,1fr)]">
        <KnowledgeSidebar
          selection={selection}
          activeCardId={activeCardId}
          topics={topics}
          onSelect={selectOutlineItem}
        />

        <section className="min-w-0">
          <KnowledgeTopicHeader selection={selection} />

          <div className="mb-5 grid gap-3 sm:grid-cols-3">
            <KnowledgeMetric
              icon={<BookOpenCheck className="h-4 w-4" />}
              label="知识卡总数"
              value={topics.length}
            />
            <KnowledgeMetric
              icon={<Target className="h-4 w-4" />}
              label="当前专题"
              value={topicScopedTopics.length}
            />
            <KnowledgeMetric
              icon={<Brain className="h-4 w-4" />}
              label="需复习"
              value={scopedStatusCounts.需复习}
            />
          </div>

          <KnowledgeFilterBar
            search={search}
            difficulty={difficulty}
            status={status}
            total={filteredTopics.length}
            statusCounts={statusCounts}
            loading={loading}
            onSearchChange={setSearch}
            onDifficultyChange={setDifficulty}
            onStatusChange={setStatus}
          />

          {notice && (
            <div className="mb-4 flex items-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-700">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{notice}</span>
            </div>
          )}

          {filteredTopics.length === 0 ? (
            <div className="coach-empty-state">
              <SearchX className="mx-auto mb-3 h-10 w-10" />
              <p className="text-sm font-semibold text-on-surface">当前专题暂无知识卡</p>
              <p className="mt-2 text-sm">
                可以切换到其他分类，或使用搜索查找相关内容。
              </p>
              <button
                type="button"
                onClick={showAllKnowledge}
                className="coach-primary-button mt-5"
              >
                查看全部知识点
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredTopics.map((topic) => (
                <div
                  key={topic.id}
                  ref={(node) => {
                    topicRefs.current[topic.id] = node;
                  }}
                  id={`knowledge-card-${topic.id}`}
                  className="scroll-mt-20"
                >
                  <KnowledgeCard
                    topic={topic}
                    expanded={expandedId === topic.id}
                    mastered={masteredIds.has(topic.id)}
                    userId={userId}
                    onToggle={() => toggleTopic(topic)}
                    onMarkMastered={() => markMastered(topic.id)}
                    onRecentScoreChange={updateRecentScore}
                  />
                </div>
              ))}
            </div>
          )}
        </section>
        </div>
      </div>
    </main>
  );
}

function KnowledgeMetric({
  icon,
  label,
  value,
}: {
  icon: ReactNode;
  label: string;
  value: number;
}) {
  return (
    <div className="coach-card p-4">
      <div className="mb-2 flex items-center gap-1.5 text-primary">
        {icon}
        <span className="text-xs font-semibold text-on-surface-variant">
          {label}
        </span>
      </div>
      <div className="text-2xl font-bold text-on-surface">{value}</div>
    </div>
  );
}
