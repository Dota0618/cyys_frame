import { Tree } from 'antd';
import type { TreeDataNode } from 'antd';
import type { Key } from 'react';

const checkedIds = (keys: Key[] | { checked: Key[] }) =>
  (Array.isArray(keys) ? keys : keys.checked).map(String);

export default function CheckedTree({
  value = [],
  onChange,
  treeData,
  label,
  menuSelection = false,
}: {
  value?: string[];
  onChange?: (ids: string[]) => void;
  treeData: TreeDataNode[];
  label: string;
  menuSelection?: boolean;
}) {
  return (
    <div style={menuSelection ? { maxHeight: 360, overflow: 'auto' } : undefined}>
    <Tree
      checkable
      checkStrictly={menuSelection}
      selectable={false}
      defaultExpandAll={!menuSelection}
      checkedKeys={value}
      onCheck={(keys, info) => {
        if (!menuSelection) { onChange?.(checkedIds(keys)); return; }
        // Menu inclusion is independent of individual button grants. Never let a
        // persisted checked parent silently re-enable an unchecked button.
        const next = new Set(value);
        const parents = new Map<string, string>();
        const nodes = new Map<string, TreeDataNode>();
        const index = (items: TreeDataNode[], parent?: string) => items.forEach(item => {
          const key = String(item.key); nodes.set(key, item);
          if (parent) parents.set(key, parent);
          if (item.children) index(item.children, key);
        });
        index(treeData);
        const toggle = (node: TreeDataNode) => {
          if (!node.disabled && !node.disableCheckbox) {
            if (info.checked) next.add(String(node.key)); else next.delete(String(node.key));
            node.children?.forEach(toggle);
          }
        };
        const node = nodes.get(String(info.node.key));
        if (node) toggle(node);
        if (info.checked) {
          let parent = parents.get(String(info.node.key));
          while (parent) { next.add(parent); parent = parents.get(parent); }
        }
        onChange?.([...next]);
      }}
      treeData={treeData}
      aria-label={label}
    />
    </div>
  );
}
