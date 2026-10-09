import Link from "next/link";
import { cn } from "@/shared/lib/cn";

export function TagChip({ name, className }: { name: string; className?: string }) {
  return (
    <Link
      href={`/tags/${name}`}
      className={cn(
        "inline-flex h-6 items-center rounded bg-surface-subtle px-2 font-mono text-xs text-text-body hover:text-brand",
        className,
      )}
    >
      {name}
    </Link>
  );
}
