"use client";

import Link from "next/link";
import { useState } from "react";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLogout } from "@/features/auth/hooks/useLogin";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { Button } from "@/shared/ui/Button";
import { cn } from "@/shared/lib/cn";
import { SearchBox } from "./SearchBox";
import { LanguageSwitcher } from "./LanguageSwitcher";

const navLinkClass = "text-sm font-medium text-text-body hover:text-text-primary";

/** 반응형 헤더 — 데스크톱은 한 줄, 모바일(md 미만)은 햄버거 메뉴로 접는다. */
export function AppHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { data: me, isLoading } = useSession();
  const logout = useLogout();
  const { data: notifications } = useNotifications(Boolean(me));
  const unreadCount = notifications?.filter((n) => !n.isRead).length ?? 0;
  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-surface">
      <div className="mx-auto flex max-w-[1360px] items-center gap-4 px-4 py-3 sm:gap-6 sm:px-6">
        {/* The live dot is the "living question" mark from the design canvas (ADR-0061). */}
        <Link
          href="/"
          className="flex shrink-0 items-center gap-2 font-mono text-xl font-semibold tracking-tight text-text-primary"
        >
          <span aria-hidden="true" className="size-2.5 rounded-full bg-live ring-4 ring-live-ring" />
          quno
        </Link>
        <SearchBox />
        <nav className="hidden items-center gap-4 md:flex">
          <Link href="/tags" className={navLinkClass}>
            Tags
          </Link>
          <Link href="/organizations" className={navLinkClass}>
            Organizations
          </Link>
          {me && (
            <>
              <Link href="/watching" className={navLinkClass}>
                Watching
              </Link>
              <Link href="/saved" className={navLinkClass}>
                Saved
              </Link>
              <Link href="/direct-asks" className={navLinkClass}>
                Direct Asks
              </Link>
              <Link href="/notifications" className={cn("relative", navLinkClass)}>
                Notifications
                {unreadCount > 0 && (
                  <span className="ml-1 inline-flex min-w-[1.25rem] items-center justify-center rounded-full bg-brand px-1 font-mono text-[11px] font-bold text-brand-foreground">
                    {unreadCount}
                  </span>
                )}
              </Link>
            </>
          )}
          <Link href="/ask">
            <Button variant="secondary">Ask</Button>
          </Link>
          <LanguageSwitcher />
          {isLoading ? null : me ? (
            <div className="flex items-center gap-3">
              <Link
                href={`/users/${me.id}`}
                className="flex items-center gap-2 rounded-full border border-border py-1 pr-3 pl-1 text-sm font-medium text-text-primary hover:border-border-strong"
              >
                <span
                  aria-hidden="true"
                  className="grid size-7 place-items-center rounded-full bg-text-primary text-xs font-semibold text-surface"
                >
                  {me.nickname.slice(0, 1).toUpperCase()}
                </span>
                {me.nickname}
              </Link>
              <Button variant="ghost" onClick={logout}>
                Log out
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2">
              <Link href="/signup">
                <Button variant="ghost">Sign up</Button>
              </Link>
              <Link href="/login">
                <Button variant="secondary">Log in</Button>
              </Link>
            </div>
          )}
        </nav>
        <button
          type="button"
          className="inline-flex shrink-0 items-center justify-center rounded-md p-2 text-text-secondary hover:bg-surface-subtle md:hidden"
          aria-label={menuOpen ? "Close menu" : "Open menu"}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" strokeWidth="2">
            {menuOpen ? (
              <path strokeLinecap="round" d="M6 6l12 12M18 6l-12 12" />
            ) : (
              <path strokeLinecap="round" d="M4 7h16M4 12h16M4 17h16" />
            )}
          </svg>
        </button>
      </div>
      {menuOpen && (
        <nav className="flex flex-col gap-1 border-t border-border px-4 py-3 md:hidden">
          <Link href="/tags" className={cn(navLinkClass, "py-2")} onClick={closeMenu}>
            Tags
          </Link>
          <Link href="/organizations" className={cn(navLinkClass, "py-2")} onClick={closeMenu}>
            Organizations
          </Link>
          {me && (
            <>
              <Link href="/watching" className={cn(navLinkClass, "py-2")} onClick={closeMenu}>
                Watching
              </Link>
              <Link href="/saved" className={cn(navLinkClass, "py-2")} onClick={closeMenu}>
                Saved
              </Link>
              <Link href="/direct-asks" className={cn(navLinkClass, "py-2")} onClick={closeMenu}>
                Direct Asks
              </Link>
              <Link href="/notifications" className={cn(navLinkClass, "flex items-center py-2")} onClick={closeMenu}>
                Notifications
                {unreadCount > 0 && (
                  <span className="ml-1 inline-flex min-w-[1.25rem] items-center justify-center rounded-full bg-brand px-1 text-xs font-semibold text-brand-foreground">
                    {unreadCount}
                  </span>
                )}
              </Link>
            </>
          )}
          <Link href="/ask" className="py-2" onClick={closeMenu}>
            <Button variant="secondary" className="w-full">
              Ask
            </Button>
          </Link>
          <div className="py-2">
            <LanguageSwitcher />
          </div>
          {isLoading ? null : me ? (
            <div className="flex items-center justify-between py-2">
              <Link href={`/users/${me.id}`} className="text-sm font-medium text-text-primary" onClick={closeMenu}>
                {me.nickname}
              </Link>
              <Button
                variant="ghost"
                onClick={() => {
                  closeMenu();
                  logout();
                }}
              >
                Log out
              </Button>
            </div>
          ) : (
            <div className="flex items-center gap-2 py-2">
              <Link href="/signup" className="flex-1" onClick={closeMenu}>
                <Button variant="ghost" className="w-full">
                  Sign up
                </Button>
              </Link>
              <Link href="/login" className="flex-1" onClick={closeMenu}>
                <Button variant="secondary" className="w-full">
                  Log in
                </Button>
              </Link>
            </div>
          )}
        </nav>
      )}
    </header>
  );
}
