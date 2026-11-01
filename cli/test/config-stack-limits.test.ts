import { mkdtempSync, statSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { loadConfig, saveConfig } from "../src/config.js";
import { boundedJson, clip } from "../src/limits.js";
import { findDependencyFile, isExcluded } from "../src/stack.js";

describe("config", () => {
  it("prefers the environment over the file over the defaults, and writes the file owner-only", () => {
    const env = { XDG_CONFIG_HOME: mkdtempSync(join(tmpdir(), "quno-")) } as NodeJS.ProcessEnv;
    expect(loadConfig(env)).toEqual({ apiUrl: "http://localhost:8081", webUrl: "http://localhost:3000", token: null });

    const path = saveConfig({ apiUrl: "https://api.example/", token: "quno_pat_file" }, env);
    expect(statSync(path).mode & 0o777).toBe(0o600);
    expect(loadConfig(env)).toMatchObject({ apiUrl: "https://api.example", token: "quno_pat_file" });
    expect(loadConfig({ ...env, QUNO_TOKEN: "quno_pat_env" }).token).toBe("quno_pat_env");
  });
});

describe("stack detection", () => {
  it("picks the first known build file", () => {
    const dir = mkdtempSync(join(tmpdir(), "quno-stack-"));
    expect(findDependencyFile(dir)).toBeNull();
    writeFileSync(join(dir, "package.json"), "{}");
    writeFileSync(join(dir, "build.gradle.kts"), "");
    expect(findDependencyFile(dir)).toBe("build.gradle.kts");
  });

  it("excludes by exact name or trailing star", () => {
    const acme = { ecosystem: "MAVEN" as const, name: "com.acme:billing", version: "1.0" };
    expect(isExcluded(acme, ["com.acme:*"])).toBe(true);
    expect(isExcluded(acme, ["com.acme:billing"])).toBe(true);
    expect(isExcluded(acme, ["com.acme:bill"])).toBe(false);
  });
});

describe("output limits", () => {
  it("clips long fields and marks them", () => {
    expect(clip("abcdef", 3)).toEqual({ text: "abc…", truncated: true });
    expect(clip("abc", 3)).toEqual({ text: "abc", truncated: false });
    expect(clip(null)).toEqual({ text: null, truncated: false });
  });

  it("drops trailing list entries until the payload fits", () => {
    const payload = { items: Array.from({ length: 50 }, (_, i) => "x".repeat(100) + i) };
    const { json, value } = boundedJson(payload, "items", 1_000);

    expect(json.length).toBeLessThanOrEqual(1_000);
    expect(value.items.length).toBeLessThan(50);
    expect(value).toMatchObject({ truncated: true });
    expect(value.items[0]).toBe(payload.items[0]);
  });
});
