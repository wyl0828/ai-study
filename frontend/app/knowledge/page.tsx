import KnowledgeTrainingClient from "@/components/KnowledgeTrainingClient";
import { knowledgeApi } from "@/lib/api";

export default async function KnowledgePage() {
  const [categoriesResponse, cardsResponse] = await Promise.all([
    knowledgeApi.categories(),
    knowledgeApi.cards(),
  ]);

  return (
    <KnowledgeTrainingClient
      initialCategories={categoriesResponse.data}
      initialCards={cardsResponse.data}
    />
  );
}
