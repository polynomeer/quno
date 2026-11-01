import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { IssueTokenForm } from "./IssueTokenForm";
import { TokenList } from "./TokenList";
import { tokenApi } from "../api/token.api";
import type { PersonalAccessToken } from "../api/token.types";

vi.mock("../api/token.api", () => ({
  tokenApi: { list: vi.fn(), issue: vi.fn(), revoke: vi.fn() },
  tokenKeys: { mine: ["tokens", "me"] },
}));

const TOKEN: PersonalAccessToken = {
  id: 7,
  name: "laptop CLI",
  displayPrefix: "quno_pat_ab1",
  scopes: ["READ"],
  createdAt: "2026-10-01T00:00:00Z",
  expiresAt: "2099-01-01T00:00:00Z",
  lastUsedAt: null,
  revokedAt: null,
};

function renderWithClient(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
}

describe("IssueTokenForm", () => {
  beforeEach(() => vi.mocked(tokenApi.issue).mockReset());

  it("issues with the chosen scopes and lifetime and shows the secret once", async () => {
    vi.mocked(tokenApi.issue).mockResolvedValue({ secret: "quno_pat_secret-value", token: TOKEN });
    renderWithClient(<IssueTokenForm />);

    expect(screen.getByRole("button", { name: "토큰 만들기" })).toBeDisabled();
    await userEvent.type(screen.getByLabelText("이름"), "agent");
    await userEvent.click(screen.getByRole("checkbox", { name: /질문 작성/ }));
    await userEvent.selectOptions(screen.getByLabelText("만료"), "30");
    await userEvent.click(screen.getByRole("button", { name: "토큰 만들기" }));

    await waitFor(() => expect(tokenApi.issue).toHaveBeenCalledWith({ name: "agent", scopes: ["READ", "ASK"], lifetimeDays: 30 }));
    expect(await screen.findByText("quno_pat_secret-value")).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("다시 볼 수 없어요");
    expect(screen.getByLabelText("이름")).toHaveValue("");
  });

  it("needs at least one scope", async () => {
    renderWithClient(<IssueTokenForm />);
    await userEvent.type(screen.getByLabelText("이름"), "agent");
    await userEvent.click(screen.getByRole("checkbox", { name: /읽기/ }));

    expect(screen.getByRole("button", { name: "토큰 만들기" })).toBeDisabled();
  });
});

describe("TokenList", () => {
  beforeEach(() => vi.mocked(tokenApi.revoke).mockReset().mockResolvedValue(undefined));

  it("shows state and revokes only after a second click", async () => {
    renderWithClient(
      <TokenList
        tokens={[TOKEN, { ...TOKEN, id: 8, name: "old", revokedAt: "2026-10-02T00:00:00Z" }]}
      />,
    );

    expect(screen.getByText("사용 중")).toBeInTheDocument();
    expect(screen.getByText("폐기됨")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /폐기/ })).toHaveLength(1);

    await userEvent.click(screen.getByRole("button", { name: "laptop CLI 폐기" }));
    expect(tokenApi.revoke).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole("button", { name: "정말 폐기" }));

    await waitFor(() => expect(tokenApi.revoke).toHaveBeenCalledWith(7));
  });
});
