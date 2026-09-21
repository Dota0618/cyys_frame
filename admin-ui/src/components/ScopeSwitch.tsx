import { useModel } from '@umijs/max';
import { App, Space, TreeSelect } from 'antd';
import { hasUnsavedChanges, session, updateSession } from '@/utils/session';
import { roleTreeData } from '@/utils/roles';

export default function ScopeSwitch() {
  const { initialState } = useModel('@@initialState');
  const { modal } = App.useApp();
  const user = initialState?.user;
  if (!user || !user.scopes.length) return null;
  const context = session();
  const allowed = new Set(user.scopes.map(scope => scope.id));
  const tree = roleTreeData(user.scopes.map(scope => ({
    id: scope.id, name: scope.name,
    parentId: allowed.has(scope.parentId) ? scope.parentId : '0',
  })));
  const change = (scopeId: string) => {
    const apply = () => updateSession({ token: context.token, scopeId });
    if (hasUnsavedChanges()) modal.confirm({
      title: '切换机构？',
      content: '当前表单尚未保存，切换后将清除填写内容。',
      okText: '切换并清除', cancelText: '继续编辑', onOk: apply,
    });
    else apply();
  };
  return <Space>
    <span className="scope-label">机构</span>
    <TreeSelect
      aria-label="机构筛选" placeholder="选择机构" style={{ minWidth: 200, maxWidth: 320 }}
      value={context.scopeId} treeData={tree} fieldNames={{ label: 'title', value: 'key' }}
      treeDefaultExpandAll showSearch={{ treeNodeFilterProp: 'title' }} onChange={change}
    />
  </Space>;
}
