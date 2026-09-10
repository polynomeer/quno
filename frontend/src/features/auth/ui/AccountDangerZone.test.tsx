import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AccountDangerZone } from "./AccountDangerZone";
import { authApi } from "@/features/auth/api/auth.api";
import { ApiError } from "@/shared/api/api-error";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

vi.mock("@/features/auth/api/auth.api", () => ({
  authApi: {
    withdraw: vi.fn(),
    exportMyData: vi.fn(),
  },
}));

function renderWithClient(ui: ReactElement) {
  const queryClient = new QueryClient();
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("AccountDangerZone", () => {
  beforeEach(() => {
    vi.mocked(authApi.withdraw).mockReset();
    vi.mocked(authApi.exportMyData).mockReset();
  });

  it("keeps the password form collapsed until 회원 탈퇴 is clicked", () => {
    renderWithClient(<AccountDangerZone />);

    expect(screen.getByRole("button", { name: "회원 탈퇴" })).toBeInTheDocument();
    expect(screen.queryByPlaceholderText("현재 비밀번호")).not.toBeInTheDocument();
  });

  it("reveals the password form when 회원 탈퇴 is clicked", async () => {
    renderWithClient(<AccountDangerZone />);

    await userEvent.click(screen.getByRole("button", { name: "회원 탈퇴" }));

    expect(screen.getByPlaceholderText("현재 비밀번호")).toBeInTheDocument();
  });

  it("shows the backend's error message when withdrawal fails, without throwing", async () => {
    vi.mocked(authApi.withdraw).mockRejectedValueOnce(new ApiError(401, "UNAUTHORIZED", "Invalid email or password"));
    renderWithClient(<AccountDangerZone />);
    await userEvent.click(screen.getByRole("button", { name: "회원 탈퇴" }));
    await userEvent.type(screen.getByPlaceholderText("현재 비밀번호"), "wrong-password");

    await userEvent.click(screen.getByRole("button", { name: "탈퇴 확정" }));

    expect(await screen.findByText("Invalid email or password")).toBeInTheDocument();
  });

  it("submits the entered password to authApi.withdraw", async () => {
    vi.mocked(authApi.withdraw).mockResolvedValueOnce(undefined);
    renderWithClient(<AccountDangerZone />);
    await userEvent.click(screen.getByRole("button", { name: "회원 탈퇴" }));
    await userEvent.type(screen.getByPlaceholderText("현재 비밀번호"), "correct-password");

    await userEvent.click(screen.getByRole("button", { name: "탈퇴 확정" }));

    await waitFor(() => expect(authApi.withdraw).toHaveBeenCalledWith("correct-password"));
  });
});
