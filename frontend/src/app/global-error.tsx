"use client";

import { useEffect } from "react";
import { reportError } from "@/shared/lib/errorReporting";

// global-error는 루트 레이아웃 자체를 대체하므로 globals.css/폰트가 적용되지 않는다 — 자체
// html/body와 인라인 스타일을 정의해야 한다(Next.js 공식 안내). 앱이 OS 색상 스킴만 따르는
// 것과 동일하게(별도 테마 토글 없음) prefers-color-scheme 미디어쿼리로 다크모드를 맞춘다.
export default function GlobalError({
  error,
  retry,
}: {
  error: Error & { digest?: string };
  retry: () => void;
}) {
  useEffect(() => {
    reportError(error);
  }, [error]);

  return (
    <html lang="ko">
      <body
        style={{
          margin: 0,
          minHeight: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontFamily:
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
          background: "#ffffff",
          color: "#16181d",
        }}
      >
        <style>{`
          @media (prefers-color-scheme: dark) {
            body { background: #0e0f12 !important; color: #f2f3f5 !important; }
            #global-error-retry { background: #60a5fa !important; color: #0e0f12 !important; }
          }
        `}</style>
        <div style={{ textAlign: "center", padding: "0 1rem" }}>
          <h2 style={{ fontSize: "1.125rem", fontWeight: 600, margin: 0 }}>문제가 발생했습니다</h2>
          <p style={{ fontSize: "0.875rem", opacity: 0.7, marginTop: "0.5rem" }}>
            앱을 불러오는 중 심각한 오류가 발생했습니다.
          </p>
          <button
            id="global-error-retry"
            onClick={() => retry()}
            style={{
              marginTop: "1rem",
              borderRadius: "0.375rem",
              padding: "0.5rem 1rem",
              fontSize: "0.875rem",
              fontWeight: 500,
              background: "#2563eb",
              color: "#ffffff",
              border: "none",
              cursor: "pointer",
            }}
          >
            다시 시도
          </button>
        </div>
      </body>
    </html>
  );
}
