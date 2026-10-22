import { createHash } from 'node:crypto';

const TOP_FRAMES = 5;
const HELPFUL_NPE_RE = /^\s*Cannot (?:invoke|read|load|store|assign|throw|enter|exit)\b/;

// 클래스 이름은 대문자로 시작해야 한다(Gradle 태스크 이름 checkKotlin...Error 같은 소문자 시작 토큰을 배제).
const EXCEPTION_RE = /(?<![\w$.])((?:[a-z_][\w$]*\.)*[A-Z][\w$]*(?:Exception|Error|Throwable))(?::\s*(.*))?/;
const JVM_FRAME_RE = /^\s*at\s+([\w$.<>]+)\(([^)]*)\)/;
const NODE_FRAME_RE = /^\s*at\s+(?:async\s+)?([^\s(]+)\s+\(.*\)\s*$/;

/**
 * 메시지의 가변 부분(경로·주소·식별자·숫자·따옴표 문자열)을 지워 같은 에러가 같은 문자열이 되게 한다.
 */
export function normalizeMessage(message) {
  const text = message ?? '';
  // JDK의 "helpful NPE" 메시지는 따옴표 안이 코드 이름(메서드·필드)이라 안정적이고 식별력이 있다 — 지우지 않는다.
  const keepQuoted = HELPFUL_NPE_RE.test(text);
  let out = text
    .replace(/\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/gi, '<uuid>')
    .replace(/\b0x[0-9a-f]+\b/gi, '<hex>')
    .replace(/\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?\b/g, '<ip>')
    // 파일 경로만. "org/springframework/..." 같은 JVM 내부 클래스 이름은 앞이 단어 문자라 걸리지 않는다.
    .replace(/(?<![\w.$])(?:[A-Za-z]:)?(?:[\\/][\w.@~+-]+){2,}/g, '<path>');
  if (!keepQuoted) out = out.replace(/'[^']*'|"[^"]*"|`[^`]*`/g, '<str>');
  return out
    .replace(/\b\d+(?:\.\d+)*\b/g, '<n>')
    .replace(/\s+/g, ' ')
    .trim()
    .toLowerCase();
}

/**
 * 프레임에서 라인 번호·프록시 접미사·람다 번호를 제거한다.
 */
export function normalizeFrame(method) {
  return method
    .replace(/\$\$[A-Za-z]+By[A-Za-z]+\$\$[0-9a-f]+/g, '$$proxy')
    .replace(/\$\$Lambda[\w$/.]*/g, '$$lambda')
    .replace(/lambda\$([\w$]+)\$\d+/g, 'lambda$$$1')
    .replace(/\$\d+/g, '$$n');
}

/**
 * 텍스트의 모든 예외 체인을 읽는다. "Caused by:" 는 앞 체인을 잇고, 그 외에 예외 이름이 나오는
 * 비-프레임 줄은 새 체인을 시작한다. 각 체인의 마지막 블록이 근본 원인이다.
 */
function parseChains(text) {
  const chains = [];
  let current = null;
  for (const line of text.split(/\r?\n/)) {
    const jvm = line.match(JVM_FRAME_RE);
    const node = jvm ? null : line.match(NODE_FRAME_RE);
    if (jvm || node) {
      current?.[current.length - 1].frames.push(normalizeFrame(jvm ? jvm[1] : node[1]));
      continue;
    }
    const causedBy = line.match(/^\s*Caused by:\s*(.*)$/);
    const m = (causedBy ? causedBy[1] : line).match(EXCEPTION_RE);
    if (!m) continue;
    const block = { exceptionClass: m[1], message: m[2] ?? '', frames: [] };
    if (causedBy && current) {
      current.push(block);
    } else {
      current = [block];
      chains.push(current);
    }
  }
  return chains;
}

const sha = (s) => createHash('sha256').update(s).digest('hex').slice(0, 16);

function toSignature(blocks) {
  const root = blocks[blocks.length - 1];
  const message = normalizeMessage(root.message);
  const frames = root.frames.slice(0, TOP_FRAMES);
  return {
    exceptionClass: root.exceptionClass,
    normalizedMessage: message,
    frames,
    messageKey: sha(`${root.exceptionClass}|${message}`),
    signature: frames.length ? sha(`${root.exceptionClass}|${frames.join('|')}`) : null,
    chain: blocks.map((b) => b.exceptionClass),
  };
}

/**
 * 텍스트에 들어 있는 모든 예외 체인의 시그니처(근본 원인 기준). 같은 체인은 한 번만 낸다.
 * 사용자가 빌드·실행 로그를 통째로 붙이면 무관한 경고 트레이스가 섞이므로 검색은 모두 시도한다.
 */
export function computeSignatures(text) {
  if (!text || typeof text !== 'string') return [];
  const seen = new Set();
  const out = [];
  for (const blocks of parseChains(text)) {
    const sig = toSignature(blocks);
    const key = sig.messageKey + '|' + (sig.signature ?? '');
    if (seen.has(key)) continue;
    seen.add(key);
    out.push(sig);
  }
  return out;
}

/**
 * 첫 번째 체인의 시그니처. 예외를 찾지 못하면 null.
 * - signature: 근본 원인의 예외 클래스 + 정규화한 상위 N개 프레임
 * - messageKey: 근본 원인의 예외 클래스 + 정규화한 메시지
 */
export function computeSignature(text) {
  return computeSignatures(text)[0] ?? null;
}
