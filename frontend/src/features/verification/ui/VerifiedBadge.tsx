/** A question whose failure a trusted run reproduced (Phase 94, ADR-0089). Says the failure exists, not that an answer works. */
export function VerifiedBadge({ level }: { level: string }) {
  return (
    <span
      className="inline-flex h-[22px] items-center gap-1 rounded-full bg-success-subtle px-2 text-[11px] font-semibold text-success"
      title="Quno 워크플로의 실행에서 보고된 에러가 그대로 재현됐어요. 해결책을 검증했다는 뜻은 아니에요."
    >
      재현됨 {level}
    </span>
  );
}
