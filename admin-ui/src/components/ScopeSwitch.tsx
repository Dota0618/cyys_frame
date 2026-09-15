import { useModel } from '@umijs/max';
import { App, Select, Space, Tag } from 'antd';
import { hasUnsavedChanges, session, updateSession } from '@/utils/session';

export default function ScopeSwitch() {
  const { initialState } = useModel('@@initialState');
  const { modal } = App.useApp();
  const user = initialState?.user;
  if (!user) return null;
  const context = session();
  if (user.mode === 'single') return <Tag>{user.scopes[0]?.name}</Tag>;
  const options = user.scopes.map((scope) => ({
    value: scope.id,
    label: scope.name,
  }));
  if (user.platformPermissions.includes('platform:data:read-all'))
    options.unshift({ value: '__all__', label: '全部单位 · 只读' });
  const change = (value: string) => {
    const apply = () =>
      updateSession({
        token: context.token,
        scopeId: value === '__all__' ? undefined : value,
        allUnits: value === '__all__',
      });
    if (hasUnsavedChanges())
      modal.confirm({
        title: '切换单位？',
        content: '当前表单尚未保存，切换后将清除填写内容。',
        okText: '切换并清除',
        cancelText: '继续编辑',
        onOk: apply,
      });
    else apply();
  };
  return (
    <Space>
      <span className="scope-label">工作单位</span>
      <Select
        aria-label="工作单位"
        placeholder="选择单位"
        style={{ minWidth: 200 }}
        value={context.allUnits ? '__all__' : context.scopeId}
        options={options}
        onChange={change}
      />
    </Space>
  );
}
