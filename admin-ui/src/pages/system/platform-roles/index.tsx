import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProForm,
  ProFormDigit,
  ProFormRadio,
  ProFormSelect,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Popconfirm, Tabs, Tag } from 'antd';
import { useMemo, useRef, useState } from 'react';
import CheckedTree from '@/components/CheckedTree';
import {
  assignPlatformRoles,
  createPlatformRole,
  deletePlatformRole,
  editPlatformRole,
  listPlatformAccounts,
  listPlatformRoles,
  platformRoleCreateOptions,
  platformRoleDetail,
  platformRoleUpdateOptions,
} from '@/services/system/platformRoles';
import type {
  CreatePlatformRole,
  EditPlatformRole,
  PlatformAccount,
  PlatformRole,
  PlatformRoleDetail,
  PlatformRoleOptions,
} from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { roleTreeData } from '@/utils/roles';
import { setDirty } from '@/utils/session';

const emptyOptions: PlatformRoleOptions = { menus: [] };

function RoleFields({ options, editing }: { options: PlatformRoleOptions; editing?: boolean }) {
  const tree = useMemo(() => roleTreeData(options.menus.map((menu) => ({
    ...menu, name: menu.type === 3 ? `${menu.name}（${menu.code}）` : menu.name,
  }))), [options.menus]);
  return (
    <>
      {!editing && <ProFormText name="code" label="角色编码" fieldProps={{ maxLength: 64 }}
        rules={[{ required: true, whitespace: true }]} />}
      <ProFormText name="name" label="角色名称" fieldProps={{ maxLength: 50 }}
        rules={[{ required: true, whitespace: true }]} />
      <ProForm.Item name="menuIds" label="平台菜单与操作权限">
        <CheckedTree treeData={tree} label="平台菜单与操作权限" />
      </ProForm.Item>
      <ProFormRadio.Group name="status" label="状态"
        options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} rules={[{ required: true }]} />
      <ProFormDigit name="sort" label="排序" fieldProps={{ precision: 0 }} rules={[{ required: true }]} />
      <ProFormTextArea name="remark" label="备注" fieldProps={{ maxLength: 500, showCount: true }} />
    </>
  );
}

function RoleTable() {
  const access = useAccess();
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [createOptions, setCreateOptions] = useState(emptyOptions);
  const [updateOptions, setUpdateOptions] = useState(emptyOptions);
  const [editing, setEditing] = useState<PlatformRoleDetail>();
  const [loadError, setLoadError] = useState('');

  async function openEdit(role: PlatformRole) {
    try {
      const [detail, options] = await Promise.all([platformRoleDetail(role.id), platformRoleUpdateOptions()]);
      if (!detail.editable) { void message.error('该角色包含超出当前授予上限的权限，不能修改'); return; }
      setUpdateOptions(options);
      setEditing(detail);
    } catch { /* 统一请求层已提示 */ }
  }

  const columns: ProColumns<PlatformRole>[] = [
    { title: '角色名称', dataIndex: 'name' },
    { title: '角色编码', dataIndex: 'code', search: false },
    { title: '状态', dataIndex: 'status', search: false, render: (_, row) =>
      <Tag color={row.status === 1 ? 'success' : 'default'}>{row.status === 1 ? '启用' : '停用'}</Tag> },
    { title: '排序', dataIndex: 'sort', search: false, width: 80 },
    { title: '操作', valueType: 'option', width: 140, render: (_, row) => [
      access.canEditPlatformRoles && <a key="edit" onClick={() => void openEdit(row)}>修改</a>,
      access.canDeletePlatformRoles && <Popconfirm key="delete" title="删除平台角色"
        description="仍授予账号的角色不能删除。" okText="删除" cancelText="取消" onConfirm={async () => {
          await deletePlatformRole(row.id, row.version);
          void message.success('平台角色已删除');
          await action.current?.reload();
        }}><a>删除</a></Popconfirm>,
    ].filter(Boolean) },
  ];

  return <>
    {loadError && <Alert type="error" showIcon title={loadError} style={{ marginBottom: 20 }} />}
    <ProTable<PlatformRole>
      actionRef={action} rowKey="id" headerTitle="平台角色" columns={columns}
      form={{ name: 'platform-role-search' }} search={{ labelWidth: 'auto' }}
      options={{ density: true, fullScreen: false, setting: false, reload: true }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100, 200] }}
      request={async (params) => {
        setLoadError('');
        try {
          const result = await listPlatformRoles(params.current || 1, params.pageSize || 20, params.name);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '平台角色加载失败');
          return { data: [], total: 0, success: false };
        }
      }}
      toolBarRender={() => access.canCreatePlatformRoles ? [
        <ModalForm<CreatePlatformRole> key="create" name="platform-role-create" title="新增平台角色" width={680}
          trigger={<Button type="primary" icon={<PlusOutlined />}>新增平台角色</Button>}
          initialValues={{ status: 1, sort: 0, menuIds: [] }} modalProps={{ destroyOnHidden: true }}
          onValuesChange={() => setDirty(true)}
          onOpenChange={(open) => {
            if (open) void platformRoleCreateOptions().then(setCreateOptions).catch(() => {});
            else { setCreateOptions(emptyOptions); setDirty(false); }
          }}
          onFinish={async (values) => {
            try {
              await createPlatformRole({ ...values, menuIds: values.menuIds || [] });
              void message.success('平台角色已新增');
              await action.current?.reload();
              return true;
            } catch { return false; }
          }}><RoleFields options={createOptions} /></ModalForm>,
      ] : []}
    />
    <ModalForm<EditPlatformRole> key={editing?.role.id || 'edit'} name="platform-role-edit"
      title="修改平台角色" width={680} open={!!editing}
      initialValues={editing ? { ...editing.role, menuIds: editing.menuIds } : undefined}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditing(undefined); setUpdateOptions(emptyOptions); setDirty(false); } }}
      onFinish={async (values) => {
        if (!editing) return false;
        try {
          await editPlatformRole({ ...values, id: editing.role.id, version: editing.role.version,
            menuIds: values.menuIds || [] });
          void message.success('平台角色已更新');
          await action.current?.reload();
          return true;
        } catch { return false; }
      }}><RoleFields options={updateOptions} editing /></ModalForm>
  </>;
}

function AccountTable() {
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [editing, setEditing] = useState<PlatformAccount>();
  const [roles, setRoles] = useState<PlatformRole[]>([]);
  const [loadError, setLoadError] = useState('');
  const columns: ProColumns<PlatformAccount>[] = [
    { title: '登录账号', dataIndex: 'loginName' },
    { title: '姓名', dataIndex: 'realName', search: false },
    { title: '平台角色数', dataIndex: 'roleIds', search: false, renderText: (value: string[]) => value.length },
    { title: '状态', dataIndex: 'status', search: false, render: (_, row) =>
      <Tag color={row.status === 1 ? 'success' : 'default'}>{row.status === 1 ? '启用' : '停用'}</Tag> },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <a key="grant" onClick={() => {
        void listPlatformRoles(1, 200).then((result) => { setRoles(result.records); setEditing(row); }).catch(() => {});
      }}>授权</a>,
    ] },
  ];
  return <>
    {loadError && <Alert type="error" showIcon title={loadError} style={{ marginBottom: 20 }} />}
    <ProTable<PlatformAccount>
      actionRef={action} rowKey="id" headerTitle="账号平台授权" columns={columns}
      form={{ name: 'platform-account-search' }} search={{ labelWidth: 'auto' }}
      options={{ density: true, fullScreen: false, setting: false, reload: true }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100, 200] }}
      request={async (params) => {
        setLoadError('');
        try {
          const result = await listPlatformAccounts(params.current || 1, params.pageSize || 20, params.loginName);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '账号授权加载失败');
          return { data: [], total: 0, success: false };
        }
      }}
    />
    <ModalForm<{ roleIds: string[] }> key={editing?.id || 'grant'} name="platform-account-grant"
      title={`平台角色授权${editing ? `：${editing.loginName}` : ''}`} open={!!editing}
      initialValues={{ roleIds: editing?.roleIds || [] }} modalProps={{ destroyOnHidden: true }}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditing(undefined); setRoles([]); setDirty(false); } }}
      onFinish={async (values) => {
        if (!editing) return false;
        try {
          await assignPlatformRoles(editing.id, values.roleIds || []);
          void message.success('平台角色授权已更新');
          await action.current?.reload();
          return true;
        } catch { return false; }
      }}>
      <ProFormSelect name="roleIds" label="平台角色" mode="multiple"
        options={roles.map((role) => ({ value: role.id, label: `${role.name}（${role.code}）` }))} />
    </ModalForm>
  </>;
}

export default function PlatformRolesPage() {
  const access = useAccess();
  const items = [
    { key: 'roles', label: '角色定义', children: <RoleTable /> },
    access.canGrantPlatformRoles && { key: 'accounts', label: '账号授权', children: <AccountTable /> },
  ].filter(Boolean) as { key: string; label: string; children: React.ReactNode }[];
  return <PageContainer title="平台角色" subTitle="GLOBAL 平台角色与账号授权；系统始终保留至少一名可用平台管理员。">
    <Tabs items={items} destroyOnHidden />
  </PageContainer>;
}
