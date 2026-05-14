"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { AlertCircle, SearchX } from "lucide-react";
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

const categoryLabels: KnowledgeCategory[] = ["Java", "MySQL", "Redis", "Spring", "JVM"];
const DEMO_USER_ID = 1;

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

export default function KnowledgeTrainingPage() {
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
  const [topics, setTopics] = useState<KnowledgeTopic[]>(knowledgeTopics);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");
  const [detailIds, setDetailIds] = useState<Set<number>>(() => new Set());
  const [recentScores, setRecentScores] = useState<Record<number, number | null>>({});
  const [masteredIds, setMasteredIds] = useState<Set<number>>(
    () => new Set(knowledgeTopics.filter((topic) => topic.mastered).map((topic) => topic.id))
  );

  useEffect(() => {
    let cancelled = false;

    const syncTargetSelection = (targetTopic: KnowledgeTopic | undefined) => {
      if (!targetTopic) return;
      const inferred = inferKnowledgeSelection(targetTopic);
      if (inferred) {
        setSelection(inferred);
      }
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

  const selectOutlineItem = (nextSelection: KnowledgeSelection) => {
    setSelection(nextSelection);
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
    setExpandedId((current) => (current === topic.id ? null : topic.id));

    if (!opening || detailIds.has(topic.id)) {
      return;
    }

    knowledgeApi
      .detail(topic.id)
      .then((response) => {
        const detailTopic = toKnowledgeTopic(response.data);
        setTopics((current) =>
          current.map((item) => (item.id === detailTopic.id ? detailTopic : item))
        );
        setDetailIds((current) => {
          const next = new Set(current);
          next.add(topic.id);
          return next;
        });
      })
      .catch((err) => {
        setNotice(formatApiError(err, "knowledge"));
      });
  };

  return (
    <main className="min-h-screen bg-surface px-4 py-6 sm:px-6 lg:px-8">
      <div className="mx-auto grid max-w-7xl grid-cols-1 gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <KnowledgeSidebar
          selection={selection}
          topics={topics}
          onSelect={selectOutlineItem}
        />

        <section className="min-w-0">
          <KnowledgeTopicHeader selection={selection} />

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
            <div className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest py-14 text-center text-on-surface-variant">
              <SearchX className="mx-auto mb-3 h-10 w-10" />
              <p className="text-sm font-semibold text-on-surface">当前专题暂无知识卡</p>
              <p className="mt-2 text-sm">
                可以切换到其他分类，或使用搜索查找相关内容。
              </p>
              <button
                type="button"
                onClick={showAllKnowledge}
                className="mt-5 rounded-lg bg-primary px-4 py-2 text-sm font-semibold text-on-primary transition hover:bg-primary-container"
              >
                查看全部知识点
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredTopics.map((topic) => (
                <KnowledgeCard
                  key={topic.id}
                  topic={topic}
                  expanded={expandedId === topic.id}
                  mastered={masteredIds.has(topic.id)}
                  userId={DEMO_USER_ID}
                  onToggle={() => toggleTopic(topic)}
                  onMarkMastered={() => markMastered(topic.id)}
                  onRecentScoreChange={updateRecentScore}
                />
              ))}
            </div>
          )}
        </section>
      </div>
    </main>
  );
}
