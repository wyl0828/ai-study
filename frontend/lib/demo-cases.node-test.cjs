const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..", "..");
const demoDir = path.join(root, "docs", "demo-cases");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

function readManifest() {
  return JSON.parse(read("docs/demo-cases/demo-cases.json"));
}

const requiredProblemIds = [1, 206, 121];

test("demo cases use current Hot100 problem ids with diagnosis metadata", () => {
  const manifest = readManifest();

  assert.equal(manifest.version, 1);
  assert.deepEqual(
    manifest.cases.map((item) => item.problemId).sort((a, b) => a - b),
    [...requiredProblemIds].sort((a, b) => a - b)
  );

  for (const item of manifest.cases) {
    assert.ok(requiredProblemIds.includes(item.problemId), `unexpected demo problem ${item.problemId}`);
    assert.match(item.title, /\S/);
    assert.match(item.bugFile, new RegExp(`^${item.problemId}-`));
    assert.match(item.fixedFile, new RegExp(`^${item.problemId}-`));
    assert.match(item.expectedFailedCase, /\S/);
    assert.match(item.expectedErrorType, /\S/);
    assert.match(item.expectedKnowledgePoint, /\S/);

    const bugPath = path.join(demoDir, item.bugFile);
    const fixedPath = path.join(demoDir, item.fixedFile);
    assert.ok(fs.existsSync(bugPath), `missing bug file ${item.bugFile}`);
    assert.ok(fs.existsSync(fixedPath), `missing fixed file ${item.fixedFile}`);

    const bugCode = fs.readFileSync(bugPath, "utf8");
    const fixedCode = fs.readFileSync(fixedPath, "utf8");
    assert.match(bugCode, /class Solution/);
    assert.match(fixedCode, /class Solution/);
    assert.doesNotMatch(bugCode, /public\s+class\s+Solution/);
    assert.doesNotMatch(fixedCode, /public\s+class\s+Solution/);
    assert.doesNotMatch(bugCode, /Scanner|System\.in|public\s+static\s+void\s+main/);
    assert.doesNotMatch(fixedCode, /Scanner|System\.in|public\s+static\s+void\s+main/);
  }
});

test("legacy numbered demo cases are no longer the active manifest", () => {
  const manifest = readManifest();
  const activeFiles = new Set(manifest.cases.flatMap((item) => [item.bugFile, item.fixedFile]));

  for (const legacyFile of [
    "101-two-sum-bug.java",
    "101-two-sum-fixed.java",
    "103-reverse-list-bug.java",
    "103-reverse-list-fixed.java",
    "104-merge-two-lists-bug.java",
    "104-merge-two-lists-fixed.java",
  ]) {
    assert.equal(activeFiles.has(legacyFile), false, `${legacyFile} should not be active`);
  }
});
