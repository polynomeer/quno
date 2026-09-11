import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { WatchButton } from "./WatchButton";
import { authApi } from "@/features/auth/api/auth.api";
import { watchApi } from "../api/watch.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { me: vi.fn() },
}));

vi.mock("../api/watch.api", () => ({
  watchApi: { myWatches: vi.fn(), watch: vi.fn(), unwatch: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("WatchButton", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockReset().mockResolvedValue(ME);
    vi.mocked(watchApi.myWatches).mockReset().mockResolvedValue([]);
    vi.mocked(watchApi.watch).mockReset().mockResolvedValue(undefined);
    vi.mocked(watchApi.unwatch).mockReset().mockResolvedValue(undefined);
    tokenStorage.clear();
  });

  it("renders nothing for an anonymous viewer", () => {
    const { container } = renderWithClient(<WatchButton questionId={7} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows 'Watch' when the viewer isn't watching this question yet", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<WatchButton questionId={7} />);

    expect(await screen.findByRole("button", { name: "Watch" })).toBeInTheDocument();
  });

  it("shows 'Watching' when the question is already in the viewer's watch list", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(watchApi.myWatches).mockResolvedValue([{ questionId: 7, title: "x", status: "OPEN" }]);
    renderWithClient(<WatchButton questionId={7} />);

    expect(await screen.findByRole("button", { name: "Watching" })).toBeInTheDocument();
  });

  it("calls watch() when clicked while not watching", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<WatchButton questionId={7} />);

    await userEvent.click(await screen.findByRole("button", { name: "Watch" }));

    await waitFor(() => expect(watchApi.watch).toHaveBeenCalledWith(7));
    expect(watchApi.unwatch).not.toHaveBeenCalled();
  });

  it("calls unwatch() when clicked while already watching", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(watchApi.myWatches).mockResolvedValue([{ questionId: 7, title: "x", status: "OPEN" }]);
    renderWithClient(<WatchButton questionId={7} />);

    await userEvent.click(await screen.findByRole("button", { name: "Watching" }));

    await waitFor(() => expect(watchApi.unwatch).toHaveBeenCalledWith(7));
    expect(watchApi.watch).not.toHaveBeenCalled();
  });
});
