"use client";

import { useMemo, useState } from "react";
import { ChevronRight } from "lucide-react";
import {
  buildKnowledgeOutline,
  getKnowledgeTopicMeta,
  matchKnowledgeTopic,
  selectionKey,
  type KnowledgeOutlineNode,
  type KnowledgeSelection,
  type KnowledgeTopic,
} from "@/lib/knowledgeData";

interface KnowledgeSidebarProps {
  selection: KnowledgeSelection;
  topics: KnowledgeTopic[];
  onSelect: (selection: KnowledgeSelection) => void;
}

function countMatches(topics: KnowledgeTopic[], node: KnowledgeOutlineNode): number {
  return topics.filter((topic) => matchKnowledgeTopic(topic, node)).length;
}

function NodeButton({
  node,
  level,
  selection,
  topics,
  onSelect,
  expandedKeys,
  onToggleExpand,
}: {
  node: KnowledgeOutlineNode;
  level: number;
  selection: KnowledgeSelection;
  topics: KnowledgeTopic[];
  onSelect: (selection: KnowledgeSelection) => void;
  expandedKeys: Set<string>;
  onToggleExpand: (key: string) => void;
}) {
  const active = selectionKey(selection) === selectionKey(node);
  const meta = getKnowledgeTopicMeta(node);
  const isCardNode = Boolean(node.cardId);
  const label = node.cardTitle || node.topic || node.section || node.domain;
  const count = countMatches(topics, node);
  const key = selectionKey(node);
  const hasChildren = Boolean(node.children?.length);
  const expanded = !hasChildren || expandedKeys.has(key);

  return (
    <div>
      <div
        className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 text-left transition ${
          active
            ? "bg-primary/10 font-semibold text-primary"
            : isCardNode
            ? "text-on-surface-variant hover:bg-surface-container"
            : "text-on-surface hover:bg-surface-container"
        } ${isCardNode ? "py-1.5 text-xs" : "py-2 text-sm"}`}
        style={{ paddingLeft: `${12 + level * 18}px` }}
      >
        <span className="inline-flex min-w-0 items-center gap-2">
          {hasChildren ? (
            <button
              type="button"
              onClick={() => onToggleExpand(key)}
              className="rounded p-0.5 text-outline transition hover:bg-surface-container-high hover:text-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/20"
              aria-label={expanded ? `收起 ${label}` : `展开 ${label}`}
              aria-expanded={expanded}
            >
              <ChevronRight
                className={`h-3.5 w-3.5 shrink-0 transition-transform ${
                  expanded ? "rotate-90" : ""
                }`}
              />
            </button>
          ) : (
            <span className="h-3.5 w-3.5 shrink-0" />
          )}
          <button
            type="button"
            onClick={() => onSelect(node)}
            className="min-w-0 truncate text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/20"
            aria-current={active ? "page" : undefined}
            title={label}
          >
            {label}
          </button>
        </span>
        {!isCardNode && count > 0 && (
          <span
            className={`rounded-full px-2 py-0.5 text-xs ${
              active ? "bg-primary/15 text-primary" : "bg-surface-container text-on-surface-variant"
            }`}
            aria-label={`${meta.title} 共 ${count} 题`}
          >
            {count}
          </span>
        )}
      </div>
      {expanded &&
        node.children?.map((child) => (
          <NodeButton
            key={selectionKey(child)}
            node={child}
            level={level + 1}
            selection={selection}
            topics={topics}
            onSelect={onSelect}
            expandedKeys={expandedKeys}
            onToggleExpand={onToggleExpand}
          />
        ))}
    </div>
  );
}

export default function KnowledgeSidebar({
  selection,
  topics,
  onSelect,
}: KnowledgeSidebarProps) {
  const outline = useMemo(() => buildKnowledgeOutline(topics), [topics]);
  const [expandedKeys, setExpandedKeys] = useState<Set<string>>(
    () =>
      new Set([
        "Java 核心",
        "Java 核心/集合框架",
      ])
  );

  const toggleExpand = (key: string) => {
    setExpandedKeys((current) => {
      const next = new Set(current);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
  };

  return (
    <aside className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-4 shadow-sm lg:sticky lg:top-20">
      <h2 className="mb-3 text-sm font-bold text-on-surface-variant">知识体系大纲</h2>
      <nav className="max-h-[360px] overflow-y-auto pr-1 lg:max-h-none" aria-label="知识体系大纲">
        <div className="min-w-[260px] space-y-1 lg:min-w-0">
          {outline.map((node) => (
            <NodeButton
              key={selectionKey(node)}
              node={node}
              level={0}
              selection={selection}
              topics={topics}
              onSelect={onSelect}
              expandedKeys={expandedKeys}
              onToggleExpand={toggleExpand}
            />
          ))}
        </div>
      </nav>
    </aside>
  );
}
