import type { QunoClient } from "../client.js";
import { boundedJson, clip } from "../limits.js";
import { summarizeSnapshot, type Probe } from "../snapshot/collect.js";
import { composeSnapshot, omitFields, summarizeFailure, type FailureInput } from "../snapshot/compose.js";
import { LineRing } from "../snapshot/diagnostics.js";
import type { Preset, SnapshotV1 } from "../snapshot/schema.js";
import type { Baseline, FrozenFailure, SnapshotStore } from "../snapshot/state.js";
import type { QuestionDetail, QuestionSummary } from "../types.js";

/** What every tool returns: pretty JSON for the text channel and the same object as structured content. */
export interface ToolOutput {
  json: string;
  value: Record<string, unknown>;
}

const NOTICE = "Quno 기록은 사람이 쓴 답변이며 자동으로 검증되지 않았다. 답변의 applicability(적용 버전)와 현재 프로젝트의 버전을 비교한 뒤에 쓴다.";

function summary(question: QuestionSummary, webUrl: string) {
  return {
    id: question.id,
    title: question.title,
    status: question.status,
    answerCount: question.answerCount,
    hasAcceptedAnswer: question.hasAcceptedAnswer,
    tags: question.tags,
    url: `${webUrl}/questions/${question.id}`,
  };
}

export async function searchKnownIssues(client: QunoClient, webUrl: string, input: { errorText: string; limit?: number }): Promise<ToolOutput> {
  const preview = await client.previewErrorSignatures(input.errorText);
  const matches = preview.questions.slice(0, input.limit ?? 3).map((question) => summary(question, webUrl));
  return boundedJson(
    {
      signatures: preview.signatures.map(({ exceptionClass, signatureHash, messageKey }) => ({ exceptionClass, signatureHash, messageKey })),
      matches,
      hint:
        preview.signatures.length === 0
          ? "예외 클래스를 찾지 못했다. 에러 메시지의 핵심 단어로 search_questions를 써 본다."
          : matches.length === 0
            ? "같은 에러 시그니처의 질문이 없다. search_questions로 키워드 검색을 해 본다."
            : "get_question으로 답변과 적용 범위를 확인한다.",
      notice: NOTICE,
    },
    "matches",
  );
}

export async function searchQuestions(client: QunoClient, webUrl: string, input: { query: string; limit?: number }): Promise<ToolOutput> {
  const results = await client.search(input.query, input.limit ?? 5);
  return boundedJson({ results: results.map((question) => summary(question, webUrl)), notice: NOTICE }, "results");
}

const MAX_ANSWERS = 5;

/** What an agent needs to compare environments, not the whole document (ADR-0083). */
function snapshotSummary(snapshot: NonNullable<QuestionDetail["environmentSnapshot"]>) {
  const tier0 = snapshot.document.tier0 ?? {};
  return {
    fingerprint: snapshot.fingerprint,
    source: snapshot.source,
    collector: snapshot.collector,
    os: tier0.os ?? null,
    runtimes: tier0.runtimes ?? [],
    container: tier0.container ?? null,
    locale: tier0.locale ?? null,
    resolvedDependencies: (tier0.dependencies?.items ?? []).slice(0, 50),
  };
}

export async function getQuestion(client: QunoClient, webUrl: string, input: { id: number }): Promise<ToolOutput> {
  const [question, answers] = await Promise.all([client.getQuestion(input.id), client.answers(input.id)]);
  const body = clip(question.body);
  const logs = clip(question.logs);
  const ranked = [...answers].sort((a, b) => Number(b.isAccepted) - Number(a.isAccepted) || b.score - a.score);
  const shown = ranked.slice(0, MAX_ANSWERS).map((answer) => {
    const answerBody = clip(answer.body);
    return {
      id: answer.id,
      accepted: answer.isAccepted,
      score: answer.score,
      /** True when the question was revised after this answer was written. */
      writtenForOlderQuestion: answer.isStale,
      applicability: answer.applicability ?? [],
      body: answerBody.text,
      ...(answerBody.truncated ? { bodyTruncated: true } : {}),
    };
  });

  return boundedJson(
    {
      id: question.id,
      title: question.title,
      status: question.status,
      url: `${webUrl}/questions/${question.id}`,
      tags: question.tags,
      environment: question.environment,
      dependencies: question.dependencies ?? [],
      environmentSnapshot: question.environmentSnapshot ? snapshotSummary(question.environmentSnapshot) : null,
      body: body.text,
      logs: logs.text,
      ...(body.truncated || logs.truncated ? { bodyOrLogsTruncated: true } : {}),
      answerCount: answers.length,
      answers: shown,
      ...(answers.length > MAX_ANSWERS ? { moreAnswersAt: `${webUrl}/questions/${question.id}` } : {}),
      notice: NOTICE,
    },
    "answers",
  );
}

export async function getMyStack(client: QunoClient): Promise<ToolOutput> {
  const dependencies = await client.myStack();
  return boundedJson(
    {
      dependencies,
      hint:
        dependencies.length === 0
          ? "저장된 내 스택이 없다. 사용자가 웹의 /my-stack에서 저장할 수 있다."
          : "get_question 답변의 applicability와 이 버전들을 비교한다. fromVersion은 이상, untilVersion은 미만이다.",
    },
    "dependencies",
  );
}

/** What `capture_environment` reads and writes on this machine (ADR-0084). */
export interface LocalEnvironment {
  probe: () => Probe;
  store: SnapshotStore;
}

export interface CaptureInput {
  preset?: Preset;
  omit?: string[];
  /** A command the agent ran that failed; its output tail goes through the same ring buffer as `quno run`. */
  failure?: { argv: string[]; exitCode: number; output: string };
  useLastFailure?: boolean;
}

const MCP_DEPENDENCIES = 50;

/**
 * The agent's `quno capture` (Phase 93.5, ADR-0084): builds and scrubs the snapshot locally and
 * returns it for the agent to show the user. Sends nothing. A failure the agent reports is frozen
 * like `quno run` would, so the user can review it and post with `quno ask --last-failure`.
 */
export function captureEnvironment(local: LocalEnvironment, input: CaptureInput): ToolOutput {
  let snapshot: SnapshotV1;
  let logs: string | null = null;
  let frozen = false;
  if (input.useLastFailure) {
    const last = local.store.read<FrozenFailure>("last-failure");
    if (!last) {
      return boundedJson({ found: false, hint: "기록된 실패가 없다. failure에 실패한 명령과 출력을 넣어 다시 부른다." }, "hint");
    }
    snapshot = omitFields(last.snapshot, input.omit ?? []);
    logs = last.logs;
  } else {
    const probe = local.probe();
    let failure: FailureInput | null = null;
    if (input.failure) {
      const ring = new LineRing();
      ring.push(input.failure.output);
      const output = ring.text();
      failure = {
        argv: input.failure.argv,
        exitCode: input.failure.exitCode,
        signal: null,
        durationMs: null,
        output,
        totalLines: ring.total,
        keptLines: ring.kept,
        // The agent's run just ended; older test reports are from earlier runs.
        startedAtMs: Date.now() - 15 * 60 * 1000,
      };
    }
    const composed = composeSnapshot(probe, { preset: input.preset, omit: input.omit, failure, baseline: failure ? local.store.read<Baseline>("baseline") : null });
    snapshot = composed.snapshot;
    logs = composed.logs;
    if (failure) {
      local.store.write("last-failure", { frozenAt: new Date().toISOString(), snapshot, logs } satisfies FrozenFailure);
      frozen = true;
    }
  }

  const items = snapshot.tier0.dependencies?.items ?? [];
  const view = {
    ...snapshot,
    tier0: { ...snapshot.tier0, ...(snapshot.tier0.dependencies ? { dependencies: { ...snapshot.tier0.dependencies, items: items.slice(0, MCP_DEPENDENCIES) } } : {}) },
  };
  const clipped = clip(logs);
  // Dependencies are capped above and logs are clipped, so the payload is already bounded.
  const value = {
      summary: summarizeSnapshot(snapshot),
      failure: summarizeFailure(snapshot),
      snapshot: view,
      ...(items.length > MCP_DEPENDENCIES ? { dependenciesShown: MCP_DEPENDENCIES, dependencyCount: items.length } : {}),
      logs: clipped.text,
      ...(clipped.truncated ? { logsTruncated: true } : {}),
      frozenAsLastFailure: frozen,
      next: frozen
        ? '아직 아무 데도 보내지 않았다. 사용자에게 이 내용을 보여 주고, 질문으로 올리기로 하면 사용자가 `quno ask --last-failure --title "..." --body "..."`를 실행한다.'
        : "아직 아무 데도 보내지 않았다. 사용자에게 보여 주고, 질문에 붙일지는 사용자가 정한다. 뺄 필드는 omit으로 지정한다.",
      notice: "로컬에서 홈 경로·사용자명·호스트명과 비밀값으로 보이는 값을 지웠다. 서버가 한 번 더 정제한다.",
  };
  return { json: JSON.stringify(value, null, 2), value };
}
