"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, CircleUserRound, Terminal } from "lucide-react";

const links = [
  { href: "/", label: "题目" },
  { href: "/knowledge", label: "知识训练" },
  { href: "/mock-interview", label: "模拟面试" },
  { href: "/dashboard", label: "学习中心" },
];

export default function Navbar() {
  const pathname = usePathname();

  return (
    <header className="bg-surface/80 backdrop-blur-md sticky top-0 z-50 border-b border-outline-variant/30 flex justify-between items-center w-full px-6 h-14 shadow-sm">
      <div className="flex items-center gap-6">
        <Link href="/" className="flex items-center gap-2 text-primary font-bold text-lg">
          <Terminal className="w-[22px] h-[22px]" />
          AI 面试教练
        </Link>
        <nav className="hidden md:flex gap-6 ml-8">
          {links.map((link) => {
            const active =
              link.href === "/"
                ? pathname === "/"
                : pathname.startsWith(link.href);
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
      <div className="flex items-center gap-3">
        {pathname.startsWith("/mock-interview") && (
          <div className="hidden items-center gap-2 rounded-full border border-primary/20 bg-primary-container/20 px-4 py-2 text-sm font-medium text-primary md:flex">
            <span className="h-2 w-2 rounded-full bg-primary" />
            面试进行中
          </div>
        )}
        <button
          className="p-1.5 rounded-full hover:bg-surface-variant text-on-surface-variant transition-colors"
          type="button"
          aria-label="通知"
        >
          <Bell className="w-5 h-5" />
        </button>
        <button
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full text-on-surface-variant transition-colors hover:bg-surface-variant hover:text-primary"
          type="button"
          aria-label="用户中心"
        >
          <CircleUserRound className="h-6 w-6" />
        </button>
      </div>
    </header>
  );
}
