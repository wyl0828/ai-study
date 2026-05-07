"use client";

import { Lightbulb } from "lucide-react";
import {
  mockWeaknesses,
  mockMistakes,
  mockTrainingPlan,
  mockSubmissionHistory,
  mockStats,
} from "@/lib/mock";
import WeaknessList from "@/components/WeaknessList";
import SubmissionHistory from "@/components/SubmissionHistory";
import MistakeCards from "@/components/MistakeCards";
import TrainingPlan from "@/components/TrainingPlan";

export default function DashboardPage() {
  return (
    <div className="max-w-6xl mx-auto px-6 py-8">
      {/* 页面标题 */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-on-surface tracking-tight">学习中心</h1>
        <p className="text-sm text-on-surface-variant mt-1">
          追踪你的薄弱知识点、查看训练计划、回顾错题
        </p>
      </div>

      {/* 统计概览卡片 */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">总提交次数</div>
          <div className="text-2xl font-bold text-on-surface">{mockStats.totalSubmissions}</div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">通过题目</div>
          <div className="text-2xl font-bold text-emerald-600">{mockStats.passedProblems}</div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">薄弱知识点</div>
          <div className="text-2xl font-bold text-amber-600">{mockStats.weakPoints}</div>
        </div>
        <div className="bg-surface-container-lowest border border-outline-variant/30 rounded-xl p-4">
          <div className="text-xs text-on-surface-variant mb-1">错题数量</div>
          <div className="text-2xl font-bold text-error">{mockStats.mistakeCount}</div>
        </div>
      </div>

      {/* 两栏布局 */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* 左栏 */}
        <div className="lg:col-span-7 space-y-8">
          <WeaknessList weaknesses={mockWeaknesses} />
          <SubmissionHistory submissions={mockSubmissionHistory} />
          <MistakeCards mistakes={mockMistakes} />
        </div>

        {/* 右栏 */}
        <aside className="lg:col-span-5">
          <div className="sticky top-20 space-y-6">
            <TrainingPlan plan={mockTrainingPlan} />

            {/* AI 教练建议 */}
            <div className="bg-primary/5 border border-primary/20 rounded-xl p-5">
              <div className="flex items-center gap-2 mb-3">
                <Lightbulb className="w-5 h-5 text-primary" />
                <h3 className="text-sm font-semibold text-on-surface">AI 教练建议</h3>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                你的 HashMap 和链表基础较弱，建议先完成第 1 天和第 2 天训练。每天完成 2 道题并理解错误原因，比泛泛刷 10 道题更有效。完成后系统会自动更新你的薄弱点画像。
              </p>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
