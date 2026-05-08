const assert = require("node:assert/strict");
const fs = require("node:fs");
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

const {
  displayExamples,
  shouldShowIoFormat,
  solutionModeHints,
} = require("./problemPresentation.ts");

test("formats reverse-list samples as LeetCode head arrays", () => {
  const problem = {
    id: 103,
    sampleCases: [
      {
        id: 1,
        input: "5\n1 2 3 4 5\n",
        expectedOutput: "5 4 3 2 1",
        sample: true,
      },
      {
        id: 2,
        input: "2\n1 2\n",
        expectedOutput: "2 1",
        sample: true,
      },
      {
        id: 3,
        input: "0\n",
        expectedOutput: "",
        sample: true,
      },
    ],
  };

  assert.deepEqual(displayExamples(problem), [
    { id: 1, input: "head = [1,2,3,4,5]", output: "[5,4,3,2,1]" },
    { id: 2, input: "head = [1,2]", output: "[2,1]" },
    { id: 3, input: "head = []", output: "[]" },
  ]);
});

test("hides input/output format only for reverse-list Solution mode", () => {
  assert.equal(shouldShowIoFormat({ id: 103 }), false);
  assert.equal(shouldShowIoFormat({ id: 101 }), true);
});

test("keeps ACM samples unchanged for other problems", () => {
  const problem = {
    id: 101,
    sampleCases: [
      {
        id: 1,
        input: "4\n2 7 11 15\n9\n",
        expectedOutput: "0 1",
        sample: true,
      },
    ],
  };

  assert.deepEqual(displayExamples(problem), [
    { id: 1, input: "4\n2 7 11 15\n9\n", output: "0 1" },
  ]);
  assert.deepEqual(solutionModeHints(101), []);
  assert.equal(solutionModeHints(103).length, 2);
});
