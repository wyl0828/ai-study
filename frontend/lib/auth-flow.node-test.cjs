const fs = require("fs");
const assert = require("assert");

const api = fs.readFileSync("frontend/lib/api.ts", "utf8");
const auth = fs.readFileSync("frontend/lib/auth.ts", "utf8");

assert.match(auth, /ai-study\.auth\.token/);
assert.match(auth, /saveAuthSession/);
assert.match(api, /Authorization/);
assert.match(api, /authApi/);
assert.match(api, /window\.location\.href = "\/login"/);
assert.match(api, /streamDiagnosis/);
assert.match(api, /Authorization: `Bearer \$\{options\.token\}`/);

const files = [
  "frontend/components/ProblemWorkspace.tsx",
  "frontend/app/dashboard/page.tsx",
  "frontend/components/KnowledgeTrainingPage.tsx",
  "frontend/components/MockInterviewPage.tsx",
  "frontend/components/RagChatPage.tsx",
];

for (const file of files) {
  const content = fs.readFileSync(file, "utf8");
  assert.doesNotMatch(content, /DEMO_USER_ID/);
}

const problemWorkspace = fs.readFileSync("frontend/components/ProblemWorkspace.tsx", "utf8");
assert.match(problemWorkspace, /getAuthToken/);
assert.match(problemWorkspace, /token: token \?\? undefined/);

console.log("auth-flow checks passed");
