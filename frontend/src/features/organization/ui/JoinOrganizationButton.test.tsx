import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { JoinOrganizationButton } from "./JoinOrganizationButton";
import { useSession } from "@/features/auth/hooks/useSession";
import { userApi } from "@/entities/user/api/user.api";
import { organizationApi } from "@/entities/organization/api/organization.api";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { UserProfile } from "@/entities/user/model/user-profile.types";
import type { Organization } from "@/entities/organization/model/organization.types";

vi.mock("@/features/auth/hooks/useSession", () => ({ useSession: vi.fn() }));

vi.mock("@/entities/user/api/user.api", () => ({
  userApi: { getProfile: vi.fn(), getReputation: vi.fn() },
}));

vi.mock("@/entities/organization/api/organization.api", () => ({
  organizationApi: { join: vi.fn(), leave: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

const ORG: Organization = {
  id: 7,
  name: "Quno",
  description: null,
  createdBy: 1,
  memberCount: 1,
  emailDomain: null,
  verified: false,
  createdAt: "",
};

function profileWithOrgs(organizations: UserProfile["organizations"]): UserProfile {
  return { userId: ME.id, nickname: ME.nickname, questions: [], answers: [], followedTags: [], organizations };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("JoinOrganizationButton", () => {
  beforeEach(() => {
    vi.mocked(userApi.getProfile).mockReset().mockResolvedValue(profileWithOrgs([]));
    vi.mocked(organizationApi.join).mockReset().mockResolvedValue(undefined);
    vi.mocked(organizationApi.leave).mockReset().mockResolvedValue(undefined);
  });

  it("renders nothing for an anonymous viewer", () => {
    vi.mocked(useSession).mockReturnValue({ data: undefined } as ReturnType<typeof useSession>);
    const { container } = renderWithClient(<JoinOrganizationButton organization={ORG} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows '가입하기' when not a member yet", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<JoinOrganizationButton organization={ORG} />);

    expect(await screen.findByRole("button", { name: "가입하기" })).toBeInTheDocument();
  });

  it("shows '가입됨 — 탈퇴' when already a member", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(profileWithOrgs([ORG]));
    renderWithClient(<JoinOrganizationButton organization={ORG} />);

    expect(await screen.findByRole("button", { name: "가입됨 — 탈퇴" })).toBeInTheDocument();
  });

  it("calls organizationApi.join() when clicked while not a member", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    renderWithClient(<JoinOrganizationButton organization={ORG} />);

    await userEvent.click(await screen.findByRole("button", { name: "가입하기" }));

    await waitFor(() => expect(organizationApi.join).toHaveBeenCalledWith(ORG.id));
    expect(organizationApi.leave).not.toHaveBeenCalled();
  });

  it("calls organizationApi.leave() when clicked while already a member", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    vi.mocked(userApi.getProfile).mockResolvedValue(profileWithOrgs([ORG]));
    renderWithClient(<JoinOrganizationButton organization={ORG} />);

    await userEvent.click(await screen.findByRole("button", { name: "가입됨 — 탈퇴" }));

    await waitFor(() => expect(organizationApi.leave).toHaveBeenCalledWith(ORG.id));
    expect(organizationApi.join).not.toHaveBeenCalled();
  });

  it("shows a notice instead of a join button for a Verified organization the viewer hasn't joined", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    const verifiedOrg = { ...ORG, verified: true, emailDomain: "acme.com" };
    renderWithClient(<JoinOrganizationButton organization={verifiedOrg} />);

    expect(await screen.findByText("업무/학교 이메일 인증으로만 가입할 수 있습니다.")).toBeInTheDocument();
    expect(screen.queryByRole("button")).not.toBeInTheDocument();
  });

  it("still shows the leave button for a Verified organization the viewer already joined", async () => {
    vi.mocked(useSession).mockReturnValue({ data: ME } as ReturnType<typeof useSession>);
    const verifiedOrg = { ...ORG, verified: true, emailDomain: "acme.com" };
    vi.mocked(userApi.getProfile).mockResolvedValue(profileWithOrgs([verifiedOrg]));
    renderWithClient(<JoinOrganizationButton organization={verifiedOrg} />);

    expect(await screen.findByRole("button", { name: "가입됨 — 탈퇴" })).toBeInTheDocument();
  });
});
