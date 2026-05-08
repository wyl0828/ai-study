import type { ProblemDetail, TestCase } from "./types";

const REVERSE_LIST_PROBLEM_ID = 103;

export interface DisplayExample {
  id: number;
  input: string;
  output: string;
}

export function isSolutionModeProblem(problemId: number): boolean {
  return problemId === REVERSE_LIST_PROBLEM_ID;
}

export function shouldShowIoFormat(problem: ProblemDetail): boolean {
  return !isSolutionModeProblem(problem.id);
}

export function displayExamples(problem: ProblemDetail): DisplayExample[] {
  const samples = problem.sampleCases?.filter((sample) => sample.sample) ?? [];
  if (!isSolutionModeProblem(problem.id)) {
    return samples.map((sample) => ({
      id: sample.id,
      input: sample.input,
      output: sample.expectedOutput,
    }));
  }

  return samples.map((sample) => ({
    id: sample.id,
    input: `head = ${formatReverseListInput(sample)}`,
    output: formatListOutput(sample.expectedOutput),
  }));
}

export function solutionModeHints(problemId: number): string[] {
  if (!isSolutionModeProblem(problemId)) {
    return [];
  }
  return [
    "链表中节点的数目范围是 [0, 5000]",
    "-5000 <= Node.val <= 5000",
  ];
}

function formatReverseListInput(sample: TestCase): string {
  const tokens = tokenize(sample.input);
  const length = Number(tokens[0] ?? 0);
  const values = tokens.slice(1, 1 + Math.max(0, length));
  return `[${values.join(",")}]`;
}

function formatListOutput(output: string): string {
  const values = tokenize(output);
  return `[${values.join(",")}]`;
}

function tokenize(value: string): string[] {
  return value.trim().split(/\s+/).filter(Boolean);
}
