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
  HashMap: "哈希表",
  LinkedList: "链表",
  Tree: "二叉树",
  DynamicProgramming: "动态规划",
  "Array Traversal": "数组遍历",
  "HashMap Lookup": "HashMap 查找",
  "String Counting": "字符计数",
  "Linked List Pointer": "链表指针",
  "Binary Tree Recursion": "二叉树递归",
  "Binary Tree BFS": "二叉树广度优先遍历",
  "Dynamic Programming State": "动态规划状态",
  "Binary Search Patience": "二分优化",
};

const categoryMap: Record<string, string> = {
  HashMap: "哈希表",
  LinkedList: "链表",
  Tree: "二叉树",
  DynamicProgramming: "动态规划",
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
  ERROR_CLASSIFICATION: "识别错误类型",
  HINT_GENERATION: "生成分层提示",
  MEMORY_UPDATE: "更新薄弱点记录",
  TRAINING_PLAN: "生成训练计划",
  COMPLETED: "诊断完成",
  FAILED: "诊断失败",
};

const trainingPhraseMap: Record<string, string> = {
  "3-day recovery plan": "3 天专项训练",
  "HashMap Lookup in Array Traversal": "数组遍历中的 HashMap 查找",
  "Linked List Pointer": "链表指针",
  "Binary Tree Recursion": "二叉树递归",
  "Dynamic Programming State": "动态规划状态",
};

export function problemTitle(text: string): string {
  return problemTitleMap[text] || text;
}

export function knowledgePoint(text: string): string {
  return knowledgePointMap[text] || text;
}

export function categoryName(text: string): string {
  return categoryMap[text] || text;
}

export function difficultyName(text: string): string {
  return difficultyMap[text] || text;
}

export function problemDescription(text: string): string {
  return descriptionMap[text] || text;
}

export function formatText(text: string): string {
  return formatMap[text] || text;
}

export function errorTypeName(text: string): string {
  return errorTypeMap[text] || text;
}

export function agentStepName(text: string): string {
  return agentStepMap[text] || text;
}

export function trainingPlanTitle(text: string): string {
  const translated = Object.entries(trainingPhraseMap).reduce(
    (value, [source, target]) => value.replace(source, target),
    text
  );
  return translated.replace(": ", "：");
}
