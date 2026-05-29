const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..", "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("local dependency preflight covers runtime services with actionable output", () => {
  const script = read("scripts/local_dependency_preflight.ps1");
  const readme = read("README.md");
  const checklist = read("docs/FINAL_ACCEPTANCE_CHECKLIST.md");

  for (const dependency of ["MySQL", "Backend", "Frontend", "Piston", "Redis", "Qdrant", "Docker"]) {
    assert.match(script, new RegExp(dependency));
  }

  assert.match(script, /Get-HttpStatus/);
  assert.match(script, /Test-NetConnection/);
  assert.match(script, /docker version/);
  assert.match(script, /local_dependency_preflight/);
  assert.match(script, /NEXT_ACTION/);
  assert.match(script, /READY_FOR_E2E_SMOKE/);

  assert.match(readme, /scripts\\local_dependency_preflight\.ps1/);
  assert.match(checklist, /scripts\\local_dependency_preflight\.ps1/);
});
