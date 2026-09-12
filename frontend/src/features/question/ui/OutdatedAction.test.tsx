import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { OutdatedAction } from "./OutdatedAction";
import { questionApi } from "../api/question.api";
import { ApiError } from "@/shared/api/api-error";

vi.mock("../api/question.api", () => ({
  questionApi: { markOutdated: vi.fn() },
}));

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("OutdatedAction", () => {
  beforeEach(() => {
    vi.mocked(questionApi.markOutdated).mockReset();
  });

  it("renders nothing when the question is already OUTDATED", () => {
    const { container } = renderWithClient(<OutdatedAction questionId={1} status="OUTDATED" />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows a collapsed trigger for a non-OUTDATED question", () => {
    renderWithClient(<OutdatedAction questionId={1} status="OPEN" />);
    expect(screen.getByRole("button", { name: "Outdated로 표시" })).toBeInTheDocument();
  });

  it("reveals the reason input when the trigger is clicked", async () => {
    renderWithClient(<OutdatedAction questionId={1} status="OPEN" />);

    await userEvent.click(screen.getByRole("button", { name: "Outdated로 표시" }));

    expect(screen.getByPlaceholderText(/Outdated 사유/)).toBeInTheDocument();
  });

  it("keeps the submit button disabled until a reason is entered", async () => {
    renderWithClient(<OutdatedAction questionId={1} status="OPEN" />);
    await userEvent.click(screen.getByRole("button", { name: "Outdated로 표시" }));

    expect(screen.getByRole("button", { name: "표시" })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText(/Outdated 사유/), "더 이상 재현되지 않음");

    expect(screen.getByRole("button", { name: "표시" })).toBeEnabled();
  });

  it("submits the trimmed reason to questionApi.markOutdated", async () => {
    vi.mocked(questionApi.markOutdated).mockResolvedValue({ id: 1, title: "t", status: "OUTDATED", versionNumber: 2 });
    renderWithClient(<OutdatedAction questionId={1} status="OPEN" />);
    await userEvent.click(screen.getByRole("button", { name: "Outdated로 표시" }));
    await userEvent.type(screen.getByPlaceholderText(/Outdated 사유/), "  더 이상 재현되지 않음  ");

    await userEvent.click(screen.getByRole("button", { name: "표시" }));

    expect(questionApi.markOutdated).toHaveBeenCalledWith(1, "더 이상 재현되지 않음");
  });

  it("shows the backend's error message when marking fails", async () => {
    vi.mocked(questionApi.markOutdated).mockRejectedValue(new ApiError(403, "FORBIDDEN", "권한이 없습니다."));
    renderWithClient(<OutdatedAction questionId={1} status="OPEN" />);
    await userEvent.click(screen.getByRole("button", { name: "Outdated로 표시" }));
    await userEvent.type(screen.getByPlaceholderText(/Outdated 사유/), "사유");

    await userEvent.click(screen.getByRole("button", { name: "표시" }));

    expect(await screen.findByText("권한이 없습니다.")).toBeInTheDocument();
  });
});
