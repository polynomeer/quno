import type { Probe } from "../src/snapshot/collect.js";

/** A machine that exists only in memory: [files] by absolute path, [commands] by `cmd arg…`. */
export function fakeProbe(overrides: Partial<Probe> = {}, files: Record<string, string> = {}, commands: Record<string, string> = {}, mtimes: Record<string, number> = {}): Probe {
  return {
    cwd: "/Users/minji/work/app",
    platform: "darwin",
    arch: "arm64",
    run: (command, args) => commands[[command, ...args].join(" ")] ?? null,
    read: (path) => files[path] ?? null,
    exists: (path) => path in files,
    list: (dir) => {
      const prefix = dir.endsWith("/") ? dir : `${dir}/`;
      return Object.keys(files)
        .filter((path) => path.startsWith(prefix) && !path.slice(prefix.length).includes("/"))
        .map((path) => path.slice(prefix.length));
    },
    mtimeMs: (path) => (path in files ? (mtimes[path] ?? Date.now()) : null),
    system: () => ({ totalMemBytes: 16 * 1024 ** 3, freeMemBytes: 2 * 1024 ** 3, cpus: 10, diskFreeBytes: 100 * 1024 ** 3 }),
    env: {},
    home: "/Users/minji",
    user: "minji",
    host: "minji-mbp.corp.example",
    kernel: "24.1.0",
    nodeVersion: "v22.11.0",
    intl: { locale: "ko-KR", timezone: "Asia/Seoul" },
    ...overrides,
  };
}
