export interface ProblemPresetHints {
  level1: string;
  level2: string;
  level3: string;
}

export function getProblemPresetHints(
  problemId: number
): ProblemPresetHints | null {
  // Legacy compatibility hook: preset hints now come from the backend problem table.
  void problemId;
  return null;
}
