import Link from "next/link";
import { ArrowRight, Circle, Target } from "lucide-react";
import DifficultyBadge from "./DifficultyBadge";
import type { HomeProblem } from "@/lib/types";
import { knowledgePoint, problemDescription, problemTitle } from "@/lib/i18n";

interface ProblemCardProps {
  problem: HomeProblem;
}

export default function ProblemCard({ problem }: ProblemCardProps) {
  const tags =
    problem.knowledgePoints && problem.knowledgePoints.length > 0
      ? problem.knowledgePoints
      : [problem.category];
  const chineseDescription = problem.description
    ? problemDescription(problem.description)
    : undefined;
  const compactDescription =
    chineseDescription && chineseDescription.length > 72
      ? `${chineseDescription.slice(0, 72)}...`
      : chineseDescription;

  return (
    <Link
      href={`/problem/${problem.id}`}
      className="coach-card coach-card-hover group flex min-h-[210px] flex-col p-5"
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <DifficultyBadge difficulty={problem.difficulty} />
          <span className="text-xs text-outline">#{problem.id}</span>
        </div>
        <span className="inline-flex items-center gap-1 text-[11px] font-medium text-on-surface-variant">
          <Circle className="h-3.5 w-3.5 text-outline/50" aria-label="未做" />
          未开始
        </span>
      </div>
      <h3 className="text-base font-semibold text-on-surface group-hover:text-primary transition-colors mb-2">
        {problemTitle(problem.title)}
      </h3>
      {compactDescription && (
        <p className="mb-4 line-clamp-3 text-xs leading-relaxed text-on-surface-variant">
          {compactDescription}
        </p>
      )}

      <div className="mt-auto border-t border-outline-variant/40 pt-3">
        <div className="mb-2 flex items-center gap-1.5 text-[11px] font-semibold text-on-surface-variant">
          <Target className="h-3.5 w-3.5 text-primary" />
          训练重点
        </div>
        <div className="flex flex-wrap gap-1.5">
          {tags.map((tag) => (
            <span
              key={tag}
              className="coach-pill px-2 py-0.5 text-[11px]"
            >
              {knowledgePoint(tag)}
            </span>
          ))}
        </div>
        <div className="mt-3 flex items-center justify-between text-xs font-semibold text-primary">
          <span>进入训练</span>
          <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-0.5" />
        </div>
      </div>
    </Link>
  );
}
