import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AppHeader } from "./AppHeader";
import { useSession } from "@/features/auth/hooks/useSession";
import { useLogout } from "@/features/auth/hooks/useLogin";
import { useNotifications } from "@/features/notification/hooks/useNotifications";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";
import type { MyProfile } from "@/features/auth/api/auth.types";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("@/features/auth/hooks/useSession", () => ({
  useSession: vi.fn(),
}));

vi.mock("@/features/auth/hooks/useLogin", () => ({
  useLogout: vi.fn(),
}));

vi.mock("@/features/notification/hooks/useNotifications", () => ({
  useNotifications: vi.fn(),
}));

const ME: MyProfile = { id: 1, email: "me@example.com", nickname: "me-nick", acceptsDirectAsk: false, createdAt: "" };

function renderHeader() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <LocaleProvider>
        <AppHeader />
      </LocaleProvider>
    </QueryClientProvider>,
  );
}

function mockLoggedOut() {
  vi.mocked(useSession).mockReturnValue({ data: undefined, isLoading: false } as ReturnType<typeof useSession>);
  vi.mocked(useNotifications).mockReturnValue({ data: undefined } as ReturnType<typeof useNotifications>);
}

function mockLoggedIn(unreadCount = 0) {
  vi.mocked(useSession).mockReturnValue({ data: ME, isLoading: false } as ReturnType<typeof useSession>);
  vi.mocked(useNotifications).mockReturnValue({
    data: Array.from({ length: unreadCount }, (_, i) => ({ id: i, isRead: false })),
  } as ReturnType<typeof useNotifications>);
}

describe("AppHeader", () => {
  beforeEach(() => {
    vi.mocked(useLogout).mockReturnValue(vi.fn());
    mockLoggedOut();
  });

  it("shows Sign up/Log in and hides member-only links for an anonymous visitor", () => {
    renderHeader();

    expect(screen.getAllByRole("link", { name: "Sign up" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Log in" }).length).toBeGreaterThan(0);
    expect(screen.queryByRole("link", { name: "Watching" })).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Direct Asks" })).not.toBeInTheDocument();
  });

  it("shows the nickname, Log out, and member-only links for a logged-in user", () => {
    mockLoggedIn();
    renderHeader();

    expect(screen.getAllByRole("link", { name: "me-nick" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("button", { name: "Log out" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Watching" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("link", { name: "Direct Asks" }).length).toBeGreaterThan(0);
  });

  it("shows the unread notification count as a badge", () => {
    mockLoggedIn(3);
    renderHeader();

    const notificationLinks = screen.getAllByRole("link", { name: /Notifications/ });
    expect(notificationLinks[0]).toHaveTextContent("3");
  });

  it("calls logout() when Log out is clicked", async () => {
    const logout = vi.fn();
    vi.mocked(useLogout).mockReturnValue(logout);
    mockLoggedIn();
    renderHeader();

    await userEvent.click(screen.getAllByRole("button", { name: "Log out" })[0]);

    expect(logout).toHaveBeenCalledTimes(1);
  });

  it("opens and closes the mobile menu via the hamburger button", async () => {
    renderHeader();

    const toggle = screen.getByRole("button", { name: "Open menu" });
    expect(toggle).toHaveAttribute("aria-expanded", "false");

    await userEvent.click(toggle);

    expect(screen.getByRole("button", { name: "Close menu" })).toHaveAttribute("aria-expanded", "true");
  });

  it("closes the mobile menu after clicking a nav link inside it", async () => {
    renderHeader();

    await userEvent.click(screen.getByRole("button", { name: "Open menu" }));
    const mobileTagsLinks = screen.getAllByRole("link", { name: "Tags" });
    await userEvent.click(mobileTagsLinks[mobileTagsLinks.length - 1]);

    expect(screen.getByRole("button", { name: "Open menu" })).toHaveAttribute("aria-expanded", "false");
  });

  it("includes a language switcher", () => {
    renderHeader();

    expect(screen.getAllByRole("button", { name: "한국어" }).length).toBeGreaterThan(0);
    expect(screen.getAllByRole("button", { name: "English" }).length).toBeGreaterThan(0);
  });
});
