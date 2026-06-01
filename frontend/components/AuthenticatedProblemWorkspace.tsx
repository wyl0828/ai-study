"use client";

import AuthGate from "@/components/AuthGate";
import ProblemWorkspace from "@/components/ProblemWorkspace";

interface AuthenticatedProblemWorkspaceProps {
  problemId: number;
}

export default function AuthenticatedProblemWorkspace({
  problemId,
}: AuthenticatedProblemWorkspaceProps) {
  return (
    <AuthGate>
      {(user) => <ProblemWorkspace problemId={problemId} userId={user.id} />}
    </AuthGate>
  );
}
