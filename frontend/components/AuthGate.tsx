"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/lib/api";
import { getStoredUser, getAuthToken, saveAuthSession } from "@/lib/auth";
import type { AuthUser } from "@/lib/types";

interface AuthGateProps {
  children: (user: AuthUser) => React.ReactNode;
}

export default function AuthGate({ children }: AuthGateProps) {
  const router = useRouter();
  const [user, setUser] = useState<AuthUser | null>(() => getStoredUser());
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    const token = getAuthToken();
    if (!token) {
      router.replace("/login");
      return;
    }
    authApi
      .me()
      .then((response) => {
        saveAuthSession(token, response.data);
        setUser(response.data);
      })
      .catch(() => {
        router.replace("/login");
      })
      .finally(() => setChecking(false));
  }, [router]);

  if (checking || !user) {
    return <main className="px-6 py-10 text-sm text-on-surface-muted">正在确认登录状态...</main>;
  }

  return <>{children(user)}</>;
}
