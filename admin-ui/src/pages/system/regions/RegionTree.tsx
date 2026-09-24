import { Button, Tree } from 'antd';
import type { TreeDataNode } from 'antd';
import { useModel } from '@umijs/max';
import { useEffect, useRef, useState } from 'react';
import { listRegions } from '@/services/system/regions';
import type { Region } from '@/services/system/regions';

interface Branch { rows: Region[]; page: number; total: number }
export default function RegionTree({ trail, onSelect, onError }: {
  trail: Region[]; onSelect: (trail: Region[]) => void; onError: (error: unknown) => void;
}) {
  const { initialState } = useModel('@@initialState');
  const provinceCode = initialState?.publicConfigurations?.SYSTEM_PROVINCE_CODE?.trim();
  const [branches, setBranches] = useState<Record<string, Branch>>({});
  const cache = useRef<Record<string, Branch>>({});
  const pending = useRef(new Map<string, Promise<void>>());
  const active = useRef(true);
  const [expanded, setExpanded] = useState<React.Key[]>(['root']);
  const load = (parent?: string, page = 1): Promise<void> => {
    const key = parent || 'root';
    if (pending.current.has(key)) return pending.current.get(key)!;
    if ((cache.current[key]?.page || 0) >= page) return Promise.resolve();
    const task = listRegions(page, 200, parent).then(result => {
      if (!active.current) return;
      const rows = page === 1 ? result.records : [...(cache.current[key]?.rows || []), ...result.records];
      cache.current = { ...cache.current, [key]: { rows, page, total: result.total } };
      setBranches(cache.current);
    }).catch(error => { if (active.current) onError(error); throw error; }).finally(() => pending.current.delete(key));
    pending.current.set(key, task);
    return task;
  };
  useEffect(() => {
    active.current = true;
    void load().catch(() => {});
    return () => { active.current = false; };
  }, []);
  const configuredRoot = provinceCode && branches.root?.rows.length === 1
    ? branches.root.rows[0] : undefined;
  useEffect(() => {
    if (configuredRoot) void load(configuredRoot.id).catch(() => {});
  }, [configuredRoot?.id]);
  const trailIds = trail.map(item => item.id).join(',');
  useEffect(() => {
    setExpanded(keys => [...new Set(['root', configuredRoot?.id, ...keys, ...trail.map(item => item.id)].filter(Boolean))] as React.Key[]);
    void (async () => { for (const row of trail) await load(row.id); })().catch(() => {});
  }, [trailIds, configuredRoot?.id]);
  const regionById = new Map(Object.values(branches).flatMap(branch => branch.rows.map(row => [row.id, row] as const)));
  const choose = (id: string) => {
    if (id === 'root') { onSelect([]); return; }
    const result: Region[] = [], seen = new Set<string>();
    let row = regionById.get(id);
    while (row && !seen.has(row.id)) { seen.add(row.id); result.unshift(row); row = row.parentId ? regionById.get(row.parentId) : undefined; }
    onSelect(result);
  };
  const children = (parent: string, seen: Set<string>): TreeDataNode[] | undefined => {
    const branch = branches[parent];
    if (!branch) return undefined;
    const nodes: TreeDataNode[] = branch.rows.filter(row => !seen.has(row.id)).map(row => ({
      key: row.id, title: row.name, isLeaf: !row.hasChildren,
      children: children(row.id, new Set([...seen, row.id])),
    }));
    if (branch.rows.length < branch.total) nodes.push({ key: `more:${parent}`, selectable: false, isLeaf: true,
      title: <Button size="small" type="link" onClick={event => { event.stopPropagation(); void load(parent === 'root' ? undefined : parent, branch.page + 1).catch(() => {}); }}>加载更多地区</Button>,
    });
    return nodes;
  };
  const rootNode: TreeDataNode = configuredRoot
    ? { key: configuredRoot.id, title: configuredRoot.name, isLeaf: !configuredRoot.hasChildren,
        children: children(configuredRoot.id, new Set([configuredRoot.id])) }
    : { key: 'root', title: '全部地区', children: children('root', new Set()) };
  return <Tree aria-label="地区树" blockNode showLine expandedKeys={expanded}
    selectedKeys={[trail.at(-1)?.id || configuredRoot?.id || 'root']} onExpand={keys => setExpanded(keys)}
    loadedKeys={Object.keys(branches)} loadData={node => load(node.key === 'root' ? undefined : String(node.key))}
    onSelect={keys => { if (keys[0]) choose(String(keys[0])); }}
    treeData={[rootNode]} />;
}
