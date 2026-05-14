const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "..", "..");

function read(relativePath) {
  return fs.readFileSync(path.join(root, relativePath), "utf8");
}

const hot100Ids = [1, 21, 49, 70, 102, 104, 121, 128, 141, 198, 206, 226];

test("Hot100 seed contains exactly the selected Solution-mode problems", () => {
  const sql = read("data/problems.sql");

  for (const id of hot100Ids) {
    assert.match(sql, new RegExp(`\\(${id},\\s*'`), `missing problem ${id}`);
  }
  for (const legacyId of [101, 103, 105, 106, 107, 108]) {
    assert.doesNotMatch(sql, new RegExp(`\\(${legacyId},\\s*'`), `legacy problem ${legacyId} should be removed`);
  }
  assert.equal((sql.match(/'solution'/g) ?? []).length, hot100Ids.length);
  assert.doesNotMatch(sql, /'acm'/i);
});

test("each selected problem has template, hints, solution outline, and three cases", () => {
  const sql = read("data/problems.sql");
  const testCasesSql = sql.slice(sql.indexOf("INSERT INTO test_case"));

  for (const id of hot100Ids) {
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?class Solution`), `missing template for ${id}`);
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?解题思路`), `missing solution outline for ${id}`);
    assert.match(
      sql,
      new RegExp(`\\(${id},[\\s\\S]*?Java 参考实现：[\\s\\S]*?\`\`\`java[\\s\\S]*?class Solution`),
      `missing Java reference code for ${id}`
    );
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?NOW\\(\\), NOW\\(\\)\\)`), `missing hints or timestamps for ${id}`);

    const caseMatches = testCasesSql.match(new RegExp(`\\(${id},\\s*'`, "g")) ?? [];
    assert.equal(caseMatches.length, 3, `problem ${id} should have 3 test cases`);
  }
});

test("each selected problem has a full interview-style statement", () => {
  const sql = read("data/problems.sql");

  for (const id of hot100Ids) {
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?任务说明：`), `missing task statement for ${id}`);
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?约束与边界：`), `missing constraints for ${id}`);
    assert.match(sql, new RegExp(`\\(${id},[\\s\\S]*?返回要求：`), `missing return contract for ${id}`);
  }
});

test("legacy frontend fallback hints are removed", () => {
  const hints = read("frontend/lib/problemHints.ts");
  const card = read("frontend/components/ProblemCard.tsx");
  const home = read("frontend/components/HomeClient.tsx");
  const presentation = read("frontend/lib/problemPresentation.ts");

  for (const legacyId of [101, 102, 103, 104, 105, 106, 107, 108]) {
    assert.doesNotMatch(hints, new RegExp(`${legacyId}:\\s*{`));
    assert.doesNotMatch(card, new RegExp(`problem\\.id\\s*===\\s*${legacyId}`));
    assert.doesNotMatch(home, new RegExp(`p\\.id\\s*===\\s*${legacyId}`));
    assert.doesNotMatch(presentation, new RegExp(`${legacyId}`));
  }
  assert.match(hints, /return null/);
});
