import type { QunoClient } from "../client.js";
import type { QunoConfig } from "../config.js";
import type { Preset, SnapshotV1 } from "../snapshot/schema.js";
import type { FrozenFailure } from "../snapshot/state.js";

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
  /** Reads this machine's environment (ADR-0083/0084); only called when a command needs it. */
  captureSnapshot: (options?: { preset?: Preset; omit?: string[] }) => SnapshotV1;
  /** The failure `quno run` froze in this project, if any (ADR-0084). */
  lastFailure: () => FrozenFailure | null;
}
