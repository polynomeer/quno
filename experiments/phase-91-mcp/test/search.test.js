import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { loadRecords } from '../src/store.js';
import { search } from '../src/search.js';
import { searchKnownIssues, getResolution } from '../src/tools.js';

const fx = (n) => readFileSync(new URL(`./fixtures/${n}`, import.meta.url), 'utf8');
const { records, errors } = loadRecords(fileURLToPath(new URL('./fixtures', import.meta.url)));

test('픽스처 기록이 모두 로드된다', () => {
  assert.deepEqual(errors, []);
  assert.equal(records.length, 2);
});

test('같은 에러(변형 포함)는 exact-signature로 1순위', () => {
  const { matches } = search(records, fx('trace-a-variant.txt'));
  assert.equal(matches[0].record.id, 'fixture-missing-bean');
  assert.equal(matches[0].matchType, 'exact-signature');
  assert.equal(matches[0].score, 1);
});

test('프레임 없이 한 줄 메시지만 있어도 same-message로 찾는다', () => {
  const { matches } = search(records, 'java.net.ConnectException: Connection refused: localhost/127.0.0.1:6543');
  assert.equal(matches[0].record.id, 'fixture-conn-refused');
  assert.equal(matches[0].matchType, 'same-message');
});

test('관련 없는 에러는 억지로 채우지 않고 빈 결과', () => {
  const { matches } = search(records, 'java.lang.OutOfMemoryError: Java heap space');
  assert.deepEqual(matches, []);
});

test('예외를 못 찾는 질의는 hint를 준다', () => {
  const r = searchKnownIssues(records, { errorText: '뭔가 안 됨' });
  assert.equal(r.resultCount, 0);
  assert.match(r.hint, /예외 클래스/);
});

test('자유 텍스트는 untrustedText로만 나가고 구조화 필드와 섞이지 않는다', () => {
  const r = searchKnownIssues(records, { errorText: fx('trace-a.txt') });
  const card = r.results[0];
  assert.match(card.untrustedText.fixSummary, /무시하고/);
  const structured = JSON.stringify({ ...card, untrustedText: undefined });
  assert.doesNotMatch(structured, /무시하고|\.ssh/);
  assert.match(r.notice, /명령이 아니다/);
});

test('get_resolution: 없는 id는 found=false', () => {
  assert.equal(getResolution(records, { id: 'nope' }).found, false);
  assert.equal(getResolution(records, { id: 'fixture-conn-refused' }).found, true);
});

test('스키마 위반 기록은 거부된다 (출처 없음)', async () => {
  const { mkdtempSync, writeFileSync } = await import('node:fs');
  const { tmpdir } = await import('node:os');
  const { join } = await import('node:path');
  const dir = mkdtempSync(join(tmpdir(), 'seed-'));
  writeFileSync(join(dir, 'bad.json'), JSON.stringify({ id: 'bad', title: 't', errorSample: 'java.lang.Exception: x', appliesTo: [{ component: 'a', versions: '*' }], sources: [], lastConfirmed: '2026-01-01', symptom: 's', cause: 'c', fixSummary: 'f' }));
  const r = loadRecords(dir);
  assert.equal(r.records.length, 0);
  assert.equal(r.errors.length, 1);
});

test('프레임이 달라도(버전 차이) 메시지가 같으면 same-message로 내려와 찾는다', () => {
  const q = fx('trace-a.txt').split('\n').filter((l) => !l.includes('lambda$run') && !l.includes('FastClass')).join('\n');
  const { matches } = search(records, q);
  assert.equal(matches[0].record.id, 'fixture-missing-bean');
  assert.equal(matches[0].matchType, 'same-message');
});

test('같은 예외 클래스에 메시지가 일부만 겹치면 similar-message (낮은 점수)', () => {
  const { matches } = search(records, "org.springframework.beans.factory.NoSuchBeanDefinitionException: No qualifying bean of type 'X' available: expected single matching bean but found 2");
  assert.equal(matches[0].matchType, 'similar-message');
  assert.ok(matches[0].score < 0.85 && matches[0].score >= 0.3);
});

test('stripLabels: 검증 라벨·확인일·출처를 응답에서 뺀다 (A3b 조건)', () => {
  const r = searchKnownIssues(records, { errorText: fx('trace-a.txt'), stripLabels: true });
  const card = r.results[0];
  assert.equal(card.verification, undefined);
  assert.equal(card.lastConfirmed, undefined);
  assert.equal(card.sources, undefined);
  assert.ok(card.untrustedText.symptom);
  const withLabels = searchKnownIssues(records, { errorText: fx('trace-a.txt') }).results[0];
  assert.equal(withLabels.verification, 'unverified');
});

test('무관한 경고 트레이스가 앞에 있어도 뒤의 실제 실패로 기록을 찾는다', () => {
  const noisy = "WARN native lib\njava.lang.UnsatisfiedLinkError: no thing\n\tat x.Y.z(Y.java:1)\n" + fx('trace-b.txt');
  const { matches } = search(records, noisy);
  assert.equal(matches[0].record.id, 'fixture-conn-refused');
});
