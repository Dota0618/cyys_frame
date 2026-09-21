import { ModalForm, PageContainer, ProForm, ProFormDatePicker, ProFormDateTimePicker, ProFormDigit, ProFormSelect, ProFormSwitch, ProFormText, ProFormTextArea, ProFormTreeSelect, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import { Alert, Button, Popconfirm } from 'antd';
import { useEffect, useRef, useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
import DictionarySelect from '@/components/DictionarySelect';
import { departmentTreeData } from '@/utils/departments';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty } from '@/utils/session';
import * as service from '@/services/business/@@FEATURE@@';
import type { @@CLASS@@Command, @@CLASS@@Options, @@CLASS@@Row } from '@/services/business/@@FEATURE@@';

type TreeRow = @@CLASS@@Row & { parentId?: string | null; children?: TreeRow[] };
function treeRows(rows: @@CLASS@@Row[]): TreeRow[] {
  const all = new Map<string, TreeRow>(rows.map(row => [row.id, { ...row }]));
  const roots: TreeRow[] = [];
  for (const row of all.values()) {
    const parent = row.parentId ? all.get(row.parentId) : undefined;
    if (parent && parent.id !== row.id) (parent.children ||= []).push(row); else roots.push(row);
  }
  return roots;
}
export default function @@CLASS@@Page() {
  const { initialState } = useModel('@@initialState');
  const user = initialState?.user;
  const allowed = (action: string) => {
    const state = session(), permission = `@@FEATURE@@:${action}`, platform = user?.platformPermissions || [];
    return !state.allUnits && (user?.mode === 'single' || !!state.scopeId) &&
      (!!user?.unitPermissions.includes(permission) || (platform.includes('platform:scope:access') && platform.includes(permission) &&
        (platform.includes('platform:data:operate') || (action === 'read' && platform.includes('platform:data:read-all')))));
  };
  const action = useRef<ActionType | null>(null);
  const [editor, setEditor] = useState<{ row?: @@CLASS@@Row; options: @@CLASS@@Options }>();
  const [error, setError] = useState('');
  useEffect(() => () => setDirty(false), []);
  const failed = (error: unknown) => { if (!(error instanceof StaleRequest)) setError(error instanceof ApiError ? error.message : '操作失败'); };
  const edit = async (row?: @@CLASS@@Row) => {
    try { setEditor({ row, options: await service.options(row?.id) }); } catch (error) { failed(error); }
  };
  const columns: ProColumns<@@CLASS@@Row>[] = [
    @@COLUMNS@@
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      allowed('update') && <Button type="link" key="edit" onClick={() => { void edit(row); }}>编辑</Button>,
      allowed('delete') && <Popconfirm key="delete" title="删除此记录？" onConfirm={async () => {
        try { await service.remove(row); void action.current?.reload(); } catch (error) { failed(error); }
      }}><Button type="link" danger>删除</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  const editing = editor?.row, options = editor?.options;
  return <PageContainer title=@@TITLE_HTML@@>
    <ScopeSwitch />
    {error && <Alert type="error" title={error} showIcon />}
    {!allowed('read') ? <Alert type="warning" title="请选择有权访问的单位" /> : <ProTable<@@CLASS@@Row> rowKey="id" actionRef={action} columns={columns}
      search={@@TREE@@ ? false : undefined} pagination={@@TREE@@ ? false : { defaultPageSize: 20 }}
      toolBarRender={() => allowed('create') ? [<Button key="create" type="primary" onClick={() => { void edit(); }}>新增</Button>] : []}
      request={async (params) => {
        try {
          if (@@TREE@@) { const rows = await service.choices(); return { data: treeRows(rows), total: rows.length, success: true }; }
          const result = await service.list(params); return { data: result.records, total: result.total, success: true };
        } catch (error) { failed(error); return { data: [], success: false }; }
      }} />}
    {editor && options && <ModalForm<@@CLASS@@Command & { departmentId?: string; parentId?: string | null }> title={editing ? '编辑' : '新增'} open omitNil={false}
      initialValues={editing || {}} modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async (values) => {
        const command: @@CLASS@@Command = {
          @@FORM_COMMAND@@
        };
        try {
          if (editing) await service.update(editing.id, { ...command, version: editing.version });
          else await service.create({ ...command, departmentId: values.departmentId!, @@PARENT_COMMAND@@ });
          setEditor(undefined); setDirty(false); void action.current?.reload(); return true;
        } catch (error) { failed(error); return false; }
      }}>
      <ScopeSwitch />
      {!editing && <><ProFormTreeSelect name="departmentId" label="所属部门" rules={[{ required: true }]}
        fieldProps={{ treeData: departmentTreeData(options.departments), treeDefaultExpandAll: true }} />
      @@PARENT_FORM@@</>}
      @@FORM_FIELDS@@
    </ModalForm>}
  </PageContainer>;
}
