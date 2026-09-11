import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReportButton } from "./ReportButton";
import { useSession } from "@/features/auth/hooks/useSession";
import { reportApi } from "../api/report.api";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { Report } from "../api/report.types";

vi.mock("@/features/auth/hooks/useSession", () => ({
  useSession: vi.fn(),
}));

vi.mock("../api/report.api", () => ({
  reportApi: { reportQuestion: vi.fn(), reportAnswer: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const REPORT: Report = {
  id: 1,
  reporterId: ME.id,
  targetType: "QUESTION",
  targetId: 10,
  reason: "SPAM",
  message: null,
  status: "PENDING",
  resolvedBy: null,
  resolvedAt: null,
  createdAt: "",
};

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ReportButton", () => {
  beforeEach(() => {
    vi.mocked(reportApi.reportQuestion).mockReset().mockResolvedValue(REPORT);
    vi.mocked(reportApi.reportAnswer).mockReset().mockResolvedValue(REPORT);
  });

  it("renders nothing for an anonymous viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    const { container } = renderWithClient(<ReportButton targetType="QUESTION" targetId={10} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the collapsed 'Report' link for a logged-in viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<ReportButton targetType="QUESTION" targetId={10} />);

    expect(screen.getByRole("button", { name: "Report" })).toBeInTheDocument();
  });

  it("reveals the reason/message form when Report is clicked", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<ReportButton targetType="QUESTION" targetId={10} />);

    await userEvent.click(screen.getByRole("button", { name: "Report" }));

    expect(screen.getByRole("combobox")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("추가 설명 (선택)")).toBeInTheDocument();
  });

  it("submits the selected reason to reportApi.reportQuestion", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<ReportButton targetType="QUESTION" targetId={10} />);
    await userEvent.click(screen.getByRole("button", { name: "Report" }));

    await userEvent.selectOptions(screen.getByRole("combobox"), "DUPLICATE");
    await userEvent.click(screen.getByRole("button", { name: "신고" }));

    expect(await screen.findByText("Reported")).toBeInTheDocument();
    expect(reportApi.reportQuestion).toHaveBeenCalledWith(10, "DUPLICATE", undefined);
  });

  it("submits to reportApi.reportAnswer for an ANSWER target", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<ReportButton targetType="ANSWER" targetId={20} />);
    await userEvent.click(screen.getByRole("button", { name: "Report" }));

    await userEvent.click(screen.getByRole("button", { name: "신고" }));

    expect(await screen.findByText("Reported")).toBeInTheDocument();
    expect(reportApi.reportAnswer).toHaveBeenCalledWith(20, "SPAM", undefined);
  });

  it("closes the form without submitting when 취소 is clicked", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<ReportButton targetType="QUESTION" targetId={10} />);
    await userEvent.click(screen.getByRole("button", { name: "Report" }));

    await userEvent.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.getByRole("button", { name: "Report" })).toBeInTheDocument();
    expect(reportApi.reportQuestion).not.toHaveBeenCalled();
  });
});
