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
});
