"use client";

import Link from "next/link";
import { ArrowRight, BookOpenCheck } from "lucide-react";

export default function KnowledgeTrainingEntry() {
  return (
    <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <BookOpenCheck className="w-5 h-5 text-primary" />
        <h3 className="text-sm font-semibold text-on-surface">后端知识训练</h3>
      </div>
      <p className="text-xs text-on-surface-variant leading-relaxed mb-4">
        复习 Java、JVM、Spring、MySQL、Redis 高频面试知识卡。
      </p>
      <Link
        href="/knowledge"
        className="inline-flex items-center gap-1.5 text-xs font-semibold text-primary hover:underline"
      >
        开始复习
        <ArrowRight className="w-3.5 h-3.5" />
      </Link>
    </div>
  );
}
