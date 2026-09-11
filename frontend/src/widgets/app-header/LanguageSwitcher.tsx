"use client";

import { useLocale, type Locale } from "@/shared/i18n/LocaleProvider";
import { cn } from "@/shared/lib/cn";

const LOCALES: { value: Locale; label: string }[] = [
  { value: "ko", label: "한국어" },
  { value: "en", label: "English" },
];

/** ADR-0053: 홈/로그인/회원가입/질문 작성 4곳만 이 스위처의 영향을 받는다 — 나머지 화면은
 * 로케일과 무관하게 항상 한국어로 보인다. */
export function LanguageSwitcher() {
  const { locale, setLocale, t } = useLocale();

  return (
    <div className="inline-flex items-center gap-1" role="group" aria-label={t.languageSwitcher.label}>
      {LOCALES.map(({ value, label }) => (
        <button
          key={value}
          type="button"
          onClick={() => setLocale(value)}
          aria-pressed={locale === value}
          className={cn(
            "rounded-md px-2 py-1 text-xs font-medium transition-colors",
            locale === value ? "bg-surface-subtle text-text-primary" : "text-text-secondary hover:text-text-primary",
          )}
        >
          {label}
        </button>
      ))}
    </div>
  );
}
