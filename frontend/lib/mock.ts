import type {
  UserWeakness,
  MistakeCard,
  TrainingPlan,
} from "./types";

export const mockWeaknesses: UserWeakness[] = [
  {
    id: 1,
    knowledgePoint: "HashMap 查找",
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
    knowledgePoint: "哈希表",
    mistakeSummary: "暴力双重循环未使用哈希表优化查找",
    correctIdea:
      "使用 HashMap 存储已遍历元素，一次遍历完成查找，时间复杂度 O(n)",
  },
  {
    id: 2,
    problemId: 104,
    problemTitle: "合并两个有序链表",
    errorType: "边界条件错误",
    knowledgePoint: "链表指针",
    mistakeSummary: "合并时未处理其中一个链表先遍历完的情况，导致空指针异常",
    correctIdea:
      "使用哨兵节点，循环比较后将剩余链表直接拼接",
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
      reviewFocus: "注意 HashMap 的 put 和 get 顺序，避免使用同一个元素",
      status: "PENDING",
    },
    {
      dayIndex: 1,
      knowledgePoint: "HashMap",
      problemTitle: "有效字母异位词",
      reason: "练习字符计数与 HashMap 统计",
      reviewFocus: "用数组替代 HashMap 处理小字符集",
      status: "PENDING",
    },
    {
      dayIndex: 2,
      knowledgePoint: "LinkedList",
      problemTitle: "反转链表",
      reason: "复习 prev/cur/next 三指针操作",
      reviewFocus: "注意循环终止条件和空指针",
      status: "PENDING",
    },
    {
      dayIndex: 2,
      knowledgePoint: "LinkedList",
      problemTitle: "合并两个有序链表",
      reason: "练习哨兵节点和链表合并",
      reviewFocus: "合并时注意处理空链表边界",
      status: "PENDING",
    },
    {
      dayIndex: 3,
      knowledgePoint: "Tree",
      problemTitle: "二叉树的最大深度",
      reason: "练习递归终止条件和空节点处理",
      reviewFocus: "递归先写终止条件",
      status: "PENDING",
    },
  ],
};

export const mockSubmissionHistory = [
  { problemId: 101, problemTitle: "两数之和", status: "ACCEPTED", passedCount: 3, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 104, problemTitle: "合并两个有序链表", status: "WRONG_ANSWER", passedCount: 1, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 101, problemTitle: "两数之和", status: "WRONG_ANSWER", passedCount: 1, totalCount: 3, createdAt: new Date().toISOString() },
  { problemId: 103, problemTitle: "反转链表", status: "ACCEPTED", passedCount: 3, totalCount: 3, createdAt: new Date().toISOString() },
];

export const mockStats = {
  totalSubmissions: 12,
  passedProblems: 2,
  weakPointCount: 3,
  mistakeCount: 4,
};
