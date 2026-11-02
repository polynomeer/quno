import type { QunoClient } from "../client.js";
import { boundedJson, clip } from "../limits.js";
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
