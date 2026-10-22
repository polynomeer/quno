import { appendFileSync, mkdirSync } from 'node:fs';
import { dirname } from 'node:path';

/**
 * 도구 호출 로그(JSONL). 호출률·적중률 측정이 목적이라 조건(QUNO_CONDITION)과 실행(QUNO_RUN_ID)을 함께 남긴다.
 * 질의 원문이 들어가므로 로컬 파일에만 두고 저장소에는 커밋하지 않는다(logs/ 는 gitignore).
 */
export function createCallLog(path, meta = {}) {
  if (!path) return () => {};
  mkdirSync(dirname(path), { recursive: true });
  return (entry) => {
    try {
      appendFileSync(path, JSON.stringify({ ts: new Date().toISOString(), ...meta, ...entry }) + '\n');
    } catch {
      // 로그 실패가 도구 응답을 막으면 실험이 왜곡된다.
    }
  };
}
