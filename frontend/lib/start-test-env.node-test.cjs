const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..", "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

test("start test env script exposes a conservative local testing workflow", () => {
  const script = read("scripts/start_test_env.ps1");

  assert.match(script, /\[switch\]\s*\$Production/);
  assert.match(script, /\$FrontendHost\s*=\s*"0\.0\.0\.0"/);
  assert.match(script, /docker compose up -d redis qdrant/);
  assert.match(script, /local_dependency_preflight\.ps1/);
  assert.doesNotMatch(script, /e2e_demo_smoke\.ps1/);
  assert.doesNotMatch(script, /docker\s+rm|docker\s+compose\s+down|Remove-Item/);
  assert.match(script, /Piston is not ready/);
  assert.match(script, /Start-Process[\s\S]+-WindowStyle Hidden/);
});

test("testing deployment guide points to the one-command helper", () => {
  const guide = read("docs/DEPLOY_FOR_TESTING.md");

  assert.match(guide, /scripts\\start_test_env\.ps1/);
  assert.match(guide, /-Production/);
});
