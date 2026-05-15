"use client";

import type { ReactNode } from "react";
import type {
  ExampleVisual,
  LinkedListVisualSection,
  TreeVisual,
  TreeVisualNode,
} from "@/lib/problemPresentation";

interface ProblemExampleVisualProps {
  visual: ExampleVisual;
}

export default function ProblemExampleVisual({
  visual,
}: ProblemExampleVisualProps) {
  if (visual.kind === "tree") {
    return (
      <div className="mt-2 grid gap-2">
        <TreePanel label={visual.input.label} tree={visual.input.tree} />
        {visual.output.type === "tree" && (
          <TreePanel label={visual.output.label} tree={visual.output.tree} />
        )}
        {visual.output.type === "levels" && (
          <LevelOutputPanel
            label={visual.output.label}
            levels={visual.output.levels}
          />
        )}
        {visual.output.type === "value" && (
          <ValueOutputPanel
            label={visual.output.label}
            value={visual.output.value}
          />
        )}
      </div>
    );
  }

  return (
    <div className="mt-2 grid gap-2">
      {visual.inputs.map((input) => (
        <LinkedListPanel key={input.label} section={input} />
      ))}
      {visual.output.type === "list" ? (
        <LinkedListPanel
          section={{
            label: visual.output.label,
            values: visual.output.values,
          }}
        />
      ) : (
        <ValueOutputPanel
          label={visual.output.label}
          value={visual.output.value}
        />
      )}
    </div>
  );
}

function TreePanel({ label, tree }: { label: string; tree: TreeVisual }) {
  if (tree.levels.length === 0) {
    return (
      <VisualShell label={label}>
        <div className="text-xs text-on-surface-variant">空树 null</div>
      </VisualShell>
    );
  }

  const layout = createTreeLayout(tree);
  const height = Math.max(72, tree.levels.length * 62);

  return (
    <VisualShell label={label}>
      <div
        className="relative min-w-[220px]"
        style={{ height: `${height}px` }}
        aria-label={label}
      >
        <svg
          className="absolute inset-0 h-full w-full"
          viewBox="0 0 100 100"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          {tree.edges.map((edge) => {
            const from = layout.get(edge.from);
            const to = layout.get(edge.to);
            if (!from || !to) return null;
            return (
              <line
                key={`${edge.from}-${edge.to}`}
                x1={from.x}
                y1={from.y}
                x2={to.x}
                y2={to.y}
                stroke="currentColor"
                strokeWidth="0.7"
                className="text-outline-variant"
              />
            );
          })}
        </svg>
        {tree.levels.flat().map((node) => {
          const point = layout.get(node.id);
          if (!point) return null;
          return (
            <div
              key={node.id}
              className="absolute flex h-9 w-9 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border border-primary/40 bg-primary/10 text-xs font-semibold text-primary shadow-sm"
              style={{ left: `${point.x}%`, top: `${point.y}%` }}
            >
              {node.value}
            </div>
          );
        })}
      </div>
    </VisualShell>
  );
}

function LinkedListPanel({ section }: { section: LinkedListVisualSection }) {
  if (typeof section.cycleToIndex === "number") {
    return <CycleLinkedListPanel section={section} />;
  }

  return (
    <VisualShell label={section.label}>
      {section.values.length === 0 ? (
        <div className="text-xs text-on-surface-variant">空链表 null</div>
      ) : (
        <div className="flex flex-wrap items-center gap-1.5">
          {section.values.map((value, index) => (
            <div key={`${section.label}-${index}`} className="flex items-center gap-1.5">
              <div className="flex h-8 min-w-8 items-center justify-center rounded-md border border-primary/40 bg-primary/10 px-2 text-xs font-semibold text-primary">
                {value}
              </div>
              {index < section.values.length - 1 && (
                <span className="text-xs font-semibold text-outline">-&gt;</span>
              )}
            </div>
          ))}
        </div>
      )}
    </VisualShell>
  );
}

function CycleLinkedListPanel({
  section,
}: {
  section: LinkedListVisualSection;
}) {
  const targetIndex = section.cycleToIndex ?? -1;
  const targetValue = section.cycleToValue ?? section.values[targetIndex] ?? "";
  const tailValue = section.values[section.values.length - 1] ?? "";
  const nodeCount = Math.max(section.values.length, 1);
  const tailX = ((section.values.length - 0.5) / nodeCount) * 100;
  const targetX = ((targetIndex + 0.5) / nodeCount) * 100;

  return (
    <VisualShell label={`${section.label}（pos = ${targetIndex}）`}>
      <div className="mb-3 rounded-md bg-primary/5 px-2 py-1.5 text-[11px] leading-relaxed text-on-surface-variant">
        pos 从 0 开始计数；pos = {targetIndex} 表示尾节点
        <span className="font-semibold text-on-surface"> {tailValue} </span>
        的 next 指向 index {targetIndex} 的节点
        <span className="font-semibold text-primary"> {targetValue}</span>。
      </div>
      <div className="relative pb-12">
        <div
          className="grid items-start gap-1.5"
          style={{
            gridTemplateColumns: `repeat(${section.values.length}, minmax(34px, 1fr))`,
          }}
        >
          {section.values.map((value, index) => {
            const isTarget = index === targetIndex;
            const isTail = index === section.values.length - 1;
            return (
              <div
                key={`${section.label}-${index}`}
                className="relative flex flex-col items-center gap-1"
              >
                <div className="text-[10px] font-medium text-on-surface-variant">
                  idx {index}
                </div>
                <div
                  className={`flex h-9 min-w-9 items-center justify-center rounded-md border px-2 text-xs font-semibold shadow-sm ${
                    isTarget
                      ? "border-primary bg-primary text-on-primary"
                      : isTail
                      ? "border-secondary/50 bg-secondary/10 text-secondary"
                      : "border-primary/40 bg-primary/10 text-primary"
                  }`}
                >
                  {value}
                </div>
                {index < section.values.length - 1 && (
                  <span className="absolute left-[calc(100%-2px)] top-[31px] text-xs font-semibold text-outline">
                    -&gt;
                  </span>
                )}
              </div>
            );
          })}
        </div>
        <svg
          className="absolute bottom-0 left-0 h-12 w-full overflow-visible text-secondary"
          viewBox="0 0 100 48"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <defs>
            <marker
              id={`cycle-arrow-${section.label}`}
              markerHeight="5"
              markerWidth="5"
              orient="auto"
              refX="4"
              refY="2.5"
            >
              <path d="M0,0 L5,2.5 L0,5 Z" fill="currentColor" />
            </marker>
          </defs>
          <path
            d={`M ${tailX} 4 C ${tailX} 42, ${targetX} 42, ${targetX} 8`}
            fill="none"
            markerEnd={`url(#cycle-arrow-${section.label})`}
            stroke="currentColor"
            strokeWidth="2"
            vectorEffect="non-scaling-stroke"
          />
        </svg>
      </div>
      <div className="text-[11px] leading-relaxed text-on-surface-variant">
        继续沿 next 前进会再次回到
        <span className="font-semibold text-primary"> {targetValue} </span>
        这个节点，因此链表存在环。
      </div>
    </VisualShell>
  );
}

function LevelOutputPanel({
  label,
  levels,
}: {
  label: string;
  levels: string[][];
}) {
  return (
    <VisualShell label={label}>
      <div className="space-y-1.5">
        {levels.map((level, index) => (
          <div key={`${label}-${index}`} className="flex items-center gap-2">
            <span className="w-10 text-[11px] font-medium text-on-surface-variant">
              第 {index + 1} 层
            </span>
            <div className="flex flex-wrap gap-1.5">
              {level.map((value, valueIndex) => (
                <span
                  key={`${value}-${valueIndex}`}
                  className="rounded-md bg-secondary/10 px-2 py-1 text-xs font-semibold text-secondary"
                >
                  {value}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </VisualShell>
  );
}

function ValueOutputPanel({ label, value }: { label: string; value: string }) {
  return (
    <VisualShell label={label}>
      <span className="inline-flex rounded-md bg-secondary/10 px-2 py-1 text-xs font-semibold text-secondary">
        {value}
      </span>
    </VisualShell>
  );
}

function VisualShell({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <div className="rounded-lg border border-outline-variant/40 bg-surface-container-low px-3 py-2">
      <div className="mb-2 text-[11px] font-semibold text-on-surface-variant">
        {label}
      </div>
      {children}
    </div>
  );
}

function createTreeLayout(tree: TreeVisual) {
  const layout = new Map<string, { x: number; y: number }>();
  const levelCount = tree.levels.length;

  tree.levels.forEach((level, levelIndex) => {
    const slots = 2 ** levelIndex;
    const y = levelCount === 1 ? 50 : 16 + (levelIndex * 68) / (levelCount - 1);
    level.forEach((node: TreeVisualNode) => {
      layout.set(node.id, {
        x: ((node.slot + 0.5) / slots) * 100,
        y,
      });
    });
  });

  return layout;
}
