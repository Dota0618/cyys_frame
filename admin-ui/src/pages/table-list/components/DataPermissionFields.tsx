import { useModel } from '@umijs/max';
import { verticalFormItemProps } from '@/components/ManagementLayout';
import { ProForm, ProFormDependency, ProFormRadio } from '@ant-design/pro-components';
import { Alert } from 'antd';
import CheckedTree from '@/components/CheckedTree';
import type { DepartmentOption } from '@/types/api';
import { roleTreeData } from '@/utils/roles';

export const dataRanges = [
  { value: 5, label: '本人' }, { value: 3, label: '本部门' },
  { value: 4, label: '本部门及下级' }, { value: 2, label: '自定义部门' },
  { value: 1, label: '本机构' }, { value: 6, label: '本机构及下级' }, { value: 0, label: '全系统' },
];

export default function DataPermissionFields({ departments, editable = true, currentRange = 5, systemAccount = false }: {
  departments: DepartmentOption[]; editable?: boolean; currentRange?: number; systemAccount?: boolean;
}) {
  const { initialState } = useModel('@@initialState');
  const actorRange = initialState?.user?.dataRange;
  const options = dataRanges.filter(range => (!systemAccount || range.value === 0)
    && (range.value === 0 ? actorRange === 0 : range.value !== 6 || actorRange === 0 || actorRange === 6));
  if (!editable) return <Alert type="info" showIcon title={`数据范围：${dataRanges.find(row => row.value === currentRange)?.label || '未配置'}`}
    description="需要“配置数据权限”授权才能修改。" />;
  const tree = roleTreeData(departments.map(row => ({ id: row.value, name: row.label, parentId: row.parentId })));
  return <>
    <ProFormRadio.Group formItemProps={verticalFormItemProps} name="dataRange" label="数据范围" options={options} rules={[{ required: true }]}
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
