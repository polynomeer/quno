import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { VerificationPanel } from "./VerificationPanel";
import { verificationApi, type QuestionVerification } from "../api/verification.api";

vi.mock("../api/verification.api", async (importOriginal) => {
  const original = await importOriginal<typeof import("../api/verification.api")>();
  return { ...original, verificationApi: { get: vi.fn(), classify: vi.fn() } };
});

const BASE: QuestionVerification = {
  level: "L1",
  flags: [],
  classification: null,
  checks: {
    ERROR_SIGNATURE: true,
    ENVIRONMENT_SNAPSHOT: true,
    EXPECTED_ACTUAL: true,
    REPRO_STEPS: true,
    NO_SECRETS: true,
    REPRODUCTION_CAPSULE: true,
    TRUSTED_REPRODUCTION: false,
    SIGNATURE_MATCH: false,
  },
  lastReproducedAt: null,
  lastRunUrl: null,
};

function renderPanel(verification: QuestionVerification, isAuthor = true) {
  vi.mocked(verificationApi.get).mockResolvedValue(verification);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <VerificationPanel questionId={7} isAuthor={isAuthor} />
    </QueryClientProvider>,
  );
}

describe("VerificationPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("tells the author what the next level needs", async () => {
    renderPanel(BASE);
    expect(await screen.findByText("L1 구조 충족")).toBeInTheDocument();
    expect(screen.getByText("L2로 올리려면")).toBeInTheDocument();
    expect(screen.getByText(/Quno 워크플로로 GitHub 호스티드 러너/)).toBeInTheDocument();
    expect(screen.queryByText(/재현 캡슐 붙이기/)).not.toBeInTheDocument();
  });

  it("asks for structure first at L0, and shows readers only the level", async () => {
    renderPanel({ ...BASE, level: "L0", checks: { ...BASE.checks, ENVIRONMENT_SNAPSHOT: false } }, false);
    expect(await screen.findByText("L0 미검증")).toBeInTheDocument();
    expect(screen.queryByText("L1로 올리려면")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("원인을 알고 보니")).not.toBeInTheDocument();
  });

  it("says what L2 means and why a level should be read with care", async () => {
    renderPanel({ ...BASE, level: "L2", flags: ["DISPUTED"], lastReproducedAt: new Date(Date.now() - 3 * 3_600_000).toISOString(), lastRunUrl: "https://github.com/o/r/actions/runs/1" });
    expect(await screen.findByText(/답변이 맞다는 뜻은 아니에요/)).toBeInTheDocument();
    expect(screen.getByText(/이의가 제기돼/)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "실행 기록" })).toHaveAttribute("href", "https://github.com/o/r/actions/runs/1");
  });

  it("saves the author's classification", async () => {
    vi.mocked(verificationApi.classify).mockResolvedValue({ ...BASE, classification: "MISUSE" });
    renderPanel(BASE);
    await userEvent.selectOptions(await screen.findByLabelText("원인을 알고 보니"), "MISUSE");
    expect(verificationApi.classify).toHaveBeenCalledWith(7, "MISUSE");
    expect(await screen.findByText("작성자 판단: 오용")).toBeInTheDocument();
  });
});
