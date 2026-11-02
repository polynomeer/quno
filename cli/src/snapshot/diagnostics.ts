import { join } from "node:path";
import { SENSITIVE_PROPERTY, scrubJvmArgs, type Probe } from "./collect.js";
import { REDACTED } from "./secrets.js";
import type { Symptom, Tier1, Tier2 } from "./schema.js";

/**
 * The last lines a command printed, kept in memory only (ADR-0084). Nothing is written anywhere
 * unless the command fails — then the caller freezes [text] once. Long lines are cut so that one
 * minified bundle can't push everything else out.
 */
export class LineRing {
  private readonly lines: string[] = [];
  private partial = "";
  total = 0;

  constructor(
    private readonly maxLines = 200,
    private readonly maxLineLength = 500,
  ) {}

  push(chunk: string): void {
    const parts = (this.partial + chunk).split("\n");
    this.partial = parts.pop() ?? "";
    parts.forEach((line) => this.add(line));
  }

  private add(line: string): void {
    this.total++;
    this.lines.push(line.length > this.maxLineLength ? `${line.slice(0, this.maxLineLength)}…` : line);
    if (this.lines.length > this.maxLines) this.lines.shift();
  }

  /** Flushes an unterminated last line and returns the window. */
  text(): string {
    if (this.partial) {
      this.add(this.partial);
      this.partial = "";
    }
    return this.lines.join("\n");
  }

  get kept(): number {
    return this.lines.length;
  }
}

/** Checked in order: the first that matches names the failure (ADR-0084). */
const SYMPTOMS: [Symptom, RegExp][] = [
  ["OUT_OF_MEMORY", /OutOfMemoryError|JavaScript heap out of memory|Cannot allocate memory|Container killed .*memory|OOMKilled/i],
  [
    "DB_TIMEOUT",
    /Connection is not available, request timed out|SQLTimeoutException|SQLTransientConnectionException|Communications link failure|could not connect to server|CannotGetJdbcConnectionException|RedisConnectionFailureException|MongoTimeoutException|Connection refused.*:(5432|3306|6379|27017)\b/i,
  ],
  ["DEPENDENCY_RESOLUTION", /Could not resolve (all |dependencies|files|artifacts?|[\w.-]+:[\w.-]+)|Could not find artifact|Could not find [\w.-]+:[\w.-]+:|npm ERR! code (ERESOLVE|E404|ETARGET)|ERESOLVE unable to resolve|No matching version found/i],
  ["TEST_FAILURE", /There were failing tests|tests completed, \d+ failed|Tests run: \d+, Failures: [1-9]|Tests run: \d+, Failures: \d+, Errors: [1-9]|Tests?\s+\d+ failed|Test Files\s+\d+ failed/i],
  ["BUILD_FAILURE", /BUILD FAILED|BUILD FAILURE|Compilation failed|COMPILATION ERROR|Execution failed for task|^e: file:|error TS\d+:/im],
];

export function detectSymptom(output: string, exitCode: number | null, failedTests: number): Symptom | null {
  if (exitCode === 137) return "OUT_OF_MEMORY";
  const found = SYMPTOMS.find(([, pattern]) => pattern.test(output))?.[0];
  if (found) return found;
  return failedTests > 0 ? "TEST_FAILURE" : null;
}

type Section = "env" | "configKeys" | "failedTests";

/**
 * Symptom profiles (ADR-0084): each symptom pulls in only the Tier 1 sections that help with it —
 * a dependency resolution failure doesn't need the application's config keys. [tier2Helps] is a
 * hint printed to the user, never a reason to collect Tier 2 without them choosing `detailed`.
 */
export const PROFILES: Record<Symptom | "UNKNOWN", { label: string; sections: Section[]; tier2Helps: boolean }> = {
  OUT_OF_MEMORY: { label: "메모리 부족", sections: ["env"], tier2Helps: true },
  DB_TIMEOUT: { label: "DB 연결 시간 초과", sections: ["env", "configKeys"], tier2Helps: false },
  DEPENDENCY_RESOLUTION: { label: "의존성 해석 실패", sections: ["env"], tier2Helps: false },
  TEST_FAILURE: { label: "테스트 실패", sections: ["env", "configKeys", "failedTests"], tier2Helps: false },
  BUILD_FAILURE: { label: "빌드 실패", sections: ["env"], tier2Helps: false },
  UNKNOWN: { label: "알 수 없는 실패", sections: ["env", "failedTests"], tier2Helps: false },
};

/** Variables whose values are worth seeing; everything else is never read (ADR-0084). */
const ENV_ALLOWLIST = ["SPRING_PROFILES_ACTIVE", "NODE_ENV", "TZ", "LANG", "LC_ALL", "JAVA_TOOL_OPTIONS", "JAVA_OPTS", "GRADLE_OPTS", "MAVEN_OPTS", "NODE_OPTIONS"];

export function allowlistedEnv(env: NodeJS.ProcessEnv): { name: string; value: string }[] {
  return ENV_ALLOWLIST.flatMap((name) => {
    const value = env[name];
    if (value === undefined) return [];
    return [{ name, value: name.endsWith("OPTS") || name.endsWith("OPTIONS") ? scrubJvmArgs(value.split(/\s+/).filter(Boolean)).join(" ") : value }];
  });
}

/** `--password=x`, `--token x` and `-Dkey=x` keep their names and lose their values. */
export function scrubArgv(argv: string[]): string[] {
  const out: string[] = [];
  argv.forEach((arg, index) => {
    const previous = argv[index - 1];
    if (previous !== undefined && /^--?[\w.-]+$/.test(previous) && SENSITIVE_PROPERTY.test(previous.replace(/^-+/, ""))) {
      out.push(REDACTED);
      return;
    }
    const option = arg.match(/^(--?[\w.-]+)=(.*)$/);
    if (option && SENSITIVE_PROPERTY.test(option[1]!.replace(/^-+/, ""))) {
      out.push(`${option[1]}=${REDACTED}`);
      return;
    }
    out.push(...scrubJvmArgs([arg]));
  });
  return out;
}

const CONFIG_DIRS = ["", "src/main/resources", "config"];
const CONFIG_FILE = /^application(-[\w-]+)?\.(ya?ml|properties)$/;
const MAX_KEYS = 200;

/** Keys only — a value never leaves the machine, not even scrubbed (ADR-0084). */
export function configKeysOf(file: string, text: string): string[] {
  const keys: string[] = [];
  if (file.endsWith(".properties")) {
    for (const line of text.split("\n")) {
      const key = line.match(/^\s*([^#!\s=:][^=:\s]*)\s*[=:]/)?.[1];
      if (key) keys.push(key);
    }
  } else {
    const stack: { indent: number; key: string }[] = [];
    for (const line of text.split("\n")) {
      if (/^---/.test(line)) stack.length = 0;
      const match = line.match(/^(\s*)([\w.\-[\]]+|"[^"]+"|'[^']+'):(\s|$)/);
      if (!match) continue;
      const indent = match[1]!.length;
      while (stack.length > 0 && stack[stack.length - 1]!.indent >= indent) stack.pop();
      stack.push({ indent, key: match[2]!.replace(/^["']|["']$/g, "") });
      // Leaves only: a key followed by a value on the same line.
      if (line.slice(match[0].length).trim() !== "") keys.push(stack.map((entry) => entry.key).join("."));
    }
  }
  return [...new Set(keys)].slice(0, MAX_KEYS);
}

export function configKeys(probe: Probe): NonNullable<Tier1["configKeys"]> {
  return CONFIG_DIRS.flatMap((dir) =>
    probe
      .list(join(probe.cwd, dir))
      .filter((name) => CONFIG_FILE.test(name))
      .flatMap((name) => {
        const file = dir ? `${dir}/${name}` : name;
        const text = probe.read(join(probe.cwd, file));
        const keys = text ? configKeysOf(name, text) : [];
        return keys.length > 0 ? [{ file, keys }] : [];
      }),
  );
}

const TEST_REPORT_DIRS = ["build/test-results/test", "build/test-results/integrationTest", "target/surefire-reports", "target/failsafe-reports"];
const MAX_FAILED_TESTS = 20;

function unescapeXml(text: string): string {
  return text
    .replace(/&#10;/g, " ")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&amp;/g, "&");
}

/** JUnit XML reports written since [sinceMs] — older ones belong to an earlier run. */
export function failedTests(probe: Probe, sinceMs: number): NonNullable<Tier1["failedTests"]> {
  const found: NonNullable<Tier1["failedTests"]> = [];
  for (const dir of TEST_REPORT_DIRS) {
    for (const name of probe.list(join(probe.cwd, dir)).filter((file) => file.endsWith(".xml"))) {
      const path = join(probe.cwd, dir, name);
      if ((probe.mtimeMs(path) ?? 0) < sinceMs) continue;
      const xml = probe.read(path) ?? "";
      for (const testcase of xml.split("<testcase").slice(1)) {
        const failure = testcase.match(/<(failure|error)\b([^>]*)/);
        if (!failure) continue;
        found.push({
          suite: unescapeXml(testcase.match(/\bclassname="([^"]*)"/)?.[1] ?? ""),
          name: unescapeXml(testcase.match(/\bname="([^"]*)"/)?.[1] ?? ""),
          message: (() => {
            const message = failure[2]!.match(/\bmessage="([^"]*)"/)?.[1];
            return message ? unescapeXml(message).split("\n")[0]!.slice(0, 300) : null;
          })(),
        });
        if (found.length >= MAX_FAILED_TESTS) return found;
      }
    }
  }
  return found;
}

const MB = 1024 * 1024;

/** cgroup v2 first, then v1; "max" or an absurd v1 sentinel means no limit. */
function cgroupMemoryLimitMb(probe: Probe): number | null {
  const raw = (probe.read("/sys/fs/cgroup/memory.max") ?? probe.read("/sys/fs/cgroup/memory/memory.limit_in_bytes"))?.trim();
  if (!raw || raw === "max") return null;
  const bytes = Number(raw);
  return Number.isFinite(bytes) && bytes < 2 ** 60 ? Math.round(bytes / MB) : null;
}

export function collectTier2(probe: Probe): Tier2 {
  const system = probe.system();
  return {
    memory: { totalMb: Math.round(system.totalMemBytes / MB), freeMb: Math.round(system.freeMemBytes / MB) },
    cpus: system.cpus,
    diskFreeMb: system.diskFreeBytes === null ? null : Math.round(system.diskFreeBytes / MB),
    cgroupMemoryLimitMb: cgroupMemoryLimitMb(probe),
  };
}
