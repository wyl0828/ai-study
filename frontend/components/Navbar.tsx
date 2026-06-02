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
    <header className="sticky top-0 z-50 w-full border-b border-outline-variant/60 bg-surface-container-lowest/95 shadow-sm backdrop-blur-md">
      <div className="coach-shell flex h-14 items-center justify-between">
        <div className="flex min-w-0 items-center gap-6">
          <Link href="/" className="flex min-w-0 items-center gap-2 text-primary font-bold text-lg">
            <Terminal className="h-[22px] w-[22px] shrink-0" />
            <span className="truncate">AI 面试教练</span>
          </Link>
          <nav className="hidden md:flex gap-6 ml-8" aria-label="桌面端主导航">
            {links.map((link) => {
              const active = isActive(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={
                    active
                      ? "text-primary border-b-2 border-primary pb-0.5 text-sm font-semibold"
                      : "text-on-surface-variant hover:text-primary transition-colors text-sm font-medium"
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
              <span className="hidden text-sm text-on-surface-variant md:inline">
                {user.username}
              </span>
              <button
                onClick={handleLogout}
                className="coach-icon-button"
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
              className="coach-primary-button px-3 py-1.5"
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
                  ? "shrink-0 rounded-full bg-primary px-3 py-1.5 text-xs font-semibold text-on-primary"
                  : "coach-pill shrink-0 px-3 py-1.5"
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
