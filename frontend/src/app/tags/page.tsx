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
      <h1 className="text-xl font-semibold">Tags</h1>
      <Input value={q} onChange={(event) => setQ(event.target.value)} placeholder="Search tags..." />

      {isLoading && <Skeleton className="h-40 w-full" />}

      {!isLoading && (
        <ul className="grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {(tags ?? []).map((tag) => (
            <li key={tag.id}>
              <Link
                href={`/tags/${encodeURIComponent(tag.name)}`}
                className="block rounded-md border border-border px-3 py-2 text-sm hover:border-text-secondary/40"
              >
                <span className="font-medium">{tag.name}</span>
                {tag.description && <p className="mt-1 truncate text-xs text-text-secondary">{tag.description}</p>}
              </Link>
            </li>
          ))}
          {tags && tags.length === 0 && <p className="text-sm text-text-secondary">태그가 없습니다.</p>}
        </ul>
      )}
    </div>
  );
}
