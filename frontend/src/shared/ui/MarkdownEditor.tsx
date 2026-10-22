"use client";

import { useState } from "react";
import { cn } from "@/shared/lib/cn";
import { Textarea } from "./Textarea";
import { MarkdownContent } from "./MarkdownContent";

type Tab = "write" | "preview";

/** Shared by Ask and Answer Composer — see docs/frontend/design.md #11/#13 (Write/Preview tabs). */
export function MarkdownEditor({
  value,
  onChange,
  placeholder,
  rows = 10,
  id,
}: {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  rows?: number;
  id?: string;
}) {
  const [tab, setTab] = useState<Tab>("write");

  return (
    <div className="overflow-hidden rounded-2xl border border-border-strong bg-surface">
      <div className="flex gap-1 border-b border-border bg-canvas p-1.5 text-sm">
        {(["write", "preview"] as const).map((t) => (
          <button
            key={t}
            type="button"
            onClick={() => setTab(t)}
            aria-pressed={tab === t}
            className={cn(
              "h-8 rounded-full px-3.5 capitalize transition-colors",
              tab === t
                ? "bg-surface font-semibold text-text-primary shadow-[var(--segment-selected-shadow)]"
                : "text-text-secondary hover:text-text-primary",
            )}
          >
            {t}
          </button>
        ))}
      </div>
      {tab === "write" ? (
        <Textarea
          id={id}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          placeholder={placeholder}
          rows={rows}
          className="rounded-none border-0 font-mono text-[13px] leading-relaxed focus:ring-0"
        />
      ) : (
        <div className="min-h-[8rem] p-3">
          {value.trim() ? (
            <MarkdownContent>{value}</MarkdownContent>
          ) : (
            <p className="text-sm text-text-secondary">
              미리볼 내용이 없습니다.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
