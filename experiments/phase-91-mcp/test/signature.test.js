import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { computeSignature, computeSignatures, normalizeMessage, normalizeFrame } from '../src/signature.js';

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

test('helpful NPE 메시지는 따옴표 안의 코드 이름을 보존해 서로 구분된다', () => {
  const a = computeSignature('java.lang.NullPointerException: Cannot invoke "java.util.List.size()" because "items" is null');
  const b = computeSignature('java.lang.NullPointerException: Cannot invoke "String.length()" because "name" is null');
  assert.notEqual(a.messageKey, b.messageKey);
});

test('NoClassDefFoundError의 클래스 이름은 경로로 지워지지 않는다', () => {
  const a = computeSignature('java.lang.NoClassDefFoundError: org/springframework/boot/jdbc/autoconfigure/DataSourceProperties');
  const b = computeSignature('java.lang.NoClassDefFoundError: org/springframework/util/StringUtils');
  assert.notEqual(a.messageKey, b.messageKey);
  assert.match(a.normalizedMessage, /datasourceproperties/);
});

test('파일 경로는 계속 정규화된다', () => {
  assert.equal(normalizeMessage('cannot read /home/alice/app/config.yml'), 'cannot read <path>');
});

test('Gradle 로그의 태스크 이름(소문자 시작)을 예외로 착각하지 않는다', () => {
  const log = "> Task :checkKotlinGradlePluginConfigurationErrors SKIPPED\n> Task :test FAILED\nJwtAuthTests > x() FAILED\n    java.lang.AssertionError: Status expected:<200> but was:<401>\n        at org.springframework.test.util.AssertionErrors.fail(AssertionErrors.java:62)";
  const s = computeSignature(log);
  assert.equal(s.exceptionClass, 'java.lang.AssertionError');
});

test('중첩 클래스·단순 이름도 파싱', () => {
  assert.equal(computeSignature('org.x.Outer$Inner$BoomException: bad').exceptionClass, 'org.x.Outer$Inner$BoomException');
  assert.equal(computeSignature('TypeError: x is not a function').exceptionClass, 'TypeError');
});

test('여러 예외 체인이 섞인 로그에서 모두 시그니처로 낸다', () => {
  const log = "WARN x : failed\njava.lang.UnsatisfiedLinkError: no foo in java.library.path\n\tat a.B.c(B.java:1)\n> Task :test FAILED\njava.lang.AssertionError: expected 1 but was 2\n\tat a.D.e(D.java:2)\nCaused by: java.io.IOException: boom\n\tat a.F.g(F.java:3)";
  const sigs = computeSignatures(log);
  assert.deepEqual(sigs.map((s) => s.exceptionClass), ['java.lang.UnsatisfiedLinkError', 'java.io.IOException']);
  assert.equal(computeSignature(log).exceptionClass, 'java.lang.UnsatisfiedLinkError');
});
