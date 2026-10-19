import { z } from 'zod';
import { computeSignature } from './signature.js';

/**
 * 해결 기록(Resolved Record)의 실험용 최소 스키마.
 * 구조화 필드(에이전트가 신뢰 판단에 쓸 수 있는 값)와 자유 텍스트(비신뢰)를 분리한다 — ADR-0069 결정 3.
 */
export const RecordSchema = z.object({
  id: z.string().regex(/^[a-z0-9][a-z0-9-]*$/),
  title: z.string().min(1),
  // 실험 단계의 기록은 모두 미검증이다. 검증 등급(L0~L4)은 Phase 94에서 정의한다.
  verification: z.literal('unverified').default('unverified'),
  // 원 에러의 대표 스택트레이스 또는 에러 한 줄. 시그니처는 로드 시점에 계산한다.
  errorSample: z.string().min(1),
  appliesTo: z
    .array(z.object({ component: z.string(), versions: z.string() }))
    .min(1),
  sources: z
    .array(z.object({ url: z.url(), license: z.string().optional() }))
    .min(1),
  lastConfirmed: z.iso.date(),
  // 아래는 자유 텍스트다. 에이전트에게는 untrustedText로 분리해 전달한다.
  symptom: z.string().min(1),
  cause: z.string().min(1),
  fixSummary: z.string().min(1),
  notes: z.string().optional(),
});

/** 스키마 검증 + 시그니처 계산. 시그니처를 얻지 못하는 기록은 거부한다. */
export function parseRecord(raw) {
  const record = RecordSchema.parse(raw);
  const sig = computeSignature(record.errorSample);
  if (!sig) {
    throw new Error(`record ${record.id}: errorSample에서 예외를 찾지 못했다`);
  }
  return { ...record, sig };
}
