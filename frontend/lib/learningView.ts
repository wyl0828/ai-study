import type { TrainingPlan, TrainingPlanItem } from "./types";
import type {
  AggregatedMistakeCard,
  AggregatedWeakness,
} from "./dashboardAggregation";
import { problemTitle, trainingPlanText } from "./i18n";

export {
  groupMistakeCards,
  groupWeaknesses,
  normalizeErrorType,
  normalizeKnowledgePoint,
} from "./dashboardAggregation";
export type {
  AggregatedMistakeCard,
  AggregatedMistakeRecord,
  AggregatedWeakness,
  CanonicalKnowledgePoint,
} from "./dashboardAggregation";

export interface TrainingPlanItemAction {
  href: string;
  label: "去做题" | "去复习";
}

export function selectTodayTrainingItem(plan: TrainingPlan | null): TrainingPlanItem | null {
  if (!plan || plan.items.length === 0) {
    return null;
  }
  const pending = plan.items.find((item) => item.status.toUpperCase() === "PENDING");
  if (pending) {
    return pending;
  }
  return (
    plan.items.find((item) => {
      const status = item.status;
      return status.toUpperCase() === "NEEDS_REVIEW" || status.toUpperCase() === "RETRY";
    }) ?? null
  );
}

export function trainingPlanItemTitle(item: TrainingPlanItem): string {
  if (item.itemType === "KNOWLEDGE_CARD") {
    return item.knowledgeCardTitle || "后端知识卡片";
  }
  return problemTitle(item.problemTitle);
}

export function trainingPlanItemPrefix(item: TrainingPlanItem): string {
  return item.itemType === "KNOWLEDGE_CARD" ? "知识卡片" : "算法题";
}

export function trainingPlanItemHref(item: TrainingPlanItem): string {
  if (item.itemType === "KNOWLEDGE_CARD") {
    return item.knowledgeCardId ? `/knowledge?cardId=${item.knowledgeCardId}` : "/knowledge";
  }
  return `/problem/${item.problemId || inferredProblemId(item)}`;
}

export function trainingPlanItemAction(item: TrainingPlanItem): TrainingPlanItemAction {
  return {
    href: trainingPlanItemHref(item),
    label: item.itemType === "PROBLEM" ? "去做题" : "去复习",
  };
}

export function buildDashboardCoachAdvice({
  weaknesses,
  mistakes,
  trainingPlan,
}: {
  weaknesses: AggregatedWeakness[];
  mistakes: AggregatedMistakeCard[];
  trainingPlan: TrainingPlan | null;
}): string {
  const todayItem = selectTodayTrainingItem(trainingPlan);
  const topWeakness = weaknesses[0];
  const recentMistake = mistakes[0];

  if (todayItem) {
    const focus = trainingPlanText(todayItem.reviewFocus || todayItem.reason);
    const weakText = topWeakness
      ? `这次主要问题集中在${topWeakness.canonicalName}和${topWeakness.errorType}。`
      : `今天先完成「${trainingPlanItemTitle(todayItem)}」，把当前训练链路跑完整。`;
    const mistakeText = recentMistake
      ? `完成后回看「${recentMistake.problemTitle}」错题，确认${recentMistake.reviewPoint}是否已经说清楚。`
      : "完成后再提交一次，观察 AI 诊断是否还指出同类问题。";
    return `${weakText}建议先完成「${trainingPlanItemTitle(todayItem)}」，重点复盘${focus}。${mistakeText}`;
  }

  if (topWeakness) {
    return `当前最高优先级是${topWeakness.canonicalName}，已累计错误 ${topWeakness.wrongCount} 次。建议先复盘对应错题，再重新生成训练计划安排下一轮练习。`;
  }

  return "还没有学习数据，去做第一道题并触发 AI 诊断吧。";
}

function inferredProblemId(item: TrainingPlanItem) {
  const text = [
    item.problemTitle,
    item.knowledgePoint,
    item.reason,
    item.reviewFocus,
  ]
    .filter(Boolean)
    .join(" ")
    .toLowerCase();

  if (/two sum|两数之和|hashmap|哈希|hash map/.test(text)) return 1;
  if (/reverse|linked|链表|反转链表/.test(text)) return 206;
  if (/stock|股票|买卖股票|贪心/.test(text)) return 121;
  return 1;
}
