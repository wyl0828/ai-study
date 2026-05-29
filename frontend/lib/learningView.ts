import type {
  MockInterviewTrace,
  MockInterviewTrend,
  TrainingPlan,
  TrainingPlanActivity,
  TrainingPlanItem,
  TrainingPlanTrace,
} from "./types";
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
  label: string;
}

export interface DashboardNextAction {
  title: string;
  description: string;
  href: string;
  label: string;
  priority: "HIGH" | "MEDIUM";
  tone: "primary" | "warning" | "neutral";
  sourceLabel: string;
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

export function trainingPlanSourceLabel(sourceType?: string | null): string {
  switch (sourceType) {
    case "SUBMISSION_FAILED":
      return "失败提交";
    case "RAG_KNOWLEDGE_CARD":
      return "RAG 知识卡";
    case "USER_WEAKNESS":
      return "薄弱点";
    case "KNOWLEDGE_CARD_REVIEW":
      return "知识卡复习";
    case "MOCK_INTERVIEW_REPORT":
      return "模拟面试报告";
    case "SELF_TEST":
      return "知识自测";
    case "GENERAL_REVIEW":
      return "通用复盘";
    default:
      return sourceType || "学习记录";
  }
}

export function trainingPlanSourceText(item: TrainingPlanItem): string {
  const label = trainingPlanSourceLabel(item.sourceType);
  const summary = trainingPlanText(item.sourceSummary);
  if (summary && summary !== label) {
    return `${label}：${summary}`;
  }
  return label;
}

export function trainingPlanActivitySourceText(activity: TrainingPlanActivity): string {
  const label = trainingPlanSourceLabel(activity.sourceType);
  const summary = trainingPlanText(activity.sourceSummary);
  if (summary && summary !== label) {
    return `${label}：${summary}`;
  }
  return label;
}

export function trainingPlanItemHref(item: TrainingPlanItem): string {
  if (item.targetHref) {
    return item.targetHref;
  }
  if (item.itemType === "KNOWLEDGE_CARD") {
    return item.knowledgeCardId ? `/knowledge?cardId=${item.knowledgeCardId}` : "/knowledge";
  }
  return `/problem/${item.problemId || inferredProblemId(item)}`;
}

export function trainingPlanItemAction(item: TrainingPlanItem): TrainingPlanItemAction {
  return {
    href: trainingPlanItemHref(item),
    label: item.targetLabel || (item.itemType === "PROBLEM" ? "去做题" : "去复习"),
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

export function buildDashboardNextActions({
  trainingPlan,
  trainingPlanTrace,
  mockInterviewTrace,
  mockInterviewTrends,
}: {
  trainingPlan: TrainingPlan | null;
  trainingPlanTrace: TrainingPlanTrace | null;
  mockInterviewTrace: MockInterviewTrace | null;
  mockInterviewTrends: MockInterviewTrend[];
}): DashboardNextAction[] {
  const actions: DashboardNextAction[] = [];
  const traceNextItem = trainingPlanTrace?.nextItem;
  const todayItem = traceNextItem || selectTodayTrainingItem(trainingPlan);

  if (
    mockInterviewTrace?.latestSessionId
    && mockInterviewTrace.latestSessionStatus
    && mockInterviewTrace.latestSessionStatus !== "REPORTED"
  ) {
    actions.push({
      title: "继续最近模拟面试",
      description: mockInterviewTrace.nextActionReason
        || mockInterviewTrace.nextAction
        || "最近还有一场模拟面试未完成，先把主问题和追问回答完整。",
      href: mockInterviewTrace.nextTargetHref || `/mock-interview?sessionId=${mockInterviewTrace.latestSessionId}`,
      label: mockInterviewTrace.nextTargetLabel || "继续面试",
      priority: normalizeActionPriority(mockInterviewTrace.nextActionPriority),
      tone: "primary",
      sourceLabel: "模拟面试",
    });
  }

  if (todayItem) {
    const action = trainingPlanItemAction(todayItem);
    actions.push({
      title: `先完成：${trainingPlanItemTitle(todayItem)}`,
      description: trainingPlanTrace?.nextActionReason || `来自${trainingPlanSourceText(todayItem)}，今天重点练${trainingPlanText(
        todayItem.reviewFocus || todayItem.reason
      )}。`,
      href: trainingPlanTrace?.nextTargetHref || action.href,
      label: trainingPlanTrace?.nextTargetLabel || action.label,
      priority: normalizeActionPriority(trainingPlanTrace?.nextActionPriority),
      tone: "primary",
      sourceLabel: "训练计划",
    });
  } else if (trainingPlanTrace?.planId && trainingPlanTrace.pendingCount === 0) {
    actions.push({
      title: "当前训练计划已清空待办",
      description: trainingPlanTrace.nextActionReason
        || trainingPlanTrace.progressSummary
        || "先复盘最近错题，再重新生成下一轮 3 天训练安排。",
      href: trainingPlanTrace.nextTargetHref || "/dashboard",
      label: trainingPlanTrace.nextTargetLabel || "查看计划",
      priority: normalizeActionPriority(trainingPlanTrace.nextActionPriority),
      tone: "neutral",
      sourceLabel: "训练计划",
    });
  }

  const weakestInterview = mockInterviewTrends.find((trend) => trend.latestScore < 70)
    || mockInterviewTrends.find((trend) => trend.deltaScore < 0);
  if (weakestInterview) {
    actions.push({
      title: `复盘面试卡点：${weakestInterview.knowledgePoint}`,
      description: weakestInterview.latestIssue
        ? `最近卡在「${weakestInterview.latestIssue}」，建议回到知识卡重新组织答案。`
        : `最近得分 ${weakestInterview.latestScore}，建议回到知识卡补齐表达。`,
      href: `/knowledge?cardId=${weakestInterview.knowledgeCardId}`,
      label: "去复盘",
      priority: "MEDIUM",
      tone: "warning",
      sourceLabel: "面试趋势",
    });
  } else if (mockInterviewTrace?.recommendedCardIds?.length) {
    const cardId = mockInterviewTrace.recommendedCardIds[0];
    const linkText = mockInterviewTrace.reportTrainingPlanLinked
      ? `报告已接入 ${mockInterviewTrace.trainingPlanItemCount} 个训练项。`
      : "报告推荐暂未接入训练计划。";
    actions.push({
      title: "复盘最近面试推荐知识卡",
      description: mockInterviewTrace.nextActionReason || (mockInterviewTrace.latestWeaknessTags.length
        ? `${linkText}最近报告标记了${mockInterviewTrace.latestWeaknessTags.slice(0, 2).join("、")}，先回到推荐知识卡补齐答案。`
        : `${linkText}先回到卡片整理一版 1 分钟回答。`),
      href: mockInterviewTrace.nextTargetHref || `/knowledge?cardId=${cardId}`,
      label: mockInterviewTrace.nextTargetLabel || "去复盘",
      priority: normalizeActionPriority(mockInterviewTrace.nextActionPriority),
      tone: "warning",
      sourceLabel: "模拟面试报告",
    });
  } else if (
    mockInterviewTrace?.latestSessionId
    && mockInterviewTrace.reportedSessionCount > 0
    && mockInterviewTrace.latestSessionStatus === "REPORTED"
  ) {
    const retestHref = `/mock-interview?category=${mockInterviewTrace.latestCategory || "SPRING"}`;
    actions.push({
      title: "查看最近模拟面试报告",
      description: mockInterviewTrace.nextActionReason
        || `报告已沉淀 ${mockInterviewTrace.trainingPlanItemCount} 个训练项，可继续追踪面试复盘闭环。`,
      href: mockInterviewTrace.nextTargetHref || retestHref,
      label: mockInterviewTrace.nextTargetLabel || "查看报告",
      priority: normalizeActionPriority(mockInterviewTrace.nextActionPriority),
      tone: "neutral",
      sourceLabel: "模拟面试报告",
    });
  } else if (!mockInterviewTrace || mockInterviewTrace.sessionCount === 0) {
    actions.push({
      title: "补一场后端知识模拟面试",
      description: mockInterviewTrace?.nextActionReason
        || "用知识卡进行一轮主问题和追问，生成报告后会写入弱点事件和训练计划。",
      href: mockInterviewTrace?.nextTargetHref || "/mock-interview",
      label: mockInterviewTrace?.nextTargetLabel || "开始面试",
      priority: normalizeActionPriority(mockInterviewTrace?.nextActionPriority),
      tone: "neutral",
      sourceLabel: "模拟面试",
    });
  }

  if (actions.length === 0) {
    actions.push({
      title: "从第一道 demo 题开始",
      description: "提交一次 Two Sum 错误代码，让诊断、记忆和训练计划先跑通。",
      href: "/problem/1",
      label: "去做题",
      priority: "HIGH",
      tone: "primary",
      sourceLabel: "默认启动",
    });
  }

  return sortDashboardNextActions(actions).slice(0, 3);
}

function normalizeActionPriority(priority?: string | null): DashboardNextAction["priority"] {
  return priority === "MEDIUM" ? "MEDIUM" : "HIGH";
}

function sortDashboardNextActions(actions: DashboardNextAction[]): DashboardNextAction[] {
  const priorityWeight: Record<DashboardNextAction["priority"], number> = {
    HIGH: 0,
    MEDIUM: 1,
  };
  return actions
    .map((action, index) => ({ action, index }))
    .sort((left, right) => {
      const priorityDiff = priorityWeight[left.action.priority] - priorityWeight[right.action.priority];
      return priorityDiff === 0 ? left.index - right.index : priorityDiff;
    })
    .map((item) => item.action);
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
