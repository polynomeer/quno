"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Input } from "@/shared/ui/Input";
import { StatusBadge } from "@/shared/ui/StatusBadge";
import { useAutocomplete } from "@/features/search/hooks/useAutocomplete";

export function SearchBox() {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const router = useRouter();
  const { enabled, questions, tags } = useAutocomplete(query);

  function commit(value: string) {
    if (!value.trim()) return;
    setOpen(false);
    router.push(`/questions?q=${encodeURIComponent(value.trim())}`);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    commit(query);
  }

  const showDropdown = open && enabled && (questions.length > 0 || tags.length > 0);

  return (
    <div className="relative flex-1">
      <form onSubmit={handleSubmit}>
        <Input
          type="search"
          placeholder="Search questions, tags, errors..."
          value={query}
          onChange={(event) => {
            setQuery(event.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
        />
      </form>
      {showDropdown && (
        <div className="absolute z-20 mt-1 w-full rounded-md border border-border bg-surface shadow-lg">
          {tags.length > 0 && (
            <div className="border-b border-border p-2">
              <p className="px-1 pb-1 text-xs font-medium text-text-secondary">Tags</p>
              <div className="flex flex-wrap gap-1 px-1 pb-1">
                {tags.map((tag) => (
                  <Link
                    key={tag.id}
                    href={`/tags/${encodeURIComponent(tag.name)}`}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => setOpen(false)}
                    className="rounded-md bg-surface-subtle px-2 py-1 text-xs text-text-secondary hover:bg-border/40"
                  >
                    {tag.name}
                  </Link>
                ))}
              </div>
            </div>
          )}
          {questions.length > 0 && (
            <ul className="p-1">
              {questions.map((question) => (
                <li key={question.id}>
                  <Link
                    href={`/questions/${question.id}`}
                    onMouseDown={(event) => event.preventDefault()}
                    onClick={() => setOpen(false)}
                    className="flex items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-surface-subtle"
                  >
                    <StatusBadge status={question.status} />
                    <span className="truncate">{question.title}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
