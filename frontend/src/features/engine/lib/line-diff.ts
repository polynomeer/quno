export interface DiffRow {
  type: "same" | "added" | "removed" | "gap";
  text: string;
}

/**
 * Line diff for previewing an engine suggestion against the current text (LCS over lines).
 * Unchanged runs are folded to [context] lines on each side of a change so a one-line fence edit
 * in a long body stays readable. Question bodies are small enough for the O(n·m) table.
 */
export function lineDiff(before: string, after: string, context = 1): DiffRow[] {
  const a = before.split("\n");
  const b = after.split("\n");
  const lcs: number[][] = Array.from({ length: a.length + 1 }, () => new Array<number>(b.length + 1).fill(0));
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      lcs[i]![j] = a[i] === b[j] ? lcs[i + 1]![j + 1]! + 1 : Math.max(lcs[i + 1]![j]!, lcs[i]![j + 1]!);
    }
  }

  const rows: DiffRow[] = [];
  let i = 0;
  let j = 0;
  while (i < a.length || j < b.length) {
    if (i < a.length && j < b.length && a[i] === b[j]) {
      rows.push({ type: "same", text: a[i]! });
      i++;
      j++;
    } else if (i < a.length && (j >= b.length || lcs[i + 1]![j]! >= lcs[i]![j + 1]!)) {
      // Removals before additions on a tie, as diffs usually read.
      rows.push({ type: "removed", text: a[i]! });
      i++;
    } else {
      rows.push({ type: "added", text: b[j]! });
      j++;
    }
  }

  const keep = rows.map((row, index) =>
    row.type !== "same" || rows.slice(Math.max(0, index - context), index + context + 1).some((near) => near.type !== "same"),
  );
  const folded: DiffRow[] = [];
  rows.forEach((row, index) => {
    if (keep[index]) folded.push(row);
    else if (folded.at(-1)?.type !== "gap") folded.push({ type: "gap", text: "⋯" });
  });
  return folded;
}
