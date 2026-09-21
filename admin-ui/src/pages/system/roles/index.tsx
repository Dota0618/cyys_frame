import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProForm,
  ProFormDigit,
  ProFormRadio,
  ProFormText,
  ProFormTextArea,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Popconfirm, Tag } from 'antd';
import { useMemo, useRef, useState } from 'react';
import CheckedTree from '@/components/CheckedTree';
import { compactModalProps, ManagementSplit, verticalFormItemProps, compactFormProps } from '@/components/ManagementLayout';
import {
  createRole,
  deleteRole,
  editRole,
  listRoles,
  roleCreateOptions,
  roleDetail,
  roleUpdateOptions,
} from '@/services/system/roles';
import type {
  CreateRole,
  EditRole,
  Role,
  RoleDetail,
  RoleOptions,
} from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { roleTreeData } from '@/utils/roles';
import { setDirty } from '@/utils/session';

const emptyOptions: RoleOptions = { menus: [] };
function RoleFields({ options, editing }: { options: RoleOptions; editing?: boolean }) {
  const tree = useMemo(() => roleTreeData(options.menus.map((menu) => ({
    ...menu, name: menu.name,
  }))), [options.menus]);
  return (
    <ManagementSplit navigation={<div>
      {!editing && <ProFormText name="code" label="角色编码" fieldProps={{ maxLength: 64 }}
        rules={[{ required: true, whitespace: true }]} />}
      <ProFormText name="name" label="角色名称" fieldProps={{ maxLength: 50 }}
        rules={[{ required: true, whitespace: true }]} />
      <ProFormRadio.Group name="status" label="状态"
        options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} rules={[{ required: true }]} />
      <ProFormDigit name="sort" label="排序" fieldProps={{ precision: 0 }} rules={[{ required: true }]} />
      <ProFormTextArea formItemProps={verticalFormItemProps} name="remark" label="备注" fieldProps={{ maxLength: 500, showCount: true }} />
    </div>}>
      <ProForm.Item {...verticalFormItemProps} name="menuIds" label="菜单与操作权限">
        <CheckedTree treeData={tree} label="菜单与操作权限" menuSelection />
      </ProForm.Item>
    </ManagementSplit>
  );
}

export default function RolesPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [createOptions, setCreateOptions] = useState<RoleOptions>(emptyOptions);
  const [updateOptions, setUpdateOptions] = useState<RoleOptions>(emptyOptions);
  const [editing, setEditing] = useState<RoleDetail>();
  const [loadError, setLoadError] = useState('');

  async function openEdit(role: Role) {
    try {
      const [detail, options] = await Promise.all([roleDetail(role.id), roleUpdateOptions()]);
      if (!detail.editable) {
        void message.error('该角色包含超出当前授予上限的权限，不能修改');
        return;
      }
      setUpdateOptions(options);
      setEditing(detail);
    } catch { /* 统一请求层已提示 */ }
  }

  const columns: ProColumns<Role>[] = [
    { title: '角色名称', dataIndex: 'name' },
    { title: '角色编码', dataIndex: 'code', search: false },
    {
      title: '状态', dataIndex: 'status', search: false,
      render: (_, row) => <Tag color={row.status === 1 ? 'success' : 'default'}>{row.status === 1 ? '启用' : '停用'}</Tag>,
    },
    { title: '排序', dataIndex: 'sort', search: false, width: 80 },
    {
      title: '操作', valueType: 'option', width: 150,
      render: (_, row) => [
        access.canEditRoles && <a key="edit" onClick={() => void openEdit(row)}>修改</a>,
        access.canDeleteRoles && (
          <Popconfirm
            key="delete"
            title="删除角色"
            description="仍授予成员的角色不能删除。"
            okText="删除"
            cancelText="取消"
            onConfirm={async () => {
              await deleteRole(row.id, row.version);
              void message.success('角色已删除');
              await action.current?.reload();
            }}
          ><a>删除</a></Popconfirm>
        ),
      ].filter(Boolean),
    },
  ];

  return (
    <PageContainer
      title="角色管理"
      subTitle="角色统一管理；用户的数据范围在用户管理中配置。"
    >
      {loadError && <Alert type="error" showIcon title={loadError} style={{ marginBottom: 20 }} />}
      <ProTable<Role>
        actionRef={action}
        rowKey="id"
        headerTitle="角色列表"
        columns={columns}
        form={{ name: 'role-search' }}
        search={{ labelWidth: 'auto' }}
        options={{ density: true, fullScreen: false, setting: false, reload: true }}
        pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [10, 20, 50, 100, 200] }}
        request={async (params) => {
          setLoadError('');
          try {
            const result = await listRoles(params.current || 1, params.pageSize || 20, params.name);
            return { data: result.records, total: result.total, success: true };
          } catch (error) {
            if (!(error instanceof StaleRequest)) {
              setLoadError(error instanceof ApiError ? error.message : '角色列表加载失败，请重试');
            }
            return { data: [], total: 0, success: false };
          }
        }}
        toolBarRender={() => access.canCreateRoles ? [
          <ModalForm<CreateRole> {...compactFormProps}
            key="create"
            name="role-create"
            title="新增角色"
            width={920}
            trigger={<Button type="primary" icon={<PlusOutlined />}>新增角色</Button>}
            initialValues={{ status: 1, sort: 0, menuIds: [] }}
            modalProps={compactModalProps}
            onValuesChange={() => setDirty(true)}
            onOpenChange={(open) => {
              if (open) void roleCreateOptions().then(setCreateOptions).catch(() => {});
              else { setCreateOptions(emptyOptions); setDirty(false); }
            }}
            onFinish={async (values) => {
              try {
                await createRole({ ...values, menuIds: values.menuIds || [] });
                void message.success('角色已新增');
                await action.current?.reload();
                return true;
              } catch { return false; }
            }}
          ><RoleFields options={createOptions} /></ModalForm>,
        ] : []}
      />

      <ModalForm<EditRole> {...compactFormProps}
        key={editing?.role.id || 'edit'}
        name="role-edit"
        title="修改角色"
        width={920}
        open={!!editing}
        initialValues={editing ? { ...editing.role, menuIds: editing.menuIds } : undefined}
        modalProps={compactModalProps}
        onValuesChange={() => setDirty(true)}
        onOpenChange={(open) => {
          if (!open) { setEditing(undefined); setUpdateOptions(emptyOptions); setDirty(false); }
        }}
        onFinish={async (values) => {
          if (!editing) return false;
          try {
            await editRole({ ...values, id: editing.role.id, version: editing.role.version,
              menuIds: values.menuIds || [] });
            void message.success('角色已更新');
            await action.current?.reload();
            return true;
          } catch { return false; }
        }}
      ><RoleFields options={updateOptions} editing /></ModalForm>
    </PageContainer>
  );
}
