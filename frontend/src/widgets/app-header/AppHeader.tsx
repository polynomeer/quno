"use client";

import Link from "next/link";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLogout } from "@/features/auth/hooks/useLogin";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { Button } from "@/shared/ui/Button";
import { SearchBox } from "./SearchBox";

/** Desktop header — see docs/frontend/design.md #6 글로벌 애플리케이션 셸. */
export function AppHeader() {
  const { data: me, isLoading } = useSession();
  const logout = useLogout();
  const { data: notifications } = useNotifications(Boolean(me));
  const unreadCount = notifications?.filter((n) => !n.isRead).length ?? 0;

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-surface">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link href="/" className="text-lg font-semibold text-brand">
          Quno
        </Link>
        <SearchBox />
        <Link href="/tags" className="text-sm font-medium text-text-secondary hover:text-text-primary">
          Tags
        </Link>
        {me && (
          <>
            <Link href="/watching" className="text-sm font-medium text-text-secondary hover:text-text-primary">
              Watching
            </Link>
            <Link href="/saved" className="text-sm font-medium text-text-secondary hover:text-text-primary">
              Saved
            </Link>
            <Link
              href="/notifications"
              className="relative text-sm font-medium text-text-secondary hover:text-text-primary"
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
        <Link href="/ask">
          <Button variant="primary">Ask</Button>
        </Link>
        {isLoading ? null : me ? (
          <div className="flex items-center gap-3">
            <Link href={`/users/${me.id}`} className="text-sm font-medium text-text-primary">
              {me.nickname}
            </Link>
            <Button variant="ghost" onClick={logout}>
              Log out
            </Button>
          </div>
        ) : (
          <Link href="/login">
            <Button variant="secondary">Log in</Button>
          </Link>
        )}
      </div>
    </header>
  );
}
