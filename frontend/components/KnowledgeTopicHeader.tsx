"use client";

import { BookOpenCheck, ChevronRight } from "lucide-react";
import {
  getKnowledgeTopicMeta,
  getSelectionBreadcrumb,
  type KnowledgeSelection,
} from "@/lib/knowledgeData";

interface KnowledgeTopicHeaderProps {
  selection: KnowledgeSelection;
  tone?: "surface" | "hero";
}

export default function KnowledgeTopicHeader({
  selection,
  tone = "surface",
}: KnowledgeTopicHeaderProps) {
  const meta = getKnowledgeTopicMeta(selection);
  const breadcrumb = getSelectionBreadcrumb(selection);
  const isHero = tone === "hero";

  return (
    <section className={isHero ? "" : "mb-5"}>
      <nav
        className={`mb-4 flex flex-wrap items-center gap-2 text-xs ${
          isHero ? "text-slate-300" : "text-on-surface-variant"
        }`}
        aria-label="知识训练路径"
      >
        {breadcrumb.map((item, index) => (
          <span key={`${item}-${index}`} className="inline-flex items-center gap-2">
            <span
              className={
                index === breadcrumb.length - 1
                  ? isHero
                    ? "font-semibold text-white"
                    : "font-semibold text-on-surface"
                  : ""
              }
            >
              {item}
            </span>
            {index < breadcrumb.length - 1 && (
              <ChevronRight className={`h-4 w-4 ${isHero ? "text-slate-400" : "text-outline"}`} />
            )}
          </span>
        ))}
      </nav>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
        <div className="min-w-0">
          <div
            className={`coach-pill mb-3 w-fit ${
              isHero
                ? "border-teal-300/25 bg-teal-300/10 text-teal-100"
                : "border-primary/20 bg-primary/5 text-primary"
            }`}
          >
            <BookOpenCheck className="h-3.5 w-3.5" aria-label={meta.iconLabel} />
            知识训练台
          </div>
          <h1 className={`text-2xl font-bold tracking-tight ${isHero ? "text-white" : "text-on-surface"}`}>
            {meta.title}
          </h1>
          <p className={`mt-2 max-w-3xl text-sm leading-6 ${isHero ? "text-slate-300" : "text-on-surface-variant"}`}>
            {meta.description}
          </p>
        </div>
      </div>
    </section>
  );
}
