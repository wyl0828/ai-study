import { Suspense } from "react";
import KnowledgeTrainingPage from "@/components/KnowledgeTrainingPage";

export default function KnowledgePage() {
  return (
    <Suspense
      fallback={
        <main className="mx-auto max-w-6xl px-6 py-8 text-sm text-on-surface-variant">
          正在加载知识训练...
        </main>
      }
    >
      <KnowledgeTrainingPage />
    </Suspense>
  );
}
