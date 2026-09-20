import { readLegacy } from './inspect-legacy-data.mjs';
import { mkdirSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { createHash } from 'node:crypto';

const [source, output] = process.argv.slice(2);
if (!source || !output) throw new Error('Usage: source.sql output-directory');
const { sourceSha256, tables } = readLegacy(source);
const now = new Date().toISOString().slice(0, 19).replace('T', ' ');
const actor = 'legacy-basic-import';
const ordinaryConfiguration = new Set(['SYSTEM_NAME', 'SYSTEM_PROVINCE_CODE', 'ONLY_DEFAULT_PROVINCE']);
// These are generator-specific options; that feature is explicitly paused.
const pausedDictionaryIds = new Set(['21','22','23','24','25','26','27']);
const active = name => tables[name].filter(r => r.is_delete === '0');
const integer = v => { if (v === null) return '0'; if (!/^-?\d+$/.test(v) || !Number.isSafeInteger(Number(v)) || Number(v)<-2147483648 || Number(v)>2147483647) throw new Error('Invalid integer'); return v; };
const text = (v, max, required = false) => {
  if (v === null) { if (required) throw new Error('Missing required text'); return null; }
  if ([...v].length > max || (required && !v.trim()) || v.includes('\0')) throw new Error('Invalid text');
  return v;
};
const date = v => { if (!v) return now; if (!/^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d$/.test(v) || !Number.isFinite(Date.parse(v.replace(' ', 'T')+'Z'))) throw new Error('Invalid date'); return v; };
const coordinate = (v, limit) => { if (!v) return null; if (!/^-?\d+(\.\d{1,15})?$/.test(v) || Math.abs(Number(v))>limit) throw new Error('Invalid coordinate'); return v; };
const meta = r => [actor, date(r.date_create), actor, date(r.date_update)];
const result = {};
result.param_basic = active('param_basic').filter(r => !pausedDictionaryIds.has(r.id)).map(r => {
  if (!/^\d{1,32}$/.test(r.id)) throw new Error('Dictionary ID incompatible');
  return [r.id, text(r.name,20,true), text(r.system_name,255,true), integer(r.sort), ...meta(r)];
});
const basicIds = new Set(result.param_basic.map(r => r[0]));
result.param_basic_type = active('param_basic_type').filter(r => !pausedDictionaryIds.has(r.basic_id)).map(r => {
  if (!basicIds.has(r.basic_id)) throw new Error('Active dictionary item has missing parent');
  return [text(r.id,32,true), r.basic_id, text(r.param_key,20,true), text(r.param_value,20,true), text(r.desp,200), integer(r.sort), ...meta(r)];
});
const regions = active('param_region'), byId = new Map(regions.map(r => [r.id,r]));
for (const row of regions) {
  const visited = new Set(); let current = row;
  while (current.parent_id && current.parent_id !== '0') {
    if (visited.has(current.id)) throw new Error('Region cycle'); visited.add(current.id);
    const parent = byId.get(current.parent_id);
    if (!parent || Number(current.level) !== Number(parent.level)+1) throw new Error('Region hierarchy incompatible');
    current = parent;
  }
  if (current.level !== '0') throw new Error('Invalid region root level');
}
result.param_region = regions.map(r => [text(r.id,32,true), !r.parent_id || r.parent_id==='0'?null:r.parent_id,
  text(r.city_code,20,true),text(r.name,20,true),text(r.short_name,20),text(r.zip_code,6),coordinate(r.longitude,180),coordinate(r.latitude,90),integer(r.level),integer(r.sort),...meta(r)]);
result.sys_configure = active('sys_configure').filter(r=>ordinaryConfiguration.has(r.name)).map(r => {
  if(r.type!=='1') throw new Error('Only ordinary configuration accepted');
  return [text(r.id,32,true),r.name,text(r.value,100),'1',text(r.desp,255),integer(r.sort),...meta(r)];
});
for (const [name,rows] of Object.entries(result)) {
  const keys = name==='param_basic_type' ? [r=>r[0], r=>r[1]+'\0'+r[2]] : [r=>r[0],r=>r[name==='param_region'?2:name==='param_basic'?2:1]];
  for(const key of keys) if(new Set(rows.map(r=>key(r).normalize('NFKD').replace(/\p{M}/gu,'').toLowerCase())).size!==rows.length) throw new Error('Duplicate key: '+name);
}
const payload = Object.entries(result).flatMap(([table,rows])=>rows.map(row=>[table,...row.map(v=>v===null?'~':Buffer.from(v).toString('base64'))].join('\t'))).join('\n')+'\n';
const report={sourceSha256,dataSha256:createHash('sha256').update(payload).digest('hex'),preparedAt:now,
  target:'existing localhost MySQL cyys_frame', counts:Object.fromEntries(Object.entries(result).map(([n,r])=>[n,r.length])),
  configurationNames:[...ordinaryConfiguration], excludedDeleted: Object.fromEntries(Object.entries(tables).map(([n,r])=>[n,r.filter(v=>v.is_delete!=='0').length])),
  excludedPausedDictionaries:tables.param_basic.filter(r=>pausedDictionaryIds.has(r.id)).map(r=>({id:r.id,name:r.system_name})),
  excludedConfigurations:tables.sys_configure.filter(r=>!ordinaryConfiguration.has(r.name)).map(r=>r.name),
  conversions:['root parent 0/empty -> NULL','null sort -> 0','missing timestamps -> preparedAt; existing timestamps retained','created_by/updated_by -> legacy-basic-import; version/deleted -> 0','coordinates -> exact decimal; IDs and dictionary keys preserved'],
};
mkdirSync(resolve(output),{recursive:true});
writeFileSync(resolve(output,'data.tsv'),payload,{flag:'wx'});
writeFileSync(resolve(output,'plan.json'),JSON.stringify(report,null,2)+'\n',{flag:'wx'});
console.log(JSON.stringify(report,null,2));
