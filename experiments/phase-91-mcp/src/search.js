import { computeSignatures } from './signature.js';

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
export function search(records, query, { limit = 3, minScore = 0.35 } = {}) {
  const qs = computeSignatures(query);
  if (qs.length === 0) return { query: null, queries: [], matches: [] };
  const matches = [];
  for (const r of records) {
    let best = null;
    for (const q of qs) {
      const cand = scoreOne(r, q);
      if (cand && (!best || cand.score > best.score)) best = cand;
    }
    if (best && best.score >= minScore) matches.push({ record: r, score: Number(best.score.toFixed(2)), matchType: best.matchType });
  }
  matches.sort((a, b) => b.score - a.score || a.record.id.localeCompare(b.record.id));
  return { query: qs[0], queries: qs, matches: matches.slice(0, limit) };
}

function scoreOne(r, q) {
  const qTokens = tokens(q.normalizedMessage);
  if (q.signature && r.sig.signature === q.signature) return { score: 1, matchType: 'exact-signature' };
  if (r.sig.messageKey === q.messageKey) return { score: 0.85, matchType: 'same-message' };
  if (r.sig.exceptionClass === q.exceptionClass) {
    const sim = jaccard(qTokens, tokens(r.sig.normalizedMessage));
    if (sim === 0) return null;
    return { score: 0.3 + 0.4 * sim, matchType: 'similar-message' };
  }
  return null;
}
