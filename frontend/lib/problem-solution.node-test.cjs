const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");
const ts = require("typescript");

require.extensions[".ts"] = function loadTs(module, filename) {
  const source = fs.readFileSync(filename, "utf8");
  const output = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true,
    },
  }).outputText;
  module._compile(output, filename);
};

const { parseProblemSolution } = require("./problemSolution.ts");
const repoRoot = path.resolve(__dirname, "..", "..");

test("parses explanation and first Java reference implementation", () => {
  const raw = `解题思路：
先统计字符频次。

Java 参考实现：
\`\`\`java
class Solution {
    public boolean isAnagram(String s, String t) {
        int index = s.charAt(0) - 'a';
        return index >= 0;
    }
}
\`\`\`

复杂度：
时间复杂度：O(n)`;

  assert.deepEqual(parseProblemSolution(raw), {
    explanation:
      "解题思路：\n先统计字符频次。\n\n复杂度：\n时间复杂度：O(n)",
    javaCode: `class Solution {
    public boolean isAnagram(String s, String t) {
        int index = s.charAt(0) - 'a';
        return index >= 0;
    }
}`,
  });
});

test("keeps explanation when no Java code block exists", () => {
  assert.deepEqual(parseProblemSolution("解题思路：\n使用双指针。"), {
    explanation: "解题思路：\n使用双指针。",
    javaCode: null,
  });
});

test("treats empty solution outline as missing content", () => {
  assert.deepEqual(parseProblemSolution("  \n\t"), {
    explanation: "",
    javaCode: null,
  });
});

test("rejects smart quotes in Java reference implementation", () => {
  const raw = `Java 参考实现：
\`\`\`java
class Solution {
    String value = “bad”;
}
\`\`\``;

  assert.throws(
    () => parseProblemSolution(raw),
    /参考实现中包含中文引号/
  );
});

test("seeded problem solution SQL does not contain smart quotes", () => {
  for (const file of ["data/problems.sql", "data/add_problem_solutions.sql"]) {
    const source = fs.readFileSync(path.join(repoRoot, file), "utf8");
    assert.doesNotMatch(source, /[“”‘’]/, `${file} contains smart quotes`);
  }
});
