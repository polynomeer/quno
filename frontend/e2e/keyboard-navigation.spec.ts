import { test, expect } from "@playwright/test";

/**
 * 키보드만으로 핵심 플로우(질문 작성)를 완주할 수 있는지 확인한다(quality-improvement-plan.md
 * Q-3). axe-core(accessibility.spec.ts)는 정적 마크업 위반은 잡지만 실제로 Tab/Enter만으로
 * 끝까지 진행되는지는 검증하지 못해 별도로 둔다. 마우스 클릭(`.click()`)을 전혀 쓰지 않는다 —
 * 포커스 이동은 Tab, 입력은 키보드 타이핑, 제출은 포커스된 버튼에서 Enter로만 한다.
 */
test("a question can be asked using only the keyboard", async ({ page }) => {
  const runId = Date.now();
  const email = `kbd-${runId}@example.com`;
  const nickname = `kbd${runId}`;
  const title = `Keyboard-only test question ${runId}`;
  const body = `Body for keyboard-only test ${runId}.`;

  // 회원가입도 키보드만으로 — Tab으로 필드를 옮겨 다니고 Enter로 제출한다.
  await page.goto("/signup");
  await page.keyboard.press("Tab"); // Skip link 등 헤더 요소를 건너뛸 수 있어 넉넉히 이동
  await page.getByPlaceholder("Email").focus();
  await page.keyboard.type(email);
  await page.keyboard.press("Tab");
  await expect(page.getByPlaceholder("Nickname")).toBeFocused();
  await page.keyboard.type(nickname);
  await page.keyboard.press("Tab");
  await expect(page.getByPlaceholder("Password")).toBeFocused();
  await page.keyboard.type("password123");
  await page.keyboard.press("Enter");

  await expect(page.getByRole("link", { name: nickname })).toBeVisible();

  await page.goto("/ask");
  await page.getByPlaceholder("Title").focus();
  await page.keyboard.type(title);

  // MarkdownEditor는 Write/Preview 탭 버튼 다음에 textarea가 온다 — Tab 순서를 가정하지 않고
  // role/placeholder로 실제 요소를 찾아 포커스만 키보드로 확인한다.
  const bodyField = page.getByPlaceholder("본문을 작성하세요 (Markdown 지원)");
  await bodyField.focus();
  await page.keyboard.type(body);

  const postButton = page.getByRole("button", { name: "Post" });
  await postButton.focus();
  await expect(postButton).toBeFocused();
  await page.keyboard.press("Enter");

  await expect(page).toHaveURL(/\/questions\/\d+$/);
  await expect(page.getByRole("heading", { name: title })).toBeVisible();
});
