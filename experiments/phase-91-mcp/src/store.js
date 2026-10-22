import { readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import { parseRecord } from './record.js';

/** 디렉터리의 *.json 파일(객체 하나 또는 객체 배열)을 모두 읽어 기록 목록을 만든다. */
export function loadRecords(dir) {
  const records = [];
  const errors = [];
  for (const name of readdirSync(dir).filter((f) => f.endsWith('.json')).sort()) {
    const parsed = JSON.parse(readFileSync(join(dir, name), 'utf8'));
    for (const raw of Array.isArray(parsed) ? parsed : [parsed]) {
      try {
        records.push(parseRecord(raw));
      } catch (e) {
        errors.push({ file: name, id: raw?.id, message: e.message });
      }
    }
  }
  const seen = new Set();
  for (const r of records) {
    if (seen.has(r.id)) errors.push({ file: '-', id: r.id, message: '중복 id' });
    seen.add(r.id);
  }
  return { records, errors };
}
