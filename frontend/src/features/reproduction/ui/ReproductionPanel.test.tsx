import type { ReactElement } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ReproductionPanel, workflowSnippet } from "./ReproductionPanel";
import { reproductionApi } from "../api/reproduction.api";
import type { ReproductionCapsule, ReproductionPanel as Panel, ReproductionRun, VersionBisect } from "../api/reproduction.types";

vi.mock("../api/reproduction.api", async (importOriginal) => {
  const original = await importOriginal<typeof import("../api/reproduction.api")>();
  return {
    ...original,
    reproductionApi: { panel: vi.fn(), attach: vi.fn(), requestRerun: vi.fn(), log: vi.fn(), compare: vi.fn(), startBisect: vi.fn(), cancelBisect: vi.fn(), decideBisect: vi.fn() },
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
        { ...RUN, id: 2, dependencyVersion: "5.1.0", result: "REPRODUCED" },
        { ...RUN, id: 1, dependencyVersion: "4.0.3", result: "NOT_REPRODUCED" },
      ],
      plannedVersions: ["6.0.0", "4.0.3"],
      nextDueAt: new Date(Date.now() + 5 * 86_400_000 + 3_600_000).toISOString(),
      recentRuns: [],
    });

    const table = await screen.findByRole("table");
    const rows = within(table).getAllByRole("row");
    expect(rows.map((row) => row.textContent)).toEqual(["버전ubuntu24 · java 21.0.5", "4.0.3재현 안 됨", "5.1.0재현됨", "6.0.0—"]);
    expect(screen.getByText("다음 정기 실행 5일 후")).toBeInTheDocument();
    expect(screen.getByText("com.zaxxer:HikariCP")).toBeInTheDocument();
  });

  it("orders versions numerically, whatever order they arrived in", async () => {
    renderPanel({
      capsule: { ...CAPSULE, matrixPackage: "p" },
      latestByCell: [{ ...RUN, dependencyVersion: "3.10.0" }],
      plannedVersions: ["3.9.0", "3.8.1"],
      nextDueAt: null,
      recentRuns: [],
    });
    const rows = within(await screen.findByRole("table")).getAllByRole("row").slice(1);
    expect(rows.map((row) => row.textContent?.slice(0, 5))).toEqual(["3.8.1", "3.9.0", "3.10."]);
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

describe("ReproductionPanel release transitions", () => {
  beforeEach(() => vi.clearAllMocks());

  it("flags a release that broke and compares the two logs on demand", async () => {
    vi.mocked(reproductionApi.compare).mockResolvedValue({
      transition: "BROKE",
      fromVersion: "3.7.0",
      toVersion: "3.8.0",
      logsAvailable: true,
      lines: [
        { type: "REMOVED", text: "BUILD SUCCESSFUL" },
        { type: "ADDED", text: "BUILD FAILED" },
      ],
    });
    renderPanel({
      capsule: CAPSULE,
      latestByCell: [RUN],
      plannedVersions: [null],
      nextDueAt: null,
      recentRuns: [],
      transitions: [{ runId: 5, transition: "BROKE", fromVersion: "3.7.0", toVersion: "3.8.0", environment: "ubuntu24", runUrl: "u", finishedAt: RUN.finishedAt }],
    });

    expect(await screen.findByText("깨짐 감지 · 3.7.0 → 3.8.0")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "로그 비교" }));
    expect(await screen.findByText(/BUILD SUCCESSFUL/)).toBeInTheDocument();
    expect(screen.getByText(/BUILD FAILED/)).toBeInTheDocument();
    expect(reproductionApi.compare).toHaveBeenCalledWith(7, 5);
  });

  it("calls the reverse a candidate fix, and says when the logs are gone", async () => {
    vi.mocked(reproductionApi.compare).mockResolvedValue({ transition: "FIXED", fromVersion: "3.8.0", toVersion: "3.9.0", logsAvailable: false, lines: [] });
    renderPanel({
      capsule: CAPSULE,
      latestByCell: [],
      plannedVersions: [null],
      nextDueAt: null,
      recentRuns: [],
      transitions: [{ runId: 6, transition: "FIXED", fromVersion: "3.8.0", toVersion: "3.9.0", environment: "ubuntu24", runUrl: "u", finishedAt: RUN.finishedAt }],
    });
    expect(await screen.findByText("수정 후보 · 3.8.0 → 3.9.0")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "로그 비교" }));
    expect(await screen.findByText("로그가 남아 있지 않아요.")).toBeInTheDocument();
  });
});

describe("ReproductionPanel version search", () => {
  beforeEach(() => vi.clearAllMocks());

  const BISECT: VersionBisect = {
    id: 3,
    fromVersion: "1.0.0",
    toVersion: "1.9.0",
    regression: true,
    status: "RUNNING",
    lowVersion: "1.0.0",
    highVersion: "1.9.0",
    confidence: null,
    runsUsed: 3,
    maxRuns: 10,
    candidateCount: 10,
    probes: ["1.4.0", "1.6.0"],
    decision: "PENDING",
    createdAt: "",
    finishedAt: null,
  };
  const panel = (bisect: VersionBisect | null, transitions: Panel["transitions"] = []): Panel => ({ capsule: CAPSULE, latestByCell: [], plannedVersions: [null], nextDueAt: null, recentRuns: [], transitions, bisect });

  it("lets the author start one from a transition", async () => {
    vi.mocked(reproductionApi.startBisect).mockResolvedValue(BISECT);
    renderPanel(panel(null, [{ runId: 5, transition: "BROKE", fromVersion: "1.0.0", toVersion: "1.9.0", environment: "ubuntu24", runUrl: "u", finishedAt: RUN.finishedAt }]), { isAuthor: true, signedIn: true });
    await userEvent.click(await screen.findByRole("button", { name: "달라진 버전 찾기" }));
    expect(reproductionApi.startBisect).toHaveBeenCalledWith(7, "1.0.0", "1.9.0");
  });

  it("shows progress, and only the author can cancel", async () => {
    renderPanel(panel(BISECT));
    expect(await screen.findByText("버전 탐색 중 · 1.0.0 ~ 1.9.0")).toBeInTheDocument();
    expect(screen.getByText("1.4.0, 1.6.0")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "탐색 취소" })).not.toBeInTheDocument();
  });

  it("offers a found boundary to its author as a suggestion", async () => {
    vi.mocked(reproductionApi.decideBisect).mockResolvedValue(BISECT);
    renderPanel(panel({ ...BISECT, status: "FOUND", lowVersion: "1.4.0", highVersion: "1.5.0", confidence: "LOW" }), { isAuthor: true, signedIn: true });
    expect(await screen.findByText("1.5.0")).toBeInTheDocument();
    expect(screen.getByText(/신뢰도 낮음/)).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "승인" }));
    expect(reproductionApi.decideBisect).toHaveBeenCalledWith(7, 3, true);
  });

  it("says what is known when it can't decide", async () => {
    renderPanel(panel({ ...BISECT, status: "BUDGET_REACHED", lowVersion: "1.2.0", highVersion: "1.7.0" }));
    expect(await screen.findByText(/좁혀진 범위 · 1.2.0 ~ 1.7.0/)).toBeInTheDocument();
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
    expect(snippet).not.toContain("runs-on:");
  });

  it("follows the question's environment recipe", () => {
    const snippet = workflowSnippet(CAPSULE, "https://api.quno.example", { runsOn: "macos-latest", javaVersion: "17", nodeVersion: "22", notes: [] });
    expect(snippet).toContain('java-version: "17"');
    expect(snippet).toContain('node-version: "22"');
    expect(snippet).toContain("runs-on: macos-latest");
  });
});
