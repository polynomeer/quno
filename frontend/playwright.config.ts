import { defineConfig, devices } from "@playwright/test";

// 백엔드+DB는 이 설정이 관리하지 않는다 — run.sh(로컬) 또는 CI의 backend 잡과 같은 방식으로
// 별도로 띄워져 있어야 한다(README/CI 참고). 여기서는 프론트엔드 dev 서버만 관리한다.
const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";

export default defineConfig({
  testDir: "./e2e",
  // 테스트마다 새로 회원가입하는 계정을 쓰지만, 그래도 백엔드 상태(질문 개수 등)를 공유하는
  // 테스트가 섞일 수 있어 순차 실행이 더 예측 가능하다.
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: "list",
  use: {
    baseURL,
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
  webServer: {
    command: "npm run dev",
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
