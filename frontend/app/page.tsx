import { problemApi } from "@/lib/api";
import HomeClient from "@/components/HomeClient";
import type { HomeProblem, ProblemDetail } from "@/lib/types";

export default async function HomePage() {
  const { data: problems } = await problemApi.list();
  const details = await Promise.all(
    problems.map(async (problem) => {
      try {
        const { data } = await problemApi.detail(problem.id);
        return data;
      } catch {
        return null;
      }
    })
  );

  const detailMap = new Map<number, ProblemDetail>();
  details.forEach((detail) => {
    if (detail) detailMap.set(detail.id, detail);
  });

  const enrichedProblems: HomeProblem[] = problems.map((problem) => {
    const detail = detailMap.get(problem.id);
    return {
      ...problem,
      description: detail?.description,
      knowledgePoints: detail?.knowledgePoints,
    };
  });

  return <HomeClient problems={enrichedProblems} />;
}
