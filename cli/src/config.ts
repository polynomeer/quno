import { chmodSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { homedir } from "node:os";
import { dirname, join } from "node:path";

export const DEFAULT_API_URL = "http://localhost:8081";
export const DEFAULT_WEB_URL = "http://localhost:3000";

export interface QunoConfig {
  apiUrl: string;
  /** Where links to questions point — the web app, not the API. */
  webUrl: string;
  token: string | null;
}

interface StoredConfig {
  apiUrl?: string;
  webUrl?: string;
  token?: string;
}

export function configPath(env: NodeJS.ProcessEnv = process.env): string {
  const base = env.XDG_CONFIG_HOME ?? join(homedir(), ".config");
  return join(base, "quno", "config.json");
}

function readStored(path: string): StoredConfig {
  try {
    return JSON.parse(readFileSync(path, "utf8")) as StoredConfig;
  } catch {
    return {};
  }
}

/** Environment first, then the config file, then the default (ADR-0080). */
export function loadConfig(env: NodeJS.ProcessEnv = process.env): QunoConfig {
  const stored = readStored(configPath(env));
  return {
    apiUrl: (env.QUNO_API_URL ?? stored.apiUrl ?? DEFAULT_API_URL).replace(/\/+$/, ""),
    webUrl: (env.QUNO_WEB_URL ?? stored.webUrl ?? DEFAULT_WEB_URL).replace(/\/+$/, ""),
    token: env.QUNO_TOKEN ?? stored.token ?? null,
  };
}

/** Writes the file readable by its owner only — it holds a token. */
export function saveConfig(update: StoredConfig, env: NodeJS.ProcessEnv = process.env): string {
  const path = configPath(env);
  const merged = { ...readStored(path), ...update };
  mkdirSync(dirname(path), { recursive: true, mode: 0o700 });
  writeFileSync(path, JSON.stringify(merged, null, 2) + "\n", { mode: 0o600 });
  chmodSync(path, 0o600);
  return path;
}
