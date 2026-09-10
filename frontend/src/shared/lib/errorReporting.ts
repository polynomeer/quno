let sentryInitialized = false;

/**
 * NEXT_PUBLIC_SENTRY_DSN이 없으면(기본값) 아무것도 하지 않는다 — 계정 개설·DSN 발급은 사람이
 * 할 일(production-readiness.md A)이라 이게 없어도 앱이 정상 동작해야 한다. 백엔드의 Sentry
 * 연동(ADR-0045)과 같은 "DSN 없으면 자동 비활성화" 원칙.
 */
async function ensureSentryInitialized(): Promise<typeof import("@sentry/browser") | null> {
  const dsn = process.env.NEXT_PUBLIC_SENTRY_DSN;
  if (!dsn) return null;

  const Sentry = await import("@sentry/browser");
  if (!sentryInitialized) {
    Sentry.init({ dsn, environment: process.env.NODE_ENV });
    sentryInitialized = true;
  }
  return Sentry;
}

export async function reportError(error: Error, context?: Record<string, unknown>): Promise<void> {
  console.error(error);
  const Sentry = await ensureSentryInitialized();
  Sentry?.captureException(error, context ? { extra: context } : undefined);
}
