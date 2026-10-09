"use client";

import type { ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/shared/lib/cn";
import { useLocale } from "@/shared/i18n/LocaleProvider";

const itemClass =
  "flex h-10 items-center justify-between gap-2 rounded-md border px-3 text-sm transition-colors";

function NavItem({ href, active, children }: { href: string; active: boolean; children: ReactNode }) {
  return (
    <Link
      href={href}
      aria-current={active ? "page" : undefined}
      className={cn(
        itemClass,
        active
          ? "border-border bg-surface font-semibold text-text-primary"
          : "border-transparent text-text-body hover:bg-surface hover:text-text-primary",
      )}
    >
      {children}
    </Link>
  );
}

/**
 * Home's left navigation from the design canvas (ADR-0061). Only routes that exist are listed:
 * the canvas's "실시간 질문방" is dropped because live chat lives inside each question with no
 * room list to link to, and "Quno Flow" jumps to the home section of that name.
 *
 * [wardChangeCount] is the number of unread Ward updates — changes to watched questions.
 */
export function SideNav({ wardChangeCount }: { wardChangeCount: number }) {
  const { t } = useLocale();
  const pathname = usePathname();

  return (
    <nav aria-label={t.sideNav.label} className="flex flex-col gap-0.5">
      <NavItem href="/" active={pathname === "/"}>
        {t.sideNav.home}
      </NavItem>
      <NavItem href="/questions" active={pathname.startsWith("/questions")}>
        {t.sideNav.questions}
      </NavItem>
      <NavItem href="/tags" active={pathname.startsWith("/tags")}>
        {t.sideNav.tags}
      </NavItem>
      <NavItem href="/#quno-flow" active={false}>
        {t.sideNav.flow}
      </NavItem>
      <NavItem href="/organizations" active={pathname.startsWith("/organizations")}>
        {t.sideNav.organizations}
      </NavItem>

      <h2 className="mx-3 mt-5 mb-1.5 text-xs font-semibold tracking-wide text-text-secondary">
        {t.sideNav.myActivity}
      </h2>
      <NavItem href="/watching" active={pathname.startsWith("/watching")}>
        {t.sideNav.watching}
        {/* Flex drops this space visually, but it keeps the accessible name "Watching 3 변화"
            instead of "Watching3 변화". */}
        {wardChangeCount > 0 && " "}
        {wardChangeCount > 0 && (
          <span className="font-mono text-xs text-brand">
            {wardChangeCount} {t.sideNav.changes}
          </span>
        )}
      </NavItem>
      <NavItem href="/saved" active={pathname.startsWith("/saved")}>
        {t.sideNav.saved}
      </NavItem>
      <NavItem href="/direct-asks" active={pathname.startsWith("/direct-asks")}>
        {t.sideNav.directAsks}
      </NavItem>
    </nav>
  );
}
