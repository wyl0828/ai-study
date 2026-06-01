"use client";

import { Suspense } from "react";
import KnowledgeTrainingPage from "@/components/KnowledgeTrainingPage";
import AuthGate from "@/components/AuthGate";

export const dynamic = "force-dynamic";

export default function KnowledgePage() {
  return (
    <AuthGate>
      {(user) => (
        <Suspense
          fallback={
            <main className="mx-auto max-w-6xl px-6 py-8 text-sm text-on-surface-variant">
              正在加载知识训练...
            </main>
          }
        >
          <KnowledgeTrainingPage userId={user.id} />
        </Suspense>
      )}
    </AuthGate>
  );
}
