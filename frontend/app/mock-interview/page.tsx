"use client";

import { Suspense } from "react";
import MockInterviewPage from "@/components/MockInterviewPage";
import AuthGate from "@/components/AuthGate";

export const dynamic = "force-dynamic";

export default function Page() {
  return (
    <AuthGate>
      {(user) => (
        <Suspense fallback={null}>
          <MockInterviewPage userId={user.id} />
        </Suspense>
      )}
    </AuthGate>
  );
}
