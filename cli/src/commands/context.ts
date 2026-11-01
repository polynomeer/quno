import type { QunoClient } from "../client.js";
import type { QunoConfig } from "../config.js";
import type { SnapshotV1 } from "../snapshot/schema.js";

/** Everything a command touches, injected so tests can run commands without a terminal or a server. */
export interface CommandContext {
  client: QunoClient;
  config: QunoConfig;
  cwd: string;
  out: (line: string) => void;
  err: (line: string) => void;
  /** Piped standard input, or null when stdin is a terminal. */
  stdin: string | null;
  /** Asks a yes/no question on the terminal; null when there is no terminal to ask on. */
  confirm: ((question: string) => Promise<boolean>) | null;
  /** Reads this machine's Tier 0 environment (ADR-0083); only called when a command needs it. */
  captureSnapshot: () => SnapshotV1;
}
