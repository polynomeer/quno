import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CreateOrganizationForm } from "./CreateOrganizationForm";
import { useSession } from "@/features/auth/hooks/useSession";
import { organizationApi } from "@/entities/organization/api/organization.api";
import { ApiError } from "@/shared/api/api-error";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { Organization } from "@/entities/organization/model/organization.types";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("@/entities/organization/api/organization.api", () => ({
  organizationApi: { create: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const ORG: Organization = {
  id: 7,
  name: "Quno",
  description: null,
  createdBy: ME.id,
  memberCount: 1,
  emailDomain: null,
  verified: false,
  createdAt: "",
};

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("CreateOrganizationForm", () => {
  beforeEach(() => {
    push.mockReset();
    vi.mocked(organizationApi.create).mockReset();
  });

  it("renders nothing for an anonymous viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    const { container } = renderWithClient(<CreateOrganizationForm />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows only the collapsed trigger for a logged-in viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<CreateOrganizationForm />);

    expect(screen.getByRole("button", { name: "새 조직 만들기" })).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("조직 이름")).not.toBeInTheDocument();
  });

  it("reveals the form when the trigger is clicked", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<CreateOrganizationForm />);

    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));

    expect(screen.getByPlaceholderText("조직 이름")).toBeInTheDocument();
  });

  it("keeps 만들기 disabled until a name is entered", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<CreateOrganizationForm />);
    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));

    expect(screen.getByRole("button", { name: "만들기" })).toBeDisabled();

    await userEvent.type(screen.getByPlaceholderText("조직 이름"), "Quno");

    expect(screen.getByRole("button", { name: "만들기" })).toBeEnabled();
  });

  it("creates the organization and navigates to its detail page", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(organizationApi.create).mockResolvedValue(ORG);
    renderWithClient(<CreateOrganizationForm />);
    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));
    await userEvent.type(screen.getByPlaceholderText("조직 이름"), "  Quno  ");
    await userEvent.type(screen.getByPlaceholderText("설명 (선택)"), "  개발자 Q&A  ");

    await userEvent.click(screen.getByRole("button", { name: "만들기" }));

    await waitFor(() => expect(organizationApi.create).toHaveBeenCalledWith({ name: "Quno", description: "개발자 Q&A" }));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/organizations/7"));
  });

  it("omits description when left blank", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(organizationApi.create).mockResolvedValue(ORG);
    renderWithClient(<CreateOrganizationForm />);
    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));
    await userEvent.type(screen.getByPlaceholderText("조직 이름"), "Quno");

    await userEvent.click(screen.getByRole("button", { name: "만들기" }));

    await waitFor(() => expect(organizationApi.create).toHaveBeenCalledWith({ name: "Quno", description: undefined }));
  });

  it("collapses back to the trigger when 취소 is clicked", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<CreateOrganizationForm />);
    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));

    await userEvent.click(screen.getByRole("button", { name: "취소" }));

    expect(screen.getByRole("button", { name: "새 조직 만들기" })).toBeInTheDocument();
    expect(organizationApi.create).not.toHaveBeenCalled();
  });

  it("shows the backend's error message when creation fails, without navigating", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(organizationApi.create).mockRejectedValue(new ApiError(409, "DUPLICATE_NAME", "이미 사용 중인 이름입니다."));
    renderWithClient(<CreateOrganizationForm />);
    await userEvent.click(screen.getByRole("button", { name: "새 조직 만들기" }));
    await userEvent.type(screen.getByPlaceholderText("조직 이름"), "Quno");

    await userEvent.click(screen.getByRole("button", { name: "만들기" }));

    expect(await screen.findByText("이미 사용 중인 이름입니다.")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
