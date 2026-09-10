import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import path from "node:path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    globals: true,
    // e2e/는 Playwright(test:e2e)가 다룬다 — 기본 include 패턴이 *.spec.ts까지 잡아서 vitest가
    // Playwright의 test()를 자기 것으로 착각해 충돌하므로 명시적으로 제외한다.
    exclude: ["node_modules", "e2e/**"],
    coverage: {
      provider: "v8",
      reporter: ["text", "html"],
      // 수치를 강제하는 게 목적이 아니라 "PR에서 급격히 떨어지면 눈에 띄게" 하는 용도라
      // 임계값 실패는 만들지 않는다(quality-improvement-plan.md Q-1).
      include: ["src/**/*.{ts,tsx}"],
      exclude: ["src/**/*.test.{ts,tsx}", "src/app/**/layout.tsx", "src/app/**/page.tsx"],
    },
  },
});
