"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogOut, Terminal } from "lucide-react";
import { useEffect, useState } from "react";
import { getStoredUser, getAuthToken, clearAuthSession } from "@/lib/auth";
import type { AuthUser } from "@/lib/types";

const links = [
  { href: "/", label: "题目" },
  { href: "/knowledge", label: "知识训练" },
  { href: "/mock-interview", label: "模拟面试" },
  { href: "/dashboard", label: "学习中心" },
];

export default function Navbar() {
  const pathname = usePathname();
  const [user, setUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    const token = getAuthToken();
    if (token) {
      setUser(getStoredUser());
    }
  }, [pathname]);

  const handleLogout = () => {
    clearAuthSession();
    window.location.href = "/login";
  };

  const isActive = (href: string) =>
    href === "/" ? pathname === "/" : pathname.startsWith(href);

  return (
    <header className="sticky top-0 z-50 w-full border-b border-white/10 bg-[#0b1220] text-white shadow-sm">
      <div className="coach-shell flex h-14 items-center justify-between">
        <div className="flex min-w-0 items-center gap-6">
          <Link href="/" className="flex min-w-0 items-center gap-2 text-white font-bold text-lg">
            <Terminal className="h-[22px] w-[22px] shrink-0" />
            <span className="truncate">AI 面试教练</span>
          </Link>
          <span className="hidden rounded-full border border-teal-300/20 bg-teal-300/10 px-2.5 py-1 text-xs font-semibold text-teal-100 lg:inline-flex">
            训练控制台
          </span>
          <nav className="hidden md:flex gap-2 ml-4" aria-label="桌面端主导航">
            {links.map((link) => {
              const active = isActive(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={
                    active
                      ? "rounded-full bg-white px-3 py-1.5 text-sm font-semibold text-[#0b1220]"
                      : "rounded-full px-3 py-1.5 text-sm font-medium text-slate-300 transition-colors hover:bg-white/10 hover:text-white"
                  }
                >
                  {link.label}
                </Link>
              );
            })}
          </nav>
        </div>
        <div className="flex shrink-0 items-center gap-3">
          {pathname.startsWith("/mock-interview") && (
            <div className="hidden items-center gap-2 rounded-full border border-primary/20 bg-primary-container/20 px-4 py-2 text-sm font-medium text-primary md:flex">
              <span className="h-2 w-2 rounded-full bg-primary" />
              面试进行中
            </div>
          )}
          {user ? (
            <>
              <span className="hidden text-sm text-slate-300 md:inline">
                {user.username}
              </span>
              <button
                onClick={handleLogout}
                className="inline-flex h-8 w-8 items-center justify-center rounded-md text-slate-300 transition hover:bg-white/10 hover:text-white"
                type="button"
                aria-label="退出登录"
                title="退出登录"
              >
                <LogOut className="w-5 h-5" />
              </button>
            </>
          ) : (
            <Link
              href="/login"
              className="inline-flex items-center justify-center rounded-md bg-teal-400 px-3 py-1.5 text-sm font-semibold text-[#062322] transition hover:bg-teal-300"
            >
              登录
            </Link>
          )}
        </div>
      </div>
      <nav
        className="coach-shell md:hidden flex gap-2 overflow-x-auto pb-2"
        aria-label="移动端主导航"
      >
        {links.map((link) => {
          const active = isActive(link.href);
          return (
            <Link
              key={link.href}
              href={link.href}
              className={
                active
                  ? "shrink-0 rounded-full bg-white px-3 py-1.5 text-xs font-semibold text-[#0b1220]"
                  : "shrink-0 rounded-full border border-white/15 bg-white/5 px-3 py-1.5 text-xs font-semibold text-slate-300"
              }
            >
              {link.label}
            </Link>
          );
        })}
      </nav>
    </header>
  );
}
