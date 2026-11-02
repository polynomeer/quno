/**
 * The collector's copy of the server's SecretDetector rules (ADR-0081): free text that the
 * collector picks up — command lines, log tails, test failure messages — is scrubbed here, on the
 * user's machine, before anything is shown or sent (ADR-0084). The server scrubs again; both run
 * the shared leak corpus in testdata/secret-leak-corpus.json, so the two copies can't drift apart
 * unnoticed.
 */
export const REDACTED = "[REDACTED]";

interface Rule {
  regex: RegExp;
  /** Capture group holding the secret; 0 = the whole match. */
  group?: number;
  accept?: (text: string, start: number, end: number) => boolean;
}

export function entropy(value: string): number {
  if (!value) return 0;
  const counts = new Map<string, number>();
  for (const char of value) counts.set(char, (counts.get(char) ?? 0) + 1);
  let bits = 0;
  for (const count of counts.values()) {
    const p = count / value.length;
    bits -= p * Math.log2(p);
  }
  return bits;
}

const NETWORK_WORDS = /\b(ip|host|hostname|addr|address|server|connect(?:ed|ing|ion)?|refused|endpoint|remote|peer|client)\W*(?:(?:to|from|on|at)\W*)?$/i;

function inNetworkContext(text: string, start: number, end: number): boolean {
  if (/^:\d{1,5}\b/.test(text.slice(end, end + 6))) return true;
  const before = text.slice(Math.max(0, start - 24), start);
  return before.endsWith("//") || before.endsWith("@") || NETWORK_WORDS.test(before);
}

function isPublicIpv4(text: string): boolean {
  const octets = text.split(".").map(Number);
  if (octets.length !== 4 || octets.some((octet) => !Number.isInteger(octet) || octet < 0 || octet > 255)) return false;
  const [a, b, c] = octets as [number, number, number, number];
  return !(
    a === 0 ||
    a === 10 ||
    a === 127 ||
    a >= 224 ||
    (a === 100 && b >= 64 && b <= 127) ||
    (a === 169 && b === 254) ||
    (a === 172 && b >= 16 && b <= 31) ||
    (a === 192 && b === 168) ||
    (a === 192 && b === 0 && (c === 0 || c === 2)) ||
    (a === 198 && (b === 18 || b === 19)) ||
    (a === 198 && b === 51 && c === 100) ||
    (a === 203 && b === 0 && c === 113)
  );
}

const RULES: Rule[] = [
  { regex: /-----BEGIN [A-Z ]*PRIVATE KEY-----[\s\S]*?-----END [A-Z ]*PRIVATE KEY-----/g },
  { regex: /\b(?:AKIA|ASIA)[0-9A-Z]{16}\b/g },
  { regex: /\b(?:gh[pousr]_[A-Za-z0-9]{36,}|github_pat_[A-Za-z0-9_]{60,})\b/g },
  { regex: /\bxox[abposr]-[A-Za-z0-9-]{10,}/g },
  { regex: /\b(?:sk|rk)_live_[A-Za-z0-9]{16,}\b/g },
  { regex: /\bAIza[0-9A-Za-z_-]{35}\b/g },
  { regex: /\bquno_pat_[A-Za-z0-9_-]{20,}/g },
  { regex: /\beyJ[A-Za-z0-9_-]{8,}\.eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}/g },
  { regex: /\b[a-zA-Z][a-zA-Z0-9+.-]*:\/\/[^\s:/@]+:([^\s@/]{3,})@/dg, group: 1 },
  {
    regex: /\b[\w.-]*(?:password|passwd|pwd|secret|token|api[_-]?key|access[_-]?key|client[_-]?secret)["']?\s*[:=]\s*["']?([^\s"',;]{8,})/dgi,
    group: 1,
    accept: (text, start, end) => {
      const value = text.slice(start, end);
      return !value.startsWith("${") && !value.startsWith("<") && value !== REDACTED && entropy(value) >= 3.0;
    },
  },
  {
    regex: /(?<![\w.:-])(?:\d{1,3}\.){3}\d{1,3}(?![\w-]|\.\d)/g,
    accept: (text, start, end) => isPublicIpv4(text.slice(start, end)) && inNetworkContext(text, start, end),
  },
];

/** Replaces every secret-looking span in [text] with [REDACTED]; returns how many it replaced. */
export function redactSecrets(text: string): { text: string; count: number } {
  const spans: [number, number][] = [];
  for (const rule of RULES) {
    for (const match of text.matchAll(rule.regex)) {
      const range = rule.group ? match.indices?.[rule.group] : [match.index, match.index + match[0].length];
      if (!range) continue;
      const [start, end] = range as [number, number];
      if (rule.accept && !rule.accept(text, start, end)) continue;
      if (spans.some(([s, e]) => s < end && start < e)) continue;
      spans.push([start, end]);
    }
  }
  if (spans.length === 0) return { text, count: 0 };
  let out = text;
  for (const [start, end] of spans.sort(([a], [b]) => b - a)) out = out.slice(0, start) + REDACTED + out.slice(end);
  return { text: out, count: spans.length };
}

/** Redacts every string inside [value]. */
export function redactDeep<T>(value: T): T {
  const walk = (node: unknown): unknown => {
    if (typeof node === "string") return redactSecrets(node).text;
    if (Array.isArray(node)) return node.map(walk);
    if (node && typeof node === "object") return Object.fromEntries(Object.entries(node).map(([key, child]) => [key, walk(child)]));
    return node;
  };
  return walk(value) as T;
}
