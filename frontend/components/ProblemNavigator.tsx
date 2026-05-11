"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { problemApi } from "@/lib/api";
import type { ProblemListItem } from "@/lib/types";

interface ProblemNavigatorProps {
  currentProblemId: number;
}

export default function ProblemNavigator({
  currentProblemId,
}: ProblemNavigatorProps) {
  const router = useRouter();
  const [problems, setProblems] = useState<ProblemListItem[]>([]);

  useEffect(() => {
    problemApi
      .list()
      .then((res) => {
        const sorted = [...res.data].sort((a, b) => a.id - b.id);
        setProblems(sorted);
      })
      .catch(() => {
        // 静默失败，导航按钮不显示
      });
  }, []);

  if (problems.length === 0) {
    return null;
  }

  const currentIndex = problems.findIndex((p) => p.id === currentProblemId);
  const hasPrev = currentIndex > 0;
  const hasNext = currentIndex < problems.length - 1;

  const goTo = (id: number) => {
    router.push(`/problem/${id}`);
  };

  return (
    <div className="flex items-center gap-1">
      <button
        type="button"
        disabled={!hasPrev}
        onClick={() => hasPrev && goTo(problems[currentIndex - 1].id)}
        title="上一题"
        className={`h-7 w-7 flex items-center justify-center rounded border transition-colors ${
          hasPrev
            ? "text-slate-600 border-slate-200 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 cursor-pointer"
            : "text-slate-300 bg-slate-50 border-slate-100 cursor-not-allowed"
        }`}
      >
        <ChevronLeft className="w-4 h-4" />
      </button>
      <button
        type="button"
        disabled={!hasNext}
        onClick={() => hasNext && goTo(problems[currentIndex + 1].id)}
        title="下一题"
        className={`h-7 w-7 flex items-center justify-center rounded border transition-colors ${
          hasNext
            ? "text-slate-600 border-slate-200 hover:text-blue-600 hover:border-blue-300 hover:bg-blue-50 cursor-pointer"
            : "text-slate-300 bg-slate-50 border-slate-100 cursor-not-allowed"
        }`}
      >
        <ChevronRight className="w-4 h-4" />
      </button>
    </div>
  );
}
