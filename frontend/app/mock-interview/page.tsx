import { Suspense } from "react";
import MockInterviewPage from "@/components/MockInterviewPage";

export default function Page() {
  return (
    <Suspense fallback={null}>
      <MockInterviewPage />
    </Suspense>
  );
}
