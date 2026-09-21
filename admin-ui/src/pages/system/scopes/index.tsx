import { ModalForm, PageContainer, ProFormDigit, ProFormSwitch, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Tag } from 'antd';
import { useEffect, useState } from 'react';
import { createScope, listScopes, updateScope } from '@/services/system/scopes';
import type { CreateScope, ScopeRow, UpdateScope } from '@/services/system/scopes';
import { ApiError, StaleRequest } from '@/utils/request';
import { refreshIdentity, setDirty } from '@/utils/session';

export default function ScopesPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const [editor, setEditor] = useState<{ row?: ScopeRow }>();
  const [error, setError] = useState('');
  useEffect(() => () => setDirty(false), []);
  const columns: ProColumns<ScopeRow>[] = [
    { title: '单位名称', dataIndex: 'name' },
    { title: '单位编码', dataIndex: 'code', search: false },
    { title: '简称', dataIndex: 'shortName', search: false },
    { title: '联系人', dataIndex: 'contactPerson', search: false },
    { title: '联系电话', dataIndex: 'contactPhone', search: false },
    { title: '状态', dataIndex: 'enabled', search: false, render: (_, row) => <Tag color={row.enabled ? 'success' : 'default'}>{row.enabled ? '启用' : '停用'}</Tag> },
    { title: '操作', valueType: 'option', render: (_, row) => [
      access.canEditScopes && <Button key="edit" type="link" onClick={() => setEditor({ row })}>修改</Button>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="单位管理" subTitle="维护业务单位的基本信息及启用状态。">
    {error && <Alert type="error" title={error} showIcon />}
    <ProTable<ScopeRow> rowKey="id" headerTitle="单位列表" columns={columns} form={{ name: 'scope-search' }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true }}
      request={async (params) => {
        try { setError(''); const page = await listScopes(params.current || 1, params.pageSize || 20, params.name); return { data: page.records, total: page.total, success: true }; }
        catch (failure) { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '单位加载失败'); return { data: [], success: false }; }
      }}
      toolBarRender={() => access.canCreateScopes ? [<Button key="create" type="primary" onClick={() => setEditor({})}>新增单位</Button>] : []} />
    {editor && <ModalForm<CreateScope & UpdateScope> name="scope-editor" title={editor.row ? '修改单位' : '新增单位'} open
      initialValues={editor.row || {}} modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async (values) => {
        const fields = { name: values.name, shortName: values.shortName, contactPerson: values.contactPerson, contactPhone: values.contactPhone, remark: values.remark };
        try {
          if (editor.row) await updateScope(editor.row.id, { ...fields, version: editor.row.version, enabled: values.enabled, sort: values.sort });
          else await createScope({ ...fields, code: values.code });
          void message.success(editor.row ? '单位已更新' : '单位已新增');
          setEditor(undefined); setDirty(false); refreshIdentity(); return true;
        } catch { return false; }
      }}>
      {!editor.row && <ProFormText name="code" label="单位编码" fieldProps={{ maxLength: 64 }} rules={[{ required: true }, { pattern: /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$/, message: '使用字母、数字、下划线或短横线' }]} />}
      <ProFormText name="name" label="单位名称" fieldProps={{ maxLength: 100 }} rules={[{ required: true, whitespace: true }]} />
      <ProFormText name="shortName" label="单位简称" fieldProps={{ maxLength: 50 }} />
      <ProFormText name="contactPerson" label="联系人" fieldProps={{ maxLength: 50 }} />
      <ProFormText name="contactPhone" label="联系电话" fieldProps={{ maxLength: 20 }} />
      {editor.row && <><ProFormSwitch name="enabled" label="启用单位" /><ProFormDigit name="sort" label="排序" min={0} max={1000000} fieldProps={{ precision: 0 }} rules={[{ required: true }]} /></>}
      <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500, showCount: true }} />
    </ModalForm>}
  </PageContainer>;
}
