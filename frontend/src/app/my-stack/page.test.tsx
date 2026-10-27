import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import MyStackPage from "./page";
import { useRequireAuth } from "@/features/auth/hooks/useRequireAuth";
import { stackApi } from "@/features/stack/api/stack.api";
import { watchApi } from "@/features/watch/api/watch.api";
import { LocaleProvider } from "@/shared/i18n/LocaleProvider";
import type { StackDependency, StackWardSuggestion } from "@/features/stack/api/stack.types";

vi.mock("@/features/auth/hooks/useRequireAuth", () => ({ useRequireAuth: vi.fn() }));
vi.mock("@/features/stack/api/stack.api", () => ({
  stackApi: { parse: vi.fn(), myStack: vi.fn(), replaceMyStack: vi.fn(), wardSuggestions: vi.fn() },
}));
vi.mock("@/features/watch/api/watch.api", () => ({ watchApi: { watch: vi.fn(), myWatches: vi.fn() } }));

const BOOT: StackDependency = { ecosystem: "GRADLE_PLUGIN", name: "org.springframework.boot", version: "4.0.8" };
const HIKARI: StackDependency = { ecosystem: "MAVEN", name: "com.zaxxer:HikariCP", version: "6.3.0" };
const SUGGESTIONS: StackWardSuggestion[] = [
  { questionId: 11, title: "Pool leak", status: "OPEN", createdAt: "2026-10-20T00:00:00Z", sharedDependencies: [BOOT, HIKARI] },
  { questionId: 12, title: "Actuator 404", status: "OPEN", createdAt: "2026-10-19T00:00:00Z", sharedDependencies: [BOOT] },
];

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <LocaleProvider>
        <MyStackPage />
      </LocaleProvider>
    </QueryClientProvider>,
  );
}

describe("MyStackPage", () => {
  beforeEach(() => {
    vi.mocked(useRequireAuth).mockReturnValue({ me: { id: 1 }, isLoading: false } as ReturnType<typeof useRequireAuth>);
    vi.mocked(stackApi.myStack).mockReset();
    vi.mocked(stackApi.replaceMyStack).mockReset();
    vi.mocked(stackApi.parse).mockReset();
    vi.mocked(stackApi.wardSuggestions).mockReset().mockResolvedValue(SUGGESTIONS);
    vi.mocked(watchApi.watch).mockReset().mockResolvedValue(undefined);
  });

  it("saves only the parsed entries left ticked", async () => {
    vi.mocked(stackApi.myStack).mockResolvedValue({ dependencies: [] });
    vi.mocked(stackApi.parse).mockResolvedValue({ format: "GRADLE", dependencies: [BOOT, HIKARI] });
    vi.mocked(stackApi.replaceMyStack).mockResolvedValue({ dependencies: [BOOT] });
    renderPage();

    expect(await screen.findByText(/아직 저장된 스택이 없어요/)).toBeInTheDocument();
    expect(stackApi.wardSuggestions).not.toHaveBeenCalled();
    await userEvent.type(screen.getByLabelText("의존성 파일 내용"), "build file");
    await userEvent.click(screen.getByRole("button", { name: "의존성 읽기" }));
    await userEvent.click(await screen.findByRole("checkbox", { name: /HikariCP/ }));
    await userEvent.click(screen.getByRole("button", { name: "선택한 1개로 저장" }));

    await waitFor(() => expect(stackApi.replaceMyStack).toHaveBeenCalledWith([BOOT]));
    expect(await screen.findByRole("heading", { name: "저장된 스택 1개" })).toBeInTheDocument();
  });

  it("suggests questions with nothing ticked and watches only the chosen ones", async () => {
    vi.mocked(stackApi.myStack).mockResolvedValue({ dependencies: [BOOT, HIKARI] });
    renderPage();

    const pick = await screen.findByRole("checkbox", { name: "Pool leak Watch 선택" });
    expect(screen.getByRole("checkbox", { name: "Actuator 404 Watch 선택" })).not.toBeChecked();
    expect(screen.getByRole("button", { name: "선택한 질문 Watch (0)" })).toBeDisabled();

    await userEvent.click(pick);
    await userEvent.click(screen.getByRole("button", { name: "선택한 질문 Watch (1)" }));

    await waitFor(() => expect(watchApi.watch).toHaveBeenCalledTimes(1));
    expect(watchApi.watch).toHaveBeenCalledWith(11);
  });

  it("clears the stack", async () => {
    vi.mocked(stackApi.myStack).mockResolvedValue({ dependencies: [BOOT] });
    vi.mocked(stackApi.replaceMyStack).mockResolvedValue({ dependencies: [] });
    renderPage();

    await userEvent.click(await screen.findByRole("button", { name: "모두 지우기" }));

    await waitFor(() => expect(stackApi.replaceMyStack).toHaveBeenCalledWith([]));
  });
});
