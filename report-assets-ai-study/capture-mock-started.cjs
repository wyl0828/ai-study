const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1440, height: 1100 }, deviceScaleFactor: 1 });
  await page.goto('http://127.0.0.1:4000/mock-interview', { waitUntil: 'networkidle', timeout: 60000 });
  await page.getByRole('button', { name: /开始模拟面试/ }).click({ timeout: 30000 });
  await page.waitForTimeout(6000);
  await page.screenshot({ path: 'D:\\code\\ai-study\\report-assets-ai-study\\mock-interview-started-ai-study.png', fullPage: false });
  console.log('url=' + page.url());
  console.log('text=' + (await page.locator('body').innerText()).slice(0, 300).replace(/\s+/g, ' '));
  await browser.close();
})();
