"use client";

import { ChevronRight } from "lucide-react";
import {
  getKnowledgeTopicMeta,
  matchKnowledgeTopic,
  selectionKey,
  type KnowledgeSelection,
  type KnowledgeTopic,
} from "@/lib/knowledgeData";

interface KnowledgeSidebarProps {
  selection: KnowledgeSelection;
  topics: KnowledgeTopic[];
  onSelect: (selection: KnowledgeSelection) => void;
}

type OutlineNode = KnowledgeSelection & {
  children?: OutlineNode[];
};

const outline: OutlineNode[] = [
  {
    domain: "Java 核心",
    children: [
      {
        domain: "Java 核心",
        section: "Java 基础",
        children: [
          { domain: "Java 核心", section: "Java 基础", topic: "面向对象" },
          { domain: "Java 核心", section: "Java 基础", topic: "数据类型" },
          { domain: "Java 核心", section: "Java 基础", topic: "异常处理" },
          { domain: "Java 核心", section: "Java 基础", topic: "反射与泛型" },
        ],
      },
      {
        domain: "Java 核心",
        section: "集合框架",
        children: [
          { domain: "Java 核心", section: "集合框架", topic: "List" },
          { domain: "Java 核心", section: "集合框架", topic: "Map" },
          { domain: "Java 核心", section: "集合框架", topic: "Set" },
        ],
      },
      { domain: "Java 核心", section: "并发编程（JUC）" },
      { domain: "Java 核心", section: "JVM 虚拟机" },
    ],
  },
  {
    domain: "数据库",
    children: [
      {
        domain: "数据库",
        section: "MySQL",
        children: [
          { domain: "数据库", section: "MySQL", topic: "索引" },
          { domain: "数据库", section: "MySQL", topic: "事务" },
          { domain: "数据库", section: "MySQL", topic: "锁" },
          { domain: "数据库", section: "MySQL", topic: "MVCC" },
        ],
      },
      {
        domain: "数据库",
        section: "Redis",
        children: [
          { domain: "数据库", section: "Redis", topic: "数据结构" },
          { domain: "数据库", section: "Redis", topic: "缓存问题" },
          { domain: "数据库", section: "Redis", topic: "持久化" },
          { domain: "数据库", section: "Redis", topic: "分布式锁" },
        ],
      },
    ],
  },
  {
    domain: "Spring",
    children: [
      { domain: "Spring", topic: "IOC" },
      { domain: "Spring", topic: "AOP" },
      { domain: "Spring", topic: "事务" },
      { domain: "Spring", topic: "Spring MVC" },
    ],
  },
];

function countMatches(topics: KnowledgeTopic[], node: OutlineNode): number {
  return topics.filter((topic) => matchKnowledgeTopic(topic, node)).length;
}

function NodeButton({
  node,
  level,
  selection,
  topics,
  onSelect,
}: {
  node: OutlineNode;
  level: number;
  selection: KnowledgeSelection;
  topics: KnowledgeTopic[];
  onSelect: (selection: KnowledgeSelection) => void;
}) {
  const active = selectionKey(selection) === selectionKey(node);
  const meta = getKnowledgeTopicMeta(node);
  const label = node.topic || node.section || node.domain;
  const count = countMatches(topics, node);

  return (
    <div>
      <button
        type="button"
        onClick={() => onSelect(node)}
        className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-2 text-left text-sm transition ${
          active
            ? "bg-primary/10 font-semibold text-primary"
            : "text-on-surface hover:bg-surface-container"
        }`}
        style={{ paddingLeft: `${12 + level * 18}px` }}
        aria-current={active ? "page" : undefined}
      >
        <span className="inline-flex min-w-0 items-center gap-2">
          {level > 0 && <ChevronRight className="h-3.5 w-3.5 shrink-0 text-outline" />}
          <span className="truncate">{label}</span>
        </span>
        {count > 0 && (
          <span
            className={`rounded-full px-2 py-0.5 text-xs ${
              active ? "bg-primary/15 text-primary" : "bg-surface-container text-on-surface-variant"
            }`}
            aria-label={`${meta.title} 共 ${count} 题`}
          >
            {count}
          </span>
        )}
      </button>
      {node.children?.map((child) => (
        <NodeButton
          key={selectionKey(child)}
          node={child}
          level={level + 1}
          selection={selection}
          topics={topics}
          onSelect={onSelect}
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
            />
          ))}
        </div>
      </nav>
    </aside>
  );
}
