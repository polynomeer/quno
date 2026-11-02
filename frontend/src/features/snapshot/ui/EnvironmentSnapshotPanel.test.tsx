import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { EnvironmentSnapshotPanel } from "./EnvironmentSnapshotPanel";
import type { EnvironmentSnapshot } from "@/features/question/api/question.types";

const SNAPSHOT: EnvironmentSnapshot = {
  versionNumber: 1,
  schemaVersion: 1,
  source: "COLLECTOR",
  collector: "quno-cli 0.2.0",
  fingerprint: "0123456789abcdef",
  serverRedactions: 1,
  createdAt: "",
  document: {
    tiers: [0],
    tier0: {
      os: { name: "macOS", version: "15.1", arch: "arm64" },
      runtimes: [{ name: "java", vendor: "Eclipse Adoptium", version: "21.0.5" }],
      container: { docker: true, wsl: false, rosetta: false },
      locale: { lang: "ko_KR", encoding: "UTF-8", timezone: "Asia/Seoul" },
      git: { commit: "a".repeat(40), dirty: true, remoteHost: "github.com" },
      dependencies: {
        resolution: "lockfile",
        source: "gradle.lockfile",
        items: Array.from({ length: 8 }, (_, i) => ({ ecosystem: "MAVEN", name: `org.example:lib-${i}`, version: `1.${i}` })),
      },
    },
  },
};

describe("EnvironmentSnapshotPanel", () => {
  it("lists the environment with provenance and the server fingerprint", () => {
    render(<EnvironmentSnapshotPanel snapshot={SNAPSHOT} />);

    expect(screen.getByText("Quno 수집기 0.2.0")).toBeInTheDocument();
    expect(screen.getByText("01234567")).toBeInTheDocument();
    expect(screen.getByText("macOS 15.1 arm64")).toBeInTheDocument();
    expect(screen.getByText("Eclipse Adoptium 21.0.5")).toBeInTheDocument();
    expect(screen.getByText("Docker")).toBeInTheDocument();
    expect(screen.getByText("aaaaaaaaaa · 수정 중 · github.com")).toBeInTheDocument();
    expect(screen.getByText("lib-5")).toBeInTheDocument();
    expect(screen.queryByText("lib-6")).not.toBeInTheDocument();
    expect(screen.getByText(/gradle.lockfile의 해석된 버전이에요 \(외 2개\)\. 서버가 비밀값으로 보이는 값 1개를 더 가렸어요\./)).toBeInTheDocument();
  });

  it("marks hand-typed snapshots and renders nothing without one", () => {
    const { container, rerender } = render(<EnvironmentSnapshotPanel snapshot={null} />);
    expect(container).toBeEmptyDOMElement();

    rerender(<EnvironmentSnapshotPanel snapshot={{ ...SNAPSHOT, source: "MANUAL", collector: null, serverRedactions: 0 }} />);
    expect(screen.getByText("수동 입력")).toBeInTheDocument();
    expect(screen.getByText(/신뢰도가 낮아요/)).toBeInTheDocument();
  });

  it("shows the failure diagnosis, what changed since the last success, Tier 2 and omitted fields", () => {
    const failure: EnvironmentSnapshot = {
      ...SNAPSHOT,
      symptom: "DB_TIMEOUT",
      document: {
        tiers: [0, 1, 2],
        tier0: { os: { name: "macOS", version: "15.1", arch: "arm64" } },
        tier1: {
          command: { argv: ["./gradlew", "test"], exitCode: 1, durationMs: 4200 },
          symptom: "DB_TIMEOUT",
          logWindow: { keptLines: 200, totalLines: 1532, attachedAs: "logs" },
          env: [{ name: "SPRING_PROFILES_ACTIVE", value: "local" }],
          configKeys: [{ file: "src/main/resources/application.yml", keys: ["spring.datasource.url", "spring.datasource.hikari.maximum-pool-size"] }],
          failedTests: [{ suite: "com.acme.BillingTest", name: "refunds", message: "Connection is not available" }],
        },
        tier2: { memory: { totalMb: 16384, freeMb: 512 }, cpus: 10, diskFreeMb: null, cgroupMemoryLimitMb: 2048 },
        delta: {
          since: { capturedAt: new Date(Date.now() - 2 * 3600_000).toISOString(), commit: "b".repeat(40) },
          commits: 3,
          changedFiles: ["build.gradle.kts", "src/main/resources/application.yml"],
          changedFileCount: 2,
          dependencies: [{ name: "com.zaxxer:HikariCP", from: "5.1.0", to: "6.0.0" }],
          configFiles: ["src/main/resources/application.yml"],
        },
        omitted: ["tier0.git"],
      },
    };
    render(<EnvironmentSnapshotPanel snapshot={failure} />);

    expect(screen.getByText("DB 연결 시간 초과")).toBeInTheDocument();
    expect(screen.getByText("./gradlew test")).toBeInTheDocument();
    expect(screen.getByText("1 · 4.2초")).toBeInTheDocument();
    expect(screen.getByText("마지막 200/1532줄 · 로그 칸에 첨부")).toBeInTheDocument();
    expect(screen.getByText("BillingTest.refunds")).toBeInTheDocument();
    expect(screen.getByText(/spring.datasource.url, spring.datasource.hikari.maximum-pool-size/)).toBeInTheDocument();
    expect(screen.getByText("16.0GB 중 512MB 여유")).toBeInTheDocument();
    expect(screen.getByText("2.0GB")).toBeInTheDocument();
    expect(screen.getByText(/마지막으로 성공한 뒤 바뀐 것 \(2시간 전 · 커밋 bbbbbbb\)/)).toBeInTheDocument();
    expect(screen.getByText("HikariCP 5.1.0 → 6.0.0")).toBeInTheDocument();
    expect(screen.getByText("build.gradle.kts")).toBeInTheDocument();
    expect(screen.getByText(/작성자가 뺀 항목: tier0.git\./)).toBeInTheDocument();
    expect(screen.queryByText(/잠금 파일이 없어/)).not.toBeInTheDocument();
  });
});
