import type { TreeDataNode } from 'antd';

interface FlatTreeItem { id: string; parentId?: string; name: string }

/** 保持服务端顺序，将稳定 ID/父 ID 适配为受控 Tree 数据。 */
export function roleTreeData<T extends FlatTreeItem>(items: T[]): TreeDataNode[] {
  const nodes = new Map<string, TreeDataNode>();
  for (const item of items) nodes.set(item.id, { key: item.id, title: item.name });
  const roots: TreeDataNode[] = [];
  for (const item of items) {
    const node = nodes.get(item.id)!;
    const parent = item.parentId && nodes.get(item.parentId);
    if (parent && parent !== node) (parent.children ||= []).push(node);
    else roots.push(node);
  }
  return roots;
}
