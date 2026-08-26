const units: [Intl.RelativeTimeFormatUnit, number][] = [
  ["year", 1000 * 60 * 60 * 24 * 365],
  ["month", 1000 * 60 * 60 * 24 * 30],
  ["day", 1000 * 60 * 60 * 24],
  ["hour", 1000 * 60 * 60],
  ["minute", 1000 * 60],
];

const formatter = new Intl.RelativeTimeFormat("ko", { numeric: "auto" });

/** e.g. "3일 전", "방금 전" — used wherever design.md shows "asked 2d ago". */
export function relativeTime(isoDate: string): string {
  const diffMs = new Date(isoDate).getTime() - Date.now();
  for (const [unit, ms] of units) {
    if (Math.abs(diffMs) >= ms) {
      return formatter.format(Math.round(diffMs / ms), unit);
    }
  }
  return diffMs >= 0 ? "곧" : "방금 전";
}
