/** Response shapes of the Quno API the CLI and MCP server read (mirrors frontend/src/features/*\/api). */

export type Ecosystem = "MAVEN" | "GRADLE_PLUGIN" | "NPM";
export type QuestionStatus = "OPEN" | "ANSWERED" | "RESOLVED" | "OUTDATED" | "REOPENED" | string;

export interface StackDependency {
  ecosystem: Ecosystem;
  name: string;
  version: string | null;
}

export interface ApplicabilityRange {
  ecosystem: Ecosystem;
  name: string;
  fromVersion: string | null;
  untilVersion: string | null;
}

export interface QuestionSummary {
  id: number;
  title: string;
  status: QuestionStatus;
  tags: string[];
  score: number;
  answerCount: number;
  hasAcceptedAnswer: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionDetail {
  id: number;
  title: string;
  status: QuestionStatus;
  versionNumber: number;
  body: string;
  environment: string | null;
  logs: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
  score: number;
  dependencies?: StackDependency[];
  environmentSnapshot?: StoredSnapshot | null;
}

export interface Answer {
  id: number;
  body: string;
  isAccepted: boolean;
  isStale: boolean;
  score: number;
  targetVersionNumber: number;
  createdAt: string;
  applicability?: ApplicabilityRange[];
}

export interface ErrorSignature {
  exceptionClass: string;
  normalizedMessage: string;
  signatureHash: string | null;
  messageKey: string;
}

export interface ErrorSignaturePreview {
  signatures: ErrorSignature[];
  questions: QuestionSummary[];
}

export interface ParsedDependencyFile {
  format: "GRADLE" | "MAVEN" | "NPM";
  dependencies: StackDependency[];
}

export interface Me {
  id: number;
  nickname: string;
}

export interface NewQuestion {
  title: string;
  body: string;
  logs?: string;
  tags?: string[];
  dependencies?: StackDependency[];
  /** A `quno.snapshot/1` document (ADR-0083). */
  environmentSnapshot?: unknown;
}

/** The server's view of a stored snapshot: its own fingerprint, provenance, and the document. */
export interface StoredSnapshot {
  source: "COLLECTOR" | "MANUAL";
  collector: string | null;
  fingerprint: string;
  serverRedactions: number;
  document: {
    capturedAt?: string;
    tier0?: {
      os?: { name?: string; version?: string | null; arch?: string };
      runtimes?: { name: string; vendor: string | null; version: string | null }[];
      container?: { docker?: boolean; wsl?: boolean; rosetta?: boolean };
      locale?: { lang?: string | null; encoding?: string | null; timezone?: string | null };
      dependencies?: { resolution?: string; source?: string | null; items?: { name: string; version: string }[] };
    };
  };
}
