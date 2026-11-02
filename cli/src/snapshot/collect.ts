import { execFileSync } from "node:child_process";
import { existsSync, readdirSync, readFileSync, statfsSync, statSync } from "node:fs";
import { availableParallelism, freemem, homedir, hostname, release, totalmem, userInfo } from "node:os";
import { join } from "node:path";
import type { SnapshotV1, Tier0 } from "./schema.js";

export const COLLECTOR = { name: "quno-cli", version: "0.2.0" } as const;

/** Everything the collector touches, injected so tests run without the real machine. */
export interface Probe {
  cwd: string;
  platform: NodeJS.Platform;
  arch: string;
  /** Runs a command with a short timeout; null on any failure. Output is stdout + stderr. */
  run: (command: string, args: string[], cwd?: string) => string | null;
  read: (path: string) => string | null;
  exists: (path: string) => boolean;
  /** File names in [dir]; empty when it doesn't exist. */
  list: (dir: string) => string[];
  mtimeMs: (path: string) => number | null;
  /** Read only for Tier 2, which is opt-in. */
  system: () => { totalMemBytes: number; freeMemBytes: number; cpus: number; diskFreeBytes: number | null };
  env: NodeJS.ProcessEnv;
  home: string;
  user: string;
  host: string;
  kernel: string;
  nodeVersion: string;
  intl: { locale: string | null; timezone: string | null };
}

export function realProbe(cwd: string): Probe {
  const options = Intl.DateTimeFormat().resolvedOptions();
  return {
    cwd,
    platform: process.platform,
    arch: process.arch,
    run: (command, args, dir) => {
      try {
        return execFileSync(command, args, { cwd: dir ?? cwd, timeout: 3_000, encoding: "utf8", stdio: ["ignore", "pipe", "pipe"] });
      } catch (error) {
        // `java -version` writes to stderr and some tools exit non-zero with useful output.
        const failed = error as { stdout?: string; stderr?: string };
        const output = `${failed.stdout ?? ""}${failed.stderr ?? ""}`.trim();
        return output || null;
      }
    },
    read: (path) => {
      try {
        return readFileSync(path, "utf8");
      } catch {
        return null;
      }
    },
    exists: existsSync,
    list: (dir) => {
      try {
        return readdirSync(dir);
      } catch {
        return [];
      }
    },
    mtimeMs: (path) => {
      try {
        return statSync(path).mtimeMs;
      } catch {
        return null;
      }
    },
    system: () => {
      let diskFreeBytes: number | null = null;
      try {
        const stats = statfsSync(cwd);
        diskFreeBytes = stats.bavail * stats.bsize;
      } catch {
        // not every filesystem reports it
      }
      return { totalMemBytes: totalmem(), freeMemBytes: freemem(), cpus: availableParallelism(), diskFreeBytes };
    },
    env: process.env,
    home: homedir(),
    user: (() => {
      try {
        return userInfo().username;
      } catch {
        return "";
      }
    })(),
    host: hostname(),
    kernel: release(),
    nodeVersion: process.version,
    intl: { locale: options.locale ?? null, timezone: options.timeZone ?? null },
  };
}

/** Environment variables whose *presence* says something; their values are never read (ADR-0083). */
const CI_MARKERS: [string, string][] = [
  ["GITHUB_ACTIONS", "github-actions"],
  ["GITLAB_CI", "gitlab-ci"],
  ["JENKINS_URL", "jenkins"],
  ["CIRCLECI", "circleci"],
  ["BUILDKITE", "buildkite"],
  ["TF_BUILD", "azure-pipelines"],
];

export const SENSITIVE_PROPERTY = /password|passwd|secret|token|credential|apikey|api_key|private[_-]?key|(^|[._-])key$/i;

function first(text: string | null, pattern: RegExp): string | null {
  return text?.match(pattern)?.[1] ?? null;
}

function osInfo(probe: Probe): Tier0["os"] {
  const arch = probe.arch === "x64" ? "x86_64" : probe.arch;
  if (probe.platform === "darwin") {
    return { name: "macOS", version: probe.run("sw_vers", ["-productVersion"])?.trim() ?? null, arch, kernel: probe.kernel };
  }
  if (probe.platform === "linux") {
    const release = probe.read("/etc/os-release");
    return {
      name: first(release, /^NAME="?([^"\n]+)"?/m) ?? "Linux",
      version: first(release, /^VERSION_ID="?([^"\n]+)"?/m),
      arch,
      kernel: probe.kernel,
    };
  }
  if (probe.platform === "win32") return { name: "Windows", version: probe.kernel, arch, kernel: probe.kernel };
  return { name: probe.platform, version: null, arch, kernel: probe.kernel };
}

const JDK_VENDORS: [RegExp, string][] = [
  [/Temurin|AdoptOpenJDK|Adoptium/i, "Eclipse Adoptium"],
  [/Corretto|Amazon/i, "Amazon Corretto"],
  [/Zulu/i, "Azul Zulu"],
  [/GraalVM/i, "GraalVM"],
  [/Microsoft/i, "Microsoft"],
  [/JetBrains/i, "JetBrains"],
  [/Oracle|Java\(TM\)/i, "Oracle"],
];

/** Prefers $JAVA_HOME/release (exact, no process), falls back to `java -version`. */
function javaRuntime(probe: Probe): Tier0["runtimes"][number] | null {
  const javaHome = probe.env.JAVA_HOME;
  const releaseFile = javaHome ? probe.read(join(javaHome, "release")) : null;
  const version = first(releaseFile, /^JAVA_VERSION="([^"]+)"/m) ?? first(probe.run("java", ["-version"]), /version "([^"]+)"/);
  if (!version) return null;
  const implementor = [first(releaseFile, /^IMPLEMENTOR="([^"]+)"/m), first(releaseFile, /^IMPLEMENTOR_VERSION="([^"]+)"/m)].filter(Boolean).join(" ");
  const vendorText = implementor || (probe.run("java", ["-version"]) ?? "");
  const vendor = JDK_VENDORS.find(([pattern]) => pattern.test(vendorText))?.[1] ?? (vendorText.includes("OpenJDK") ? "OpenJDK" : null);
  return { name: "java", vendor, version };
}

function containerInfo(probe: Probe): Tier0["container"] {
  const cgroup = probe.read("/proc/1/cgroup") ?? "";
  return {
    docker: probe.exists("/.dockerenv") || /docker|containerd|kubepods/.test(cgroup),
    wsl: /microsoft/i.test(probe.kernel) || probe.env.WSL_DISTRO_NAME !== undefined,
    rosetta: probe.platform === "darwin" && probe.run("sysctl", ["-n", "sysctl.proc_translated"])?.trim() === "1",
  };
}

function tools(probe: Probe): Tier0["tools"] {
  const found: Tier0["tools"] = [];
  const wrapper = probe.read(join(probe.cwd, "gradle/wrapper/gradle-wrapper.properties"));
  const gradle = first(wrapper, /gradle-([\d.]+(?:-[\w.]+)?)-(?:bin|all)\.zip/);
  if (gradle) found.push({ name: "gradle", version: gradle });
  const mavenWrapper = probe.read(join(probe.cwd, ".mvn/wrapper/maven-wrapper.properties"));
  const maven = first(mavenWrapper, /apache-maven-([\d.]+)-bin/);
  if (maven) found.push({ name: "maven", version: maven });
  if (probe.exists(join(probe.cwd, "package.json"))) {
    const npm = probe.run("npm", ["--version"])?.trim();
    if (npm && /^\d/.test(npm)) found.push({ name: "npm", version: npm });
  }
  const git = first(probe.run("git", ["--version"]), /git version ([\d.]+)/);
  if (git) found.push({ name: "git", version: git });
  return found;
}

function ide(env: NodeJS.ProcessEnv): string | null {
  if (env.TERMINAL_EMULATOR?.includes("JetBrains")) return "IntelliJ IDEA";
  if (env.TERM_PROGRAM === "vscode") return "VS Code";
  return null;
}

/** `git@github.com:acme/app.git` and `https://user:pw@gitlab.example/x` both become just the host. */
export function remoteHost(url: string | null): string | null {
  if (!url) return null;
  const scp = url.trim().match(/^[\w.-]+@([\w.-]+):/);
  if (scp) return scp[1] ?? null;
  try {
    return new URL(url.trim()).hostname || null;
  } catch {
    return null;
  }
}

function gitInfo(probe: Probe): Tier0["git"] {
  const commit = probe.run("git", ["rev-parse", "HEAD"])?.trim();
  if (!commit || !/^[0-9a-f]{40}$/.test(commit)) return null;
  const status = probe.run("git", ["status", "--porcelain"]);
  return { commit, dirty: status === null ? null : status.trim().length > 0, remoteHost: remoteHost(probe.run("git", ["remote", "get-url", "origin"])) };
}

/** Resolved versions from lock files only — running a Gradle resolution would take far too long (ADR-0083). */
export function lockedDependencies(probe: Probe): Tier0["dependencies"] {
  const npmLock = probe.read(join(probe.cwd, "package-lock.json"));
  if (npmLock) {
    try {
      const lock = JSON.parse(npmLock) as { packages?: Record<string, { version?: string; dependencies?: Record<string, string>; devDependencies?: Record<string, string> }> };
      const root = lock.packages?.[""];
      const names = [...Object.keys(root?.dependencies ?? {}), ...Object.keys(root?.devDependencies ?? {})];
      const items = names.flatMap((name) => {
        const version = lock.packages?.[`node_modules/${name}`]?.version;
        return version ? [{ ecosystem: "NPM" as const, name, version }] : [];
      });
      if (items.length > 0) return { resolution: "lockfile", source: "package-lock.json", items: items.slice(0, 500) };
    } catch {
      // unreadable lock file — fall through
    }
  }
  const gradleLock = probe.read(join(probe.cwd, "gradle.lockfile"));
  if (gradleLock) {
    const items = gradleLock
      .split("\n")
      .map((line) => line.match(/^([\w.-]+:[\w.-]+):([^=\s]+)=/))
      .flatMap((match) => (match ? [{ ecosystem: "MAVEN" as const, name: match[1]!, version: match[2]! }] : []));
    if (items.length > 0) return { resolution: "lockfile", source: "gradle.lockfile", items: items.slice(0, 500) };
  }
  return { resolution: "none", source: null, items: [] };
}

/** `-Ddb.password=x` keeps its name and loses its value. */
export function scrubJvmArgs(args: string[]): string[] {
  return args.map((arg) => {
    const property = arg.match(/^-D([^=]+)=(.*)$/);
    return property && SENSITIVE_PROPERTY.test(property[1]!) ? `-D${property[1]}=[REDACTED]` : arg;
  });
}

function jvmArgs(probe: Probe): string[] {
  const fromEnv = probe.env.JAVA_TOOL_OPTIONS?.split(/\s+/) ?? [];
  const gradleProperties = probe.read(join(probe.cwd, "gradle.properties"));
  const fromGradle = first(gradleProperties, /^org\.gradle\.jvmargs=(.+)$/m)?.split(/\s+/) ?? [];
  return scrubJvmArgs([...fromEnv, ...fromGradle].filter(Boolean));
}

/** Removes the home directory, user name and host name from every string in [value]. */
export function scrubIdentity<T>(value: T, probe: Pick<Probe, "home" | "user" | "host">): T {
  const candidates: [string, string][] = [
    [probe.home, "~"],
    [probe.user, "<user>"],
    [probe.host, "<host>"],
  ];
  const replacements = candidates
    .filter(([needle]) => needle.length >= 3)
    // Longest first, so a host name that contains the user name is replaced whole.
    .sort(([a], [b]) => b.length - a.length);
  const walk = (node: unknown): unknown => {
    if (typeof node === "string") return replacements.reduce((text, [needle, replacement]) => text.split(needle).join(replacement), node);
    if (Array.isArray(node)) return node.map(walk);
    if (node && typeof node === "object") return Object.fromEntries(Object.entries(node).map(([key, child]) => [key, walk(child)]));
    return node;
  };
  return walk(value) as T;
}

/** Tier 0 only, for now (Phase 93.2). Event-driven: called when the user captures or asks, never in the background. */
export function collectSnapshot(probe: Probe, now = new Date()): SnapshotV1 {
  const lang = probe.env.LC_ALL ?? probe.env.LANG ?? null;
  const ciProvider = CI_MARKERS.find(([name]) => probe.env[name] !== undefined)?.[1] ?? null;
  const java = javaRuntime(probe);
  const tier0: Tier0 = {
    os: osInfo(probe),
    runtimes: [...(java ? [java] : []), { name: "node", vendor: null, version: probe.nodeVersion.replace(/^v/, "") }],
    container: containerInfo(probe),
    locale: { lang: lang?.split(".")[0] ?? probe.intl.locale, encoding: lang?.split(".")[1] ?? null, timezone: probe.intl.timezone },
    tools: tools(probe),
    ide: ide(probe.env),
    ci: { isCi: probe.env.CI !== undefined || ciProvider !== null, provider: ciProvider },
    git: gitInfo(probe),
    dependencies: lockedDependencies(probe),
    jvmArgs: jvmArgs(probe),
  };
  return scrubIdentity(
    { schemaVersion: 1, source: "COLLECTOR", collector: { ...COLLECTOR }, capturedAt: now.toISOString(), tiers: [0], tier0 },
    probe,
  );
}

/** One line for previews: `macOS 15.1 arm64 · Eclipse Adoptium 21.0.5 · lockfile 의존성 42개`. */
export function summarizeSnapshot(snapshot: SnapshotV1): string {
  // Fields may be missing when the user omitted them (ADR-0084).
  const { os, runtimes, container, dependencies } = snapshot.tier0 as Partial<SnapshotV1["tier0"]>;
  const java = runtimes?.find((runtime) => runtime.name === "java");
  return [
    os ? [os.name, os.version, os.arch].filter(Boolean).join(" ") : null,
    java ? [java.vendor, java.version].filter(Boolean).join(" ") : null,
    container?.docker ? "Docker" : null,
    container?.wsl ? "WSL" : null,
    container?.rosetta ? "Rosetta" : null,
    dependencies === undefined ? null : dependencies.resolution === "lockfile" ? `${dependencies.source} 의존성 ${dependencies.items.length}개` : "잠금 파일 없음",
  ]
    .filter(Boolean)
    .join(" · ");
}
