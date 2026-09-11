import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FollowTagButton } from "./FollowTagButton";
import { authApi } from "@/features/auth/api/auth.api";
import { userApi } from "@/entities/user/api/user.api";
import { tagApi } from "@/entities/tag/api/tag.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "@/features/auth/api/auth.types";
import type { UserProfile } from "@/entities/user/model/user-profile.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { me: vi.fn() },
}));

vi.mock("@/entities/user/api/user.api", () => ({
  userApi: { getProfile: vi.fn(), getReputation: vi.fn() },
}));

vi.mock("@/entities/tag/api/tag.api", () => ({
  tagApi: { follow: vi.fn(), unfollow: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function profileWithTags(tags: UserProfile["followedTags"]): UserProfile {
  return { userId: ME.id, nickname: ME.nickname, questions: [], answers: [], followedTags: tags, organizations: [] };
}

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("FollowTagButton", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockReset().mockResolvedValue(ME);
    vi.mocked(userApi.getProfile).mockReset().mockResolvedValue(profileWithTags([]));
    vi.mocked(tagApi.follow).mockReset().mockResolvedValue(undefined);
    vi.mocked(tagApi.unfollow).mockReset().mockResolvedValue(undefined);
    tokenStorage.clear();
  });

  it("renders nothing for an anonymous viewer", () => {
    const { container } = renderWithClient(<FollowTagButton tagId={5} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows 'Follow' when the tag isn't followed yet", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<FollowTagButton tagId={5} />);

    expect(await screen.findByRole("button", { name: "Follow" })).toBeInTheDocument();
  });

  it("shows 'Following' when the tag is already in the viewer's followed tags", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithTags([{ id: 5, name: "kotlin", slug: "kotlin", description: null, docsUrl: null }]),
    );
    renderWithClient(<FollowTagButton tagId={5} />);

    expect(await screen.findByRole("button", { name: "Following" })).toBeInTheDocument();
  });

  it("calls tagApi.follow() when clicked while not following", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<FollowTagButton tagId={5} />);

    await userEvent.click(await screen.findByRole("button", { name: "Follow" }));

    await waitFor(() => expect(tagApi.follow).toHaveBeenCalledWith(5));
    expect(tagApi.unfollow).not.toHaveBeenCalled();
  });

  it("calls tagApi.unfollow() when clicked while already following", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(userApi.getProfile).mockResolvedValue(
      profileWithTags([{ id: 5, name: "kotlin", slug: "kotlin", description: null, docsUrl: null }]),
    );
    renderWithClient(<FollowTagButton tagId={5} />);

    await userEvent.click(await screen.findByRole("button", { name: "Following" }));

    await waitFor(() => expect(tagApi.unfollow).toHaveBeenCalledWith(5));
    expect(tagApi.follow).not.toHaveBeenCalled();
  });
});
