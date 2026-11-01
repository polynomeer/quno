import type {
  Answer,
  ErrorSignaturePreview,
  Me,
  NewQuestion,
  ParsedDependencyFile,
  QuestionDetail,
  QuestionSummary,
  StackDependency,
} from "./types.js";

export const MAX_SIGNATURE_INPUT = 20_000;

export class QunoApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "QunoApiError";
  }
}

/** Thin wrapper over the REST API; every call sends the personal access token when there is one (ADR-0079). */
export class QunoClient {
  constructor(
    private readonly apiUrl: string,
    private readonly token: string | null,
    private readonly fetchImpl: typeof fetch = fetch,
  ) {}

  me = () => this.request<Me>("GET", "/api/v1/me");

  search = (query: string, limit = 10) =>
    this.request<QuestionSummary[]>("GET", `/api/v1/search?q=${encodeURIComponent(query)}&limit=${limit}`);

  /** The server reads at most 20,000 characters; the root cause sits at the end of a trace, so the tail is kept. */
  previewErrorSignatures = (text: string) =>
    this.request<ErrorSignaturePreview>("POST", "/api/v1/error-signatures/preview", { text: text.slice(-MAX_SIGNATURE_INPUT) });

  parseDependencies = (fileName: string, content: string) =>
    this.request<ParsedDependencyFile>("POST", "/api/v1/stack-fingerprints/parse", { fileName, content });

  createQuestion = (question: NewQuestion) => this.request<QuestionDetail>("POST", "/api/v1/questions", question);

  getQuestion = (id: number) => this.request<QuestionDetail>("GET", `/api/v1/questions/${id}`);

  answers = (questionId: number) => this.request<Answer[]>("GET", `/api/v1/questions/${questionId}/answers`);

  myStack = async () => (await this.request<{ dependencies: StackDependency[] }>("GET", "/api/v1/me/stack")).dependencies;

  private async request<T>(method: "GET" | "POST", path: string, body?: unknown): Promise<T> {
    const headers: Record<string, string> = { Accept: "application/json" };
    if (this.token) headers.Authorization = `Bearer ${this.token}`;
    if (body !== undefined) headers["Content-Type"] = "application/json";

    let response: Response;
    try {
      response = await this.fetchImpl(this.apiUrl + path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    } catch (cause) {
      throw new QunoApiError(0, "UNREACHABLE", `Quno 서버(${this.apiUrl})에 연결하지 못했습니다: ${(cause as Error).message}`);
    }

    const text = await response.text();
    if (!response.ok) {
      let code = "HTTP_" + response.status;
      let message = text || response.statusText;
      try {
        const parsed = JSON.parse(text) as { code?: string; message?: string };
        code = parsed.code ?? code;
        message = parsed.message || message;
      } catch {
        // not JSON — keep the raw text
      }
      throw new QunoApiError(response.status, code, message);
    }
    return (text ? JSON.parse(text) : undefined) as T;
  }
}
