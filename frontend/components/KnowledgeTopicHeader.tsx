"use client";

import { BookOpenCheck, ChevronRight } from "lucide-react";
import {
  getKnowledgeTopicMeta,
  getSelectionBreadcrumb,
  type KnowledgeSelection,
} from "@/lib/knowledgeData";

interface KnowledgeTopicHeaderProps {
  selection: KnowledgeSelection;
}

export default function KnowledgeTopicHeader({ selection }: KnowledgeTopicHeaderProps) {
  const meta = getKnowledgeTopicMeta(selection);
  const breadcrumb = getSelectionBreadcrumb(selection);

  return (
    <section className="mb-5">
      <nav
        className="mb-4 flex flex-wrap items-center gap-2 text-xs text-on-surface-variant"
        aria-label="知识训练路径"
      >
        {breadcrumb.map((item, index) => (
          <span key={`${item}-${index}`} className="inline-flex items-center gap-2">
            <span className={index === breadcrumb.length - 1 ? "font-semibold text-on-surface" : ""}>
              {item}
            </span>
            {index < breadcrumb.length - 1 && (
              <ChevronRight className="h-4 w-4 text-outline" />
            )}
          </span>
        ))}
      </nav>

      <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
        <div className="min-w-0">
          <div className="coach-pill mb-3 w-fit border-primary/20 bg-primary/5 text-primary">
            <BookOpenCheck className="h-3.5 w-3.5" aria-label={meta.iconLabel} />
            知识训练台
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-on-surface">
            {meta.title}
          </h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-on-surface-variant">
            {meta.description}
          </p>
        </div>
      </div>
    </section>
  );
}
