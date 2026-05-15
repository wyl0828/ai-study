import type { ProblemDetail, TestCase } from "./types";

export interface DisplayExample {
  id: number;
  input: string;
  output: string;
  visual?: ExampleVisual;
}

export interface TreeVisualNode {
  id: string;
  value: string;
  slot: number;
}

export interface TreeVisual {
  levels: TreeVisualNode[][];
  edges: Array<{ from: string; to: string }>;
}

export interface LinkedListVisualSection {
  label: string;
  values: string[];
  cycleToIndex?: number;
  cycleToValue?: string;
}

export type ExampleVisual =
  | {
      kind: "tree";
      input: {
        label: string;
        tree: TreeVisual;
      };
      output:
        | {
            type: "tree";
            label: string;
            tree: TreeVisual;
          }
        | {
            type: "levels";
            label: string;
            levels: string[][];
          }
        | {
            type: "value";
            label: string;
            value: string;
          };
    }
  | {
      kind: "linkedList";
      inputs: LinkedListVisualSection[];
      output:
        | {
            type: "list";
            label: string;
            values: string[];
          }
        | {
            type: "value";
            label: string;
            value: string;
          };
    };

type TreeOutputVisual = Extract<ExampleVisual, { kind: "tree" }>["output"];
type LinkedListOutputVisual = Extract<
  ExampleVisual,
  { kind: "linkedList" }
>["output"];

export function shouldShowIoFormat(problem: ProblemDetail): boolean {
  void problem;
  return false;
}

export function displayExamples(problem: ProblemDetail): DisplayExample[] {
  const samples = problem.sampleCases?.filter((sample) => sample.sample) ?? [];
  return samples.map((sample) => {
    const visual = buildExampleVisual(problem, sample);
    return {
      id: sample.id,
      input: formatInput(problem, sample),
      output: formatOutput(problem, sample.expectedOutput),
      ...(visual ? { visual } : {}),
    };
  });
}

export function buildExampleVisual(
  problem: ProblemDetail,
  sample: TestCase
): ExampleVisual | undefined {
  if (problem.category === "Tree") {
    return buildTreeExampleVisual(problem, sample);
  }
  if (problem.category === "LinkedList" || problem.id === 206 || problem.id === 141) {
    return buildLinkedListExampleVisual(problem, sample);
  }
  return undefined;
}

function formatInput(problem: ProblemDetail, sample: TestCase): string {
  const tokens = tokenize(sample.input);
  if (problem.id === 1) {
    const { values, rest } = readArray(tokens);
    return `nums = [${values.join(",")}], target = ${rest[0] ?? ""}`;
  }
  if (problem.id === 206) {
    const { values } = readArray(tokens);
    return `head = [${values.join(",")}]`;
  }
  if (problem.id === 141) {
    const { values, rest } = readArray(tokens);
    return `head = [${values.join(",")}], pos = ${rest[0] ?? "-1"}`;
  }
  if (problem.id === 121) {
    const { values } = readArray(tokens);
    return `prices = [${values.join(",")}]`;
  }
  if (problem.category === "LinkedList") {
    return formatLinkedListInput(tokens);
  }
  if (problem.category === "Tree") {
    const { values } = readArray(tokens);
    return `root = [${values.join(",")}]`;
  }
  if (problem.category === "DynamicProgramming" || problem.category === "Greedy") {
    const { values } = readArray(tokens);
    return values.length > 0 ? `nums = [${values.join(",")}]` : sample.input.trim();
  }
  return sample.input.trim();
}

function formatOutput(problem: ProblemDetail, output: string): string {
  const trimmed = output.trim();
  if (!trimmed) {
    return "[]";
  }
  if (trimmed.startsWith("[")) {
    return trimmed;
  }
  if (
    problem.id === 1 ||
    problem.id === 206 ||
    (problem.category === "LinkedList" && problem.id !== 141)
  ) {
    return `[${tokenize(trimmed).join(",")}]`;
  }
  return trimmed;
}

function formatLinkedListInput(tokens: string[]): string {
  const first = readArray(tokens);
  const second = readArray(first.rest);
  if (second.values.length > 0 || first.rest.length > 0) {
    return `list1 = [${first.values.join(",")}], list2 = [${second.values.join(",")}]`;
  }
  return `head = [${first.values.join(",")}]`;
}

function buildTreeExampleVisual(
  problem: ProblemDetail,
  sample: TestCase
): ExampleVisual {
  const { values } = readArray(tokenize(sample.input));
  return {
    kind: "tree",
    input: {
      label: "输入二叉树",
      tree: buildTreeVisual(values),
    },
    output: buildTreeOutputVisual(problem, sample.expectedOutput),
  };
}

function buildTreeOutputVisual(
  problem: ProblemDetail,
  output: string
): TreeOutputVisual {
  const trimmed = output.trim();
  const parsed = parseJsonLikeArray(trimmed);
  if (Array.isArray(parsed) && parsed.every(Array.isArray)) {
    return {
      type: "levels",
      label: "输出层序结果",
      levels: parsed.map((level) => level.map(formatVisualValue)),
    };
  }
  if (
    problem.id === 226 &&
    Array.isArray(parsed) &&
    parsed.every((value) => !Array.isArray(value))
  ) {
    return {
      type: "tree",
      label: "输出二叉树",
      tree: buildTreeVisual(parsed.map(formatVisualValue)),
    };
  }
  return {
    type: "value",
    label: "输出结果",
    value: formatOutput(problem, output),
  };
}

function buildLinkedListExampleVisual(
  problem: ProblemDetail,
  sample: TestCase
): ExampleVisual {
  const tokens = tokenize(sample.input);
  const first = readArray(tokens);
  const second = readArray(first.rest);
  const inputs: LinkedListVisualSection[] =
    second.values.length > 0 || (problem.id !== 141 && first.rest.length > 0)
      ? [
          { label: "list1", values: first.values },
          { label: "list2", values: second.values },
        ]
      : [{ label: "head", values: first.values }];

  if (problem.id === 141) {
    const pos = Number(first.rest[0] ?? -1);
    if (Number.isInteger(pos) && pos >= 0 && pos < first.values.length) {
      inputs[0] = {
        ...inputs[0],
        cycleToIndex: pos,
        cycleToValue: first.values[pos],
      };
    }
  }

  return {
    kind: "linkedList",
    inputs,
    output: buildLinkedListOutputVisual(problem, sample.expectedOutput),
  };
}

function buildLinkedListOutputVisual(
  problem: ProblemDetail,
  output: string
): LinkedListOutputVisual {
  if (problem.id === 141) {
    return {
      type: "value",
      label: "输出结果",
      value: formatOutput(problem, output),
    };
  }
  return {
    type: "list",
    label: "输出链表",
    values: parseFlatValues(output),
  };
}

function buildTreeVisual(values: string[]): TreeVisual {
  const nodesByIndex = new Map<number, TreeVisualNode>();
  const edges: Array<{ from: string; to: string }> = [];

  values.forEach((value, index) => {
    if (isNullToken(value)) return;
    nodesByIndex.set(index, {
      id: `n${index}`,
      value,
      slot: index === 0 ? 0 : index - (2 ** Math.floor(Math.log2(index + 1)) - 1),
    });
  });

  nodesByIndex.forEach((node, index) => {
    if (index === 0) return;
    const parent = nodesByIndex.get(Math.floor((index - 1) / 2));
    if (parent) {
      edges.push({ from: parent.id, to: node.id });
    }
  });

  const maxLevel = values.length > 0 ? Math.floor(Math.log2(values.length)) : 0;
  const levels: TreeVisualNode[][] = [];
  for (let level = 0; level <= maxLevel; level += 1) {
    const start = 2 ** level - 1;
    const end = Math.min(values.length - 1, 2 ** (level + 1) - 2);
    const levelNodes: TreeVisualNode[] = [];
    for (let index = start; index <= end; index += 1) {
      const node = nodesByIndex.get(index);
      if (node) {
        levelNodes.push(node);
      }
    }
    if (levelNodes.length > 0) {
      levels.push(levelNodes);
    }
  }

  return { levels, edges };
}

function parseFlatValues(value: string): string[] {
  const parsed = parseJsonLikeArray(value.trim());
  if (Array.isArray(parsed)) {
    return parsed.map(formatVisualValue);
  }
  return tokenize(value).map(formatVisualValue);
}

function parseJsonLikeArray(value: string): unknown {
  if (!value.startsWith("[")) return undefined;
  try {
    return JSON.parse(value);
  } catch {
    return undefined;
  }
}

function formatVisualValue(value: unknown): string {
  return value === null || typeof value === "undefined" ? "null" : String(value);
}

function isNullToken(value: string): boolean {
  return value.toLowerCase() === "null";
}

function readArray(tokens: string[]): { values: string[]; rest: string[] } {
  const length = Number(tokens[0] ?? 0);
  const count = Number.isFinite(length) ? Math.max(0, length) : 0;
  return {
    values: tokens.slice(1, 1 + count),
    rest: tokens.slice(1 + count),
  };
}

function tokenize(value: string): string[] {
  return value.trim().split(/\s+/).filter(Boolean);
}
