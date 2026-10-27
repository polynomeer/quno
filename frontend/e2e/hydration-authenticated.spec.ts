import { test, expect, type ConsoleMessage } from "@playwright/test";

/**
 * Signed-in pages must hydrate without a mismatch. The server never sees the localStorage token,
 * so anything a page derives from the session has to look exactly like "no session" on the first
 * client render too — including content inside a <Suspense> boundary, which hydrates in a later
 * pass after AppHeader has already started the shared `/me` query (기술 딥다이브 16절).
 * Unit tests can't catch this (no SSR HTML to compare against), so it's checked here against the
 * real dev server.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";
const RUN = `${Date.now()}`;
const TAG = `hydration-e2e-${RUN}`;

let accessToken: string;
let refreshToken: string;
let userId: number;
let questionId: number;

test.beforeAll(async ({ playwright }) => {
  const api = await playwright.request.newContext();
  const unique = `hydration${RUN}`;
  const email = `${unique}@example.com`;
  const signUp = await api.post(`${API_BASE_URL}/api/v1/auth/signup`, {
    data: { email, nickname: unique, password: "password123" },
  });
  expect(signUp.ok(), await signUp.text()).toBeTruthy();
  userId = ((await signUp.json()) as { id: number }).id;
  const login = await api.post(`${API_BASE_URL}/api/v1/auth/login`, { data: { email, password: "password123" } });
  expect(login.ok(), await login.text()).toBeTruthy();
  ({ accessToken, refreshToken } = (await login.json()) as { accessToken: string; refreshToken: string });
  const created = await api.post(`${API_BASE_URL}/api/v1/questions`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    data: { title: `Hydration check ${RUN}`, body: "Body.", tags: [TAG] },
  });
  expect(created.ok(), await created.text()).toBeTruthy();
  questionId = ((await created.json()) as { id: number }).id;
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  await page.addInitScript(
    ([access, refresh]) => {
      localStorage.setItem("quno.accessToken", access);
      localStorage.setItem("quno.refreshToken", refresh);
    },
    [accessToken, refreshToken],
  );
});

const paths: Array<() => string> = [
  () => "/",
  () => "/questions",
  () => "/questions?q=hydration",
  () => `/questions/${questionId}`,
  () => `/questions/${questionId}/versions`,
  () => "/tags",
  () => `/tags/${TAG}`,
  () => "/organizations",
  () => `/users/${userId}`,
  () => "/ask",
  () => "/saved",
  () => "/watching",
  () => "/notifications",
  () => "/direct-asks",
];

for (const path of paths) {
  test(`${path().replace(/\d+/g, ":id")} hydrates without a mismatch when signed in`, async ({ page }) => {
    const hydrationErrors: string[] = [];
    page.on("console", (message: ConsoleMessage) => {
      if (message.type() === "error" && /hydrat/i.test(message.text())) {
        hydrationErrors.push(message.text().slice(0, 500));
      }
    });
    page.on("pageerror", (error) => {
      if (/hydrat/i.test(error.message)) hydrationErrors.push(error.message.slice(0, 500));
    });

    await page.goto(path());
    // The /me request resolving is the point after which a late-hydrating boundary would disagree.
    await expect(page.getByRole("button", { name: /로그아웃|Log out/i })).toBeVisible();
    await page.waitForLoadState("networkidle");

    expect(hydrationErrors).toEqual([]);
  });
}
