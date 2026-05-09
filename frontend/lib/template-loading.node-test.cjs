const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("problem page does not fetch the code template on the server", () => {
  const page = read("app/problem/[id]/page.tsx");

  assert.doesNotMatch(page, /problemApi\.template/);
  assert.match(page, /<ProblemWorkspace/);
});

test("ProblemWorkspace fetches the template for the active problem", () => {
  const workspace = read("components/ProblemWorkspace.tsx");

  assert.match(workspace, /problemApi\.template\(problemId\)/);
  assert.doesNotMatch(workspace, /defaultCode/);
});

test("frontend API requests opt out of stale fetch caches", () => {
  const api = read("lib/api.ts");

  assert.match(api, /cache:\s*"no-store"/);
});
