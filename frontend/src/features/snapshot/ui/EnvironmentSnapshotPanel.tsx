import type { EnvironmentSnapshot, SnapshotSymptom } from "@/features/question/api/question.types";
import type { ReactNode } from "react";
import { cn } from "@/shared/lib/cn";
import { relativeTime } from "@/shared/lib/relative-time";

export const SYMPTOM_LABEL: Record<SnapshotSymptom, string> = {
  OUT_OF_MEMORY: "메모리 부족",
  DB_TIMEOUT: "DB 연결 시간 초과",
  DEPENDENCY_RESOLUTION: "의존성 해석 실패",
  TEST_FAILURE: "테스트 실패",
  BUILD_FAILURE: "빌드 실패",
};

const VISIBLE_FILES = 8;
const VISIBLE_KEYS = 8;

const VISIBLE_DEPENDENCIES = 6;

/** [wide] rows (a long command line) take the full width of the two-column grid. */
type Row = { key: string; value: string; wide?: boolean };

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

function formatMb(mb: number): string {
  return mb >= 1024 ? `${(mb / 1024).toFixed(1)}GB` : `${mb}MB`;
}

/** Tier 1 (Phase 93.3), Tier 2 (opt-in) and the delta since the last success (93.4), as rows. */
function diagnosticRows(snapshot: EnvironmentSnapshot): Row[] {
  const { tier1, tier2 } = snapshot.document;
  const rows: Row[] = [];
  const push = (key: string, value: string | null | undefined) => {
    if (value) rows.push({ key, value });
  };
  if (tier1) {
    const command = tier1.command?.argv?.join(" ");
    if (command) rows.push({ key: "명령", value: command, wide: true });
    const exit = tier1.command?.exitCode ?? tier1.command?.signal;
    const seconds = tier1.command?.durationMs != null ? ` · ${(tier1.command.durationMs / 1000).toFixed(1)}초` : "";
    if (exit != null) push("종료 코드", `${exit}${seconds}`);
    if (tier1.logWindow) push("로그", `마지막 ${tier1.logWindow.keptLines}/${tier1.logWindow.totalLines}줄 · 로그 칸에 첨부`);
    tier1.env?.forEach((variable) => push(variable.name, variable.value));
  }
  if (tier2) {
    if (tier2.memory) push("메모리", `${formatMb(tier2.memory.totalMb)} 중 ${formatMb(tier2.memory.freeMb)} 여유`);
    if (tier2.cpus) push("CPU", `${tier2.cpus}개`);
    if (tier2.diskFreeMb != null) push("디스크 여유", formatMb(tier2.diskFreeMb));
    if (tier2.cgroupMemoryLimitMb != null) push("cgroup 메모리 한도", formatMb(tier2.cgroupMemoryLimitMb));
  }
  return rows;
}

function Rows({ rows }: { rows: Row[] }) {
  return (
    <dl className="grid grid-cols-1 gap-x-5 sm:grid-cols-2">
      {rows.map((row) => (
        <div key={`${row.key}-${row.value}`} className={cn("flex justify-between gap-2.5 border-b border-border py-1.5", row.wide && "sm:col-span-2")}>
          <dt className="shrink-0 text-text-secondary">{row.key}</dt>
          <dd className="text-right font-mono text-xs break-all">{row.value}</dd>
        </div>
      ))}
    </dl>
  );
}

function SectionTitle({ children }: { children: ReactNode }) {
  return <h4 className="pt-3 pb-1 text-xs font-semibold text-text-secondary">{children}</h4>;
}

function Delta({ delta }: { delta: NonNullable<EnvironmentSnapshot["document"]["delta"]> }) {
  const files = delta.changedFiles ?? [];
  const hiddenFiles = (delta.changedFileCount ?? files.length) - Math.min(files.length, VISIBLE_FILES);
  const since = [delta.since?.capturedAt ? relativeTime(delta.since.capturedAt) : null, delta.since?.commit ? `커밋 ${delta.since.commit.slice(0, 7)}` : null]
    .filter(Boolean)
    .join(" · ");
  return (
    <>
      <SectionTitle>마지막으로 성공한 뒤 바뀐 것{since && ` (${since})`}</SectionTitle>
      <p className="text-xs text-text-secondary">
        커밋 {delta.commits ?? "알 수 없음"}개 · 파일 {delta.changedFileCount ?? files.length}개 · 의존성 {delta.dependencies?.length ?? 0}개
        {delta.configFiles?.length ? ` · 설정 파일 ${delta.configFiles.join(", ")}` : ""}
      </p>
      {(delta.dependencies?.length ?? 0) > 0 && (
        <ul className="mt-1 font-mono text-xs break-all">
          {delta.dependencies!.map((dependency) => (
            <li key={dependency.name}>
              {dependency.name.split(":").pop()} {dependency.from ?? "(없음)"} → {dependency.to ?? "(제거)"}
            </li>
          ))}
        </ul>
      )}
      {files.length > 0 && (
        <ul className="mt-1 font-mono text-xs break-all text-text-secondary">
          {files.slice(0, VISIBLE_FILES).map((file) => (
            <li key={file}>{file}</li>
          ))}
          {hiddenFiles > 0 && <li>외 {hiddenFiles}개</li>}
        </ul>
      )}
    </>
  );
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
  const { tier1, delta, omitted } = snapshot.document;
  const symptom = snapshot.symptom ?? tier1?.symptom ?? null;
  const diagnostics = diagnosticRows(snapshot);

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
        {tier1 && (
          <span className="inline-flex h-[22px] items-center rounded-full bg-danger-subtle px-2 text-[11px] font-semibold text-danger">
            {symptom ? SYMPTOM_LABEL[symptom] : "실패 기록"}
          </span>
        )}
        <span className="ml-auto text-xs font-medium text-text-secondary">
          Tier {(snapshot.document.tiers ?? [0]).join(" · ")} · 지문 <code className="font-mono">{snapshot.fingerprint.slice(0, 8)}</code>
        </span>
      </summary>
      {(tier1 || diagnostics.length > 0) && (
        <>
          <SectionTitle>{tier1 ? "실패 진단" : "런타임 상태"}</SectionTitle>
          <Rows rows={diagnostics} />
          {tier1?.failedTests && tier1.failedTests.length > 0 && (
            <ul className="mt-2 space-y-0.5 text-xs">
              {tier1.failedTests.map((test) => (
                <li key={`${test.suite}.${test.name}`}>
                  <code className="font-mono">{`${test.suite.split(".").pop()}.${test.name}`}</code>
                  {test.message && <span className="text-text-secondary"> — {test.message}</span>}
                </li>
              ))}
            </ul>
          )}
          {tier1?.configKeys?.map((config) => (
            <p key={config.file} className="mt-2 text-xs text-text-secondary">
              <code className="font-mono">{config.file}</code> 키 {config.keys.length}개(값은 수집하지 않음):{" "}
              <span className="font-mono">{config.keys.slice(0, VISIBLE_KEYS).join(", ")}</span>
              {config.keys.length > VISIBLE_KEYS && ` 외 ${config.keys.length - VISIBLE_KEYS}개`}
            </p>
          ))}
        </>
      )}
      {delta && <Delta delta={delta} />}
      <SectionTitle>환경</SectionTitle>
      <Rows rows={rows} />
      <p className="mt-2 mb-3.5 text-xs text-text-secondary">
        {dependencies === undefined
          ? null
          : dependencies.resolution === "lockfile"
            ? `의존성은 ${dependencies.source}의 해석된 버전이에요${hiddenDependencies > 0 ? ` (외 ${hiddenDependencies}개)` : ""}.`
            : "잠금 파일이 없어 해석된 의존성 버전은 없어요."}
        {snapshot.serverRedactions > 0 && ` 서버가 비밀값으로 보이는 값 ${snapshot.serverRedactions}개를 더 가렸어요.`}
        {!collected && " 사람이 직접 입력한 값이라 수집기 값보다 신뢰도가 낮아요."}
        {omitted && omitted.length > 0 && ` 작성자가 뺀 항목: ${omitted.join(", ")}.`}
      </p>
    </details>
  );
}
