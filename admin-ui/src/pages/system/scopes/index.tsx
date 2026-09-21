import { ModalForm, PageContainer, ProFormDigit, ProFormSwitch, ProFormText, ProFormTextArea, ProFormTreeSelect, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Card, Tag, Tree } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import { createScope, scopeTree, updateScope } from '@/services/system/scopes';
import type { CreateScope, ScopeRow, UpdateScope } from '@/services/system/scopes';
import { compactFormProps, compactModalProps, CompactFields, ManagementSplit, verticalFormItemProps } from '@/components/ManagementLayout';
import { ApiError, StaleRequest } from '@/utils/request';
import { refreshIdentity, setDirty } from '@/utils/session';
import { roleTreeData } from '@/utils/roles';

export default function ScopesPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const [rows, setRows] = useState<ScopeRow[]>([]);
  const [selected, setSelected] = useState('');
  const [editor, setEditor] = useState<{ row?: ScopeRow }>();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const load = useCallback(async () => {
    setLoading(true); setError('');
    try {
      const data = await scopeTree(); setRows(data);
      setSelected(current => data.some(row => row.id === current) ? current : data.find(row => row.parentId === '0')?.id || '');
    }
    catch (failure) { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '机构加载失败'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); return () => setDirty(false); }, [load]);
  const selectedRow = rows.find(row => row.id === selected);
  const root = rows.find(row => row.parentId === '0');
  const editingRoot = editor?.row?.parentId === '0';
  const tree = roleTreeData(rows.map(row => ({ id: row.id, parentId: row.parentId, name: row.name })));
  const excluded = new Set(rows.filter(row => row.id === editor?.row?.id || row.ancestors.split(',').includes(editor?.row?.id || '-')).map(row => row.id));
  const parentTree = roleTreeData(rows.filter(row => !excluded.has(row.id)).map(row => ({ id: row.id, parentId: row.parentId, name: row.name })));
  const columns: ProColumns<ScopeRow>[] = [
    { title: '机构名称', dataIndex: 'name' },
    { title: '机构编码', dataIndex: 'code' },
    { title: '简称', dataIndex: 'shortName' },
    { title: '联系人', dataIndex: 'contactPerson' },
    { title: '联系电话', dataIndex: 'contactPhone' },
    { title: '状态', dataIndex: 'enabled', render: (_, row) => <Tag color={row.enabled ? 'success' : 'default'}>{row.enabled ? '启用' : '停用'}</Tag> },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <Button key="children" type="link" onClick={() => setSelected(row.id)}>下级机构</Button>,
      access.canEditScopes && <Button key="edit" type="link" onClick={() => setEditor({ row })}>修改</Button>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="机构管理" subTitle="总单位统一管理下属机构。">
    {error && <Alert type="error" title={error} showIcon />}
    <ManagementSplit navigation={<Card title="机构树" size="small" extra={<Button type="link" size="small" loading={loading} onClick={() => void load()}>刷新</Button>}>
      <Tree key={root?.id || 'empty'} aria-label="机构树" blockNode defaultExpandedKeys={root ? [root.id] : []} selectedKeys={[selected]}
        treeData={tree}
        onSelect={keys => { if (keys.length) setSelected(String(keys[0])); }} />
    </Card>}>
      <ProTable<ScopeRow> rowKey="id" headerTitle={selectedRow ? selectedRow.name + ' · 下级机构' : '总单位'}
        columns={columns} search={false} options={false} loading={loading}
        dataSource={rows.filter(row => row.parentId === selected)} pagination={{ defaultPageSize: 20, showSizeChanger: true }}
        toolBarRender={() => [
          selectedRow && access.canEditScopes && <Button key="edit" onClick={() => setEditor({ row: selectedRow })}>修改当前机构</Button>,
          !error && access.canCreateScopes && <Button key="create" type="primary" disabled={loading} onClick={() => setEditor({})}>{root ? '新增下级机构' : '建立总单位'}</Button>,
        ].filter(Boolean)} />
    </ManagementSplit>
    {editor && <ModalForm<CreateScope & UpdateScope> {...compactFormProps} name="scope-editor" title={editor.row ? (editingRoot ? '修改总单位' : '修改机构') : (root ? '新增下级机构' : '建立总单位')} open width={860}
      initialValues={editor.row || { parentId: selected || root?.id || '0' }} modalProps={compactModalProps} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async values => {
        const fields = { name: values.name, parentId: editingRoot || !root ? '0' : values.parentId, shortName: values.shortName, contactPerson: values.contactPerson, contactPhone: values.contactPhone, remark: values.remark };
        try {
          if (editor.row) await updateScope(editor.row.id, { ...fields, version: editor.row.version, enabled: editingRoot || values.enabled, sort: values.sort });
          else await createScope({ ...fields, code: values.code });
          void message.success(editor.row ? '机构已更新' : '机构已新增');
          setEditor(undefined); setDirty(false); refreshIdentity(); return true;
        } catch { return false; }
      }}>
      <CompactFields>
        {!editor.row && <ProFormText name="code" label="机构编码" fieldProps={{ maxLength: 64 }} rules={[{ required: true }, { pattern: /^[A-Za-z0-9][A-Za-z0-9_-]{0,63}$/, message: '使用字母、数字、下划线或短横线' }]} />}
        <ProFormText name="name" label="机构名称" fieldProps={{ maxLength: 100 }} rules={[{ required: true, whitespace: true }]} />
        {root && !editingRoot && <ProFormTreeSelect name="parentId" label="上级机构" rules={[{ required: true }]}
          fieldProps={{ treeDefaultExpandAll: true, fieldNames: { label: 'title', value: 'key' }, treeData: parentTree }} />}
        <ProFormText name="shortName" label="机构简称" fieldProps={{ maxLength: 50 }} />
        <ProFormText name="contactPerson" label="联系人" fieldProps={{ maxLength: 50 }} />
        <ProFormText name="contactPhone" label="联系电话" fieldProps={{ maxLength: 20 }} />
        {editor.row && <>{!editingRoot && <ProFormSwitch name="enabled" label="启用机构" />}<ProFormDigit name="sort" label="排序" min={0} max={1000000} fieldProps={{ precision: 0 }} rules={[{ required: true }]} /></>}
      </CompactFields>
      <ProFormTextArea formItemProps={verticalFormItemProps} name="remark" label="备注" fieldProps={{ maxLength: 500, showCount: true }} />
    </ModalForm>}
  </PageContainer>;
}
