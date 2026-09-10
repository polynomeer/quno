import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    // eslint-config-next의 typescript 프리셋은 이 둘을 'warn'으로만 둔다 — 경고는 npm run
    // lint/CI를 실패시키지 않아 사실상 방치되기 쉽다(quality-improvement-plan.md Q-1). 이
    // 코드베이스는 이미 any를 전혀 쓰지 않아 error로 올려도 즉시 고칠 게 없었다.
    rules: {
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-unused-vars": "error",
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    // vitest --coverage 산출물(quality-improvement-plan.md Q-1) — 생성된 리포트 스크립트가
    // eslint 대상에 잡혀 무관한 경고를 냈다.
    "coverage/**",
    "playwright-report/**",
    "test-results/**",
  ]),
]);

export default eslintConfig;
