import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { computeSignature, normalizeMessage, normalizeFrame } from '../src/signature.js';

const fx = (n) => readFileSync(new URL(`./fixtures/${n}`, import.meta.url), 'utf8');

test('근본 원인(마지막 Caused by)을 시그니처 기준으로 삼는다', () => {
  const s = computeSignature(fx('trace-a.txt'));
  assert.equal(s.exceptionClass, 'org.springframework.beans.factory.NoSuchBeanDefinitionException');
  assert.equal(s.chain.length, 2);
});

test('경로·라인 번호·프록시 해시·람다 번호·따옴표 이름이 달라도 같은 시그니처', () => {
  const a = computeSignature(fx('trace-a.txt'));
  const b = computeSignature(fx('trace-a-variant.txt'));
  assert.equal(a.signature, b.signature);
  assert.equal(a.messageKey, b.messageKey);
});

test('다른 에러는 다른 시그니처', () => {
  const a = computeSignature(fx('trace-a.txt'));
  const c = computeSignature(fx('trace-b.txt'));
  assert.notEqual(a.signature, c.signature);
  assert.notEqual(a.messageKey, c.messageKey);
  assert.equal(c.exceptionClass, 'java.net.ConnectException');
});

test('메시지 정규화: IP·포트·경로·uuid·숫자', () => {
  const m = normalizeMessage("Failed at /var/app/x/y.txt 10.0.0.5:8080 id=3f2b8a52-aaaa-bbbb-cccc-1234567890ab 'name' 42");
  assert.equal(m, 'failed at <path> <ip> id=<uuid> <str> <n>');
});

test('프레임 정규화', () => {
  assert.equal(normalizeFrame('a.B$$FastClassBySpringCGLIB$$9fa31c2e.invoke'), 'a.B$proxy.invoke');
  assert.equal(normalizeFrame('a.B.lambda$run$12'), 'a.B.lambda$run');
});

test('예외가 없으면 null', () => {
  assert.equal(computeSignature('그냥 문장'), null);
  assert.equal(computeSignature(''), null);
  assert.equal(computeSignature(undefined), null);
});

test('Node 스타일 에러도 파싱', () => {
  const s = computeSignature("TypeError: Cannot read properties of undefined (reading 'x')\n    at foo (/app/a.js:10:5)\n    at bar (/app/b.js:2:1)");
  assert.equal(s.exceptionClass, 'TypeError');
  assert.deepEqual(s.frames, ['foo', 'bar']);
});
