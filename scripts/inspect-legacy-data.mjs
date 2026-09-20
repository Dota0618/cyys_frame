import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { resolve } from 'node:path';

// Parse data literals only. Never execute the legacy dump's SQL, DDL or directives.
export function literals(text) {
  const values = [];
  let i = 0;
  while (i < text.length) {
    while (/\s/.test(text[i] ?? '') && i < text.length) i++;
    let value;
    if (text[i] === "'") {
      i++; value = ''; let closed = false;
      while (i < text.length) {
        const c = text[i++];
        if (c === '\\') {
          const next = text[i++];
          const escapes = { '0': '\0', n: '\n', r: '\r', t: '\t', b: '\b', Z: '\x1a', "'": "'", '"': '"', '\\': '\\' };
          if (!(next in escapes)) throw new Error('Unsupported dump escape');
          value += escapes[next];
        } else if (c === "'") {
          if (text[i] === "'") { value += "'"; i++; }
          else { closed = true; break; }
        } else value += c;
      }
      if (!closed) throw new Error('Unclosed dump literal');
    } else {
      const token = text.slice(i).match(/^(NULL|-?\d+(?:\.\d+)?)/)?.[0];
      if (!token) throw new Error('Nonliteral dump data refused');
      value = token === 'NULL' ? null : token; i += token.length;
    }
    values.push(value);
    while (i < text.length && /\s/.test(text[i])) i++;
    if (i === text.length) break;
    if (text[i++] !== ',') throw new Error('Unexpected dump delimiter');
  }
  return values;
}

export function readLegacy(source) {
  const bytes = readFileSync(source), sql = bytes.toString('utf8');
  const tables = {};
  for (const name of ['param_basic', 'param_basic_type', 'param_region', 'sys_configure']) {
    const schema = sql.match(new RegExp('CREATE TABLE `' + name + '`[^\\n]*\\n([\\s\\S]*?)^\\)', 'm'))?.[1];
    if (!schema) throw new Error('Missing selected source schema');
    const columns = [...schema.matchAll(/^\s+`([^`]+)`/gm)].map(m => m[1]);
    const rows = [];
    for (const line of sql.split(/\r?\n/).filter(line => line.startsWith('INSERT INTO `' + name + '`'))) {
      const match = line.match(new RegExp('^INSERT INTO `' + name + '` VALUES \\((.*)\\);$'));
      if (!match) throw new Error('Unsupported insert layout: ' + name);
      const values = literals(match[1]);
      if (values.length !== columns.length) throw new Error('Column count mismatch: ' + name);
      rows.push(Object.fromEntries(columns.map((c, i) => [c, values[i]])));
    }
    tables[name] = rows;
  }
  return { sourceSha256: createHash('sha256').update(bytes).digest('hex'), tables };
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(import.meta.filename)) {
  const data = readLegacy(process.argv[2]);
  // Only names, counts and structural facts; configuration values never enter reports.
  console.log(JSON.stringify({ sourceSha256: data.sourceSha256,
    tables: Object.fromEntries(Object.entries(data.tables).map(([name, rows]) => [name, {
      count: rows.length, deleted: rows.filter(r => r.is_delete !== '0').length,
      nullSort: rows.filter(r => r.sort === null).length,
    }])),
    configurations: data.tables.sys_configure.map(({ name, type, is_delete }) => ({ name, type, deleted: is_delete })),
    dictionaries: data.tables.param_basic.map(({ id, system_name }) => ({ id, systemName: system_name })),
    regionRoots: data.tables.param_region.filter(r => !r.parent_id || r.parent_id === '0').map(({ id, level }) => ({ id, level })),
  }, null, 2));
}
