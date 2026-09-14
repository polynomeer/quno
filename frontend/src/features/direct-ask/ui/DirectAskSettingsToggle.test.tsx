import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DirectAskSettingsToggle } from "./DirectAskSettingsToggle";
import { authApi } from "@/features/auth/api/auth.api";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: { updateDirectAskSettings: vi.fn() },
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me", acceptsDirectAsk: true, createdAt: "" };

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("DirectAskSettingsToggle", () => {
  beforeEach(() => {
    vi.mocked(authApi.updateDirectAskSettings).mockReset();
  });

  it("shows 받는 중 — 끄기 when currently accepting Direct Asks", () => {
    renderWithClient(<DirectAskSettingsToggle accepts />);
    expect(screen.getByRole("button", { name: "받는 중 — 끄기" })).toBeInTheDocument();
  });

  it("shows 꺼짐 — 켜기 when currently not accepting Direct Asks", () => {
    renderWithClient(<DirectAskSettingsToggle accepts={false} />);
    expect(screen.getByRole("button", { name: "꺼짐 — 켜기" })).toBeInTheDocument();
  });

  it("turns settings off when clicked while currently on", async () => {
    vi.mocked(authApi.updateDirectAskSettings).mockResolvedValue({ ...ME, acceptsDirectAsk: false });
    renderWithClient(<DirectAskSettingsToggle accepts />);

    await userEvent.click(screen.getByRole("button", { name: "받는 중 — 끄기" }));

    await waitFor(() => expect(authApi.updateDirectAskSettings).toHaveBeenCalledWith(false));
  });

  it("turns settings on when clicked while currently off", async () => {
    vi.mocked(authApi.updateDirectAskSettings).mockResolvedValue({ ...ME, acceptsDirectAsk: true });
    renderWithClient(<DirectAskSettingsToggle accepts={false} />);

    await userEvent.click(screen.getByRole("button", { name: "꺼짐 — 켜기" }));

    await waitFor(() => expect(authApi.updateDirectAskSettings).toHaveBeenCalledWith(true));
  });
});
