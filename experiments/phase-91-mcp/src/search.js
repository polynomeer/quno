import { computeSignature } from './signature.js';

const tokens = (s) => new Set(s.split(/[^a-z0-9<>]+/).filter((t) => t.length > 2 && !/^<.*>$/.test(t)));

function jaccard(a, b) {
  if (a.size === 0 || b.size === 0) return 0;
  let inter = 0;
  for (const t of a) if (b.has(t)) inter++;
  return inter / (a.size + b.size - inter);
}

/**
 * 질의 텍스트(스택트레이스 또는 에러 메시지)와 기록을 비교한다.
 * - exact-signature: 근본 원인의 예외 클래스 + 상위 프레임이 같다 (1.0)
 * - same-message: 예외 클래스와 정규화한 메시지가 같다 (0.85)
 * - similar-message: 같은 예외 클래스이고 메시지 토큰이 겹친다 (0.3~0.7)
 * 점수가 minScore 미만이면 결과에서 제외한다 — 억지로 채우지 않는 것이 적중률 측정의 정직성이다.
 */
export function search(records, query, { limit = 3, minScore = 0.3 } = {}) {
  const q = computeSignature(query);
  if (!q) return { query: null, matches: [] };
  const qTokens = tokens(q.normalizedMessage);
  const matches = [];
  for (const r of records) {
    let score = 0;
    let matchType = null;
    if (q.signature && r.sig.signature === q.signature) {
      score = 1;
      matchType = 'exact-signature';
    } else if (r.sig.messageKey === q.messageKey) {
      score = 0.85;
      matchType = 'same-message';
    } else if (r.sig.exceptionClass === q.exceptionClass) {
      const sim = jaccard(qTokens, tokens(r.sig.normalizedMessage));
      score = 0.3 + 0.4 * sim;
      matchType = 'similar-message';
      if (sim === 0) score = 0;
    }
    if (score >= minScore) matches.push({ record: r, score: Number(score.toFixed(2)), matchType });
  }
  matches.sort((a, b) => b.score - a.score || a.record.id.localeCompare(b.record.id));
  return { query: q, matches: matches.slice(0, limit) };
}
