import type {
  UserWeakness,
  MistakeCard,
  TrainingPlan,
} from "./types";

export const mockWeaknesses: UserWeakness[] = [
  {
    id: 1,
    knowledgePoint: "HashMap 基础查找",
    errorType: "LOGIC_ERROR",
    wrongCount: 3,
    weaknessScore: 4.2,
  },
  {
    id: 2,
    knowledgePoint: "链表指针",
    errorType: "BOUNDARY_ERROR",
    wrongCount: 2,
    weaknessScore: 3.1,
  },
  {
    id: 3,
    knowledgePoint: "动态规划状态",
    errorType: "ALGORITHM_ERROR",
    wrongCount: 1,
    weaknessScore: 1.8,
  },
];

export const mockMistakes: MistakeCard[] = [
  {
    id: 1,
    problemId: 101,
    problemTitle: "两数之和",
    errorType: "逻辑错误",
    knowledgePoint: "HashMap 基础查找",
    mistakeSummary: "未实现真实求解逻辑，结果被硬编码为 -1 -1。",
    correctIdea:
      "需要掌握如何使用 HashMap 在数组中高效查找目标配对。",
  },
  {
    id: 2,
    problemId: 104,
    problemTitle: "合并两个有序链表",
    errorType: "边界条件错误",
    knowledgePoint: "链表指针",
    mistakeSummary: "合并时未处理其中一个链表先遍历完的情况。",
    correctIdea:
      "使用哨兵节点，循环比较后将剩余链表直接拼接。",
  },
];

export const mockTrainingPlan: TrainingPlan = {
  title: "3 天哈希表与链表专项训练",
  summary:
    "根据你最近的错误记录，系统检测到 HashMap 查找和链表指针操作是主要薄弱点。",
  items: [
    {
      dayIndex: 1,
      knowledgePoint: "HashMap",
      problemTitle: "两数之和",
      reason: "巩固 HashMap 查询与写入顺序",
      reviewFocus: "注意 HashMap 的插入与查询顺序，避免使用同一个元素",
      status: "待完成",
    },
    {
      dayIndex: 1,
      knowledgePoint: "HashMap",
      problemTitle: "有效字母异位词",
      reason: "练习字符计数与 HashMap 统计",
      reviewFocus: "用数组替代 HashMap 处理小字符集",
      status: "待完成",
    },
    {
      dayIndex: 2,
      knowledgePoint: "LinkedList",
      problemTitle: "反转链表",
      reason: "复习前驱、当前、后继三个指针的移动顺序",
      reviewFocus: "注意循环终止条件和空指针",
      status: "待完成",
    },
    {
      dayIndex: 2,
      knowledgePoint: "LinkedList",
      problemTitle: "合并两个有序链表",
      reason: "练习哨兵节点和链表合并",
      reviewFocus: "合并时注意处理空链表边界",
      status: "需要重做",
    },
    {
      dayIndex: 3,
      knowledgePoint: "Tree",
      problemTitle: "二叉树的最大深度",
      reason: "练习递归终止条件和空节点处理",
      reviewFocus: "递归先写终止条件",
      status: "已通过",
    },
  ],
};

export const mockSubmissionHistory = [
  { problemId: 101, problemTitle: "两数之和", status: "ACCEPTED", passedCount: 3, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 102, problemTitle: "有效字母异位词", status: "ACCEPTED", passedCount: 3, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 104, problemTitle: "合并两个有序链表", status: "WRONG_ANSWER", passedCount: 1, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 105, problemTitle: "二叉树的最大深度", status: "RETRY", passedCount: 1, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 108, problemTitle: "最长递增子序列", status: "WRONG_ANSWER", passedCount: 2, totalCount: 3, createdAt: new Date().toISOString() },
];

export const mockStats = {
  totalSubmissions: 12,
  passedProblems: 2,
  weakPointCount: 3,
  mistakeCount: 4,
};
