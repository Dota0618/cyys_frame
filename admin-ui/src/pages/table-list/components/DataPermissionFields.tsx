import { verticalFormItemProps } from '@/components/ManagementLayout';
import { ProForm, ProFormDependency, ProFormRadio } from '@ant-design/pro-components';
import { Alert } from 'antd';
import CheckedTree from '@/components/CheckedTree';
import type { DepartmentOption } from '@/types/api';
import { roleTreeData } from '@/utils/roles';

export const dataRanges = [
  { value: 5, label: '本人' }, { value: 3, label: '本部门' },
  { value: 4, label: '本部门及下级' }, { value: 2, label: '自定义部门' },
  { value: 1, label: '本单位全部' },
];

export default function DataPermissionFields({ departments, editable = true, currentRange = 5 }: {
  departments: DepartmentOption[]; editable?: boolean; currentRange?: number;
}) {
  if (!editable) return <Alert type="info" showIcon title={`数据范围：${dataRanges.find(row => row.value === currentRange)?.label || '未配置'}`}
    description="需要“配置数据权限”授权才能修改。" />;
  const tree = roleTreeData(departments.map(row => ({ id: row.value, name: row.label, parentId: row.parentId })));
  return <>
    <Alert type="info" showIcon title="数据范围统一适用于当前单位，菜单和操作权限由角色分配。" style={{ marginBottom: 14 }} />
    <ProFormRadio.Group formItemProps={verticalFormItemProps} name="dataRange" label="数据范围" options={dataRanges} rules={[{ required: true }]}
      fieldProps={{ style: { display: 'flex', flexWrap: 'wrap', gap: 8 } }} />
    <ProFormDependency name={['dataRange']}>
      {({ dataRange }) => dataRange === 2 ? <ProForm.Item {...verticalFormItemProps} name="orgIds" label="可访问部门"
        extra="按勾选部门生效；需要下级部门时请一并勾选。"
        rules={[{ required: true, type: 'array', min: 1, message: '至少选择一个部门' }]}>
        <CheckedTree treeData={tree} label="可访问部门" independentSelection />
      </ProForm.Item> : null}
    </ProFormDependency>
  </>;
}
