import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReproductionPanel, workflowSnippet } from "./ReproductionPanel";
import { reproductionApi } from "../api/reproduction.api";
import type { ReproductionCapsule, ReproductionPanel as Panel, ReproductionRun } from "../api/reproduction.types";

vi.mock("../api/reproduction.api", async (importOriginal) => {
  const original = await importOriginal<typeof import("../api/reproduction.api")>();
  return {
    ...original,
    reproductionApi: { panel: vi.fn(), attach: vi.fn(), requestRerun: vi.fn(), log: vi.fn() },
  };
});

const CAPSULE: ReproductionCapsule = {
  id: 12,
  repository: "octo/repro-demo",
  ref: "main",
  kind: "GRADLE",
  command: "test --tests 'demo.PoolTest'",
  createdBy: 1,
  createdAt: "2026-11-01T00:00:00Z",
  rerunRequestedAt: null,
  rerunReason: null,
  matrixPackage: null,
  matrixVersions: [],
};

const RUN: ReproductionRun = {
  id: 5,
  result: "REPRODUCED",
  exitCode: 1,
  commitSha: "c".repeat(40),
  environment: "ubuntu24 · java 21.0.5",
  runnerEnvironment: "github-hosted",
  trustedWorkflow: true,
  runUrl: "https://github.com/octo/repro-demo/actions/runs/1/attempts/1",
  durationMs: 4200,
  finishedAt: new Date(Date.now() - 3 * 3_600_000).toISOString(),
  hasLog: true,
  dependencyVersion: null,
};

function renderPanel(panel: Panel, props: { isAuthor?: boolean; signedIn?: boolean } = {}) {
  vi.mocked(reproductionApi.panel).mockResolvedValue(panel);
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const ui: ReactElement = <ReproductionPanel questionId={7} isAuthor={props.isAuthor ?? false} signedIn={props.signedIn ?? false} />;
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("ReproductionPanel", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows the newest result per environment and its log on demand", async () => {
    vi.mocked(reproductionApi.log).mockResolvedValue({ runId: 5, logTail: "PoolTest > leaks FAILED\n[REDACTED]", maskedSecretCount: 1 });
    renderPanel({ capsule: CAPSULE, latestByCell: [RUN, { ...RUN, id: 6, result: "NOT_REPRODUCED", environment: "macos15", trustedWorkflow: false }], recentRuns: [], plannedVersions: [null], nextDueAt: null });

    expect(await screen.findByText("재현됨")).toBeInTheDocument();
    expect(screen.getByText("ubuntu24 · java 21.0.5")).toBeInTheDocument();
    expect(screen.getByText("재현 안 됨")).toBeInTheDocument();
    expect(screen.getByText("직접 작성한 워크플로")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "octo/repro-demo@main" })).toHaveAttribute("href", "https://github.com/octo/repro-demo/tree/main");

    await userEvent.click(screen.getAllByRole("button", { name: "실행 로그 보기" })[0]);
    expect(await screen.findByText(/PoolTest > leaks FAILED/)).toBeInTheDocument();
    expect(screen.getByText("비밀값으로 보이는 1곳을 가렸어요.")).toBeInTheDocument();
    expect(reproductionApi.log).toHaveBeenCalledWith(7, 5);
  });

  it("lets a signed-in reader dispute a result, and shows an open request instead", async () => {
    vi.mocked(reproductionApi.requestRerun).mockResolvedValue({ ...CAPSULE, rerunRequestedAt: new Date().toISOString(), rerunReason: "21.0.6에선 안 돼요" });
    renderPanel({ capsule: CAPSULE, latestByCell: [RUN], recentRuns: [], plannedVersions: [null], nextDueAt: null }, { signedIn: true });

    await userEvent.click(await screen.findByRole("button", { name: "결과에 이의 제기" }));
    await userEvent.type(screen.getByLabelText("무엇이 다른가요?"), "21.0.6에선 안 돼요");
    await userEvent.click(screen.getByRole("button", { name: "재실행 요청" }));
    expect(reproductionApi.requestRerun).toHaveBeenCalledWith(7, "21.0.6에선 안 돼요");
  });

  it("shows an outstanding rerun request to everyone", async () => {
    renderPanel({ capsule: { ...CAPSULE, rerunRequestedAt: new Date().toISOString(), rerunReason: "환경이 달라요" }, latestByCell: [], recentRuns: [], plannedVersions: [null], nextDueAt: null });
    expect(await screen.findByText(/재실행 요청됨 · 환경이 달라요/)).toBeInTheDocument();
    expect(screen.getByText("아직 실행 결과가 없어요.")).toBeInTheDocument();
  });

  it("offers the author a capsule form when there is none, and hides the panel from others", async () => {
    const { container, unmount } = renderPanel({ capsule: null, latestByCell: [], recentRuns: [], plannedVersions: [null], nextDueAt: null });
    await vi.waitFor(() => expect(reproductionApi.panel).toHaveBeenCalled());
    expect(container).toBeEmptyDOMElement();
    unmount();

    vi.mocked(reproductionApi.attach).mockResolvedValue(CAPSULE);
    renderPanel({ capsule: null, latestByCell: [], recentRuns: [], plannedVersions: [null], nextDueAt: null }, { isAuthor: true, signedIn: true });
    await userEvent.click(await screen.findByRole("button", { name: "재현 캡슐 붙이기" }));
    await userEvent.type(screen.getByLabelText("공개 GitHub 저장소"), "octo/repro-demo");
    await userEvent.type(screen.getByLabelText("명령"), "test");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(reproductionApi.attach).toHaveBeenCalledWith(7, { repository: "octo/repro-demo", ref: "main", kind: "GRADLE", command: "test" });
  });
});

describe("ReproductionPanel matrix", () => {
  beforeEach(() => vi.clearAllMocks());

  it("lays out versions against environments, keeping planned versions without a run", async () => {
    const matrix = { ...CAPSULE, matrixPackage: "com.zaxxer:HikariCP", matrixVersions: ["4.0.3"] };
    renderPanel({
      capsule: matrix,
      latestByCell: [
        { ...RUN, id: 1, dependencyVersion: "4.0.3", result: "NOT_REPRODUCED" },
        { ...RUN, id: 2, dependencyVersion: "5.1.0", result: "REPRODUCED" },
      ],
      plannedVersions: ["4.0.3", "5.1.0", "6.0.0"],
      nextDueAt: new Date(Date.now() + 5 * 86_400_000 + 3_600_000).toISOString(),
      recentRuns: [],
    });

    const table = await screen.findByRole("table");
    const rows = within(table).getAllByRole("row");
    expect(rows.map((row) => row.textContent)).toEqual(["버전ubuntu24 · java 21.0.5", "4.0.3재현 안 됨", "5.1.0재현됨", "6.0.0—"]);
    expect(screen.getByText("다음 정기 실행 5일 후")).toBeInTheDocument();
    expect(screen.getByText("com.zaxxer:HikariCP")).toBeInTheDocument();
  });

  it("sends the matrix package and versions from the form", async () => {
    vi.mocked(reproductionApi.attach).mockResolvedValue(CAPSULE);
    renderPanel({ capsule: null, latestByCell: [], recentRuns: [], plannedVersions: [null], nextDueAt: null }, { isAuthor: true, signedIn: true });
    await userEvent.click(await screen.findByRole("button", { name: "재현 캡슐 붙이기" }));
    await userEvent.type(screen.getByLabelText("공개 GitHub 저장소"), "octo/repro-demo");
    await userEvent.type(screen.getByLabelText("명령"), "test");
    await userEvent.type(screen.getByLabelText("버전 매트릭스 패키지 (선택)"), "com.zaxxer:HikariCP");
    await userEvent.type(screen.getByLabelText("꼭 돌릴 버전 (쉼표로 구분, 선택)"), "4.0.3, 6.0.0");
    await userEvent.click(screen.getByRole("button", { name: "저장" }));
    expect(reproductionApi.attach).toHaveBeenCalledWith(7, {
      repository: "octo/repro-demo",
      ref: "main",
      kind: "GRADLE",
      command: "test",
      matrixPackage: "com.zaxxer:HikariCP",
      matrixVersions: ["4.0.3", "6.0.0"],
    });
  });
});

describe("workflowSnippet", () => {
  it("calls Quno's reusable workflow with the capsule's exact spec", () => {
    const snippet = workflowSnippet(CAPSULE, "https://api.quno.example");
    expect(snippet).toContain("uses: polynomeer/quno/.github/workflows/reproduce.yml@main");
    expect(snippet).toContain("capsule-id: 12");
    expect(snippet).toContain(`command: "test --tests 'demo.PoolTest'"`);
    expect(snippet).toContain("id-token: write");
    expect(snippet).toContain('java-version: "21"');
  });
});
