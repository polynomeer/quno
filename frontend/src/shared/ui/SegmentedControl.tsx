import { cn } from "@/shared/lib/cn";

export interface SegmentOption<T extends string> {
  value: T;
  label: string;
  /** Shown after the label in a quieter weight, e.g. a section's item count. */
  count?: number;
}

/**
 * The glass capsule switch from web design v2 (ADR-0071) — feed filters, answer sort, list tabs.
 * Each option is a toggle button with `aria-pressed`, grouped under one label: it selects which
 * slice of the same list is shown, so it isn't a tablist with separate panels.
 */
export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  label,
  size = "md",
  className,
}: {
  options: SegmentOption<T>[];
  value: T;
  onChange: (value: T) => void;
  label: string;
  size?: "sm" | "md";
  className?: string;
}) {
  return (
    <div
      role="group"
      aria-label={label}
      className={cn("glass inline-flex max-w-full gap-1 overflow-x-auto rounded-full p-1", className)}
    >
      {options.map((option) => {
        const selected = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            aria-pressed={selected}
            onClick={() => onChange(option.value)}
            className={cn(
              "inline-flex shrink-0 items-center gap-1.5 rounded-full whitespace-nowrap transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand",
              size === "sm" ? "h-8 px-3.5 text-[13px]" : "h-9 px-4 text-sm",
              selected
                ? "bg-surface font-semibold text-text-primary shadow-[var(--segment-selected-shadow)]"
                : "font-medium text-text-body hover:text-text-primary",
            )}
          >
            {option.label}
            {/* Keeps the accessible name "인기 5" rather than "인기5"; flex gap hides the space. */}
            {option.count !== undefined && " "}
            {option.count !== undefined && (
              <span className="font-mono text-xs text-text-secondary">{option.count}</span>
            )}
          </button>
        );
      })}
    </div>
  );
}
