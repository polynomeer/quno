import Link from "next/link";
import { cn } from "@/shared/lib/cn";

export function TagChip({ name, className }: { name: string; className?: string }) {
  return (
    <Link
      href={`/tags/${name}`}
      className={cn(
        "inline-flex items-center rounded-md bg-surface-subtle px-2 py-1 text-xs font-medium text-text-secondary hover:bg-border/40",
        className,
      )}
    >
      {name}
    </Link>
  );
}
