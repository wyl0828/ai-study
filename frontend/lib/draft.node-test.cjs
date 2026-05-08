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

class MemoryStorage {
  constructor() {
    this.store = new Map();
    this.failWrites = false;
  }

  getItem(key) {
    return this.store.has(key) ? this.store.get(key) : null;
  }

  setItem(key, value) {
    if (this.failWrites) {
      throw new Error("storage full");
    }
    this.store.set(key, value);
  }

  removeItem(key) {
    this.store.delete(key);
  }

  clear() {
    this.store.clear();
    this.failWrites = false;
  }
}

const storage = new MemoryStorage();
global.localStorage = storage;

const {
  clearDraft,
  formatDraftTime,
  loadDraft,
  saveDraft,
} = require("./draft.ts");

test.beforeEach(() => {
  storage.clear();
});

test("saves and loads a complete Java problem draft", () => {
  saveDraft(1, 101, {
    code: "class Main {}",
    language: "java",
    lastResult: {
      submissionId: 7,
      status: "WRONG_ANSWER",
      passedCount: 1,
      totalCount: 3,
      runtime: null,
      memory: null,
      errorMessage: "failed",
      failedCases: [],
      codeSnapshot: "class Main {}",
      submittedAt: "2026-05-08T01:00:00.000Z",
    },
  });

  const draft = loadDraft(1, 101);

  assert.equal(draft.userId, 1);
  assert.equal(draft.problemId, 101);
  assert.equal(draft.language, "java");
  assert.equal(draft.code, "class Main {}");
  assert.equal(draft.lastResult.status, "WRONG_ANSWER");
  assert.equal(draft.lastResult.codeSnapshot, "class Main {}");
});

test("merges partial saves into an existing draft", () => {
  saveDraft(1, 101, {
    code: "first",
    language: "java",
  });

  saveDraft(1, 101, {
    code: "second",
  });

  const draft = loadDraft(1, 101);

  assert.equal(draft.code, "second");
  assert.equal(draft.language, "java");
});

test("clears stale diagnosis when a new submission omits it explicitly", () => {
  saveDraft(1, 101, {
    code: "first",
    language: "java",
    lastDiagnosis: {
      agentRunId: 1,
      submissionId: 7,
      errorType: "LOGIC_ERROR",
      knowledgePoint: "HashMap",
      specificError: "old issue",
      diagnosis: "old diagnosis",
      hintLevel1: "hint 1",
      hintLevel2: "hint 2",
      hintLevel3: "hint 3",
      trainingPlanTitle: "old plan",
      steps: [],
      codeSnapshot: "first",
    },
  });

  saveDraft(1, 101, {
    code: "second",
    language: "java",
    lastDiagnosis: undefined,
  });

  const draft = loadDraft(1, 101);

  assert.equal(draft.code, "second");
  assert.equal(draft.lastDiagnosis, undefined);
});

test("returns null for invalid JSON and missing core fields", () => {
  storage.setItem("interview_coach_draft_1_101", "{bad json");
  assert.equal(loadDraft(1, 101), null);

  storage.setItem(
    "interview_coach_draft_1_101",
    JSON.stringify({
      userId: 1,
      problemId: 101,
      language: "java",
      updatedAt: "2026-05-08T01:00:00.000Z",
    })
  );
  assert.equal(loadDraft(1, 101), null);
});

test("clears drafts and ignores write failures", () => {
  saveDraft(1, 101, {
    code: "class Main {}",
    language: "java",
  });
  clearDraft(1, 101);
  assert.equal(loadDraft(1, 101), null);

  storage.failWrites = true;
  assert.doesNotThrow(() =>
    saveDraft(1, 101, {
      code: "class Main {}",
      language: "java",
    })
  );
});

test("formats draft time as MM-DD HH:mm", () => {
  assert.equal(formatDraftTime("2026-05-08T09:06:00+08:00"), "05-08 09:06");
});
