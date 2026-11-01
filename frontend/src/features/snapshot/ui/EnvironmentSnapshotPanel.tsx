import type { EnvironmentSnapshot } from "@/features/question/api/question.types";
import { cn } from "@/shared/lib/cn";

const VISIBLE_DEPENDENCIES = 6;

type Row = { key: string; value: string };

function rowsOf(snapshot: EnvironmentSnapshot): Row[] {
  const tier0 = snapshot.document.tier0 ?? {};
  const rows: Row[] = [];
  const push = (key: string, value: string | null | undefined) => {
    if (value) rows.push({ key, value });
  };

  push("OS", [tier0.os?.name, tier0.os?.version, tier0.os?.arch].filter(Boolean).join(" "));
  tier0.runtimes?.forEach((runtime) => {
    const label = runtime.name === "java" ? "JDK" : runtime.name === "node" ? "Node" : runtime.name;
    push(label, [runtime.vendor, runtime.version].filter(Boolean).join(" "));
  });
  const container = tier0.container ?? {};
  push(
    "컨테이너",
    [container.docker && "Docker", container.wsl && "WSL", container.rosetta && "Rosetta"].filter(Boolean).join(" · ") || "네이티브",
  );
  push("로케일", [tier0.locale?.lang, tier0.locale?.encoding, tier0.locale?.timezone].filter(Boolean).join(" · "));
  push("도구", tier0.tools?.map((tool) => `${tool.name} ${tool.version}`).join(" · "));
  push("IDE", tier0.ide);
  if (tier0.ci?.isCi) push("CI", tier0.ci.provider ?? "예");
  if (tier0.git?.commit) {
    push("커밋", `${tier0.git.commit.slice(0, 10)}${tier0.git.dirty ? " · 수정 중" : ""}${tier0.git.remoteHost ? ` · ${tier0.git.remoteHost}` : ""}`);
  }
  tier0.dependencies?.items?.slice(0, VISIBLE_DEPENDENCIES).forEach((dependency) => {
    push(dependency.name.split(":").pop() ?? dependency.name, dependency.version);
  });
  return rows;
}

/**
 * "환경 스냅샷" (Phase 93, the panel of web design v2): what the asker's machine looked like when
 * they asked. Shows provenance rather than a "signature" — an open client can't hold a secret key
 * (ADR-0083) — and the fingerprint the server computed for grouping.
 */
export function EnvironmentSnapshotPanel({ snapshot }: { snapshot: EnvironmentSnapshot | null | undefined }) {
  if (!snapshot) return null;
  const rows = rowsOf(snapshot);
  const dependencies = snapshot.document.tier0?.dependencies;
  const hiddenDependencies = (dependencies?.items?.length ?? 0) - VISIBLE_DEPENDENCIES;
  const collected = snapshot.source === "COLLECTOR";

  return (
    <details className="rounded-2xl border border-border px-4 text-sm">
      <summary className="flex min-h-12 cursor-pointer list-none flex-wrap items-center gap-2.5 font-bold text-text-primary">
        환경 스냅샷
        <span
          className={cn(
            "inline-flex h-[22px] items-center rounded-full px-2 text-[11px] font-semibold",
            collected ? "bg-success-subtle text-success" : "bg-warning-subtle text-warning",
          )}
        >
          {collected ? (snapshot.collector ? `${snapshot.collector.replace(/^quno-cli/, "Quno 수집기")}` : "Quno 수집기") : "수동 입력"}
        </span>
        <span className="ml-auto text-xs font-medium text-text-secondary">
          Tier {(snapshot.document.tiers ?? [0]).join(" · ")} · 지문 <code className="font-mono">{snapshot.fingerprint.slice(0, 8)}</code>
        </span>
      </summary>
      <dl className="grid grid-cols-1 gap-x-5 pt-1 pb-3.5 sm:grid-cols-2">
        {rows.map((row) => (
          <div key={`${row.key}-${row.value}`} className="flex justify-between gap-2.5 border-b border-border py-1.5">
            <dt className="text-text-secondary">{row.key}</dt>
            <dd className="text-right font-mono text-xs break-all">{row.value}</dd>
          </div>
        ))}
      </dl>
      <p className="mb-3.5 text-xs text-text-secondary">
        {dependencies?.resolution === "lockfile"
          ? `의존성은 ${dependencies.source}의 해석된 버전이에요${hiddenDependencies > 0 ? ` (외 ${hiddenDependencies}개)` : ""}.`
          : "잠금 파일이 없어 해석된 의존성 버전은 없어요."}
        {snapshot.serverRedactions > 0 && ` 서버가 비밀값으로 보이는 값 ${snapshot.serverRedactions}개를 더 가렸어요.`}
        {!collected && " 사람이 직접 입력한 값이라 수집기 값보다 신뢰도가 낮아요."}
      </p>
    </details>
  );
}
