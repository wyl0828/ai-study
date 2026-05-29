import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";
import { createRequire } from "node:module";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, "..");

function loadPlaywright() {
  try {
    return require("playwright");
  } catch {
    try {
      return require(path.join(rootDir, "output", "playwright-runner", "node_modules", "playwright"));
    } catch (error) {
      throw new Error(
        "Playwright is not installed. Run: npm install --prefix output\\playwright-runner playwright"
      );
    }
  }
}

const { chromium } = loadPlaywright();

const appUrl = process.env.APP_URL ?? "http://localhost:3000/problem/1";
const outputDir = path.resolve(rootDir, "output", "playwright", "videos");
const tempVideoDir = path.join(outputDir, ".tmp");
const rawWebmPath = path.join(outputDir, "1-two-sum-real-demo-raw.webm");
const finalWebmPath = path.join(outputDir, "1-two-sum-real-demo.webm");
const finalMp4Path = path.join(outputDir, "1-two-sum-real-demo.mp4");
const viewport = { width: 1440, height: 900 };
const diagnosisGapSeconds = 10;

const bugCode = fs.readFileSync(
  path.join(rootDir, "docs", "demo-cases", "1-two-sum-bug.java"),
  "utf8"
);
const fixedCode = fs.readFileSync(
  path.join(rootDir, "docs", "demo-cases", "1-two-sum-fixed.java"),
  "utf8"
);

fs.mkdirSync(tempVideoDir, { recursive: true });

async function setCaption(page, title, body = "") {
  await page.evaluate(
    ({ title, body }) => {
      let caption = document.getElementById("codex-demo-caption");
      if (!caption) {
        caption = document.createElement("div");
        caption.id = "codex-demo-caption";
        caption.style.position = "fixed";
        caption.style.left = "28px";
        caption.style.bottom = "28px";
        caption.style.zIndex = "999999";
        caption.style.maxWidth = "520px";
        caption.style.padding = "16px 18px";
        caption.style.borderRadius = "12px";
        caption.style.background = "rgba(13, 17, 23, 0.92)";
        caption.style.color = "#f0f6fc";
        caption.style.boxShadow = "0 18px 50px rgba(0, 0, 0, 0.35)";
        caption.style.border = "1px solid rgba(255, 255, 255, 0.14)";
        caption.style.fontFamily = "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";
        caption.style.pointerEvents = "none";
        caption.style.transition = "opacity 220ms ease, transform 220ms ease";
        document.body.appendChild(caption);
      }
      caption.innerHTML = `
        <div style="font-size: 15px; font-weight: 750; letter-spacing: .02em;">${title}</div>
        ${body ? `<div style="margin-top: 6px; font-size: 13px; line-height: 1.45; color: #c9d1d9;">${body}</div>` : ""}
      `;
      caption.style.opacity = "1";
      caption.style.transform = "translateY(0)";
    },
    { title, body }
  );
}

async function setEditorCode(page, code) {
  await page.waitForSelector(".monaco-editor", { timeout: 30000 });
  await page.evaluate((nextCode) => {
    const monacoApi = window.monaco;
    const model = monacoApi?.editor?.getModels?.()[0];
    if (!model) {
      throw new Error("Monaco model is not ready");
    }
    model.setValue(nextCode);
  }, code);
  await page.locator(".monaco-editor").click();
}

async function clickSubmit(page) {
  await page.getByRole("button", { name: /提交代码|重新提交/ }).click();
}

async function waitForText(page, pattern, timeout = 90000) {
  await page.getByText(pattern).first().waitFor({ timeout });
}

async function waitForExactText(page, text, timeout = 90000) {
  await page.getByText(text, { exact: true }).first().waitFor({ timeout });
}

async function main() {
  let recordStartedAt = Date.now();
  let failureVisibleAt = null;
  let diagnosisVisibleAt = null;
  const elapsedSeconds = () => (Date.now() - recordStartedAt) / 1000;

  const browser = await chromium.launch({
    headless: process.env.HEADED === "1" ? false : true,
  });
  const context = await browser.newContext({
    viewport,
    recordVideo: {
      dir: tempVideoDir,
      size: viewport,
    },
  });
  await context.addInitScript(() => {
    window.localStorage.clear();
  });

  const page = await context.newPage();
  page.setDefaultTimeout(30000);

  try {
    recordStartedAt = Date.now();
    await page.goto(appUrl, { waitUntil: "networkidle" });
    await setCaption(page, "Demo Case 1：两数之和", "真实页面录制：错误提交 -> 判题失败 -> AI 诊断 -> 修正通过");
    await page.waitForTimeout(1800);

    await setCaption(page, "1. 粘贴错误代码", "bug：先写入 HashMap，再查询 complement，重复元素时可能自匹配。");
    await setEditorCode(page, bugCode);
    await page.waitForTimeout(1600);

    await setCaption(page, "2. 提交并观察失败用例", "后端会调用真实 POST /api/submissions，通过 Piston 执行 Java。");
    await clickSubmit(page);
    await waitForText(page, /判题中/);
    await waitForExactText(page, "未通过", 90000);
    await waitForText(page, /失败用例/, 90000);
    failureVisibleAt = elapsedSeconds();
    await page.waitForTimeout(2200);

    await setCaption(page, "3. AI 自动诊断", "失败后页面切到 AI 诊断，只解释本次错误和训练建议。");
    await waitForText(page, /AI 正在分析|错误诊断|诊断完成/, 120000);
    await waitForText(page, /诊断完成|错误诊断/, 120000);
    diagnosisVisibleAt = elapsedSeconds();
    await page.waitForTimeout(2800);

    await setCaption(page, "4. 粘贴修正代码", "修正：先查询 target - nums[i]，未命中时再写入当前数字。");
    await setEditorCode(page, fixedCode);
    await page.waitForTimeout(1800);

    await setCaption(page, "5. 再次提交", "同一个题目、同一套测试，确认修正后通过。");
    await clickSubmit(page);
    await waitForText(page, /判题中/);
    await waitForExactText(page, "通过", 90000);
    await page.waitForTimeout(2600);

    await setCaption(page, "演示闭环完成", "Judge -> AI Diagnosis -> Weakness Memory -> Training Plan，真实页面与真实接口跑通。");
    await page.waitForTimeout(2600);
  } finally {
    const video = page.video();
    await context.close();
    await browser.close();

    const rawVideoPath = await video?.path();
    if (!rawVideoPath) {
      throw new Error("Playwright did not produce a video file");
    }
    fs.mkdirSync(outputDir, { recursive: true });
    fs.copyFileSync(rawVideoPath, rawWebmPath);

    let sourceWebmPath = rawWebmPath;
    const canShortenDiagnosisGap =
      failureVisibleAt != null &&
      diagnosisVisibleAt != null &&
      diagnosisVisibleAt - failureVisibleAt > diagnosisGapSeconds + 0.75;

    if (canShortenDiagnosisGap) {
      const keepAfterFailure = 5.5;
      const keepBeforeDiagnosis = diagnosisGapSeconds - keepAfterFailure;
      const cutStart = Math.max(0, failureVisibleAt + keepAfterFailure);
      const cutEnd = Math.max(cutStart + 0.1, diagnosisVisibleAt - keepBeforeDiagnosis);
      const editResult = spawnSync(
        "ffmpeg",
        [
          "-y",
          "-i",
          rawWebmPath,
          "-filter_complex",
          `[0:v]trim=start=0:end=${cutStart.toFixed(3)},setpts=PTS-STARTPTS[v0];[0:v]trim=start=${cutEnd.toFixed(3)},setpts=PTS-STARTPTS[v1];[v0][v1]concat=n=2:v=1:a=0[v]`,
          "-map",
          "[v]",
          "-c:v",
          "libvpx",
          "-b:v",
          "1M",
          finalWebmPath,
        ],
        { stdio: "inherit" }
      );

      if (editResult.status === 0) {
        sourceWebmPath = finalWebmPath;
        console.log(
          `Shortened failure-to-diagnosis gap from ${(diagnosisVisibleAt - failureVisibleAt).toFixed(1)}s to about ${diagnosisGapSeconds}s`
        );
      } else {
        fs.copyFileSync(rawWebmPath, finalWebmPath);
      }
    } else {
      fs.copyFileSync(rawWebmPath, finalWebmPath);
    }

    const ffmpegResult = spawnSync(
      "ffmpeg",
      [
        "-y",
        "-i",
        sourceWebmPath,
        "-an",
        "-c:v",
        "libx264",
        "-pix_fmt",
        "yuv420p",
        "-movflags",
        "+faststart",
        finalMp4Path,
      ],
      { stdio: "inherit" }
    );

    if (ffmpegResult.status === 0) {
      console.log(finalMp4Path);
    } else {
      console.log(finalWebmPath);
    }
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
