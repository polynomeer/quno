import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { FollowUserButton } from "./FollowUserButton";
import { authApi } from "@/features/auth/api/auth.api";
import { followApi } from "../api/follow.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { me: vi.fn() },
}));

vi.mock("../api/follow.api", () => ({
  followApi: { myFollowing: vi.fn(), follow: vi.fn(), unfollow: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };
const OTHER_USER_ID = 7;

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("FollowUserButton", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockReset().mockResolvedValue(ME);
    vi.mocked(followApi.myFollowing).mockReset().mockResolvedValue([]);
    vi.mocked(followApi.follow).mockReset().mockResolvedValue(undefined);
    vi.mocked(followApi.unfollow).mockReset().mockResolvedValue(undefined);
    tokenStorage.clear();
  });

  it("renders nothing for an anonymous viewer", () => {
    const { container } = renderWithClient(<FollowUserButton userId={OTHER_USER_ID} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("renders nothing on the viewer's own profile", async () => {
    tokenStorage.setTokens("access", "refresh");
    const { container } = renderWithClient(<FollowUserButton userId={ME.id} />);

    await waitFor(() => expect(authApi.me).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
  });

  it("shows 'Follow' when not already following this user", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<FollowUserButton userId={OTHER_USER_ID} />);

    expect(await screen.findByRole("button", { name: "Follow" })).toBeInTheDocument();
  });

  it("shows 'Following' when this user is already followed", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(followApi.myFollowing).mockResolvedValue([{ userId: OTHER_USER_ID, nickname: "other" }]);
    renderWithClient(<FollowUserButton userId={OTHER_USER_ID} />);

    expect(await screen.findByRole("button", { name: "Following" })).toBeInTheDocument();
  });

  it("calls follow() when clicked while not following", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<FollowUserButton userId={OTHER_USER_ID} />);

    await userEvent.click(await screen.findByRole("button", { name: "Follow" }));

    await waitFor(() => expect(followApi.follow).toHaveBeenCalledWith(OTHER_USER_ID));
    expect(followApi.unfollow).not.toHaveBeenCalled();
  });

  it("calls unfollow() when clicked while already following", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(followApi.myFollowing).mockResolvedValue([{ userId: OTHER_USER_ID, nickname: "other" }]);
    renderWithClient(<FollowUserButton userId={OTHER_USER_ID} />);

    await userEvent.click(await screen.findByRole("button", { name: "Following" }));

    await waitFor(() => expect(followApi.unfollow).toHaveBeenCalledWith(OTHER_USER_ID));
    expect(followApi.follow).not.toHaveBeenCalled();
  });
});
