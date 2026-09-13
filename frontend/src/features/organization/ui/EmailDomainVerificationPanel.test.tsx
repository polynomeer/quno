import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { EmailDomainVerificationPanel } from "./EmailDomainVerificationPanel";
import { organizationApi } from "@/entities/organization/api/organization.api";
import { ApiError } from "@/shared/api/api-error";
import type { Organization } from "@/entities/organization/model/organization.types";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("@/entities/organization/api/organization.api", () => ({
  organizationApi: { requestEmailVerification: vi.fn(), confirmEmailVerification: vi.fn() },
}));

const VERIFIED_ORG: Organization = {
  id: 7,
  name: "Acme Corp",
  description: null,
  createdBy: 1,
  memberCount: 1,
  emailDomain: "acme.com",
  verified: true,
  createdAt: "",
};

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("EmailDomainVerificationPanel", () => {
  beforeEach(() => {
    push.mockReset();
    vi.mocked(organizationApi.requestEmailVerification).mockReset();
    vi.mocked(organizationApi.confirmEmailVerification).mockReset();
  });

  it("renders nothing for an anonymous viewer", () => {
    const { container } = renderWithClient(<EmailDomainVerificationPanel viewerId={undefined} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("keeps 인증 코드 보내기 disabled until an email is entered", () => {
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);

    expect(screen.getByRole("button", { name: "인증 코드 보내기" })).toBeDisabled();
  });

  it("does not show the code input before a code has been sent", () => {
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);
    expect(screen.queryByPlaceholderText("6자리 코드")).not.toBeInTheDocument();
  });

  it("sends the verification email and reveals the code input", async () => {
    vi.mocked(organizationApi.requestEmailVerification).mockResolvedValue({ email: "me@acme.com", expiresAt: "" });
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);
    await userEvent.type(screen.getByPlaceholderText("you@company.com"), "me@acme.com");

    await userEvent.click(screen.getByRole("button", { name: "인증 코드 보내기" }));

    await waitFor(() => expect(organizationApi.requestEmailVerification).toHaveBeenCalledWith("me@acme.com"));
    expect(await screen.findByText("인증 코드를 보냈습니다. 메일함을 확인하세요.")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("6자리 코드")).toBeInTheDocument();
  });

  it("shows the backend's error message when the request fails, without revealing the code input", async () => {
    vi.mocked(organizationApi.requestEmailVerification).mockRejectedValue(
      new ApiError(400, "PUBLIC_DOMAIN", "공개 웹메일 도메인은 인증할 수 없습니다."),
    );
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);
    await userEvent.type(screen.getByPlaceholderText("you@company.com"), "me@gmail.com");

    await userEvent.click(screen.getByRole("button", { name: "인증 코드 보내기" }));

    expect(await screen.findByText("공개 웹메일 도메인은 인증할 수 없습니다.")).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("6자리 코드")).not.toBeInTheDocument();
  });

  it("confirms the code and navigates to the resulting organization", async () => {
    vi.mocked(organizationApi.requestEmailVerification).mockResolvedValue({ email: "me@acme.com", expiresAt: "" });
    vi.mocked(organizationApi.confirmEmailVerification).mockResolvedValue(VERIFIED_ORG);
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);
    await userEvent.type(screen.getByPlaceholderText("you@company.com"), "me@acme.com");
    await userEvent.click(screen.getByRole("button", { name: "인증 코드 보내기" }));
    await userEvent.type(await screen.findByPlaceholderText("6자리 코드"), "123456");

    await userEvent.click(screen.getByRole("button", { name: "확인" }));

    await waitFor(() => expect(organizationApi.confirmEmailVerification).toHaveBeenCalledWith("123456"));
    await waitFor(() => expect(push).toHaveBeenCalledWith("/organizations/7"));
  });

  it("shows the backend's error message when confirmation fails, without navigating", async () => {
    vi.mocked(organizationApi.requestEmailVerification).mockResolvedValue({ email: "me@acme.com", expiresAt: "" });
    vi.mocked(organizationApi.confirmEmailVerification).mockRejectedValue(new ApiError(400, "INVALID_CODE", "코드가 올바르지 않습니다."));
    renderWithClient(<EmailDomainVerificationPanel viewerId={1} />);
    await userEvent.type(screen.getByPlaceholderText("you@company.com"), "me@acme.com");
    await userEvent.click(screen.getByRole("button", { name: "인증 코드 보내기" }));
    await userEvent.type(await screen.findByPlaceholderText("6자리 코드"), "000000");

    await userEvent.click(screen.getByRole("button", { name: "확인" }));

    expect(await screen.findByText("코드가 올바르지 않습니다.")).toBeInTheDocument();
    expect(push).not.toHaveBeenCalled();
  });
});
