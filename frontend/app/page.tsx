import { formatApiError, problemApi } from "@/lib/api";
import HomeClient from "@/components/HomeClient";
import type { HomeProblem, ProblemDetail, ProblemListItem } from "@/lib/types";

export default async function HomePage() {
  let problems: ProblemListItem[];
  try {
    const response = await problemApi.list();
    problems = response.data;
  } catch (err) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-10">
        <div className="rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
          {formatApiError(err)}
        </div>
      </main>
    );
  }

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
