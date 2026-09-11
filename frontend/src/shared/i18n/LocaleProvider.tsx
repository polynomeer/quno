"use client";

import { createContext, useCallback, useContext, useEffect, useSyncExternalStore, type ReactNode } from "react";
import { ko, en, type Dictionary } from "./dictionary";

export type Locale = "ko" | "en";

const STORAGE_KEY = "quno:locale";
const dictionaries: Record<Locale, Dictionary> = { ko, en };
const listeners = new Set<() => void>();

function isLocale(value: string | null): value is Locale {
  return value === "ko" || value === "en";
}

function readStoredLocale(): Locale {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return isLocale(stored) ? stored : "ko";
  } catch {
    return "ko";
  }
}

function subscribe(callback: () => void) {
  listeners.add(callback);
  return () => listeners.delete(callback);
}

function getServerSnapshot(): Locale {
  return "ko";
}

/** 서버는 항상 ko 스냅샷을 쓰고, 클라이언트는 하이드레이션 직후부터 localStorage를 읽는다 —
 * useSyncExternalStore가 이 전환을 자체적으로 처리해 이펙트에서 setState하며 생기는
 * cascading render 없이 하이드레이션 불일치도 피한다(ADR-0053). */
export function LocaleProvider({ children }: { children: ReactNode }) {
  const locale = useSyncExternalStore(subscribe, readStoredLocale, getServerSnapshot);

  useEffect(() => {
    document.documentElement.lang = locale;
  }, [locale]);

  const setLocale = useCallback((next: Locale) => {
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // 저장 실패해도 아래 listeners 알림으로 이번 세션 동안은 계속 반영됨
    }
    listeners.forEach((listener) => listener());
  }, []);

  return <LocaleContext.Provider value={{ locale, setLocale, t: dictionaries[locale] }}>{children}</LocaleContext.Provider>;
}

interface LocaleContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: Dictionary;
}

const LocaleContext = createContext<LocaleContextValue | null>(null);

export function useLocale() {
  const ctx = useContext(LocaleContext);
  if (!ctx) {
    throw new Error("useLocale must be used within LocaleProvider");
  }
  return ctx;
}
