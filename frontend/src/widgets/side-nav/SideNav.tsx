"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/shared/lib/cn";
import { useLocale } from "@/shared/i18n/LocaleProvider";

/** Single-path stroke icons from the web design v2 canvas (ADR-0071). */
const ICONS = {
  home: "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z",
  questions: "M21 12a8 8 0 0 1-11.6 7.1L4 20l1-4.6A8 8 0 1 1 21 12z",
  tags: "M3 12V4a1 1 0 0 1 1-1h8l9 9-9 9zM7.5 7.5h.01",
  flow: "m3 17 6-6 4 4 8-8M14 7h7v7",
  organizations: "M4 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16M16 9h2a2 2 0 0 1 2 2v10M3 21h18M8 7h4M8 11h4M8 15h4",
  watching: "M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12zM12 9a3 3 0 1 0 0 6 3 3 0 0 0 0-6z",
  saved: "M6 3h12v18l-6-4-6 4z",
  directAsks: "M22 2 11 13M22 2l-7 20-4-9-9-4z",
  notifications: "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9M10.3 21a1.94 1.94 0 0 0 3.4 0",
  profile: "M12 4a4 4 0 1 0 0 8 4 4 0 0 0 0-8zM4 21a8 8 0 0 1 16 0",
} as const;

function NavItem({
  href,
  active,
  icon,
  children,
}: {
  href: string;
  active: boolean;
  icon: keyof typeof ICONS;
  children: ReactNode;
}) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={cn(
        "flex h-[42px] items-center gap-2.5 rounded-[14px] px-3 text-sm transition-colors",
        active
          ? "bg-surface font-semibold text-text-primary shadow-[var(--segment-selected-shadow)]"
          : "text-text-body hover:bg-surface hover:text-text-primary",
      )}
    >
      <svg
        aria-hidden="true"
        viewBox="0 0 24 24"
        width="18"
        height="18"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        className="shrink-0"
      >
        <path d={ICONS[icon]} />
      </svg>
      {children}
    </Link>
  );
}

/** Small count after a label. The leading space keeps the accessible name "Watching 3 변화"
 * instead of "Watching3 변화" — flex drops it visually. */
function Count({ children }: { children: ReactNode }) {
  return (
    <>
      {" "}
      <span className="ml-auto font-mono text-xs font-semibold text-brand-on-subtle">{children}</span>
    </>
  );
}

/**
 * Left navigation from the design canvas (ADR-0061, restyled per ADR-0071). Only routes that
 * exist are listed: the v2 canvas's 깨짐 레이더 / 내 스택 / 팀 기억 / 에스컬레이션 / 메인테이너
 * 허브 / 연동 entries arrive with their PLAN.md phases (see Phase 100's mapping table).
 *
 * [wardChangeCount] is the number of unread Ward updates — changes to watched questions.
 * [userId] adds the profile link; [unreadNotificationCount] badges the notifications link.
 */
export function SideNav({
  wardChangeCount,
  userId,
  unreadNotificationCount = 0,
}: {
  wardChangeCount: number;
  userId?: number;
  unreadNotificationCount?: number;
}) {
  const { t } = useLocale();
  const pathname = usePathname();

  return (
    <nav aria-label={t.sideNav.label} className="flex flex-col gap-0.5 rounded-3xl bg-text-primary/[0.025] p-2">
      <NavItem href="/" icon="home" active={pathname === "/"}>
        {t.sideNav.home}
      </NavItem>
      <NavItem href="/questions" icon="questions" active={pathname.startsWith("/questions")}>
        {t.sideNav.questions}
      </NavItem>
      <NavItem href="/tags" icon="tags" active={pathname.startsWith("/tags")}>
        {t.sideNav.tags}
      </NavItem>
      <NavItem href="/#quno-flow" icon="flow" active={false}>
        {t.sideNav.flow}
      </NavItem>
      <NavItem href="/organizations" icon="organizations" active={pathname.startsWith("/organizations")}>
        {t.sideNav.organizations}
      </NavItem>

      <h2 className="mx-3 mt-4 mb-1.5 text-xs font-semibold tracking-wide text-text-secondary">
        {t.sideNav.myActivity}
      </h2>
      <NavItem href="/watching" icon="watching" active={pathname.startsWith("/watching")}>
        {t.sideNav.watching}
        {wardChangeCount > 0 && (
          <Count>
            {wardChangeCount} {t.sideNav.changes}
          </Count>
        )}
      </NavItem>
      <NavItem href="/saved" icon="saved" active={pathname.startsWith("/saved")}>
        {t.sideNav.saved}
      </NavItem>
      <NavItem href="/direct-asks" icon="directAsks" active={pathname.startsWith("/direct-asks")}>
        {t.sideNav.directAsks}
      </NavItem>
      <NavItem href="/notifications" icon="notifications" active={pathname.startsWith("/notifications")}>
        {t.sideNav.notifications}
        {unreadNotificationCount > 0 && <Count>{unreadNotificationCount}</Count>}
      </NavItem>
      {userId !== undefined && (
        <NavItem href={`/users/${userId}`} icon="profile" active={pathname === `/users/${userId}`}>
          {t.sideNav.profile}
        </NavItem>
      )}
    </nav>
  );
}
