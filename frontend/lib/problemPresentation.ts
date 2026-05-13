import type { ProblemDetail, TestCase } from "./types";

export interface DisplayExample {
  id: number;
  input: string;
  output: string;
}

export function shouldShowIoFormat(problem: ProblemDetail): boolean {
  void problem;
  return false;
}

export function displayExamples(problem: ProblemDetail): DisplayExample[] {
  const samples = problem.sampleCases?.filter((sample) => sample.sample) ?? [];
  return samples.map((sample) => ({
    id: sample.id,
    input: formatInput(problem, sample),
    output: formatOutput(problem, sample.expectedOutput),
  }));
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
  if (problem.id === 1 || problem.id === 206 || problem.category === "LinkedList") {
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
