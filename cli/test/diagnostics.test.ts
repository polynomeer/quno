import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
import { composeSnapshot, omitFields, OmitPathError, summarizeFailure, type FailureInput } from "../src/snapshot/compose.js";
import { configKeysOf, detectSymptom, failedTests, LineRing, scrubArgv } from "../src/snapshot/diagnostics.js";
import { redactSecrets } from "../src/snapshot/secrets.js";
import { baselineOf, deltaSince } from "../src/snapshot/state.js";
import { collectSnapshot } from "../src/snapshot/collect.js";
import { fakeProbe } from "./fake-probe.js";

const APP = "/Users/minji/work/app";

function failure(output: string, overrides: Partial<FailureInput> = {}): FailureInput {
  const ring = new LineRing();
  ring.push(output);
  const text = ring.text();
  return { argv: ["./gradlew", "test"], exitCode: 1, signal: null, durationMs: 4200, output: text, totalLines: ring.total, keptLines: ring.kept, startedAtMs: 1_000, ...overrides };
}

describe("LineRing", () => {
  it("keeps only the last lines, counts all of them and cuts very long ones", () => {
    const ring = new LineRing(3, 10);
    ring.push("one\ntwo\nth");
    ring.push("ree\nfour\n");
    ring.push("x".repeat(25));
    expect(ring.text()).toBe(`three\nfour\n${"x".repeat(10)}…`);
    expect([ring.total, ring.kept]).toEqual([5, 3]);
  });
});

describe("detectSymptom", () => {
  it.each([
    ["java.lang.OutOfMemoryError: Java heap space", "OUT_OF_MEMORY"],
    ["HikariPool-1 - Connection is not available, request timed out after 30000ms.", "DB_TIMEOUT"],
    ["Could not resolve all files for configuration ':compileClasspath'.", "DEPENDENCY_RESOLUTION"],
    ["npm ERR! code ERESOLVE", "DEPENDENCY_RESOLUTION"],
    ["12 tests completed, 2 failed\nThere were failing tests.", "TEST_FAILURE"],
    ["Execution failed for task ':compileKotlin'.", "BUILD_FAILURE"],
    ["something odd happened", null],
  ])("%s → %s", (output, symptom) => {
    expect(detectSymptom(output, 1, 0)).toBe(symptom);
  });

  it("treats exit 137 as memory and falls back to failed test reports", () => {
    expect(detectSymptom("", 137, 0)).toBe("OUT_OF_MEMORY");
    expect(detectSymptom("exit 1", 1, 2)).toBe("TEST_FAILURE");
  });
});

describe("scrubArgv", () => {
  it("keeps option names and drops sensitive values", () => {
    expect(scrubArgv(["deploy", "--token", "abc", "--api-key=xyz", "-Ddb.password=pw", "--verbose", "--name", "app"])).toEqual([
      "deploy",
      "--token",
      "[REDACTED]",
      "--api-key=[REDACTED]",
      "-Ddb.password=[REDACTED]",
      "--verbose",
      "--name",
      "app",
    ]);
  });
});

describe("configKeysOf", () => {
  it("lists leaf keys of YAML and properties, never values", () => {
    const yaml = "spring:\n  datasource:\n    url: jdbc:postgresql://db/app\n    password: hunter2\n  jpa:\n    open-in-view: false\n---\nserver:\n  port: 8081\n";
    expect(configKeysOf("application.yml", yaml)).toEqual(["spring.datasource.url", "spring.datasource.password", "spring.jpa.open-in-view", "server.port"]);
    expect(configKeysOf("application.properties", "# c\nserver.port=8081\nspring.datasource.password = x\n")).toEqual(["server.port", "spring.datasource.password"]);
  });
});

describe("failedTests", () => {
  it("reads JUnit reports written during this run only", () => {
    const xml = `<testsuite><testcase name="pays" classname="com.acme.BillingTest"/><testcase name="refunds" classname="com.acme.BillingTest"><failure message="expected: &lt;200&gt; but was: &lt;500&gt;">trace</failure></testcase></testsuite>`;
    const probe = fakeProbe({}, { [`${APP}/build/test-results/test/TEST-new.xml`]: xml, [`${APP}/build/test-results/test/TEST-old.xml`]: xml }, {}, {
      [`${APP}/build/test-results/test/TEST-new.xml`]: 2_000,
      [`${APP}/build/test-results/test/TEST-old.xml`]: 500,
    });
    expect(failedTests(probe, 1_000)).toEqual([{ suite: "com.acme.BillingTest", name: "refunds", message: "expected: <200> but was: <500>" }]);
  });
});

describe("composeSnapshot", () => {
  const files = {
    [`${APP}/src/main/resources/application.yml`]: "spring:\n  datasource:\n    password: hunter2\n",
    [`${APP}/build/test-results/test/TEST-a.xml`]: '<testcase name="t" classname="A"><failure message="boom"/></testcase>',
  };

  it("adds Tier 1 sections chosen by the symptom's profile", () => {
    const probe = fakeProbe({ env: { SPRING_PROFILES_ACTIVE: "local", HOME_SECRET: "never-read" } }, files);
    const db = composeSnapshot(probe, { failure: failure("Connection is not available, request timed out") }).snapshot;
    expect(db.tiers).toEqual([0, 1]);
    expect(db.tier1?.symptom).toBe("DB_TIMEOUT");
    expect(db.tier1?.configKeys).toEqual([{ file: "src/main/resources/application.yml", keys: ["spring.datasource.password"] }]);
    expect(db.tier1?.failedTests).toBeUndefined();
    expect(db.tier1?.env).toEqual([{ name: "SPRING_PROFILES_ACTIVE", value: "local" }]);
    expect(JSON.stringify(db)).not.toContain("hunter2");
    expect(JSON.stringify(db)).not.toContain("never-read");

    const deps = composeSnapshot(probe, { failure: failure("Could not find artifact com.acme:x:1.0") }).snapshot;
    expect(deps.tier1?.configKeys).toBeUndefined();
    expect(summarizeFailure(deps)).toContain("의존성 해석 실패 · ./gradlew test → 1 · 로그 1/1줄");
  });

  it("collects Tier 2 only for the detailed preset and trims for minimal", () => {
    const probe = fakeProbe({}, files);
    expect(composeSnapshot(probe).snapshot.tier2).toBeUndefined();
    const detailed = composeSnapshot(probe, { preset: "detailed" }).snapshot;
    expect(detailed.tiers).toEqual([0, 2]);
    expect(detailed.tier2).toMatchObject({ memory: { totalMb: 16384, freeMb: 2048 }, cpus: 10, cgroupMemoryLimitMb: null });

    const minimal = composeSnapshot(probe, { preset: "minimal", failure: failure("boom") }).snapshot;
    expect(minimal.tier0.os.name).toBe("macOS");
    expect("dependencies" in minimal.tier0).toBe(false);
    expect(minimal.tier1?.command.argv).toEqual(["./gradlew", "test"]);
    expect(minimal.tier1?.env).toBeUndefined();
    expect(minimal.omitted).toContain("tier0.git");
  });

  it("omits chosen fields, records them, and refuses paths outside the tiers", () => {
    const probe = fakeProbe();
    const snapshot = composeSnapshot(probe, { preset: "detailed", failure: failure("boom") }).snapshot;
    const trimmed = omitFields(snapshot, ["tier2", "tier1.env"]);
    expect(trimmed.tiers).toEqual([0, 1]);
    expect(trimmed.omitted).toEqual(["tier1.env", "tier2"]);
    expect(() => omitFields(snapshot, ["schemaVersion"])).toThrow(OmitPathError);
    expect(() => omitFields(snapshot, ["tier0"])).toThrow(OmitPathError);
  });
});

describe("delta since the last success", () => {
  it("counts commits, lists file names and resolved version changes", () => {
    const base = "a".repeat(40);
    const lock = (version: string) => JSON.stringify({ packages: { "": { dependencies: { zod: "^4" } }, "node_modules/zod": { version } } });
    const before = fakeProbe({}, { [`${APP}/package-lock.json`]: lock("4.6.0") }, { "git rev-parse HEAD": base });
    const baseline = baselineOf(before, collectSnapshot(before), new Date("2026-11-01T09:00:00Z"));

    const after = fakeProbe({}, { [`${APP}/package-lock.json`]: lock("4.6.7") }, {
      "git rev-parse HEAD": "b".repeat(40),
      [`git rev-list --count ${base}..HEAD`]: "3\n",
      [`git diff --name-only ${base}`]: "package-lock.json\nsrc/pool.ts\n",
    });
    expect(deltaSince(after, baseline, collectSnapshot(after))).toEqual({
      since: { capturedAt: "2026-11-01T09:00:00.000Z", commit: base },
      commits: 3,
      changedFiles: ["package-lock.json", "src/pool.ts"],
      changedFileCount: 2,
      dependencies: [{ name: "zod", from: "4.6.0", to: "4.6.7" }],
      configFiles: [],
    });
  });
});

describe("secret leak corpus (Phase 93.7, shared with the server)", () => {
  const corpus = (JSON.parse(readFileSync(new URL("../../testdata/secret-leak-corpus.json", import.meta.url), "utf8")) as { entries: { label: string; value: string; context: string }[] }).entries;

  it("redacts every corpus value from free text", () => {
    const leaked = corpus.filter((entry) => redactSecrets(entry.context).text.includes(entry.value)).map((entry) => entry.label);
    expect(leaked).toEqual([]);
    expect(redactSecrets("jdbc:postgresql://app:[REDACTED]@db/app").count).toBe(0);
  });

  it("leaves no corpus value in a failure snapshot, wherever it was planted", () => {
    const contexts = corpus.map((entry) => entry.context);
    const probe = fakeProbe(
      { env: { SPRING_PROFILES_ACTIVE: contexts.join(" | "), JAVA_TOOL_OPTIONS: "-Ddb.password=Vb1Kc9Wd3Fs6 -Xmx1g" } },
      {
        [`${APP}/gradle.properties`]: "org.gradle.jvmargs=-Dapi.token=Ht0Yj5Pa2Ue8Gi -Xmx2g",
        [`${APP}/src/main/resources/application.yml`]: corpus.map((entry, index) => `key${index}: ${entry.context.replace(/\n/g, " ")}`).join("\n"),
        [`${APP}/build/test-results/test/TEST-a.xml`]: corpus.map((entry) => `<testcase name="t" classname="A"><failure message="${entry.context.replace(/\n/g, "&#10;")}"/></testcase>`).join(""),
      },
      { "git rev-parse HEAD": "c".repeat(40), "git remote get-url origin": "https://deploy:Q8zr2Lm4nx@git.example.com/acme/app.git" },
    );
    const { snapshot, logs } = composeSnapshot(probe, {
      preset: "detailed",
      failure: failure(contexts.join("\n"), { argv: ["./gradlew", "test", ...contexts] }),
    });
    const serialized = JSON.stringify({ snapshot, logs });
    expect(corpus.filter((entry) => serialized.includes(entry.value)).map((entry) => entry.label)).toEqual([]);
    expect(serialized).not.toContain("minji");
  });
});
