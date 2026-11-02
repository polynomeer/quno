/**
 * The lock on a private question (Phase 79, ADR-0085). Shown wherever the question appears, so a
 * member never mistakes an internal question for a public one before quoting it elsewhere.
 */
export function PrivateBadge({ organizationName }: { organizationName?: string | null }) {
  return (
    <span
      className="inline-flex h-[22px] items-center gap-1 rounded-full bg-warning-subtle px-2 text-[11px] font-semibold text-warning"
      title="이 조직 멤버만 볼 수 있는 질문이에요"
    >
      <span aria-hidden="true">🔒</span>
      {organizationName ? `${organizationName} 멤버만` : "비공개"}
    </span>
  );
}
