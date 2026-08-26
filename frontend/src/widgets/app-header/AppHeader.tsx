"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLogout } from "@/features/auth/hooks/useLogin";
import { Button } from "@/shared/ui/Button";
import { Input } from "@/shared/ui/Input";

/** Desktop header — see docs/frontend/design.md #6 글로벌 애플리케이션 셸. */
export function AppHeader() {
  const { data: me, isLoading } = useSession();
  const logout = useLogout();
  const router = useRouter();
  const [query, setQuery] = useState("");

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (query.trim()) {
      router.push(`/questions?q=${encodeURIComponent(query.trim())}`);
    }
  }

  return (
    <header className="sticky top-0 z-10 border-b border-border bg-surface">
      <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3">
        <Link href="/" className="text-lg font-semibold text-brand">
          Quno
        </Link>
        <form onSubmit={handleSearch} className="flex-1">
          <Input
            type="search"
            placeholder="Search questions, tags, errors..."
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </form>
        <Link href="/tags" className="text-sm font-medium text-text-secondary hover:text-text-primary">
          Tags
        </Link>
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
