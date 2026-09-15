import type { DepartmentOption } from '@/types/api';

export interface DepartmentTreeNode {
  value: string;
  title: string;
  children?: DepartmentTreeNode[];
  disabled?: boolean;
}

/** 将服务端稳定 ID/父 ID 适配为 TreeSelect 数据，不改变提交值。 */
export function departmentTreeData(
  options: DepartmentOption[],
  disabledIds: ReadonlySet<string> = new Set(),
): DepartmentTreeNode[] {
  const nodes = new Map<string, DepartmentTreeNode>();
  for (const option of options) {
    nodes.set(option.value, {
      value: option.value,
      title: option.label,
      disabled: disabledIds.has(option.value) || undefined,
    });
  }
  const roots: DepartmentTreeNode[] = [];
  for (const option of options) {
    const node = nodes.get(option.value)!;
    const parent = option.parentId && nodes.get(option.parentId);
    if (parent && parent !== node) {
      (parent.children ||= []).push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}
