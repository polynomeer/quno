import { cn } from "@/shared/lib/cn";

/**
 * The "nothing here yet" line for lists (design.md #7.4 EmptyState, #21). In a page's main column
 * it's a dashed box so an empty list still reads as a list-shaped area; inside a card or a narrow
 * side panel (`inline`) it stays a plain line, since a box inside a box is just noise. An empty
 * message renders nothing — some callers pass "" for lists they only show when non-empty.
 */
export function EmptyState({ message, inline = false, className }: { message: string; inline?: boolean; className?: string }) {
  if (!message) return null;
  return (
    <p
      className={cn(
        "text-sm text-text-secondary",
        !inline && "rounded-[20px] border border-dashed border-border-strong px-4 py-6 text-center",
        className,
      )}
    >
      {message}
    </p>
  );
}
