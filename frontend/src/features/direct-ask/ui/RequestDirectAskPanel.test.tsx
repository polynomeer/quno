import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { RequestDirectAskPanel } from "./RequestDirectAskPanel";
import { useSession } from "@/features/auth/hooks/useSession";
import { userApi } from "@/entities/user/api/user.api";
import { directAskApi } from "../api/direct-ask.api";
import { ApiError } from "@/shared/api/api-error";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { UserProfile } from "@/entities/user/model/user-profile.types";
import type { CreateDirectAskRequestResult } from "../api/direct-ask.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("@/entities/user/api/user.api", () => ({
  userApi: { getProfile: vi.fn(), getReputation: vi.fn() },
}));

vi.mock("../api/direct-ask.api", () => ({
  directAskApi: { create: vi.fn(), accept: vi.fn(), decline: vi.fn(), confirmPayment: vi.fn() },
}));

// The hosted Toss checkout redirect itself is out of scope (same call as the E2E golden path,
// ADR-0047) — the SDK never "loads" here, so the payment-launch effect simply never fires.
vi.mock("../lib/toss", () => ({ useTossPayments: () => null }));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };
const TARGET_ID = 2;

function profileWithQuestions(questions: UserProfile["questions"]): UserProfile {
  return { userId: ME.id, nickname: ME.nickname, questions, answers: [], followedTags: [], organizations: [] };
}

const RESULT: CreateDirectAskRequestResult = {
  request: {
    id: 1,
    questionId: 10,
    requesterId: ME.id,
    targetUserId: TARGET_ID,
    message: null,
    status: "AWAITING_PAYMENT",
    createdAt: "",
    respondedAt: null,
  },
  payment: { orderId: "order-1", amount: 5000, status: "PENDING", clientKey: "test-client-key" },
};

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("RequestDirectAskPanel", () => {
  beforeEach(() => {
    vi.mocked(userApi.getProfile).mockReset().mockResolvedValue(profileWithQuestions([]));
    vi.mocked(directAskApi.create).mockReset();
  });

  it("renders nothing for an anonymous viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    const { container } = renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing on the viewer's own profile", () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    const { container } = renderWithClient(<RequestDirectAskPanel targetUserId={ME.id} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows the collapsed trigger for a logged-in viewer on someone else's profile", () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    expect(screen.getByRole("button", { name: "Direct Ask 요청" })).toBeInTheDocument();
  });

  it("tells the viewer to write a question first when they have none", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    await userEvent.click(screen.getByRole("button", { name: "Direct Ask 요청" }));

    expect(await screen.findByText("먼저 질문을 작성해야 Direct Ask를 요청할 수 있습니다.")).toBeInTheDocument();
  });

  it("lists the viewer's own questions to choose from", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithQuestions([{ id: 10, title: "내 질문", status: "OPEN", tags: [], score: 0 }]),
    );
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    await userEvent.click(screen.getByRole("button", { name: "Direct Ask 요청" }));

    expect(await screen.findByRole("option", { name: "내 질문" })).toBeInTheDocument();
  });

  it("keeps 요청하고 결제하기 disabled until a question is selected", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithQuestions([{ id: 10, title: "내 질문", status: "OPEN", tags: [], score: 0 }]),
    );
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    await userEvent.click(screen.getByRole("button", { name: "Direct Ask 요청" }));
    await screen.findByRole("option", { name: "내 질문" });

    expect(screen.getByRole("button", { name: "요청하고 결제하기" })).toBeDisabled();

    await userEvent.selectOptions(screen.getByRole("combobox"), "10");

    expect(screen.getByRole("button", { name: "요청하고 결제하기" })).toBeEnabled();
  });

  it("submits the selected question and message to directAskApi.create", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithQuestions([{ id: 10, title: "내 질문", status: "OPEN", tags: [], score: 0 }]),
    );
    vi.mocked(directAskApi.create).mockResolvedValue(RESULT);
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    await userEvent.click(screen.getByRole("button", { name: "Direct Ask 요청" }));
    await screen.findByRole("option", { name: "내 질문" });
    await userEvent.selectOptions(screen.getByRole("combobox"), "10");
    await userEvent.type(screen.getByPlaceholderText("전달할 메시지 (선택)"), "확인 부탁드립니다");

    await userEvent.click(screen.getByRole("button", { name: "요청하고 결제하기" }));

    await waitFor(() =>
      expect(directAskApi.create).toHaveBeenCalledWith(10, TARGET_ID, "확인 부탁드립니다"),
    );
  });

  it("shows the backend's error message when the request fails", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithQuestions([{ id: 10, title: "내 질문", status: "OPEN", tags: [], score: 0 }]),
    );
    vi.mocked(directAskApi.create).mockRejectedValue(new ApiError(403, "DIRECT_ASK_DISABLED", "이 사용자는 Direct Ask를 받지 않습니다."));
    renderWithClient(<RequestDirectAskPanel targetUserId={TARGET_ID} />);
    await userEvent.click(screen.getByRole("button", { name: "Direct Ask 요청" }));
    await screen.findByRole("option", { name: "내 질문" });
    await userEvent.selectOptions(screen.getByRole("combobox"), "10");

    await userEvent.click(screen.getByRole("button", { name: "요청하고 결제하기" }));

    expect(await screen.findByText("이 사용자는 Direct Ask를 받지 않습니다.")).toBeInTheDocument();
  });
});
