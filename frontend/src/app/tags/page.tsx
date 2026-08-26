"use client";

import { useState } from "react";
import Link from "next/link";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { useTagSearch } from "@/entities/tag/hooks/useTagSearch";
import { Input } from "@/shared/ui/Input";
import { Skeleton } from "@/shared/ui/Skeleton";

export default function TagDirectoryPage() {
  const { isLoading: authLoading } = useRequireAuth();
  const [q, setQ] = useState("");
  const { data: tags, isLoading } = useTagSearch(q);

  if (authLoading) {
    return <Skeleton className="h-40 w-full" />;
  }

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
                className="block rounded-md border border-border px-3 py-2 text-sm font-medium hover:border-text-secondary/40"
              >
                {tag.name}
              </Link>
            </li>
          ))}
          {tags && tags.length === 0 && <p className="text-sm text-text-secondary">태그가 없습니다.</p>}
        </ul>
      )}
    </div>
  );
}
