"use client";

import { useState } from "react";
import { API_BASE_URL } from "@/shared/api/http-client";
import { relativeTime } from "@/shared/lib/relative-time";
import { Button } from "@/shared/ui/Button";
import { FormError } from "@/shared/ui/FormError";
import { Input } from "@/shared/ui/Input";
import { Textarea } from "@/shared/ui/Textarea";
import { DiffView } from "@/features/question/ui/DiffView";
import { useAttachCapsule, useReproductionComparison, useReproductionPanel, useReproductionRunLog, useRequestRerun } from "../hooks/useReproduction";
import type { EnvironmentRecipe, ReproductionCapsule, ReproductionKind, ReproductionResult, ReproductionRun, ReproductionTransition } from "../api/reproduction.types";

const RESULT: Record<ReproductionResult, { label: string; className: string }> = {
  REPRODUCED: { label: "재현됨", className: "bg-danger-subtle text-danger" },
  NOT_REPRODUCED: { label: "재현 안 됨", className: "bg-success-subtle text-success" },
  BUILD_FAILED: { label: "빌드 실패", className: "bg-warning-subtle text-warning" },
  INCONCLUSIVE: { label: "판단 불가", className: "bg-surface-subtle text-text-secondary" },
};

const KIND_HINT: Record<ReproductionKind, string> = {
  SCRIPT: "bash로 실행할 명령 (예: ./repro.sh)",
  GRADLE: "./gradlew 뒤에 붙일 태스크 (예: test --tests 'demo.PoolTest')",
  DOCKERFILE: "Dockerfile 경로 (예: repro/Dockerfile)",
};

/**
 * Reproduction runner v0 (Phase 80, ADR-0087): the question's capsule and what its repository's
 * GitHub Actions last reported per environment — `재현됨 · ubuntu24 · java 21 · 3시간 전`.
 * Anyone signed in can dispute a result (이의 제기), which asks the repository to run it again.
 */
export function ReproductionPanel({ questionId, isAuthor, signedIn }: { questionId: number; isAuthor: boolean; signedIn: boolean }) {
  const { data } = useReproductionPanel(questionId);
  const [editing, setEditing] = useState(false);

  if (!data) return null;
  const capsule = data.capsule;
  if (!capsule && !isAuthor) return null;

  return (
    <section aria-labelledby="reproduction-heading" className="space-y-3 rounded-[20px] border border-border bg-surface p-5">
      <div className="flex items-center justify-between gap-2">
        <h2 id="reproduction-heading" className="text-sm font-semibold text-text-primary">
          재현
        </h2>
        {isAuthor && !editing && (
          <button type="button" className="text-xs font-semibold text-brand hover:underline" onClick={() => setEditing(true)}>
            {capsule ? "캡슐 바꾸기" : "재현 캡슐 붙이기"}
          </button>
        )}
      </div>

      {editing ? (
        <CapsuleForm questionId={questionId} current={capsule} onDone={() => setEditing(false)} />
      ) : capsule ? (
        <>
          <CapsuleSummary capsule={capsule} />
          {data.transitions?.map((transition) => (
            <TransitionNotice key={transition.runId} questionId={questionId} transition={transition} />
          ))}
          {data.latestByCell.length === 0 ? (
            <p className="text-sm text-text-secondary">아직 실행 결과가 없어요.</p>
          ) : capsule.matrixPackage ? (
            <>
              <MatrixTable runs={data.latestByCell} plannedVersions={data.plannedVersions} />
              <details className="text-xs">
                <summary className="cursor-pointer font-semibold text-text-secondary hover:text-text-primary">칸별 실행 로그</summary>
                <ul className="mt-2 space-y-2">
                  {data.latestByCell.map((run) => (
                    <RunRow key={run.id} questionId={questionId} run={run} />
                  ))}
                </ul>
              </details>
            </>
          ) : (
            <ul className="space-y-2">
              {data.latestByCell.map((run) => (
                <RunRow key={run.id} questionId={questionId} run={run} />
              ))}
            </ul>
          )}
          {data.nextDueAt && <p className="text-xs text-text-secondary">다음 정기 실행 {relativeTime(data.nextDueAt)}</p>}
          <RerunRequest questionId={questionId} capsule={capsule} signedIn={signedIn} />
          {isAuthor && <WorkflowSnippet capsule={capsule} recipe={data.recipe} />}
        </>
      ) : (
        <p className="text-sm text-text-secondary">
          재현 코드가 있는 공개 GitHub 저장소를 붙이면, 그 저장소의 GitHub Actions가 환경별 재현 결과를 보내요.
        </p>
      )}
    </section>
  );
}

function CapsuleSummary({ capsule }: { capsule: ReproductionCapsule }) {
  return (
    <div className="space-y-1 text-sm">
      <a
        href={`https://github.com/${capsule.repository}/tree/${capsule.ref}`}
        target="_blank"
        rel="noopener noreferrer"
        className="font-semibold break-all text-text-primary hover:text-brand"
      >
        {capsule.repository}@{capsule.ref.length === 40 ? capsule.ref.slice(0, 7) : capsule.ref}
      </a>
      <p className="font-mono text-[12px] break-all text-text-secondary">
        {capsule.kind} · {capsule.command}
      </p>
      {capsule.matrixPackage && (
        <p className="text-xs text-text-secondary">
          버전 매트릭스: <span className="font-mono break-all">{capsule.matrixPackage}</span>
        </p>
      )}
    </div>
  );
}

/**
 * Versions down, environments across (Phase 81.3): where the bug shows and where it stops showing,
 * at a glance. Planned versions without a run yet stay as empty rows so the gaps are visible.
 */
function MatrixTable({ runs, plannedVersions }: { runs: ReproductionRun[]; plannedVersions: (string | null)[] }) {
  const environments = [...new Set(runs.map((run) => run.environment))];
  const versions = [...new Set([...plannedVersions, ...runs.map((run) => run.dependencyVersion)].filter((v): v is string => v !== null))];
  const cell = (version: string, environment: string) => runs.find((run) => run.dependencyVersion === version && run.environment === environment);

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-left text-xs">
        <thead>
          <tr className="text-text-secondary">
            <th scope="col" className="py-1 pr-2 font-semibold">
              버전
            </th>
            {environments.map((environment) => (
              <th key={environment} scope="col" className="py-1 pr-2 font-semibold">
                {environment}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {versions.map((version) => (
            <tr key={version} className="border-t border-border">
              <th scope="row" className="py-1.5 pr-2 font-mono font-semibold text-text-primary">
                {version}
              </th>
              {environments.map((environment) => {
                const run = cell(version, environment);
                return (
                  <td key={environment} className="py-1.5 pr-2">
                    {run ? (
                      <span className={`inline-flex h-[20px] items-center rounded-full px-1.5 text-[11px] font-semibold ${RESULT[run.result].className}`}>
                        {RESULT[run.result].label}
                      </span>
                    ) : (
                      <span className="text-text-secondary">—</span>
                    )}
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/**
 * A release that changed behaviour (Phase 82, ADR-0090). It's evidence for a person to read, so
 * nothing changes state: the logs side by side are what the reader decides from.
 */
function TransitionNotice({ questionId, transition }: { questionId: number; transition: ReproductionTransition }) {
  const [open, setOpen] = useState(false);
  const { data, isLoading } = useReproductionComparison(questionId, transition.runId, open);
  const broke = transition.transition === "BROKE";

  return (
    <div className={`space-y-1.5 rounded-xl px-3 py-2 text-xs ${broke ? "bg-danger-subtle text-danger" : "bg-success-subtle text-success"}`}>
      <p className="font-semibold">
        {broke ? "깨짐 감지" : "수정 후보"} · {transition.fromVersion} → {transition.toVersion}
      </p>
      <p>
        {transition.environment} · {relativeTime(transition.finishedAt)}
      </p>
      <button type="button" className="font-semibold underline" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        {open ? "로그 비교 닫기" : "로그 비교"}
      </button>
      {open &&
        (isLoading ? (
          <p>불러오는 중...</p>
        ) : data?.logsAvailable ? (
          <div className="max-h-64 overflow-auto">
            <DiffView lines={data.lines} />
          </div>
        ) : (
          <p>로그가 남아 있지 않아요.</p>
        ))}
    </div>
  );
}

function RunRow({ questionId, run }: { questionId: number; run: ReproductionRun }) {
  const [open, setOpen] = useState(false);
  const { data: log, isLoading } = useReproductionRunLog(questionId, run.id, open);
  const result = RESULT[run.result];

  return (
    <li className="space-y-1.5">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
        <span className={`inline-flex h-[22px] items-center rounded-full px-2 text-[11px] font-semibold ${result.className}`}>{result.label}</span>
        {run.dependencyVersion && <span className="font-mono text-[12px] text-text-primary">{run.dependencyVersion}</span>}
        <span className="text-text-body">{run.environment}</span>
        <span className="text-text-secondary">· {relativeTime(run.finishedAt)}</span>
      </div>
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-text-secondary">
        {run.hasLog && (
          <button type="button" className="font-semibold text-brand hover:underline" onClick={() => setOpen((value) => !value)} aria-expanded={open}>
            {open ? "실행 로그 닫기" : "실행 로그 보기"}
          </button>
        )}
        <a href={run.runUrl} target="_blank" rel="noopener noreferrer" className="hover:text-text-primary">
          GitHub 실행 기록
        </a>
        <span className="font-mono">{run.commitSha.slice(0, 7)}</span>
        {!run.trustedWorkflow && <span className="text-warning">직접 작성한 워크플로</span>}
        {run.runnerEnvironment === "self-hosted" && <span className="text-warning">자체 러너</span>}
      </div>
      {open && (
        <div className="space-y-1">
          {isLoading ? (
            <p className="text-xs text-text-secondary">불러오는 중...</p>
          ) : (
            <pre className="max-h-64 overflow-auto rounded-xl bg-surface-subtle p-3 font-mono text-[11px] leading-relaxed whitespace-pre-wrap break-all text-text-primary">
              {log?.logTail ?? "보관 기간이 지나 로그가 없어요."}
            </pre>
          )}
          {log && log.maskedSecretCount > 0 && (
            <p className="text-xs text-text-secondary">비밀값으로 보이는 {log.maskedSecretCount}곳을 가렸어요.</p>
          )}
        </div>
      )}
    </li>
  );
}

function RerunRequest({ questionId, capsule, signedIn }: { questionId: number; capsule: ReproductionCapsule; signedIn: boolean }) {
  const requestRerun = useRequestRerun(questionId);
  const [open, setOpen] = useState(false);
  const [reason, setReason] = useState("");

  if (capsule.rerunRequestedAt) {
    return (
      <p className="rounded-xl bg-warning-subtle px-3 py-2 text-xs text-warning">
        재실행 요청됨 · {capsule.rerunReason} ({relativeTime(capsule.rerunRequestedAt)}). 다음 실행 결과가 오면 닫혀요.
      </p>
    );
  }
  if (!signedIn) return null;
  if (!open) {
    return (
      <button type="button" className="text-xs font-semibold text-text-secondary hover:text-text-primary" onClick={() => setOpen(true)}>
        결과에 이의 제기
      </button>
    );
  }

  async function submit() {
    try {
      await requestRerun.mutateAsync(reason.trim());
      setOpen(false);
      setReason("");
    } catch {
      // surfaced below via requestRerun.error
    }
  }

  return (
    <div className="space-y-2">
      <label htmlFor="rerun-reason" className="block text-xs font-semibold text-text-primary">
        무엇이 다른가요?
      </label>
      <Textarea
        id="rerun-reason"
        rows={2}
        maxLength={500}
        value={reason}
        placeholder="예: JDK 21.0.6에서는 재현되지 않아요"
        onChange={(event) => setReason(event.target.value)}
      />
      <FormError error={requestRerun.error} fallback="이의 제기를 남기지 못했습니다." size="xs" />
      <div className="flex gap-2">
        <Button className="min-h-9 px-4 text-xs" disabled={!reason.trim() || requestRerun.isPending} onClick={submit}>
          재실행 요청
        </Button>
        <Button variant="ghost" className="min-h-9 px-3 text-xs" onClick={() => setOpen(false)}>
          취소
        </Button>
      </div>
    </div>
  );
}

function CapsuleForm({ questionId, current, onDone }: { questionId: number; current: ReproductionCapsule | null; onDone: () => void }) {
  const attach = useAttachCapsule(questionId);
  const [repository, setRepository] = useState(current?.repository ?? "");
  const [ref, setRef] = useState(current?.ref ?? "main");
  const [kind, setKind] = useState<ReproductionKind>(current?.kind ?? "GRADLE");
  const [command, setCommand] = useState(current?.command ?? "");
  const [matrixPackage, setMatrixPackage] = useState(current?.matrixPackage ?? "");
  const [matrixVersions, setMatrixVersions] = useState(current?.matrixVersions.join(", ") ?? "");

  async function submit() {
    const versions = matrixVersions
      .split(",")
      .map((version) => version.trim())
      .filter(Boolean);
    try {
      await attach.mutateAsync({
        repository: repository.trim(),
        ref: ref.trim(),
        kind,
        command: command.trim(),
        ...(matrixPackage.trim() ? { matrixPackage: matrixPackage.trim(), matrixVersions: versions } : {}),
      });
      onDone();
    } catch {
      // surfaced below via attach.error
    }
  }

  return (
    <div className="space-y-3">
      <div className="space-y-1">
        <label htmlFor="capsule-repository" className="block text-xs font-semibold">
          공개 GitHub 저장소
        </label>
        <Input id="capsule-repository" placeholder="owner/name" value={repository} onChange={(event) => setRepository(event.target.value)} />
      </div>
      <div className="space-y-1">
        <label htmlFor="capsule-ref" className="block text-xs font-semibold">
          브랜치·태그·커밋
        </label>
        <Input id="capsule-ref" value={ref} onChange={(event) => setRef(event.target.value)} />
        <p className="text-xs text-text-secondary">커밋 sha 40자를 넣으면 그 커밋의 실행만 인정해요.</p>
      </div>
      <div className="space-y-1">
        <label htmlFor="capsule-kind" className="block text-xs font-semibold">
          실행 방식
        </label>
        <select
          id="capsule-kind"
          className="w-full rounded-2xl border border-border-strong bg-surface px-4 py-2.5 text-[15px] text-text-primary"
          value={kind}
          onChange={(event) => setKind(event.target.value as ReproductionKind)}
        >
          <option value="GRADLE">Gradle</option>
          <option value="SCRIPT">스크립트</option>
          <option value="DOCKERFILE">Dockerfile</option>
        </select>
      </div>
      <div className="space-y-1">
        <label htmlFor="capsule-command" className="block text-xs font-semibold">
          명령
        </label>
        <Input id="capsule-command" className="font-mono text-[13px]" value={command} onChange={(event) => setCommand(event.target.value)} />
        <p className="text-xs text-text-secondary">{KIND_HINT[kind]}. 버그가 보이면 실패(0이 아닌 종료 코드), 고쳐졌으면 성공해야 해요.</p>
      </div>
      <div className="space-y-1">
        <label htmlFor="capsule-matrix-package" className="block text-xs font-semibold">
          버전 매트릭스 패키지 (선택)
        </label>
        <Input
          id="capsule-matrix-package"
          className="font-mono text-[13px]"
          placeholder="com.zaxxer:HikariCP"
          value={matrixPackage}
          onChange={(event) => setMatrixPackage(event.target.value)}
        />
        <p className="text-xs text-text-secondary">
          질문의 스택 지문과 답변의 적용 범위 경계에서 최대 5개 버전을 골라 버전마다 실행해요. 빌드는 QUNO_VERSION(Gradle은 qunoVersion 속성)으로 버전을 받아요.
        </p>
      </div>
      {matrixPackage.trim() && (
        <div className="space-y-1">
          <label htmlFor="capsule-matrix-versions" className="block text-xs font-semibold">
            꼭 돌릴 버전 (쉼표로 구분, 선택)
          </label>
          <Input
            id="capsule-matrix-versions"
            className="font-mono text-[13px]"
            placeholder="4.0.3, 6.0.0"
            value={matrixVersions}
            onChange={(event) => setMatrixVersions(event.target.value)}
          />
        </div>
      )}
      {current && <p className="text-xs text-warning">캡슐을 바꾸면 지금까지의 실행 결과는 새 캡슐에 이어지지 않아요.</p>}
      <FormError error={attach.error} fallback="재현 캡슐을 저장하지 못했습니다." size="xs" />
      <div className="flex gap-2">
        <Button className="min-h-9 px-4 text-xs" disabled={!repository.trim() || !ref.trim() || !command.trim() || attach.isPending} onClick={submit}>
          저장
        </Button>
        <Button variant="ghost" className="min-h-9 px-3 text-xs" onClick={onDone}>
          취소
        </Button>
      </div>
    </div>
  );
}

/** What the author adds to the repository: Quno's reusable workflow with this capsule's spec. */
export function workflowSnippet(capsule: ReproductionCapsule, apiBaseUrl: string, recipe?: EnvironmentRecipe | null): string {
  const java = recipe?.javaVersion ?? (capsule.kind === "GRADLE" ? "21" : null);
  const javaLine = java ? `\n      java-version: "${java}"` : "";
  const nodeLine = recipe?.nodeVersion ? `\n      node-version: "${recipe.nodeVersion}"` : "";
  const runnerLine = recipe && recipe.runsOn !== "ubuntu-latest" ? `\n      runs-on: ${recipe.runsOn}` : "";
  const matrixNote = capsule.matrixPackage
    ? `\n# ${capsule.matrixPackage} 버전은 QUNO_VERSION(Gradle은 findProperty("qunoVersion"))으로 들어옵니다.`
    : "";
  return `name: Quno reproduction${matrixNote}
on:
  workflow_dispatch:
  schedule: [{ cron: "17 3 * * *" }]
permissions:
  contents: read
  id-token: write
jobs:
  reproduce:
    uses: polynomeer/quno/.github/workflows/reproduce.yml@main
    with:
      quno-url: ${apiBaseUrl}
      capsule-id: ${capsule.id}
      kind: ${capsule.kind}
      command: ${JSON.stringify(capsule.command)}${javaLine}${nodeLine}${runnerLine}
`;
}

function WorkflowSnippet({ capsule, recipe }: { capsule: ReproductionCapsule; recipe?: EnvironmentRecipe | null }) {
  const [copied, setCopied] = useState(false);
  const snippet = workflowSnippet(capsule, API_BASE_URL, recipe);

  async function copy() {
    try {
      await navigator.clipboard.writeText(snippet);
      setCopied(true);
    } catch {
      // Clipboard can be unavailable; the snippet stays selectable.
    }
  }

  return (
    <details className="text-xs">
      <summary className="cursor-pointer font-semibold text-text-secondary hover:text-text-primary">워크플로 설정</summary>
      <div className="mt-2 space-y-2">
        <p className="text-text-secondary">
          저장소의 <code className="font-mono">.github/workflows/quno-reproduce.yml</code>로 추가하세요. 매일 Quno에 실행할 차례인지 묻고(조회가 많은
          질문일수록 자주), Actions 탭에서 직접 실행하거나 이의 제기가 있으면 바로 돌아요. 비밀값은 필요 없어요.
        </p>
        {recipe && (
          <p className="text-text-secondary">
            질문의 환경 스냅샷에 가장 가까운 러너와 런타임으로 채웠어요.
            {recipe.notes.length > 0 && ` ${recipe.notes.join(" ")}`}
          </p>
        )}
        <pre className="overflow-auto rounded-xl bg-surface-subtle p-3 font-mono text-[11px] leading-relaxed text-text-primary">{snippet}</pre>
        <Button variant="secondary" className="min-h-8 px-3 text-xs" onClick={copy}>
          {copied ? "복사했어요" : "복사"}
        </Button>
      </div>
    </details>
  );
}
