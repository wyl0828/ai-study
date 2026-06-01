"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { authApi, formatApiError } from "@/lib/api";
import { saveAuthSession } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const response =
        mode === "login"
          ? await authApi.login(username, password)
          : await authApi.register(username, password);
      saveAuthSession(response.data.token, response.data.user);
      router.push("/");
    } catch (err) {
      setError(formatApiError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-6 py-12">
      <section className="rounded-lg border border-border bg-surface p-6 shadow-sm">
        <h1 className="text-xl font-semibold text-on-surface">
          {mode === "login" ? "登录 AI 面试教练" : "创建测试账号"}
        </h1>
        <form onSubmit={submit} className="mt-6 space-y-4">
          <label className="block text-sm">
            <span className="text-on-surface-muted">用户名</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
              autoComplete="username"
            />
          </label>
          <label className="block text-sm">
            <span className="text-on-surface-muted">密码</span>
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="mt-1 w-full rounded-md border border-border bg-background px-3 py-2"
              autoComplete={mode === "login" ? "current-password" : "new-password"}
            />
          </label>
          {error && <p className="text-sm text-error">{error}</p>}
          <button
            type="submit"
            disabled={loading}
            className="w-full rounded-md bg-primary px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
          >
            {loading ? "处理中..." : mode === "login" ? "登录" : "注册并登录"}
          </button>
        </form>
        <button
          type="button"
          onClick={() => setMode(mode === "login" ? "register" : "login")}
          className="mt-4 text-sm text-primary"
        >
          {mode === "login" ? "没有账号？创建一个测试账号" : "已有账号？返回登录"}
        </button>
      </section>
    </main>
  );
}
