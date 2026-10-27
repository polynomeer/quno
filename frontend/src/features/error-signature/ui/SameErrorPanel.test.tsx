import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SameErrorPanel } from "./SameErrorPanel";
import { errorSignatureApi } from "../api/error-signature.api";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";
import type { QuestionSummary } from "@/features/question/api/question.types";

vi.mock("../api/error-signature.api", () => ({ errorSignatureApi: { preview: vi.fn() } }));

const TRACE = "java.net.ConnectException: Connection refused\n\tat a.B.c(B.java:1)";
const SIGNATURE = {
  exceptionClass: "java.net.ConnectException",
  normalizedMessage: "connection refused",
  frames: ["a.B.c"],
  signatureHash: "0123456789abcdef",
  messageKey: "fedcba9876543210",
};

function question(overrides: Partial<QuestionSummary> = {}): QuestionSummary {
  return {
    id: 7,
    title: "Postgres 연결이 거부됩니다",
    status: "RESOLVED",
    tags: [],
    score: 0,
    answerCount: 1,
    hasAcceptedAnswer: true,
    versionNumber: 1,
    viewCount: 0,
    createdAt: "",
    updatedAt: "",
    ...overrides,
  } as QuestionSummary;
}

function renderPanel(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider>{ui}</LocaleProvider>
    </QueryClientProvider>,
  );
}

describe("SameErrorPanel", () => {
  beforeEach(() => {
    vi.mocked(errorSignatureApi.preview).mockReset();
  });

  it("lists questions with the same error and marks those already answered", async () => {
    vi.mocked(errorSignatureApi.preview).mockResolvedValue({ signatures: [SIGNATURE], questions: [question()] });
    renderPanel(<SameErrorPanel text={TRACE} />);

    expect(await screen.findByRole("heading", { name: "같은 에러 1건" })).toBeInTheDocument();
    expect(screen.getByText("ConnectException")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Postgres 연결이 거부됩니다/ })).toHaveAttribute("href", "/questions/7");
    expect(screen.getByText("채택된 답변 있음")).toBeInTheDocument();
  });

  it("says it's the first when the signature has no questions yet", async () => {
    vi.mocked(errorSignatureApi.preview).mockResolvedValue({ signatures: [SIGNATURE], questions: [] });
    renderPanel(<SameErrorPanel text={TRACE} />);

    expect(await screen.findByText("이 에러로 올라온 질문은 아직 없어요. 첫 질문이 될 거예요.")).toBeInTheDocument();
  });

  it("never calls the server for text without an exception name", async () => {
    const { container } = renderPanel(<SameErrorPanel text="그냥 설명만 있는 본문" />);

    await waitFor(() => expect(errorSignatureApi.preview).not.toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });
});
