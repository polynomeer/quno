import { test, expect } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

/**
 * 자동 접근성 검사(quality-improvement-plan.md Q-3). axe-core의 "wcag2a"/"wcag2aa" 룰셋만
 * 돌린다 — best-practice 룰까지 포함하면 색 대비처럼 디자인 토큰 조정이 필요한 지적까지 한 번에
 * 쏟아져서, 우선 WCAG A/AA 위반(스크린 리더로 못 읽는 수준의 문제)부터 잡는다.
 */
const PUBLIC_PAGES = ["/", "/questions", "/tags", "/organizations", "/login", "/signup"];

for (const path of PUBLIC_PAGES) {
  test(`${path} has no WCAG A/AA violations`, async ({ page }) => {
    await page.goto(path);
    const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();

    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";

test("question detail (anonymous, with the login-to-answer inline link) has no WCAG A/AA violations", async ({ page, request }) => {
  const search = await request.get(`${API_BASE_URL}/api/v1/search?q=a&limit=1`);
  const [question] = (await search.json()) as Array<{ id: number }>;
  test.skip(!question, "검색 결과가 없어 질문 상세 페이지를 찾을 수 없습니다");

  await page.goto(`/questions/${question.id}`);
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();

  expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
});

test("/ask (authenticated) has no WCAG A/AA violations", async ({ page }) => {
  const email = `a11y-${Date.now()}@example.com`;
  await page.goto("/signup");
  await page.getByPlaceholder("Email").fill(email);
  await page.getByPlaceholder("Nickname").fill(`a11y${Date.now()}`);
  await page.getByPlaceholder("Password").fill("password123");
  await page.getByRole("button", { name: "회원가입" }).click();
  await expect(page).toHaveURL("/");

  await page.goto("/ask");
  const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();

  expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
});
