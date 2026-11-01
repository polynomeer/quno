import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { QunoBotSuggestions } from "./QunoBotSuggestions";
import { MaskedSecretsNotice } from "./MaskedSecretsNotice";
import { engineApi, type EngineSuggestion } from "../api/engine.api";

vi.mock("../api/engine.api", () => ({
  engineApi: { list: vi.fn(), accept: vi.fn(), dismiss: vi.fn() },
  engineKeys: { suggestions: (id: number) => ["questions", id, "engine-suggestions"] },
}));

const SUGGESTION: EngineSuggestion = {
  id: 5,
  questionId: 10,
  targetVersionNumber: 1,
  kind: "SECRET_REDACTION",
  status: "OPEN",
  summary: "비밀값으로 보이는 문자열 1개(AWS 액세스 키)를 공개 화면에서 가렸어요. 원본에서도 지울까요?",
  engine: "secret-scan",
  engineVersion: 1,
  inputHash: "abcdef0123456789",
  changedFields: ["logs"],
  proposedTitle: null,
  proposedBody: null,
  proposedEnvironment: null,
  proposedLogs: "aws.accessKeyId=[REDACTED]",
  createdAt: "",
  resolvedAt: null,
};

const CURRENT = { title: "t", body: "b", environment: null, logs: "aws.accessKeyId=AKIA-placeholder" };

function renderWithClient(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe("QunoBotSuggestions", () => {
  beforeEach(() => {
    vi.mocked(engineApi.list).mockReset();
    vi.mocked(engineApi.accept).mockReset().mockResolvedValue({ ...SUGGESTION, status: "ACCEPTED" });
    vi.mocked(engineApi.dismiss).mockReset().mockResolvedValue({ ...SUGGESTION, status: "DISMISSED" });
  });

  it("shows only open suggestions with what would change and where it came from", async () => {
    vi.mocked(engineApi.list).mockResolvedValue([SUGGESTION, { ...SUGGESTION, id: 4, status: "SUPERSEDED" }]);
    renderWithClient(<QunoBotSuggestions questionId={10} current={CURRENT} />);

    expect(await screen.findByRole("heading", { name: "QunoBot 제안 1건" })).toBeInTheDocument();
    expect(screen.getByLabelText("로그 변경")).toHaveTextContent("- aws.accessKeyId=AKIA-placeholder+ aws.accessKeyId=[REDACTED]");
    expect(screen.getByText(/secret-scan@1 · v1 · 입력 abcdef012345/)).toBeInTheDocument();
    expect(screen.getByText(/폐기하고 새로 발급/)).toBeInTheDocument();
  });

  it("accepts or dismisses by id", async () => {
    vi.mocked(engineApi.list).mockResolvedValue([SUGGESTION]);
    renderWithClient(<QunoBotSuggestions questionId={10} current={CURRENT} />);

    await userEvent.click(await screen.findByRole("button", { name: "수락" }));
    await waitFor(() => expect(engineApi.accept).toHaveBeenCalledWith(5));
    await userEvent.click(screen.getByRole("button", { name: "무시 — 비밀값이 아니에요" }));
    await waitFor(() => expect(engineApi.dismiss).toHaveBeenCalledWith(5));
  });

  it("labels cleanup kinds with their own basis and dismissal", async () => {
    vi.mocked(engineApi.list).mockResolvedValue([
      { ...SUGGESTION, kind: "ADD_CODE_FENCE_LANGUAGE", engine: "format-tidy", summary: "언어를 붙여요", changedFields: ["body"], proposedLogs: null, proposedBody: "```json\n{}\n```" },
    ]);
    renderWithClient(<QunoBotSuggestions questionId={10} current={{ ...CURRENT, body: "```\n{}\n```" }} />);

    expect(await screen.findByText(/근거: 코드 모양/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "무시 — 이 질문에서 다시 제안하지 않아요" })).toBeInTheDocument();
    expect(screen.queryByText(/폐기하고 새로 발급/)).not.toBeInTheDocument();
    expect(screen.getByLabelText("본문 변경")).toHaveTextContent("- ```+ ```json");
  });

  it("renders nothing without open suggestions", async () => {
    vi.mocked(engineApi.list).mockResolvedValue([{ ...SUGGESTION, status: "ACCEPTED" }]);
    const { container } = renderWithClient(<QunoBotSuggestions questionId={10} current={CURRENT} />);
    await waitFor(() => expect(engineApi.list).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });
});

describe("MaskedSecretsNotice", () => {
  it("says how many values were masked, and nothing at zero", () => {
    const { container, rerender } = render(<MaskedSecretsNotice count={0} />);
    expect(container).toBeEmptyDOMElement();
    rerender(<MaskedSecretsNotice count={2} />);
    expect(screen.getByText(/2개를 가렸어요/)).toBeInTheDocument();
  });
});
