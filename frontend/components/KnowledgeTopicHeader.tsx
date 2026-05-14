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
        className="mb-5 flex flex-wrap items-center gap-2 text-sm text-on-surface-variant"
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

      <div className="flex items-start gap-4">
        <div className="mt-1 rounded-xl bg-primary/10 p-2 text-primary">
          <BookOpenCheck className="h-7 w-7" aria-label={meta.iconLabel} />
        </div>
        <div className="min-w-0">
          <h1 className="text-3xl font-bold tracking-tight text-on-surface">
            {meta.title}
          </h1>
          <p className="mt-3 max-w-3xl text-base leading-relaxed text-on-surface-variant">
            {meta.description}
          </p>
        </div>
      </div>
    </section>
  );
}
