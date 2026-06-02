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

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

function createStorage() {
  const values = new Map();
  return {
    getItem: (key) => (values.has(key) ? values.get(key) : null),
    setItem: (key, value) => values.set(key, String(value)),
    removeItem: (key) => values.delete(key),
  };
}

test("knowledge view state stores the last active card per user only", () => {
  const {
    knowledgeLastCardKey,
    readLastKnowledgeCardId,
    saveLastKnowledgeCardId,
  } = require("./knowledgeViewState.ts");

  const storage = createStorage();

  assert.equal(readLastKnowledgeCardId(3, storage), null);
  saveLastKnowledgeCardId(3, 42, storage);
  assert.equal(storage.getItem(knowledgeLastCardKey(3)), "42");
  assert.equal(readLastKnowledgeCardId(3, storage), 42);
  assert.equal(readLastKnowledgeCardId(4, storage), null);

  storage.setItem(knowledgeLastCardKey(3), "0");
  assert.equal(readLastKnowledgeCardId(3, storage), null);
  storage.setItem(knowledgeLastCardKey(3), "not-a-card");
  assert.equal(readLastKnowledgeCardId(3, storage), null);
});

test("knowledge page restores saved card after first visit without overriding URL cardId", () => {
  const page = read("components/KnowledgeTrainingPage.tsx");

  assert.match(page, /readLastKnowledgeCardId/);
  assert.match(page, /saveLastKnowledgeCardId/);
  assert.match(page, /explicitTargetCardId/);
  assert.match(page, /explicitTargetCardId\s*\?\?\s*storedTargetCardId/);
  assert.match(page, /saveLastKnowledgeCardId\(userId,\s*nextSelection\.cardId\)/);
  assert.match(page, /saveLastKnowledgeCardId\(userId,\s*topic\.id\)/);
  assert.match(page, /clearLastKnowledgeCardId\(userId\)/);
});

test("knowledge page first visit defaults to the Java core overview", () => {
  const { defaultKnowledgeSelection } = require("./knowledgeData.ts");

  assert.deepEqual(defaultKnowledgeSelection, { domain: "Java 核心" });
});
