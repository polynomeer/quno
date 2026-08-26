import { cn } from "@/shared/lib/cn";
import type { DiffLine } from "../api/question.types";

const lineClasses: Record<DiffLine["type"], string> = {
  ADDED: "bg-success-subtle text-success",
  REMOVED: "bg-danger-subtle text-danger",
  EQUAL: "text-text-secondary",
};

const prefix: Record<DiffLine["type"], string> = {
  ADDED: "+",
  REMOVED: "-",
  EQUAL: " ",
};

export function DiffView({ lines }: { lines: DiffLine[] }) {
  return (
    <pre className="overflow-x-auto rounded-md border border-border bg-surface-subtle p-3 font-mono text-xs">
      {lines.map((line, index) => (
        <div key={index} className={cn("whitespace-pre-wrap px-1", lineClasses[line.type])}>
          {prefix[line.type]} {line.text}
        </div>
      ))}
    </pre>
  );
}
