"use client";

import { useState, type ChangeEvent, type KeyboardEvent } from "react";
import { Input } from "@/shared/ui/Input";

const MAX_TAGS = 5;

/** Client-side cap only (design.md #11 "최대 개수 제한") — the backend has no tag-count limit. */
export function TagInput({ value, onChange }: { value: string[]; onChange: (tags: string[]) => void }) {
  const [draft, setDraft] = useState("");

  function addTags(candidates: string[]) {
    const merged = [...value];
    for (const raw of candidates) {
      const tag = raw.trim();
      if (tag && !merged.includes(tag) && merged.length < MAX_TAGS) {
        merged.push(tag);
      }
    }
    if (merged.length !== value.length) {
      onChange(merged);
    }
  }

  function addDraft() {
    addTags([draft]);
    setDraft("");
  }

  // Handles both a real comma keypress and a pasted/bulk-set value containing commas — typing
  // one character at a time isn't the only way text ends up in this field.
  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    const raw = event.target.value;
    if (raw.includes(",")) {
      const parts = raw.split(",");
      addTags(parts.slice(0, -1));
      setDraft(parts[parts.length - 1]);
    } else {
      setDraft(raw);
    }
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      addDraft();
    } else if (event.key === "Backspace" && draft === "" && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap gap-2">
        {value.map((tag) => (
          <span
            key={tag}
            className="inline-flex items-center gap-1 rounded-md bg-surface-subtle px-2 py-1 text-xs font-medium text-text-secondary"
          >
            {tag}
            <button
              type="button"
              onClick={() => onChange(value.filter((t) => t !== tag))}
              className="text-text-secondary hover:text-danger"
              aria-label={`Remove ${tag}`}
            >
              ×
            </button>
          </span>
        ))}
      </div>
      <Input
        value={draft}
        onChange={handleChange}
        onKeyDown={handleKeyDown}
        onBlur={addDraft}
        placeholder={value.length >= MAX_TAGS ? `최대 ${MAX_TAGS}개까지 추가할 수 있습니다` : "태그 입력 후 Enter"}
        disabled={value.length >= MAX_TAGS}
      />
    </div>
  );
}
