import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReportQueueItem } from "./ReportQueueItem";
import { moderationApi } from "../api/moderation.api";
import type { Report } from "@/features/report/api/report.types";

vi.mock("../api/moderation.api", () => ({
  moderationApi: { listReports: vi.fn(), dismiss: vi.fn(), hide: vi.fn() },
}));

function report(overrides: Partial<Report> = {}): Report {
  return {
    id: 1,
    reporterId: 5,
    targetType: "QUESTION",
    targetId: 10,
    reason: "SPAM",
    message: null,
    status: "PENDING",
    resolvedBy: null,
    resolvedAt: null,
    createdAt: "2026-01-01T00:00:00.000Z",
    ...overrides,
  };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ReportQueueItem", () => {
  beforeEach(() => {
    vi.mocked(moderationApi.dismiss).mockReset();
    vi.mocked(moderationApi.hide).mockReset();
  });

  it("shows the Korean reason label", () => {
    renderWithClient(<ReportQueueItem report={report({ reason: "DUPLICATE" })} />);
    expect(screen.getByText("중복 질문")).toBeInTheDocument();
  });

  it("links to the question for a QUESTION report", () => {
    renderWithClient(<ReportQueueItem report={report({ targetType: "QUESTION", targetId: 10 })} />);
    expect(screen.getByRole("link", { name: "Question #10" })).toHaveAttribute("href", "/questions/10");
  });

  it("shows plain text (no link) for an ANSWER report", () => {
    renderWithClient(<ReportQueueItem report={report({ targetType: "ANSWER", targetId: 20 })} />);
    expect(screen.getByText("Answer #20")).toBeInTheDocument();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });

  it("shows the reporter's message when present", () => {
    renderWithClient(<ReportQueueItem report={report({ message: "이건 스팸입니다" })} />);
    expect(screen.getByText("이건 스팸입니다")).toBeInTheDocument();
  });

  it("dismisses the report via moderationApi.dismiss", async () => {
    vi.mocked(moderationApi.dismiss).mockResolvedValue(undefined);
    renderWithClient(<ReportQueueItem report={report({ id: 7 })} />);

    await userEvent.click(screen.getByRole("button", { name: "Keep (Dismiss)" }));

    await waitFor(() => expect(moderationApi.dismiss).toHaveBeenCalledWith(7));
    expect(moderationApi.hide).not.toHaveBeenCalled();
  });

  it("hides the content via moderationApi.hide", async () => {
    vi.mocked(moderationApi.hide).mockResolvedValue(undefined);
    renderWithClient(<ReportQueueItem report={report({ id: 7 })} />);

    await userEvent.click(screen.getByRole("button", { name: "Hide" }));

    await waitFor(() => expect(moderationApi.hide).toHaveBeenCalledWith(7));
    expect(moderationApi.dismiss).not.toHaveBeenCalled();
  });
});
