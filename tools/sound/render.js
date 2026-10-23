// sfx.html을 헤드리스 크롬으로 열어 "이름 base64(WAV)" 줄들을 출력
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ executablePath: process.env.CHROME || '/opt/pw-browsers/chromium-1194/chrome-linux/chrome' });
  const page = await browser.newPage();
  await page.goto('file://' + require('path').resolve(__dirname, 'sfx.html'));
  await page.waitForFunction(() => document.getElementById('out').textContent.endsWith('DONE'), null, { timeout: 60000 });
  process.stdout.write(await page.$eval('#out', e => e.textContent));
  await browser.close();
})();
