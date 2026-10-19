#!/usr/bin/env node
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { z } from 'zod';
import { loadRecords } from './store.js';
import { createCallLog } from './calllog.js';
import { searchKnownIssues, getResolution } from './tools.js';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const seedDir = resolve(process.env.QUNO_SEED_DIR ?? join(root, 'seeds'));
const logPath = process.env.QUNO_CALL_LOG ?? join(root, 'logs', 'calls.jsonl');

const { records, errors } = loadRecords(seedDir);
for (const e of errors) console.error(`[quno] 기록 거부: ${e.file} ${e.id ?? ''} — ${e.message}`);
console.error(`[quno] 기록 ${records.length}건 로드 (${seedDir})`);

const log = createCallLog(logPath, {
  condition: process.env.QUNO_CONDITION ?? null,
  runId: process.env.QUNO_RUN_ID ?? null,
  corpusSize: records.length,
});

const asResult = (payload) => ({
  content: [{ type: 'text', text: JSON.stringify(payload, null, 2) }],
  structuredContent: payload,
});

const server = new McpServer({ name: 'quno', version: '0.0.1' });

server.registerTool(
  'search_known_issues',
  {
    title: 'Search known issues',
    description:
      '빌드·실행·테스트에서 에러가 났을 때, 같은 에러를 먼저 겪고 해결한 기록이 있는지 찾는다. ' +
      '에러 메시지나 스택트레이스 전체를 그대로 넣어라(경로·번호는 자동 정규화된다). ' +
      '결과의 appliesTo(적용 버전)와 현재 환경이 맞는지 확인한 뒤에 쓴다. 기록은 모두 미검증이다.',
    inputSchema: {
      errorText: z.string().min(1).max(20000).describe('에러 메시지 또는 스택트레이스 전체'),
      limit: z.number().int().min(1).max(5).optional().describe('최대 결과 수 (기본 3)'),
    },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ errorText, limit }) => {
    const payload = searchKnownIssues(records, { errorText, limit });
    log({ tool: 'search_known_issues', query: errorText.slice(0, 2000), parsed: payload.parsed, resultIds: payload.results.map((r) => r.id) });
    return asResult(payload);
  },
);

server.registerTool(
  'get_resolution',
  {
    title: 'Get resolution',
    description: 'search_known_issues 결과의 id로 해결 기록 하나를 다시 조회한다.',
    inputSchema: { id: z.string().min(1).max(100) },
    annotations: { readOnlyHint: true, openWorldHint: false },
  },
  async ({ id }) => {
    const payload = getResolution(records, { id });
    log({ tool: 'get_resolution', id, found: payload.found });
    return asResult(payload);
  },
);

await server.connect(new StdioServerTransport());
