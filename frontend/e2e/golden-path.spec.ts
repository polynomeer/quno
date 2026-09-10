import { test, expect } from "@playwright/test";

/**
 * 핵심 골든 패스: 회원가입 → 질문 작성 → 답변 작성(production-readiness.md B-5).
 * 매 실행마다 고유한 이메일/닉네임으로 새 계정을 만들어 다른 테스트/시드 데이터와 충돌하지 않는다.
 * 회원가입 화면(`/signup`)은 이 Phase에서 처음 발견한 격차라 함께 새로 만들었다 — 이전에는 API만
 * 있고 UI가 전혀 없어 실제로는 아무도 가입할 수 없었다.
 */
test("a new user can sign up, ask a question, and answer it", async ({ page }) => {
  const runId = Date.now();
  const email = `e2e-${runId}@example.com`;
  const nickname = `e2e${runId}`;
  const password = "password123";
  const questionTitle = `E2E test question ${runId}`;
  const questionBody = `Body for E2E test question ${runId}.`;
  const answerBody = `E2E test answer ${runId}.`;

  await test.step("sign up", async () => {
    await page.goto("/signup");
    await page.getByPlaceholder("Email").fill(email);
    await page.getByPlaceholder("Nickname").fill(nickname);
    await page.getByPlaceholder("Password").fill(password);
    await page.getByRole("button", { name: "회원가입" }).click();

    await expect(page.getByRole("link", { name: nickname })).toBeVisible();
  });

  await test.step("ask a question", async () => {
    await page.goto("/ask");
    await page.getByPlaceholder("Title").fill(questionTitle);
    await page.getByPlaceholder("본문을 작성하세요 (Markdown 지원)").fill(questionBody);
    await page.getByRole("button", { name: "Post" }).click();

    await expect(page).toHaveURL(/\/questions\/\d+$/);
    await expect(page.getByRole("heading", { name: questionTitle })).toBeVisible();
  });

  await test.step("answer the question", async () => {
    await page.getByPlaceholder("답변을 작성하세요 (Markdown 지원)").fill(answerBody);
    await page.getByRole("button", { name: "Post Answer" }).click();

    await expect(page.getByText(answerBody)).toBeVisible();
    await expect(page.getByRole("heading", { name: "1 Answers" })).toBeVisible();
  });
});
