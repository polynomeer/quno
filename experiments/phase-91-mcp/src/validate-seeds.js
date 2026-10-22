import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';
import { loadRecords } from './store.js';

const dir = resolve(process.argv[2] ?? join(dirname(fileURLToPath(import.meta.url)), '..', 'seeds'));
const { records, errors } = loadRecords(dir);
for (const e of errors) console.error(`✗ ${e.file} ${e.id ?? ''}: ${e.message}`);
console.log(`${records.length}건 통과, ${errors.length}건 거부 (${dir})`);
process.exit(errors.length ? 1 : 0);
