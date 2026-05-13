export interface ParsedProblemSolution {
  explanation: string;
  javaCode: string | null;
}

const JAVA_CODE_BLOCK = /```java\s*([\s\S]*?)```/i;
const SMART_QUOTES = /[“”‘’]/;

export function parseProblemSolution(
  raw: string | null | undefined
): ParsedProblemSolution {
  const source = raw?.trim() ?? "";
  if (!source) {
    return {
      explanation: "",
      javaCode: null,
    };
  }

  const match = source.match(JAVA_CODE_BLOCK);
  if (!match || match.index === undefined) {
    return {
      explanation: source,
      javaCode: null,
    };
  }

  const javaCode = match[1].trim();
  if (SMART_QUOTES.test(javaCode)) {
    throw new Error("参考实现中包含中文引号，请改为 Java 可编译的英文引号。");
  }

  const before = stripTrailingCodeHeading(source.slice(0, match.index));
  const after = source.slice(match.index + match[0].length).trim();
  return {
    explanation: [before, after].filter(Boolean).join("\n\n"),
    javaCode,
  };
}

function stripTrailingCodeHeading(value: string): string {
  return value
    .replace(/(?:^|\n)\s*Java\s*参考实现：?\s*$/i, "")
    .trim();
}
