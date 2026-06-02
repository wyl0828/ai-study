import { formatApiError, problemApi } from "@/lib/api";
import ProblemDescription from "@/components/ProblemDescription";
import AuthenticatedProblemWorkspace from "@/components/AuthenticatedProblemWorkspace";
import type { ProblemDetail } from "@/lib/types";

export const dynamic = "force-dynamic";

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
    <main className="grid min-h-[calc(100dvh-88px)] grid-cols-1 overflow-visible bg-background md:h-[calc(100vh-56px)] md:min-h-0 md:grid-cols-[minmax(280px,24vw)_minmax(0,1fr)_minmax(320px,30vw)] md:overflow-hidden">
      <ProblemDescription problem={problem} />
      <AuthenticatedProblemWorkspace problemId={problem.id} />
    </main>
  );
}
