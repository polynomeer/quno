"use client";

import { useState } from "react";
import Link from "next/link";
import { useTagSearch } from "@/entities/tag/hooks/useTagSearch";
import { Input } from "@/shared/ui/Input";
import { Skeleton } from "@/shared/ui/Skeleton";

/** Publicly readable (Phase 30, ADR-0042) — no auth gate, no `useSession` needed at all since
 * this page has no action-only UI to hide. */
export default function TagDirectoryPage() {
  const [q, setQ] = useState("");
  const { data: tags, isLoading } = useTagSearch(q);

  return (
    <div className="space-y-6">
      <h1 className="text-[32px] leading-10 font-bold tracking-tight">Tags</h1>
      <Input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search tags..." />

      {isLoading && <Skeleton className="h-40 w-full" />}

      {/* The empty state lives outside the <ul> — a <p> as a direct list child fails axe's
          `list` rule, which only shows up when there are no tags (e.g. CI's empty database). */}
      {!isLoading && tags && tags.length === 0 && <p className="text-sm text-text-secondary">태그가 없습니다.</p>}

      {!isLoading && tags && tags.length > 0 && (
        <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {tags.map((tag) => (
            <li key={tag.id}>
              <Link
                href={`/tags/${encodeURIComponent(tag.name)}`}
                className="block rounded-[20px] border border-border bg-surface p-4 text-sm transition-colors hover:border-border-strong"
              >
                <span className="font-mono font-medium text-text-primary">{tag.name}</span>
                {tag.description && <p className="mt-1.5 truncate text-[13px] text-text-secondary">{tag.description}</p>}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
