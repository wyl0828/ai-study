"use client";

import { useEffect, useMemo, useState } from "react";
import { formatApiError, knowledgeApi } from "@/lib/api";
import type {
  KnowledgeCardDetail,
  KnowledgeCardListItem,
  KnowledgeCategory,
} from "@/lib/types";
import KnowledgeCardDetailView from "./KnowledgeCardDetail";
import KnowledgeCardList from "./KnowledgeCardList";
import KnowledgeCategoryTabs from "./KnowledgeCategoryTabs";

interface KnowledgeTrainingClientProps {
  initialCategories: KnowledgeCategory[];
  initialCards: KnowledgeCardListItem[];
}

export default function KnowledgeTrainingClient({
  initialCategories,
  initialCards,
}: KnowledgeTrainingClientProps) {
  const [categories] = useState(initialCategories);
  const [activeCategory, setActiveCategory] = useState("ALL");
  const [cards, setCards] = useState(initialCards);
  const [activeId, setActiveId] = useState<number | null>(initialCards[0]?.id ?? null);
  const [detail, setDetail] = useState<KnowledgeCardDetail | null>(null);
  const [listLoading, setListLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const totalCount = useMemo(
    () => categories.reduce((sum, category) => sum + category.count, 0),
    [categories]
  );

  useEffect(() => {
    let cancelled = false;

    async function loadCards() {
      setListLoading(true);
      setError(null);
      try {
        const response = await knowledgeApi.cards(activeCategory);
        if (cancelled) return;
        setCards(response.data);
        setActiveId(response.data[0]?.id ?? null);
      } catch (err) {
        if (!cancelled) {
          setError(formatApiError(err, "knowledge"));
          setCards([]);
          setActiveId(null);
        }
      } finally {
        if (!cancelled) {
          setListLoading(false);
        }
      }
    }

    loadCards();

    return () => {
      cancelled = true;
    };
  }, [activeCategory]);

  useEffect(() => {
    let cancelled = false;

    async function loadDetail(id: number) {
      setDetailLoading(true);
      setError(null);
      try {
        const response = await knowledgeApi.detail(id);
        if (!cancelled) {
          setDetail(response.data);
        }
      } catch (err) {
        if (!cancelled) {
          setError(formatApiError(err, "knowledge"));
          setDetail(null);
        }
      } finally {
        if (!cancelled) {
          setDetailLoading(false);
        }
      }
    }

    if (activeId == null) {
      setDetail(null);
      return () => {
        cancelled = true;
      };
    }

    loadDetail(activeId);

    return () => {
      cancelled = true;
    };
  }, [activeId]);

  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-on-surface tracking-tight">后端知识训练</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          Java 后端高频面试知识卡，覆盖 Java、JVM、Spring、MySQL、Redis
        </p>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 mb-6">
        <KnowledgeCategoryTabs
          categories={categories}
          activeCategory={activeCategory}
          onChange={setActiveCategory}
        />
        <div className="text-xs text-on-surface-variant">
          共 {activeCategory === "ALL" ? totalCount : cards.length} 张卡片
        </div>
      </div>

      {error && (
        <div className="mb-6 rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <div className="lg:col-span-5">
          <KnowledgeCardList
            cards={cards}
            activeId={activeId}
            loading={listLoading}
            onSelect={setActiveId}
          />
        </div>
        <aside className="lg:col-span-7">
          <div className="lg:sticky lg:top-20">
            <KnowledgeCardDetailView card={detail} loading={detailLoading} />
          </div>
        </aside>
      </div>
    </div>
  );
}
