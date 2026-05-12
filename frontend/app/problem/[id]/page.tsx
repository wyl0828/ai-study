import { formatApiError, problemApi } from "@/lib/api";
import ProblemDescription from "@/components/ProblemDescription";
import ProblemWorkspace from "@/components/ProblemWorkspace";
import type { ProblemDetail } from "@/lib/types";

interface ProblemPageProps {
  params: { id: string };
}

export default async function ProblemPage({ params }: ProblemPageProps) {
  let problem: ProblemDetail;
  try {
    const response = await problemApi.detail(Number(params.id));
    problem = response.data;
  } catch (err) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-10">
        <div className="rounded-lg border border-error/20 bg-error/5 px-4 py-3 text-sm text-error">
          {formatApiError(err)}
        </div>
      </main>
    );
  }

  return (
    <main className="h-[calc(100vh-56px)] flex overflow-hidden bg-background">
      <ProblemDescription problem={problem} />
      <ProblemWorkspace problemId={problem.id} />
    </main>
  );
}
