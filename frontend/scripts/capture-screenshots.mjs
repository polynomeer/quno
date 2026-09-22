// README용 스크린샷을 실제 렌더링된 화면에서 캡처하는 1회성 스크립트.
// 사용법: node scripts/capture-screenshots.mjs (프론트엔드 dev 서버가 http://localhost:3000에 떠 있어야 함)
import { chromium } from "@playwright/test";
import { mkdirSync } from "node:fs";
import path from "node:path";

const OUT_DIR = path.resolve(import.meta.dirname, "../../docs/screenshots");
mkdirSync(OUT_DIR, { recursive: true });

const BASE_URL = "http://localhost:3000";
const DEMO = { email: "demo@quno.dev", password: "password123" };

async function shot(page, name, { fullPage = false } = {}) {
  await page.screenshot({ path: path.join(OUT_DIR, `${name}.png`), fullPage });
  console.log(`saved ${name}.png`);
}

async function main() {
  const browser = await chromium.launch();
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: "dark" });
  const page = await context.newPage();

  // 1. 홈 (비로그인)
  await page.goto(BASE_URL);
  await page.waitForTimeout(500);
  await shot(page, "home-guest");

  // 2. 로그인
  await page.goto(`${BASE_URL}/login`);
  await page.waitForTimeout(300);
  await shot(page, "login");

  // 3. 회원가입
  await page.goto(`${BASE_URL}/signup`);
  await page.waitForTimeout(300);
  await shot(page, "signup");

  // 로그인 진행 (이후 화면은 로그인 상태로 캡처)
  await page.goto(`${BASE_URL}/login`);
  await page.getByPlaceholder("Email").fill(DEMO.email);
  await page.getByPlaceholder("Password").fill(DEMO.password);
  await page.getByRole("button", { name: "로그인" }).click();
  await page.waitForURL(BASE_URL + "/");
  await page.waitForTimeout(500);
  await shot(page, "home-dashboard");

  // 4. 질문 상세 (채택된 답변 포함)
  await page.goto(`${BASE_URL}/questions/887`);
  await page.waitForTimeout(500);
  await shot(page, "question-detail", { fullPage: true });

  // 5. 질문 작성
  await page.goto(`${BASE_URL}/ask`);
  await page.waitForTimeout(500);
  await shot(page, "ask");

  // 6. 태그 상세
  await page.goto(`${BASE_URL}/tags/kotlin`);
  await page.waitForTimeout(500);
  await shot(page, "tag-detail");

  // 7. 조직 목록
  await page.goto(`${BASE_URL}/organizations`);
  await page.waitForTimeout(500);
  await shot(page, "organizations");

  // 8. 모바일 반응형 헤더
  await context.close();
  const mobileContext = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: "dark" });
  const mobilePage = await mobileContext.newPage();
  await mobilePage.goto(`${BASE_URL}/questions/887`);
  await mobilePage.waitForTimeout(500);
  await shot(mobilePage, "mobile-question-detail", { fullPage: true });
  await mobileContext.close();

  await browser.close();
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
