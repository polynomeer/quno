import { createHash } from 'node:crypto';

const TOP_FRAMES = 5;

const EXCEPTION_RE = /([A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*)*(?:Exception|Error|Throwable))(?::\s*(.*))?/;
const JVM_FRAME_RE = /^\s*at\s+([\w$.<>]+)\(([^)]*)\)/;
const NODE_FRAME_RE = /^\s*at\s+(?:async\s+)?([^\s(]+)\s+\(.*\)\s*$/;

/**
 * 메시지의 가변 부분(경로·주소·식별자·숫자·따옴표 문자열)을 지워 같은 에러가 같은 문자열이 되게 한다.
 */
export function normalizeMessage(message) {
  return (message ?? '')
    .replace(/\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b/gi, '<uuid>')
    .replace(/\b0x[0-9a-f]+\b/gi, '<hex>')
    .replace(/\b\d{1,3}(?:\.\d{1,3}){3}(?::\d+)?\b/g, '<ip>')
    .replace(/(?:[A-Za-z]:)?(?:[\\/][\w.@~+-]+){2,}/g, '<path>')
    .replace(/'[^']*'|"[^"]*"|`[^`]*`/g, '<str>')
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

function parseBlocks(text) {
  // "Caused by:" 로 나뉜 블록들. 마지막 블록이 근본 원인이다.
  const lines = text.split(/\r?\n/);
  const blocks = [];
  let current = null;
  for (const line of lines) {
    const jvm = line.match(JVM_FRAME_RE);
    const node = jvm ? null : line.match(NODE_FRAME_RE);
    if (jvm || node) {
      current?.frames.push(normalizeFrame(jvm ? jvm[1] : node[1]));
      continue;
    }
    const causedBy = line.match(/^\s*Caused by:\s*(.*)$/);
    if (causedBy || current === null) {
      const m = (causedBy ? causedBy[1] : line).match(EXCEPTION_RE);
      if (m) {
        current = { exceptionClass: m[1], message: m[2] ?? '', frames: [] };
        blocks.push(current);
      }
    }
  }
  return blocks;
}

const sha = (s) => createHash('sha256').update(s).digest('hex').slice(0, 16);

/**
 * 스택트레이스(또는 에러 한 줄)를 시그니처로 정규화한다.
 * - signature: 근본 원인의 예외 클래스 + 정규화한 상위 N개 프레임
 * - messageKey: 근본 원인의 예외 클래스 + 정규화한 메시지
 * 예외를 찾지 못하면 null.
 */
export function computeSignature(text) {
  if (!text || typeof text !== 'string') return null;
  const blocks = parseBlocks(text);
  if (blocks.length === 0) return null;
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
