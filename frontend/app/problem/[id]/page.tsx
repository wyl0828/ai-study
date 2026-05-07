import { problemApi } from "@/lib/api";
import ProblemDescription from "@/components/ProblemDescription";
import ProblemWorkspace from "@/components/ProblemWorkspace";

interface ProblemPageProps {
  params: { id: string };
}

export default async function ProblemPage({ params }: ProblemPageProps) {
  const [{ data: problem }, { data: template }] = await Promise.all([
    problemApi.detail(Number(params.id)),
    problemApi.template(Number(params.id)),
  ]);

  return (
    <main className="h-[calc(100vh-56px)] flex overflow-hidden bg-background">
      <ProblemDescription problem={problem} />
      <ProblemWorkspace
        problemId={problem.id}
        defaultCode={template.templateCode}
      />
    </main>
  );
}
