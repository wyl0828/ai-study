import Link from "next/link";
import { Circle } from "lucide-react";
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
      className="group block bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5 hover:shadow-md hover:border-primary/30 transition-all duration-200"
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <DifficultyBadge difficulty={problem.difficulty} />
          <span className="text-xs text-outline">#{problem.id}</span>
        </div>
        <Circle className="w-[18px] h-[18px] text-outline/40" aria-label="未做" />
      </div>
      <h3 className="text-base font-semibold text-on-surface group-hover:text-primary transition-colors mb-2">
        {problemTitle(problem.title)}
      </h3>
      {compactDescription && (
        <p className="text-xs text-on-surface-variant leading-relaxed mb-3 line-clamp-2">
          {compactDescription}
        </p>
      )}
      <div className="flex flex-wrap gap-1.5">
        {tags.map((tag) => (
          <span
            key={tag}
            className="bg-surface-container text-on-surface-variant text-[11px] px-2 py-0.5 rounded-full border border-outline-variant/30"
          >
            {knowledgePoint(tag)}
          </span>
        ))}
      </div>
    </Link>
  );
}
