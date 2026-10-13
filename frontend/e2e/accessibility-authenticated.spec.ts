import { test, expect, type APIRequestContext } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

/**
 * Signed-in counterpart to accessibility.spec.ts. That file only reaches what an anonymous visitor
 * sees (plus /ask), so member-only UI — the home side nav, question detail's vote rail, QPR form,
 * Cluster/Fork/live-chat panels, Watching/Saved lists, notifications, the profile — was never
 * checked by axe. Same rule set (wcag2a + wcag2aa) and the same "zero violations" bar.
 *
 * CI runs against an empty database, so the setup creates its own data through the API: user A
 * asks a tagged question, user B (the viewer) answers, comments, watches and saves it. Viewing as
 * a non-author is deliberate — that's the state that renders the most interactive controls.
 */
const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8081";
// Unique per run so a reused local database (unlike CI's empty one) never has look-alike rows.
const RUN = `${Date.now()}`;
const TAG = `a11y-e2e-${RUN}`;
const TITLE = `Accessibility check ${RUN}: coroutine transaction context`;

interface Session {
  userId: number;
  accessToken: string;
  refreshToken: string;
}

async function signUpAndLogIn(api: APIRequestContext, label: string): Promise<Session> {
  const unique = `${label}${Date.now()}${Math.floor(Math.random() * 1000)}`;
  const email = `${unique}@example.com`;
  const signUp = await api.post(`${API_BASE_URL}/api/v1/auth/signup`, {
    data: { email, nickname: unique, password: "password123" },
  });
  expect(signUp.ok(), await signUp.text()).toBeTruthy();
  const { id } = (await signUp.json()) as { id: number };

  const login = await api.post(`${API_BASE_URL}/api/v1/auth/login`, { data: { email, password: "password123" } });
  expect(login.ok(), await login.text()).toBeTruthy();
  const { accessToken, refreshToken } = (await login.json()) as { accessToken: string; refreshToken: string };
  return { userId: id, accessToken, refreshToken };
}

async function apiPost(api: APIRequestContext, session: Session, path: string, data?: unknown) {
  const response = await api.post(`${API_BASE_URL}${path}`, {
    headers: { Authorization: `Bearer ${session.accessToken}` },
    data,
  });
  expect(response.ok(), `${path}: ${await response.text()}`).toBeTruthy();
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

let viewer: Session;
let questionId: number;

test.beforeAll(async ({ playwright }) => {
  const api = await playwright.request.newContext();
  const author = await signUpAndLogIn(api, "a11yauthor");
  viewer = await signUpAndLogIn(api, "a11yviewer");

  const question = await apiPost(api, author, "/api/v1/questions", {
    title: TITLE,
    body: "Body with `inline code`.\n\n```kotlin\nsuspend fun run() = Unit\n```",
    tags: [TAG],
  });
  questionId = question.id;

  const answer = await apiPost(api, viewer, `/api/v1/questions/${questionId}/answers`, { body: "An answer from the viewer." });
  await apiPost(api, viewer, `/api/v1/questions/${questionId}/comments`, { body: "A question comment." });
  await apiPost(api, viewer, `/api/v1/answers/${answer.id}/comments`, { body: "An answer comment." });
  await apiPost(api, viewer, `/api/v1/questions/${questionId}/watch`);
  await apiPost(api, viewer, `/api/v1/questions/${questionId}/save`);
  await api.dispose();
});

test.beforeEach(async ({ page }) => {
  // Wide enough for the home page's three-column layout, so the xl-only side nav is rendered.
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.addInitScript(
    ([accessToken, refreshToken]) => {
      localStorage.setItem("quno.accessToken", accessToken);
      localStorage.setItem("quno.refreshToken", refreshToken);
    },
    [viewer.accessToken, viewer.refreshToken],
  );
});

const pages: Array<{ name: string; path: () => string; ready: (page: import("@playwright/test").Page) => Promise<void> }> = [
  {
    name: "home with side nav",
    path: () => "/",
    ready: async (page) => {
      await expect(page.getByRole("navigation", { name: "주 메뉴" })).toBeVisible();
    },
  },
  {
    name: "question detail (signed in, non-author)",
    path: () => `/questions/${questionId}`,
    ready: async (page) => {
      await expect(page.getByRole("button", { name: "Upvote" }).first()).toBeVisible();
      await expect(page.getByText("An answer from the viewer.")).toBeVisible();
      await expect(page.getByRole("heading", { name: "실시간 질문방" })).toBeVisible();
    },
  },
  {
    name: "question revision history",
    path: () => `/questions/${questionId}/versions`,
    ready: async (page) => {
      await expect(page.getByText(/revision 1/)).toBeVisible();
    },
  },
  {
    name: "watching list",
    path: () => "/watching",
    ready: async (page) => {
      await expect(page.getByRole("link", { name: TITLE })).toBeVisible();
    },
  },
  {
    name: "saved list",
    path: () => "/saved",
    ready: async (page) => {
      await expect(page.getByRole("link", { name: TITLE })).toBeVisible();
    },
  },
  {
    name: "notifications",
    path: () => "/notifications",
    ready: async (page) => {
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    },
  },
  {
    name: "own profile",
    path: () => `/users/${viewer.userId}`,
    ready: async (page) => {
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    },
  },
  {
    name: "tag detail",
    path: () => `/tags/${TAG}`,
    ready: async (page) => {
      await expect(page.getByRole("link", { name: TITLE })).toBeVisible();
    },
  },
  {
    name: "direct asks",
    path: () => "/direct-asks",
    ready: async (page) => {
      await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    },
  },
];

for (const target of pages) {
  test(`${target.name} has no WCAG A/AA violations`, async ({ page }) => {
    await page.goto(target.path());
    await target.ready(page);
    await page.waitForLoadState("networkidle");

    const results = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa"]).analyze();

    expect(results.violations, JSON.stringify(results.violations, null, 2)).toEqual([]);
  });
}
