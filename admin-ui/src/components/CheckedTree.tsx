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
}: {
  value?: string[];
  onChange?: (ids: string[]) => void;
  treeData: TreeDataNode[];
  label: string;
}) {
  return (
    <Tree
      checkable
      selectable={false}
      defaultExpandAll
      checkedKeys={value}
      onCheck={(keys) => onChange?.(checkedIds(keys))}
      treeData={treeData}
      aria-label={label}
    />
  );
}
