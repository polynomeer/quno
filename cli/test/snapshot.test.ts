import { describe, expect, it } from "vitest";
import { collectSnapshot, lockedDependencies, remoteHost, scrubIdentity, scrubJvmArgs, summarizeSnapshot } from "../src/snapshot/collect.js";
import { fakeProbe } from "./fake-probe.js";

const probe = fakeProbe;

describe("collectSnapshot", () => {
  it("collects Tier 0 from release files, wrappers, lock files and git without touching other env values", () => {
    const snapshot = collectSnapshot(
      probe(
        { env: { JAVA_HOME: "/jdk", LANG: "ko_KR.UTF-8", GITHUB_ACTIONS: "true", AWS_SECRET_ACCESS_KEY: "never-read", JAVA_TOOL_OPTIONS: "-Xmx2g -Ddb.password=hunter2" } },
        {
          "/jdk/release": 'JAVA_VERSION="21.0.5"\nIMPLEMENTOR="Eclipse Adoptium"',
          "/Users/minji/work/app/gradle/wrapper/gradle-wrapper.properties": "distributionUrl=https\\://services.gradle.org/distributions/gradle-9.1.0-bin.zip",
          "/Users/minji/work/app/gradle.lockfile": "org.springframework.boot:spring-boot:4.0.8=runtimeClasspath\nempty=",
        },
        {
          "sw_vers -productVersion": "15.1\n",
          "git --version": "git version 2.47.1",
          "git rev-parse HEAD": "a".repeat(40),
          "git status --porcelain": " M src/Main.kt\n",
          "git remote get-url origin": "https://minji:ghp_secret@github.com/acme/app.git",
          "sysctl -n sysctl.proc_translated": "0",
        },
      ),
      new Date("2026-11-01T00:00:00Z"),
    );

    expect(snapshot).toMatchObject({ schemaVersion: 1, source: "COLLECTOR", tiers: [0], capturedAt: "2026-11-01T00:00:00.000Z" });
    expect(snapshot.tier0.os).toEqual({ name: "macOS", version: "15.1", arch: "arm64", kernel: "24.1.0" });
    expect(snapshot.tier0.runtimes).toEqual([
      { name: "java", vendor: "Eclipse Adoptium", version: "21.0.5" },
      { name: "node", vendor: null, version: "22.11.0" },
    ]);
    expect(snapshot.tier0.locale).toEqual({ lang: "ko_KR", encoding: "UTF-8", timezone: "Asia/Seoul" });
    expect(snapshot.tier0.tools).toEqual([{ name: "gradle", version: "9.1.0" }, { name: "git", version: "2.47.1" }]);
    expect(snapshot.tier0.ci).toEqual({ isCi: true, provider: "github-actions" });
    expect(snapshot.tier0.git).toEqual({ commit: "a".repeat(40), dirty: true, remoteHost: "github.com" });
    expect(snapshot.tier0.dependencies).toEqual({ resolution: "lockfile", source: "gradle.lockfile", items: [{ ecosystem: "MAVEN", name: "org.springframework.boot:spring-boot", version: "4.0.8" }] });
    expect(snapshot.tier0.jvmArgs).toEqual(["-Xmx2g", "-Ddb.password=[REDACTED]"]);

    const json = JSON.stringify(snapshot);
    for (const leak of ["never-read", "hunter2", "ghp_secret", "minji", "corp.example"]) expect(json).not.toContain(leak);
  });

  it("reads resolved npm versions of direct dependencies from package-lock", () => {
    const lock = JSON.stringify({
      packages: {
        "": { dependencies: { next: "^16.0.0" }, devDependencies: { vitest: "^3" } },
        "node_modules/next": { version: "16.0.1" },
        "node_modules/vitest": { version: "3.2.7" },
        "node_modules/transitive": { version: "1.0.0" },
      },
    });
    expect(lockedDependencies(probe({}, { "/Users/minji/work/app/package-lock.json": lock }))).toEqual({
      resolution: "lockfile",
      source: "package-lock.json",
      items: [
        { ecosystem: "NPM", name: "next", version: "16.0.1" },
        { ecosystem: "NPM", name: "vitest", version: "3.2.7" },
      ],
    });
    expect(lockedDependencies(probe())).toEqual({ resolution: "none", source: null, items: [] });
  });

  it("names the JDK vendor from IMPLEMENTOR or java -version", () => {
    const corretto = collectSnapshot(probe({ env: { JAVA_HOME: "/jdk" } }, { "/jdk/release": 'JAVA_VERSION="21.0.10"\nIMPLEMENTOR="Amazon.com Inc."' }));
    expect(corretto.tier0.runtimes[0]).toEqual({ name: "java", vendor: "Amazon Corretto", version: "21.0.10" });

    const zulu = collectSnapshot(probe({}, {}, { "java -version": 'openjdk version "17.0.12"\nOpenJDK Runtime Environment Zulu17.52+17-CA' }));
    expect(zulu.tier0.runtimes[0]).toEqual({ name: "java", vendor: "Azul Zulu", version: "17.0.12" });
  });

  it("summarises in one line", () => {
    const snapshot = collectSnapshot(probe({ platform: "linux", arch: "x64" }, { "/etc/os-release": 'NAME="Ubuntu"\nVERSION_ID="24.04"', "/.dockerenv": "" }));
    expect(summarizeSnapshot(snapshot)).toBe("Ubuntu 24.04 x86_64 · Docker · 잠금 파일 없음");
  });
});

describe("scrubbing helpers", () => {
  it("keeps only the host of a git remote", () => {
    expect(remoteHost("git@github.com:acme/app.git")).toBe("github.com");
    expect(remoteHost("https://user:pw@gitlab.example.com/x/y")).toBe("gitlab.example.com");
    expect(remoteHost("not a url")).toBeNull();
  });

  it("redacts sensitive system properties and identity strings", () => {
    expect(scrubJvmArgs(["-Dapi.token=abc", "-Dserver.port=8080", "-Dsigning.key=x", "-Xmx1g"])).toEqual(["-Dapi.token=[REDACTED]", "-Dserver.port=8080", "-Dsigning.key=[REDACTED]", "-Xmx1g"]);
    expect(scrubIdentity({ path: "/Users/minji/app", list: ["minji@minji-mbp"] }, { home: "/Users/minji", user: "minji", host: "minji-mbp" })).toEqual({
      path: "~/app",
      list: ["<user>@<host>"],
    });
  });
});
