const problemTitleMap: Record<string, string> = {
  "Two Sum": "两数之和",
  "Valid Anagram": "有效字母异位词",
  "Reverse Linked List": "反转链表",
  "Merge Two Sorted Lists": "合并两个有序链表",
  "Maximum Depth of Binary Tree": "二叉树的最大深度",
  "Binary Tree Level Order": "二叉树层序遍历",
  "Climbing Stairs": "爬楼梯",
  "Longest Increasing Subsequence": "最长递增子序列",
};

const knowledgePointMap: Record<string, string> = {
  HashMap: "HashMap",
  LinkedList: "链表",
  Tree: "二叉树",
  DynamicProgramming: "动态规划",
  "Array Traversal": "数组遍历",
  "HashMap Lookup": "HashMap 基础查找",
  "HashMap Lookup Order": "HashMap 基础查找",
  "Handling Duplicate Values in HashMap": "HashMap 冲突处理",
  "HashMap Lookup for Two Sum": "HashMap 在两数之和中的应用",
  "Handling duplicate indices in HashMap lookup": "HashMap 冲突处理",
  "HashMap Lookup in Array Traversal": "HashMap 遍历逻辑",
  "String Counting": "字符计数",
  "Linked List Pointer": "链表指针",
  "Binary Tree Recursion": "二叉树递归",
  "Binary Tree BFS": "二叉树广度优先遍历",
  "Dynamic Programming State": "动态规划状态",
  "Binary Search Patience": "二分优化",
};

const categoryMap: Record<string, string> = {
  Array: "数组",
  HashMap: "HashMap",
  LinkedList: "链表",
  Tree: "二叉树",
  DynamicProgramming: "动态规划",
  Greedy: "贪心",
  SlidingWindow: "滑动窗口",
  TwoPointers: "双指针",
  Stack: "栈",
  BinarySearch: "二分查找",
  Graph: "图搜索",
};

const difficultyMap: Record<string, string> = {
  EASY: "简单",
  MEDIUM: "中等",
  HARD: "困难",
  Easy: "简单",
  Medium: "中等",
  Hard: "困难",
};

const descriptionMap: Record<string, string> = {
  "Given n integers and a target, return the indices of two numbers whose sum is target. Return -1 -1 if no pair exists.":
    "给定一个整数数组和一个目标值，请找出数组中和为目标值的两个元素下标。若不存在满足条件的组合，输出 -1 -1。",
  "Given two lowercase strings s and t, determine whether t is an anagram of s.":
    "给定两个只包含小写字母的字符串 s 和 t，判断 t 是否是 s 的字母异位词。",
  "Given a linked list, reverse it and print node values from new head to tail.":
    "给定一个单链表，请将链表反转，并从新的头节点开始依次输出节点值。",
  "Given two sorted integer lists, merge them into one sorted list and print the values.":
    "给定两个升序链表，请将它们合并为一个升序链表，并输出合并后的节点值。",
  "Given a binary tree in level-order form, compute its maximum depth. The token null means an empty node.":
    "给定一棵按层序表示的二叉树，计算它的最大深度。其中 null 表示空节点。",
  "Given a binary tree in level-order form, compute its maximum depth. The token null means an empty child.":
    "给定一棵按层序表示的二叉树，请计算它的最大深度。其中 null 表示空节点。",
  "Given a binary tree in level-order form, print each level from top to bottom.":
    "给定一棵按层序表示的二叉树，请从上到下按层输出每一层的节点值。",
  "You can climb 1 or 2 steps each move. Count how many distinct ways can reach the nth step.":
    "每次可以爬 1 个或 2 个台阶，计算到达第 n 阶共有多少种不同走法。",
  "Given an integer array, return the length of the longest strictly increasing subsequence.":
    "给定一个整数数组，返回其中最长严格递增子序列的长度。",
};

const formatMap: Record<string, string> = {
  "Line 1: n. Line 2: n integers. Line 3: target.":
    "第 1 行输入数组长度 n，第 2 行输入 n 个整数，第 3 行输入目标值 target。",
  "Print two indices separated by one space, or -1 -1.":
    "输出两个下标，用空格分隔；若不存在答案，输出 -1 -1。",
  "Line 1: s. Line 2: t.": "第 1 行输入字符串 s，第 2 行输入字符串 t。",
  "Print true or false.": "输出 true 或 false。",
  "Line 1: n. Line 2: n integers.":
    "第 1 行输入长度 n，第 2 行输入 n 个整数。",
  "Print reversed values separated by one space. Print an empty line for n = 0.":
    "输出反转后的节点值，用空格分隔；当 n = 0 时输出空行。",
  "Line 1: n. Line 2: n integers. Line 3: m. Line 4: m integers.":
    "第 1 行输入长度 n，第 2 行输入 n 个整数，第 3 行输入长度 m，第 4 行输入 m 个整数。",
  "Print merged values separated by one space.":
    "输出合并后的节点值，用空格分隔。",
  "Line 1: n. Line 2: n level-order tokens such as 3 9 20 null null 15 7.":
    "第 1 行输入节点数量 n，第 2 行输入 n 个层序节点，例如 3 9 20 null null 15 7。",
  "Print the maximum depth as an integer.":
    "输出最大深度，结果为一个整数。",
  "Line 1: n. Line 2: n level-order tokens. The token null means an empty child.":
    "第 1 行输入节点数量 n，第 2 行输入 n 个层序节点，其中 null 表示空节点。",
  "Print levels separated by semicolon. Values inside a level are separated by one space.":
    "按层输出节点值，层与层之间用分号分隔，同一层内用空格分隔。",
  "Line 1: n.": "第 1 行输入 n。",
  "Print the number of ways.": "输出不同走法数量。",
  "Print the LIS length.": "输出最长递增子序列的长度。",
};

const errorTypeMap: Record<string, string> = {
  LOGIC_ERROR: "逻辑错误",
  BOUNDARY_ERROR: "边界条件错误",
  SYNTAX_ERROR: "语法错误",
  ALGORITHM_ERROR: "算法思路错误",
  TIMEOUT: "超时",
  RUNTIME_ERROR: "运行时错误",
  SYSTEM_ERROR: "系统错误",
  ACCEPTED_REVIEW: "通过代码点评",
};

const agentStepMap: Record<string, string> = {
  PLANNING: "准备诊断上下文",
  CODE_EXECUTION: "重新执行代码",
  OBSERVATION: "分析测试结果",
  RAG_RETRIEVAL: "检索相关知识和历史错题",
  ERROR_CLASSIFICATION: "定位错误原因",
  CODE_REVIEW: "代码点评",
  MEMORY_UPDATE: "更新薄弱点记录",
  TRAINING_PLAN: "生成训练建议",
  COMPLETED: "诊断完成",
  FAILED: "诊断失败",
};

const trainingPhraseMap: Record<string, string> = {
  "3-day recovery plan": "3 天专项训练",
  "HashMap Lookup": "HashMap 基础查找",
  "HashMap Lookup Order": "HashMap 基础查找",
  "HashMap Lookup in Array Traversal": "HashMap 遍历逻辑",
  "Handling Duplicate Values in HashMap": "HashMap 冲突处理",
  "HashMap Lookup for Two Sum": "HashMap 在两数之和中的应用",
  "Handling duplicate indices in HashMap lookup": "HashMap 冲突处理",
  "Linked List Pointer": "链表指针",
  "Binary Tree Recursion": "二叉树递归",
  "Dynamic Programming State": "动态规划状态",
};

const trainingCopyMap: Record<string, string> = {
  "Focus on the failed knowledge point, one adjacent topic, and a retry of the original problem.":
    "围绕失败知识点、相邻题型和原题重做安排训练。",
  "Repeat the failed knowledge point while the mistake is fresh.":
    "趁错误记忆还清晰，先复盘本次失败的知识点。",
  "Explain why the failed case breaks the submitted idea.":
    "说明失败用例为什么会击穿当前思路。",
  "Practice one adjacent topic from the same problem category.":
    "练习同类题目中的相邻知识点。",
  "Compare the category pattern with the original failed approach.":
    "对比同类题型规律和原来的错误做法。",
  "Review the mistake card and retry the original problem.":
    "回顾错题卡后重新挑战原题。",
  "Write the invariant or boundary condition before coding.":
    "编码前先写出不变量或边界条件。",
  "Checks complement after adding current element, causing self-match.":
    "当前元素先写入 HashMap，随后查找互补值时可能匹配到自身。",
  "Code checks for needed value after adding current element, causing self-match error.":
    "代码先插入当前元素再查找目标差值，导致同一元素被重复使用。",
  "No solution implemented; output is hardcoded to -1 -1.":
    "未实现真实求解逻辑，结果被硬编码为 -1 -1。",
  "Learn to implement HashMap for efficient pair finding in arrays.":
    "需要掌握如何使用 HashMap 在数组中高效查找目标配对。",
  "Check for complement before adding current element to map.":
    "先判断互补值是否存在，再把当前元素写入 HashMap。",
  "Add current element to map only after checking for needed value to ensure distinct indices.":
    "先完成键存在性判断，再写入当前元素，确保两个下标不同。",
  "No Two Sum algorithm implemented; prints placeholder output.":
    "尚未实现两数之和查找逻辑，当前输出仍停留在占位结果。",
  "Learn and implement Two Sum algorithm with HashMap to store complements and return indices.":
    "复习两数之和的 HashMap 解法，用互补值查找返回正确下标。",
  "Code allows self-pairing by not checking index distinctness.":
    "代码没有校验两个下标必须不同，可能出现自配对。",
  "Add condition to verify map.get(need) != i before printing results.":
    "输出前校验命中的下标与当前下标不同，避免重复使用同一元素。",
  "Check complement before inserting into HashMap to ensure distinct indices":
    "先查找互补值，再写入当前元素，确保不会重复使用同一下标",
  "checking self-match": "检查自匹配问题",
  "placeholder output": "占位输出",
  "containsKey": "键存在性判断",
};

function replaceKnownPhrases(text: string | null | undefined, phrases: Record<string, string>): string {
  if (text == null) return "";
  return Object.entries(phrases)
    .sort(([a], [b]) => b.length - a.length)
    .reduce((value, [source, target]) => value.replaceAll(source, target), text)
    .replaceAll("Two Sum", "两数之和")
    .replaceAll("Lookup", "查找")
    .replaceAll("lookup", "查找")
    .replaceAll("Handling", "处理")
    .replaceAll("duplicate indices", "重复下标")
    .replaceAll("Duplicate Values", "重复值")
    .replaceAll("failed approach", "错误做法")
    .replaceAll("failed case", "失败用例")
    .replaceAll("failed knowledge point", "失败知识点")
    .replace(": ", "：");
}

export function problemTitle(text: string | null | undefined): string {
  if (text == null) return "";
  return problemTitleMap[text] || text;
}

export function knowledgePoint(text: string | null | undefined): string {
  if (text == null) return "";
  return replaceKnownPhrases(knowledgePointMap[text] || text, trainingPhraseMap);
}

export function categoryName(text: string | null | undefined): string {
  if (text == null) return "";
  return categoryMap[text] || text;
}

export function difficultyName(text: string | null | undefined): string {
  if (text == null) return "";
  return difficultyMap[text] || text;
}

export function problemDescription(text: string | null | undefined): string {
  if (text == null) return "";
  return descriptionMap[text] || text;
}

export function formatText(text: string | null | undefined): string {
  if (text == null) return "";
  return formatMap[text] || text;
}

export function errorTypeName(text: string | null | undefined): string {
  if (text == null) return "";
  return errorTypeMap[text] || text;
}

export function agentStepName(text: string | null | undefined): string {
  if (text == null) return "";
  return agentStepMap[text] || text;
}

export function trainingPlanTitle(text: string | null | undefined): string {
  return replaceKnownPhrases(text, trainingPhraseMap);
}

export function trainingPlanText(text: string | null | undefined): string {
  return replaceKnownPhrases(text, trainingCopyMap);
}

export function learningText(text: string | null | undefined): string {
  return replaceKnownPhrases(text, { ...trainingPhraseMap, ...trainingCopyMap });
}

export function diagnosisDisplay(
  diagnosis: string | null | undefined,
  specificError: string | null | undefined
): { diagnosisText: string; suggestionText: string } {
  return {
    diagnosisText: normalizeDiagnosisText(diagnosis ?? ""),
    suggestionText: normalizeSuggestionText(specificError ?? ""),
  };
}

function normalizeDiagnosisText(text: string): string {
  const cleaned = productText(learningText(text));
  if (isDefaultOutputText(cleaned)) {
    return "当前代码未实现本题核心逻辑，仅返回默认值，因此无法通过测试用例。";
  }
  return cleaned;
}

function normalizeSuggestionText(text: string): string {
  const cleaned = productText(learningText(text));
  if (isDefaultOutputText(cleaned)) {
    return "";
  }
  return cleaned;
}

function isDefaultOutputText(text: string): boolean {
  const lower = text.toLowerCase();
  return (
    lower.includes("no solution") ||
    lower.includes("hardcoded") ||
    lower.includes("-1 -1") ||
    text.includes("硬编码") ||
    text.includes("默认值") ||
    text.includes("默认结果") ||
    text.includes("未实现真实求解逻辑")
  );
}

function productText(text: string): string {
  return text
    .replace(
      "候选人可能不了解如何使用 HashMap 优化两数之和查找。",
      "当前提交可能没有正确使用 HashMap 来优化两数之和的查找过程。"
    )
    .replace("候选人可能不了解", "当前提交可能没有正确理解")
    .replace("候选人", "当前提交");
}
