import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, mkdtempSync, existsSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js';

const fx = (n) => readFileSync(new URL(`./fixtures/${n}`, import.meta.url), 'utf8');

test('MCP stdio: 도구 2개 노출, 검색·조회 동작, 호출 로그 기록', async () => {
  const logPath = join(mkdtempSync(join(tmpdir(), 'qlog-')), 'calls.jsonl');
  const transport = new StdioClientTransport({
    command: process.execPath,
    args: [fileURLToPath(new URL('../src/server.js', import.meta.url))],
    env: {
      ...process.env,
      QUNO_SEED_DIR: fileURLToPath(new URL('./fixtures', import.meta.url)),
      QUNO_CALL_LOG: logPath,
      QUNO_CONDITION: 'A1',
      QUNO_RUN_ID: 'test-run',
    },
  });
  const client = new Client({ name: 'test', version: '0' });
  await client.connect(transport);
  try {
    const { tools } = await client.listTools();
    assert.deepEqual(tools.map((t) => t.name).sort(), ['get_resolution', 'search_known_issues']);
    assert.ok(tools.every((t) => t.annotations?.readOnlyHint === true));

    const res = await client.callTool({ name: 'search_known_issues', arguments: { errorText: fx('trace-a-variant.txt') } });
    assert.equal(res.structuredContent.results[0].id, 'fixture-missing-bean');

    const got = await client.callTool({ name: 'get_resolution', arguments: { id: 'fixture-conn-refused' } });
    assert.equal(got.structuredContent.found, true);
  } finally {
    await client.close();
  }
  assert.ok(existsSync(logPath));
  const lines = readFileSync(logPath, 'utf8').trim().split('\n').map((l) => JSON.parse(l));
  assert.equal(lines.length, 2);
  assert.equal(lines[0].condition, 'A1');
  assert.equal(lines[0].runId, 'test-run');
  assert.deepEqual(lines[0].resultIds, ['fixture-missing-bean']);
});
