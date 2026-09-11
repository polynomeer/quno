import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SaveButton } from "./SaveButton";
import { authApi } from "@/features/auth/api/auth.api";
import { saveApi } from "../api/save.api";
import { tokenStorage } from "@/shared/lib/token-storage";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { me: vi.fn() },
}));

vi.mock("../api/save.api", () => ({
  saveApi: { mySaves: vi.fn(), save: vi.fn(), unsave: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: false, createdAt: "" };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("SaveButton", () => {
  beforeEach(() => {
    vi.mocked(authApi.me).mockReset().mockResolvedValue(ME);
    vi.mocked(saveApi.mySaves).mockReset().mockResolvedValue([]);
    vi.mocked(saveApi.save).mockReset().mockResolvedValue(undefined);
    vi.mocked(saveApi.unsave).mockReset().mockResolvedValue(undefined);
    tokenStorage.clear();
  });

  it("renders nothing for an anonymous viewer", () => {
    const { container } = renderWithClient(<SaveButton questionId={7} />);
    expect(container).toBeEmptyDOMElement();
  });

  it("shows 'Save' when the question isn't saved yet", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<SaveButton questionId={7} />);

    expect(await screen.findByRole("button", { name: "Save" })).toBeInTheDocument();
  });

  it("shows 'Saved' when the question is already in the viewer's saves", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(saveApi.mySaves).mockResolvedValue([{ questionId: 7, title: "x", status: "OPEN" }]);
    renderWithClient(<SaveButton questionId={7} />);

    expect(await screen.findByRole("button", { name: "Saved" })).toBeInTheDocument();
  });

  it("calls save() when clicked while not saved", async () => {
    tokenStorage.setTokens("access", "refresh");
    renderWithClient(<SaveButton questionId={7} />);

    await userEvent.click(await screen.findByRole("button", { name: "Save" }));

    await waitFor(() => expect(saveApi.save).toHaveBeenCalledWith(7));
    expect(saveApi.unsave).not.toHaveBeenCalled();
  });

  it("calls unsave() when clicked while already saved", async () => {
    tokenStorage.setTokens("access", "refresh");
    vi.mocked(saveApi.mySaves).mockResolvedValue([{ questionId: 7, title: "x", status: "OPEN" }]);
    renderWithClient(<SaveButton questionId={7} />);

    await userEvent.click(await screen.findByRole("button", { name: "Saved" }));

    await waitFor(() => expect(saveApi.unsave).toHaveBeenCalledWith(7));
    expect(saveApi.save).not.toHaveBeenCalled();
  });
});
