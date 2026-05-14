import type { MistakeCard, UserWeakness } from "./types";
import { errorTypeName, learningText, problemTitle } from "./i18n";

export interface MistakeCardView {
  id: number;
  problemId: number;
  problemTitle: string;
  errorType: string;
  knowledgePoint: string;
  errorReason: string;
  codeBehavior: string;
  aiDiagnosis: string;
  reviewPoint: string;
  nextTrainingAdvice: string;
  count: number;
  repeatCount: number;
  lastSeenAt?: string | null;
  status?: string | null;
}

export function knowledgePointGroup(text: string): string {
  const source = `${text} ${learningText(text)}`.toLowerCase();

  if (source.includes("two sum") || source.includes("两数之和")) {
    return "HashMap 在两数之和中的应用";
  }
  if (
    source.includes("duplicate") ||
    source.includes("重复") ||
    source.includes("冲突") ||
    source.includes("distinct indices") ||
    source.includes("self-match") ||
    source.includes("自匹配")
  ) {
    return "HashMap 冲突处理";
  }
  if (source.includes("traversal") || source.includes("遍历")) {
    return "HashMap 遍历逻辑";
  }
  if (source.includes("lookup") || source.includes("查找") || source.includes("hashmap")) {
    return "HashMap 基础查找";
  }
  return learningText(text);
}

export function aggregateWeaknesses(weaknesses: UserWeakness[]): UserWeakness[] {
  const groups = new Map<string, UserWeakness>();

  weaknesses.forEach((weakness) => {
    const groupName = knowledgePointGroup(weakness.knowledgePoint);
    const existing = groups.get(groupName);

    if (!existing) {
      groups.set(groupName, {
        ...weakness,
        knowledgePoint: groupName,
        weaknessScore: roundScore(weakness.weaknessScore),
      });
      return;
    }

    const previousScore = existing.weaknessScore;
    existing.wrongCount += weakness.wrongCount;
    existing.weaknessScore = roundScore(existing.weaknessScore + weakness.weaknessScore);
    if (weakness.weaknessScore > previousScore) {
      existing.errorType = weakness.errorType;
    }
  });

  return Array.from(groups.values()).sort((a, b) => b.weaknessScore - a.weaknessScore);
}

export function buildMistakeCardViews(mistakes: MistakeCard[]): MistakeCardView[] {
  const groups = new Map<string, MistakeCardView>();

  mistakes.forEach((mistake) => {
    const reviewPoint = knowledgePointGroup(mistake.knowledgePoint);
    const key = `${mistake.problemId}-${reviewPoint}-${errorTypeName(mistake.errorType)}`;
    const current = toMistakeCardView(mistake, reviewPoint);
    const existing = groups.get(key);

    if (!existing) {
      groups.set(key, current);
      return;
    }

    existing.count += 1;
    existing.repeatCount += current.repeatCount;
  });

  return Array.from(groups.values());
}

function toMistakeCardView(mistake: MistakeCard, reviewPoint: string): MistakeCardView {
  const combined = `${mistake.knowledgePoint} ${mistake.mistakeSummary} ${mistake.correctIdea}`;
  const text = combined.toLowerCase();
  const isSelfMatch =
    text.includes("self-match") ||
    text.includes("self-pair") ||
    text.includes("distinct indices") ||
    text.includes("adding current");
  const isPlaceholder =
    text.includes("placeholder") ||
    text.includes("no two sum") ||
    text.includes("no solution") ||
    text.includes("hardcoded");

  return {
    id: mistake.id,
    problemId: mistake.problemId,
    problemTitle: problemTitle(mistake.problemTitle),
    errorType: errorTypeName(mistake.errorType),
    knowledgePoint: reviewPoint,
    errorReason: reasonByErrorType(mistake.errorType, reviewPoint),
    codeBehavior: codeBehavior(mistake, isSelfMatch, isPlaceholder),
    aiDiagnosis: aiDiagnosis(mistake, isSelfMatch, isPlaceholder),
    reviewPoint,
    nextTrainingAdvice: nextTrainingAdvice(reviewPoint, isSelfMatch, isPlaceholder),
    count: 1,
    repeatCount: mistake.repeatCount || 1,
    lastSeenAt: mistake.lastSeenAt,
    status: mistake.status || "OPEN",
  };
}

function reasonByErrorType(errorType: string, reviewPoint: string): string {
  const typeName = errorTypeName(errorType);
  if (typeName === "逻辑错误") {
    return `${reviewPoint}存在判断顺序问题。`;
  }
  if (typeName === "算法思路错误") {
    return `尚未形成可通过用例的${reviewPoint}解题流程。`;
  }
  if (typeName === "边界条件错误") {
    return `${reviewPoint}的边界条件覆盖不足。`;
  }
  return `${typeName} 需要结合失败用例复盘。`;
}

function codeBehavior(
  mistake: MistakeCard,
  isSelfMatch: boolean,
  isPlaceholder: boolean
): string {
  if (isSelfMatch) {
    return "先插入当前元素再查找互补值，可能把当前下标当作答案。";
  }
  if (isPlaceholder) {
    return "没有建立 HashMap 查找流程，输出仍停留在占位逻辑。";
  }
  return learningText(mistake.mistakeSummary);
}

function aiDiagnosis(
  mistake: MistakeCard,
  isSelfMatch: boolean,
  isPlaceholder: boolean
): string {
  if (isSelfMatch) {
    return "未处理互补值已存在于 HashMap 时的判断顺序，导致同一元素可能被重复使用。";
  }
  if (isPlaceholder) {
    return "核心问题是算法流程缺失，需要先构建互补值查找，再返回两个下标。";
  }
  return learningText(mistake.correctIdea || mistake.mistakeSummary);
}

function nextTrainingAdvice(
  reviewPoint: string,
  isSelfMatch: boolean,
  isPlaceholder: boolean
): string {
  if (isSelfMatch) {
    return "重做两数之和，先判断目标差值是否存在，再写入 HashMap。";
  }
  if (isPlaceholder) {
    return "先用伪代码写出遍历、查找、插入、返回四步，再补 Java 实现。";
  }
  if (reviewPoint.includes("冲突")) {
    return "用重复值和重复下标用例验证 HashMap 的匹配规则。";
  }
  if (reviewPoint.includes("遍历")) {
    return "复盘数组遍历顺序，明确每一轮 HashMap 中已有的数据。";
  }
  return "复习 HashMap 插入顺序与键存在性判断逻辑。";
}

function roundScore(score: number): number {
  return Math.round(score * 10) / 10;
}
