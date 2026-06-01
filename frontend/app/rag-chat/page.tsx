"use client";

import RagChatPage from "@/components/RagChatPage";
import AuthGate from "@/components/AuthGate";

export const dynamic = "force-dynamic";

export default function Page() {
  return (
    <AuthGate>
      {(user) => <RagChatPage userId={user.id} />}
    </AuthGate>
  );
}
