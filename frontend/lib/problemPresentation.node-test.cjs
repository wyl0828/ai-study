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
} = require("./problemPresentation.ts");

test("formats reverse-list samples as LeetCode head arrays", () => {
  const problem = {
    id: 206,
    category: "LinkedList",
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
    {
      id: 1,
      input: "head = [1,2,3,4,5]",
      output: "[5,4,3,2,1]",
      visual: {
        kind: "linkedList",
        inputs: [{ label: "head", values: ["1", "2", "3", "4", "5"] }],
        output: { type: "list", label: "输出链表", values: ["5", "4", "3", "2", "1"] },
      },
    },
    {
      id: 2,
      input: "head = [1,2]",
      output: "[2,1]",
      visual: {
        kind: "linkedList",
        inputs: [{ label: "head", values: ["1", "2"] }],
        output: { type: "list", label: "输出链表", values: ["2", "1"] },
      },
    },
    {
      id: 3,
      input: "head = []",
      output: "[]",
      visual: {
        kind: "linkedList",
        inputs: [{ label: "head", values: [] }],
        output: { type: "list", label: "输出链表", values: [] },
      },
    },
  ]);
});

test("hides input/output format only for reverse-list Solution mode", () => {
  assert.equal(shouldShowIoFormat({ id: 206 }), false);
  assert.equal(shouldShowIoFormat({ id: 1 }), false);
});

test("formats Solution-mode array samples with compact argument names", () => {
  const problem = {
    id: 1,
    title: "两数之和",
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
    { id: 1, input: "nums = [2,7,11,15], target = 9", output: "[0,1]" },
  ]);
});

test("builds tree structure and level output visual for binary tree level order", () => {
  const problem = {
    id: 102,
    category: "Tree",
    sampleCases: [
      {
        id: 1,
        input: "7\n3 9 20 null null 15 7\n",
        expectedOutput: "[[3],[9,20],[15,7]]",
        sample: true,
      },
    ],
  };

  assert.deepEqual(displayExamples(problem), [
    {
      id: 1,
      input: "root = [3,9,20,null,null,15,7]",
      output: "[[3],[9,20],[15,7]]",
      visual: {
        kind: "tree",
        input: {
          label: "输入二叉树",
          tree: {
            levels: [
              [{ id: "n0", value: "3", slot: 0 }],
              [
                { id: "n1", value: "9", slot: 0 },
                { id: "n2", value: "20", slot: 1 },
              ],
              [
                { id: "n5", value: "15", slot: 2 },
                { id: "n6", value: "7", slot: 3 },
              ],
            ],
            edges: [
              { from: "n0", to: "n1" },
              { from: "n0", to: "n2" },
              { from: "n2", to: "n5" },
              { from: "n2", to: "n6" },
            ],
          },
        },
        output: {
          type: "levels",
          label: "输出层序结果",
          levels: [["3"], ["9", "20"], ["15", "7"]],
        },
      },
    },
  ]);
});

test("builds tree visual with scalar output for binary tree max depth", () => {
  const problem = {
    id: 104,
    category: "Tree",
    sampleCases: [
      {
        id: 1,
        input: "7\n3 9 20 null null 15 7\n",
        expectedOutput: "3",
        sample: true,
      },
    ],
  };

  const [example] = displayExamples(problem);

  assert.equal(example.visual.kind, "tree");
  assert.deepEqual(example.visual.output, {
    type: "value",
    label: "输出结果",
    value: "3",
  });
});

test("builds input and output tree visuals for invert binary tree", () => {
  const problem = {
    id: 226,
    category: "Tree",
    sampleCases: [
      {
        id: 1,
        input: "7\n4 2 7 1 3 6 9\n",
        expectedOutput: "[4,7,2,9,6,3,1]",
        sample: true,
      },
    ],
  };

  const [example] = displayExamples(problem);

  assert.equal(example.visual.kind, "tree");
  assert.deepEqual(example.visual.output, {
    type: "tree",
    label: "输出二叉树",
    tree: {
      levels: [
        [{ id: "n0", value: "4", slot: 0 }],
        [
          { id: "n1", value: "7", slot: 0 },
          { id: "n2", value: "2", slot: 1 },
        ],
        [
          { id: "n3", value: "9", slot: 0 },
          { id: "n4", value: "6", slot: 1 },
          { id: "n5", value: "3", slot: 2 },
          { id: "n6", value: "1", slot: 3 },
        ],
      ],
      edges: [
        { from: "n0", to: "n1" },
        { from: "n0", to: "n2" },
        { from: "n1", to: "n3" },
        { from: "n1", to: "n4" },
        { from: "n2", to: "n5" },
        { from: "n2", to: "n6" },
      ],
    },
  });
});

test("builds merge-two-lists input and output visuals", () => {
  const problem = {
    id: 21,
    category: "LinkedList",
    sampleCases: [
      {
        id: 1,
        input: "3\n1 2 4\n3\n1 3 4\n",
        expectedOutput: "[1,1,2,3,4,4]",
        sample: true,
      },
    ],
  };

  const [example] = displayExamples(problem);

  assert.deepEqual(example.visual, {
    kind: "linkedList",
    inputs: [
      { label: "list1", values: ["1", "2", "4"] },
      { label: "list2", values: ["1", "3", "4"] },
    ],
    output: {
      type: "list",
      label: "输出链表",
      values: ["1", "1", "2", "3", "4", "4"],
    },
  });
});

test("builds cycle-list visual with pos and boolean output", () => {
  const problem = {
    id: 141,
    category: "LinkedList",
    sampleCases: [
      {
        id: 1,
        input: "4\n3 2 0 -4\n1\n",
        expectedOutput: "true",
        sample: true,
      },
    ],
  };

  const [example] = displayExamples(problem);

  assert.deepEqual(example.visual, {
    kind: "linkedList",
    inputs: [
      {
        label: "head",
        values: ["3", "2", "0", "-4"],
        cycleToIndex: 1,
        cycleToValue: "2",
      },
    ],
    output: { type: "value", label: "输出结果", value: "true" },
  });
});
