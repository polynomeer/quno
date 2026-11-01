import { QunoClient } from "../src/client.js";

export interface Recorded {
  method: string;
  path: string;
  body: unknown;
  authorization: string | null;
}

type Handler = (body: unknown) => { status?: number; json: unknown };

/** A QunoClient whose fetch answers from [routes] ("GET /api/v1/me" → handler) and records every call. */
export function fakeClient(routes: Record<string, Handler>, token: string | null = "quno_pat_test") {
  const calls: Recorded[] = [];
  const fetchImpl = (async (input: string | URL | Request, init?: RequestInit) => {
    const url = new URL(String(input));
    const method = init?.method ?? "GET";
    const body = init?.body ? JSON.parse(String(init.body)) : undefined;
    const headers = (init?.headers ?? {}) as Record<string, string>;
    calls.push({ method, path: url.pathname + url.search, body, authorization: headers.Authorization ?? null });
    const handler = routes[`${method} ${url.pathname}`];
    if (!handler) return new Response(JSON.stringify({ code: "NOT_FOUND", message: "no route" }), { status: 404 });
    const { status = 200, json } = handler(body);
    return new Response(json === undefined ? "" : JSON.stringify(json), { status });
  }) as typeof fetch;
  return { client: new QunoClient("http://api.test", token, fetchImpl), calls };
}

export const QUESTION_SUMMARY = {
  id: 11,
  title: "HikariPool connection is not available",
  status: "OPEN",
  tags: ["spring-boot"],
  score: 3,
  answerCount: 2,
  hasAcceptedAnswer: true,
  createdAt: "2026-10-20T00:00:00Z",
  updatedAt: "2026-10-20T00:00:00Z",
};
