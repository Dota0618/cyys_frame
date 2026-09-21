import type { MenuDataItem } from '@ant-design/pro-components';
import type { NavigationMenu, PlatformMenu } from '@/types/api';

export type MenuRow = PlatformMenu & { children?: MenuRow[] };

/** Shared parent links for menu management, parent selectors and the sidebar. */
export function menuRows(items: PlatformMenu[]): MenuRow[] {
  const nodes = new Map(items.map(item => [item.id, { ...item } as MenuRow]));
  const roots: MenuRow[] = [];
  for (const row of nodes.values()) {
    const seen = new Set([row.id]);
    let parent = nodes.get(row.parentId);
    while (parent) {
      if (seen.has(parent.id)) throw new Error('菜单层级存在循环，请修正父菜单');
      seen.add(parent.id); parent = nodes.get(parent.parentId);
    }
    const direct = nodes.get(row.parentId);
    if (direct) (direct.children ||= []).push(row); else roots.push(row);
  }
  return roots;
}

/** Only locally registered and authorized routes may become navigation links. */
export function navigationTree(nodes: NavigationMenu[], routes: MenuDataItem[], access: Record<string, boolean>): MenuDataItem[] {
  const known = new Map<string, MenuDataItem>();
  const visit = (items: MenuDataItem[]) => items.forEach(item => {
    if (item.path && !item.hideInMenu && !item.unAccessible && (!item.access || access[String(item.access)] !== false)) known.set(item.path, item);
    if (item.children) visit(item.children);
  });
  visit(routes);
  const byParent = new Map<string, NavigationMenu[]>();
  nodes.forEach(node => {
    const parent = node.parentId || '0';
    byParent.set(parent, [...(byParent.get(parent) || []), node]);
  });
  const build = (parent: string, seen: Set<string>): MenuDataItem[] => (byParent.get(parent) || []).flatMap(node => {
    if (seen.has(node.id)) return [];
    const children = build(node.id, new Set([...seen, node.id]));
    if (node.type === 1) return children.length ? [{ key: node.id, name: node.name, children }] : [];
    const route = node.path && known.get(node.path);
    return route ? [{ ...route, key: node.id, name: node.name, children: children.length ? children : undefined }] : [];
  });
  const workspace = known.get('/workspace');
  return [...(workspace ? [workspace] : []), ...build('0', new Set())];
}
