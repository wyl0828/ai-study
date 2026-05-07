"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Terminal, Bell } from "lucide-react";

const links = [
  { href: "/", label: "题目" },
  { href: "/dashboard", label: "仪表盘" },
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
        <button className="p-1.5 rounded-full hover:bg-surface-variant text-on-surface-variant transition-colors">
          <Bell className="w-5 h-5" />
        </button>
        <div className="w-8 h-8 rounded-full bg-primary text-on-primary flex items-center justify-center text-sm font-medium cursor-pointer">
          D
        </div>
      </div>
    </header>
  );
}
