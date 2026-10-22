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

const navLinkClass =
  "inline-flex h-10 items-center rounded-full px-3.5 text-sm font-medium text-text-body transition-colors hover:bg-surface-subtle hover:text-text-primary";

function BellIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      width="20"
      height="20"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
    >
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </svg>
  );
}

/** 반응형 헤더 — 데스크톱은 한 줄, 모바일(md 미만)은 햄버거 메뉴로 접는다. Web design v2 (ADR-0071)
 * floats it as a glass capsule inset from the page edge, so content scrolling under it shows
 * through the blur instead of meeting a hard border. */
export function AppHeader() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { data: me, isLoading } = useSession();
  const logout = useLogout();
  const { data: notifications } = useNotifications(Boolean(me));
  const unreadCount = notifications?.filter((n) => !n.isRead).length ?? 0;
  const closeMenu = () => setMenuOpen(false);

  return (
    <header className="sticky top-0 z-20 px-3 pt-3 sm:px-6">
      <div className="glass mx-auto max-w-[1360px] rounded-[28px]">
        <div className="flex items-center gap-3 py-2.5 pr-2.5 pl-5 sm:gap-5">
          {/* The live dot is the "living question" mark from the design canvas (ADR-0061). */}
          <Link
            href="/"
            className="flex shrink-0 items-center gap-2 font-mono text-xl font-semibold tracking-tight text-text-primary"
          >
            <span
              aria-hidden="true"
              className="size-2.5 rounded-full bg-live ring-4 ring-live-ring"
            />
            quno
          </Link>
          <SearchBox />
          <nav className="hidden items-center gap-1 md:flex">
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
                <Link
                  href="/notifications"
                  aria-label={
                    unreadCount > 0
                      ? `Notifications, ${unreadCount} unread`
                      : "Notifications"
                  }
                  className="relative grid size-11 place-items-center rounded-full text-text-primary transition-colors hover:bg-surface-subtle"
                >
                  <BellIcon />
                  {unreadCount > 0 && (
                    <span
                      aria-hidden="true"
                      className="absolute top-0.5 -right-0.5 inline-flex h-[18px] min-w-[18px] items-center justify-center rounded-full bg-brand px-1 font-mono text-[11px] font-bold text-brand-foreground"
                    >
                      {unreadCount}
                    </span>
                  )}
                </Link>
              </>
            )}
            <LanguageSwitcher />
            <Link
              href="/ask"
              className="inline-flex h-11 items-center gap-1.5 rounded-full bg-brand px-5 text-sm font-bold text-brand-foreground shadow-[inset_0_1px_0_rgb(255_255_255/0.3)] hover:opacity-90"
            >
              <svg
                aria-hidden="true"
                viewBox="0 0 24 24"
                width="16"
                height="16"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.6"
                strokeLinecap="round"
              >
                <path d="M12 5v14M5 12h14" />
              </svg>
              Ask
            </Link>
            {isLoading ? null : me ? (
              <div className="flex items-center gap-1">
                <Link
                  href={`/users/${me.id}`}
                  className="flex h-11 items-center gap-2 rounded-full py-1 pr-3.5 pl-1 text-sm font-medium text-text-primary hover:bg-surface-subtle"
                >
                  <span
                    aria-hidden="true"
                    className="grid size-9 place-items-center rounded-full bg-brand-subtle text-sm font-bold text-brand-on-subtle"
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
            className="ml-auto grid size-11 shrink-0 place-items-center rounded-full text-text-secondary hover:bg-surface-subtle md:hidden"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <svg
              viewBox="0 0 24 24"
              width="22"
              height="22"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
            >
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
            <Link
              href="/tags"
              className={cn(navLinkClass, "py-2")}
              onClick={closeMenu}
            >
              Tags
            </Link>
            <Link
              href="/organizations"
              className={cn(navLinkClass, "py-2")}
              onClick={closeMenu}
            >
              Organizations
            </Link>
            {me && (
              <>
                <Link
                  href="/watching"
                  className={cn(navLinkClass, "py-2")}
                  onClick={closeMenu}
                >
                  Watching
                </Link>
                <Link
                  href="/saved"
                  className={cn(navLinkClass, "py-2")}
                  onClick={closeMenu}
                >
                  Saved
                </Link>
                <Link
                  href="/direct-asks"
                  className={cn(navLinkClass, "py-2")}
                  onClick={closeMenu}
                >
                  Direct Asks
                </Link>
                <Link
                  href="/notifications"
                  className={cn(navLinkClass, "flex items-center py-2")}
                  onClick={closeMenu}
                >
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
              <Button className="w-full">Ask</Button>
            </Link>
            <div className="py-2">
              <LanguageSwitcher />
            </div>
            {isLoading ? null : me ? (
              <div className="flex items-center justify-between py-2">
                <Link
                  href={`/users/${me.id}`}
                  className="text-sm font-medium text-text-primary"
                  onClick={closeMenu}
                >
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
      </div>
    </header>
  );
}
