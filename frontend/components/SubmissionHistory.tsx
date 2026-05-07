"use client";

import Link from "next/link";
import { History, CheckCircle, XCircle } from "lucide-react";
import { problemTitle } from "@/lib/i18n";

interface Submission {
  problemId: number;
  problemTitle: string;
  status: string;
  passedCount: number;
  totalCount: number;
  time: string;
}

interface SubmissionHistoryProps {
  submissions: Submission[];
}

export default function SubmissionHistory({
  submissions,
}: SubmissionHistoryProps) {
  return (
    <section>
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <History className="w-5 h-5 text-primary" />
          <h2 className="text-lg font-semibold text-on-surface">最近提交记录</h2>
        </div>
        <button className="text-xs text-primary font-medium hover:underline">
          查看全部
        </button>
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
            {submissions.map((s, i) => {
              const isPassed = s.status === "ACCEPTED";
              return (
                <tr
                  key={i}
                  className={`hover:bg-surface-container-low transition-colors ${
                    i < submissions.length - 1
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
                      className={`text-xs font-medium px-2 py-0.5 rounded-full flex items-center gap-1 w-fit ${
                        isPassed
                          ? "bg-emerald-50 text-emerald-700"
                          : "bg-red-50 text-red-700"
                      }`}
                    >
                      {isPassed ? (
                        <CheckCircle className="w-3 h-3" />
                      ) : (
                        <XCircle className="w-3 h-3" />
                      )}
                      {isPassed ? "通过" : "未通过"}
                    </span>
                  </td>
                  <td className="px-5 py-3 text-xs text-on-surface-variant font-mono">
                    {s.passedCount}/{s.totalCount}
                  </td>
                  <td className="px-5 py-3 text-xs text-outline">{s.time}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
