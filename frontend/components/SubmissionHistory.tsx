"use client";

import { useState } from "react";
import Link from "next/link";
import { History, CheckCircle, XCircle } from "lucide-react";
import type { SubmissionHistoryVO } from "@/lib/types";
import { problemTitle } from "@/lib/i18n";

interface SubmissionHistoryProps {
  submissions: SubmissionHistoryVO[];
}

function formatCreatedAt(createdAt: string | null) {
  if (!createdAt) {
    return "未知时间";
  }
  const date = new Date(createdAt);
  if (Number.isNaN(date.getTime())) {
    return createdAt;
  }
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export default function SubmissionHistory({
  submissions,
}: SubmissionHistoryProps) {
  const [showAll, setShowAll] = useState(false);
  const visibleSubmissions = showAll ? submissions : submissions.slice(0, 8);
  const hiddenCount = Math.max(submissions.length - visibleSubmissions.length, 0);

  const statusView = (status: string) => {
    if (status === "ACCEPTED") {
      return {
        label: "已通过",
        icon: <CheckCircle className="w-3 h-3" />,
        className: "bg-emerald-50 text-emerald-700",
      };
    }
    if (status === "RETRY" || status === "NEEDS_REVIEW") {
      return {
        label: "待重试",
        icon: <XCircle className="w-3 h-3" />,
        className: "bg-amber-50 text-amber-700",
      };
    }
    return {
      label: "未通过",
      icon: <XCircle className="w-3 h-3" />,
      className: "bg-red-50 text-red-700",
    };
  };

  return (
    <section>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <History className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">最近提交记录</h2>
        </div>
        {submissions.length > 8 && (
          <button
            type="button"
            onClick={() => setShowAll((current) => !current)}
            className="text-xs text-primary font-medium hover:underline"
          >
            {showAll ? "收起提交记录" : `查看全部提交记录 ${hiddenCount} 条`}
          </button>
        )}
      </div>
      <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-surface-container-low border-b border-outline-variant/30">
              <th className="text-left px-5 py-2.5 text-xs font-semibold text-on-surface-variant">
                题目
              </th>
              <th className="text-left px-5 py-2.5 text-xs font-semibold text-on-surface-variant">
                状态
              </th>
              <th className="text-left px-5 py-2.5 text-xs font-semibold text-on-surface-variant">
                通过率
              </th>
              <th className="text-left px-5 py-2.5 text-xs font-semibold text-on-surface-variant">
                时间
              </th>
            </tr>
          </thead>
          <tbody>
            {submissions.length === 0 && (
              <tr>
                <td
                  colSpan={4}
                  className="px-5 py-8 text-sm text-on-surface-variant"
                >
                  还没有提交记录，去题库看看。
                </td>
              </tr>
            )}
            {visibleSubmissions.map((s, i) => {
              const status = statusView(s.status);
              return (
                <tr
                  key={i}
                  className={`hover:bg-surface-container-low transition-colors ${
                    i < visibleSubmissions.length - 1
                      ? "border-b border-outline-variant/15"
                      : ""
                  }`}
                >
                  <td className="px-5 py-3">
                    <Link
                      href={`/problem/${s.problemId}`}
                      className="text-on-surface hover:text-primary font-medium"
                    >
                      #{s.problemId} {problemTitle(s.problemTitle)}
                    </Link>
                  </td>
                  <td className="px-5 py-3">
                    <span
                      className={`text-xs font-medium px-2 py-0.5 rounded-full flex items-center gap-1 w-fit ${status.className}`}
                    >
                      {status.icon}
                      {status.label}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-xs text-on-surface-variant font-mono">
                    {s.passedCount}/{s.totalCount}
                  </td>
                  <td className="px-5 py-3 text-xs text-outline">
                    {formatCreatedAt(s.createdAt)}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
