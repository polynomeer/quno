import type { PromotionField, PromotionFinding } from "../api/question.types";

const FIELD_LABEL: Record<PromotionField, string> = {
  title: "제목",
  body: "본문",
  environment: "Environment",
  logs: "Logs",
  tags: "태그",
  solution: "해결 방법",
};

/**
 * What may still be internal in a promotion draft (Phase 79.3, ADR-0086). "지워야 해요" items
 * (organization identifiers, secrets) block publishing; "확인해 주세요" items are the author's call.
 */
export function PromotionFindings({ findings, checking }: { findings: PromotionFinding[]; checking: boolean }) {
  const blocking = findings.filter((finding) => finding.severity === "BLOCK");
  const warnings = findings.filter((finding) => finding.severity === "WARN");

  return (
    <section
      aria-label="남은 사내 정보"
      aria-busy={checking}
      className="space-y-4 rounded-3xl border border-border bg-surface p-5 lg:sticky lg:top-28"
    >
      <div className="flex items-baseline justify-between">
        <h2 className="text-[15px] font-semibold">남은 사내 정보</h2>
        <span className="text-xs text-text-secondary">{checking ? "검사 중..." : "자동 검사"}</span>
      </div>

      {findings.length === 0 ? (
        <p className="text-sm text-success">찾은 항목이 없어요. 그래도 회사에서만 통하는 이름이 없는지 한 번 더 읽어 주세요.</p>
      ) : (
        <>
          <FindingGroup title="지워야 해요" tone="danger" findings={blocking} />
          <FindingGroup title="확인해 주세요" tone="warning" findings={warnings} />
        </>
      )}

      <p className="border-t border-border pt-4 text-xs text-text-secondary">
        조직 이름·도메인과 비밀값이 남아 있으면 만들 수 없어요. 사설 IP·내부 호스트·이메일·사용자 경로는 예시 값일
        수도 있어서 판단을 맡겨요.
      </p>
    </section>
  );
}

function FindingGroup({ title, tone, findings }: { title: string; tone: "danger" | "warning"; findings: PromotionFinding[] }) {
  if (findings.length === 0) return null;
  return (
    <div className="space-y-2">
      <h3 className={tone === "danger" ? "text-sm font-bold text-danger" : "text-sm font-bold text-warning"}>
        {title} {findings.length}
      </h3>
      <ul className="space-y-1.5">
        {findings.map((finding) => (
          <li key={`${finding.field}-${finding.start}`} className="text-sm">
            <span className="text-text-secondary">
              {FIELD_LABEL[finding.field]} · {finding.label}
            </span>{" "}
            <code className="rounded bg-surface-subtle px-1 font-mono text-[12px] break-all text-text-primary">
              {finding.excerpt}
            </code>
          </li>
        ))}
      </ul>
    </div>
  );
}
