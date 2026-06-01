"use client";

import { useState } from "react";
import type { ReactNode } from "react";
import { ChevronDown, ChevronUp, ServerCog } from "lucide-react";
import type { CacheMaintenanceStatus, RagHealth } from "@/lib/types";

interface SystemHealthPanelProps {
  cacheStatus: CacheMaintenanceStatus | null;
  ragHealth: RagHealth | null;
  children: ReactNode;
}

function compactStatus(value: string | null | undefined) {
  return value || "待检查";
}

function statusTone(value: string | null | undefined) {
  if (!value) return "text-on-surface-variant";
  if (/healthy|available|ok|正常|可用/i.test(value)) return "text-emerald-700";
  if (/degraded|unavailable|failed|异常|不可用|降级/i.test(value)) return "text-amber-700";
  return "text-primary";
}

export default function SystemHealthPanel({
  cacheStatus,
  ragHealth,
  children,
}: SystemHealthPanelProps) {
  const [expanded, setExpanded] = useState(false);

  return (
    <section className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest p-5">
      <div className="flex items-start gap-3">
        <div className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <ServerCog className="h-5 w-5" />
        </div>
        <div className="min-w-0 flex-1">
          <h2 className="text-base font-semibold text-on-surface">系统诊断</h2>
          <p className="mt-1 text-xs leading-5 text-on-surface-variant">
            面向项目演示和开发排查，默认收起；学习功能异常时再展开查看。
          </p>
        </div>
        <button
          type="button"
          onClick={() => setExpanded((current) => !current)}
          className="inline-flex shrink-0 items-center gap-1 rounded-lg border border-outline-variant/50 px-3 py-2 text-xs font-medium text-on-surface transition hover:border-primary hover:text-primary"
        >
          {expanded ? "收起详情" : "展开查看"}
          {expanded ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
        </button>
      </div>

      <div className="mt-4 grid gap-3">
        <div className="rounded-lg border border-outline-variant/30 bg-surface-container px-3 py-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-on-surface">系统加速状态</div>
              <p className="mt-1 text-xs leading-5 text-on-surface-variant">
                用于加快题目和知识卡片加载；异常时会自动回退数据库，不影响学习。
              </p>
            </div>
            <div className={`shrink-0 text-right text-sm font-semibold ${statusTone(cacheStatus?.statusLabel)}`}>
              {compactStatus(cacheStatus?.statusLabel)}
              <div className="mt-1 text-[11px] font-normal text-on-surface-variant">
                Key {cacheStatus?.cachedKeyCount ?? 0}
              </div>
            </div>
          </div>
        </div>

        <div className="rounded-lg border border-outline-variant/30 bg-surface-container px-3 py-3">
          <div className="flex items-center justify-between gap-3">
            <div>
              <div className="text-sm font-semibold text-on-surface">AI 知识库状态</div>
              <p className="mt-1 text-xs leading-5 text-on-surface-variant">
                用于支持 AI 诊断检索题目知识、历史错题和用户记忆。
              </p>
            </div>
            <div className={`shrink-0 text-right text-sm font-semibold ${statusTone(ragHealth?.statusLabel)}`}>
              {compactStatus(ragHealth?.statusLabel)}
              <div className="mt-1 text-[11px] font-normal text-on-surface-variant">
                Chunk {ragHealth?.systemChunkCount ?? 0}
              </div>
            </div>
          </div>
        </div>
      </div>

      {expanded && (
        <div className="mt-5 space-y-5 border-t border-outline-variant/30 pt-5">
          {children}
        </div>
      )}
    </section>
  );
}
