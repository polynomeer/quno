import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DirectAskRequestList } from "./DirectAskRequestList";
import { directAskApi } from "../api/direct-ask.api";
import { ApiError } from "@/shared/api/api-error";
import type { DirectAskRequestListItem } from "../api/direct-ask.types";

vi.mock("../api/direct-ask.api", () => ({
  directAskApi: { accept: vi.fn(), decline: vi.fn() },
}));

function item(overrides: Partial<DirectAskRequestListItem> = {}): DirectAskRequestListItem {
  return {
    id: 1,
    questionId: 10,
    questionTitle: "Spring Boot 4에서 빈 등록 문제",
    requesterId: 2,
    requesterNickname: "requester",
    targetUserId: 1,
    targetUserNickname: "target",
    message: "확인 부탁드립니다",
    status: "PENDING",
    createdAt: "2026-01-01T00:00:00.000Z",
    respondedAt: null,
    ...overrides,
  };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("DirectAskRequestList", () => {
  beforeEach(() => {
    vi.mocked(directAskApi.accept).mockReset();
    vi.mocked(directAskApi.decline).mockReset();
  });

  it("shows the empty message when there are no items", () => {
    renderWithClient(<DirectAskRequestList items={[]} role="received" emptyMessage="받은 요청이 없습니다." />);
    expect(screen.getByText("받은 요청이 없습니다.")).toBeInTheDocument();
  });

  it("shows the requester for a received request", () => {
    renderWithClient(<DirectAskRequestList items={[item()]} role="received" emptyMessage="" />);

    expect(screen.getByText("요청자")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "requester" })).toHaveAttribute("href", "/users/2");
  });

  it("shows the target user for a sent request", () => {
    renderWithClient(<DirectAskRequestList items={[item()]} role="sent" emptyMessage="" />);

    expect(screen.getByText("대상")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "target" })).toHaveAttribute("href", "/users/1");
  });

  it("links the question title to its detail page", () => {
    renderWithClient(<DirectAskRequestList items={[item()]} role="received" emptyMessage="" />);
    expect(screen.getByRole("link", { name: item().questionTitle })).toHaveAttribute("href", "/questions/10");
  });

  it("shows accept/decline only for a PENDING request the viewer received", () => {
    renderWithClient(<DirectAskRequestList items={[item({ status: "PENDING" })]} role="received" emptyMessage="" />);
    expect(screen.getByRole("button", { name: "수락" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "거절 (자동 환불)" })).toBeInTheDocument();
  });

  it("hides accept/decline for a sent request even if PENDING", () => {
    renderWithClient(<DirectAskRequestList items={[item({ status: "PENDING" })]} role="sent" emptyMessage="" />);
    expect(screen.queryByRole("button", { name: "수락" })).not.toBeInTheDocument();
  });

  it("hides accept/decline once the request is no longer PENDING", () => {
    renderWithClient(<DirectAskRequestList items={[item({ status: "ACCEPTED" })]} role="received" emptyMessage="" />);
    expect(screen.queryByRole("button", { name: "수락" })).not.toBeInTheDocument();
    expect(screen.getByText("수락됨")).toBeInTheDocument();
  });

  it("accepts a request via directAskApi.accept", async () => {
    vi.mocked(directAskApi.accept).mockResolvedValue(undefined);
    renderWithClient(<DirectAskRequestList items={[item()]} role="received" emptyMessage="" />);

    await userEvent.click(screen.getByRole("button", { name: "수락" }));

    await waitFor(() => expect(directAskApi.accept).toHaveBeenCalledWith(1));
    expect(directAskApi.decline).not.toHaveBeenCalled();
  });

  it("declines a request via directAskApi.decline", async () => {
    vi.mocked(directAskApi.decline).mockResolvedValue(undefined);
    renderWithClient(<DirectAskRequestList items={[item()]} role="received" emptyMessage="" />);

    await userEvent.click(screen.getByRole("button", { name: "거절 (자동 환불)" }));

    await waitFor(() => expect(directAskApi.decline).toHaveBeenCalledWith(1));
    expect(directAskApi.accept).not.toHaveBeenCalled();
  });

  it("shows the backend's error message when responding fails", async () => {
    vi.mocked(directAskApi.accept).mockRejectedValue(new ApiError(409, "ALREADY_RESPONDED", "이미 응답한 요청입니다."));
    renderWithClient(<DirectAskRequestList items={[item()]} role="received" emptyMessage="" />);

    await userEvent.click(screen.getByRole("button", { name: "수락" }));

    expect(await screen.findByText("이미 응답한 요청입니다.")).toBeInTheDocument();
  });
});
