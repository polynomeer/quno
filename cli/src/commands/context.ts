import type { QunoClient } from "../client.js";
import type { QunoConfig } from "../config.js";

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
}
