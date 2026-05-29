"use client";

import { ArrowRight, BookOpen, ClipboardList } from "lucide-react";
import Link from "next/link";
import type { DashboardNextAction } from "@/lib/learningView";

interface DashboardNextActionsProps {
  actions: DashboardNextAction[];
}

const toneClass: Record<DashboardNextAction["tone"], string> = {
  primary: "border-primary/25 bg-primary/5 text-primary",
  warning: "border-amber-200 bg-amber-50 text-amber-700",
  neutral: "border-outline-variant/40 bg-surface-container-lowest text-on-surface-variant",
};

const priorityLabel: Record<DashboardNextAction["priority"], string> = {
  HIGH: "高优先级",
  MEDIUM: "中优先级",
};

export default function DashboardNextActions({ actions }: DashboardNextActionsProps) {
  return (
    <section className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-5">
      <div className="flex items-center gap-2">
        <ClipboardList className="h-5 w-5 text-primary" />
        <div>
          <h2 className="text-base font-semibold text-on-surface">下一步动作</h2>
          <p className="mt-1 text-xs text-on-surface-variant">
            根据训练计划追踪和模拟面试闭环，给出当前最该执行的动作。
          </p>
        </div>
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-3">
        {actions.map((action) => (
          <Link
            key={`${action.href}-${action.title}`}
            href={action.href}
            className={`group rounded-lg border px-4 py-3 transition hover:shadow-sm ${toneClass[action.tone]}`}
          >
            <div className="flex items-start justify-between gap-3">
              <BookOpen className="mt-0.5 h-4 w-4 shrink-0" />
              <span className="rounded-full border border-current/20 px-2 py-0.5 text-[11px] font-medium">
                优先级：{priorityLabel[action.priority]}
              </span>
              <ArrowRight className="h-4 w-4 shrink-0 opacity-60 transition group-hover:translate-x-0.5" />
            </div>
            <div className="mt-3 text-[11px] font-medium text-on-surface-variant">
              来源：{action.sourceLabel}
            </div>
            <h3 className="mt-3 text-sm font-semibold text-on-surface">
              {action.title}
            </h3>
            <p className="mt-2 text-xs leading-relaxed text-on-surface-variant">
              {action.description}
            </p>
            <span className="mt-3 inline-flex text-xs font-medium">
              {action.label}
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
