import { compactFormProps, verticalFormItemProps } from '@/components/ManagementLayout';
import { useAccess, useModel } from '@umijs/max';
import {
  PageContainer,
  ProTable,
  ProDescriptions,
  ModalForm,
  ProFormSelect,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Alert, App, Button, Drawer, Tag } from 'antd';
import { useEffect, useRef, useState } from 'react';
import type { DepartmentOption, Member, MemberDetail, MemberDataPermission, Option } from '@/types/api';
import {
  departmentOptions,
  exportMembers,
  grantRoles,
  listMembers,
  memberDetail,
  memberDataPermission,
  roleOptions,
} from '@/services/system/users';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty } from '@/utils/session';
import { saveDownload } from '@/utils/download';
import CreateForm from './components/CreateForm';
import UpdateForm from './components/UpdateForm';
import ResetPasswordForm from './components/ResetPasswordForm';
import DataPermissionForm from './components/DataPermissionForm';
import { dataRanges } from './components/DataPermissionFields';

export default function TableList() {
  const { initialState } = useModel('@@initialState');
  const state = session();
  if (!state.scopeId) {
    return <PageContainer title="用户管理" />;
  }
  return <MemberTable />;
}

function MemberTable() {
  const access = useAccess();
  const { initialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const querySequence = useRef(0);
  const [departments, setDepartments] = useState<DepartmentOption[]>([]);
  const [roles, setRoles] = useState<Option[]>([]);
  const [editing, setEditing] = useState<Member>();
  const [editingPermission, setEditingPermission] = useState<MemberDataPermission>();
  const [dataPermission, setDataPermission] = useState<{member: Member; permission: MemberDataPermission}>();
  const [resetting, setResetting] = useState<Member>();
  const [detail, setDetail] = useState<MemberDetail>();
  const [granting, setGranting] = useState<MemberDetail>();
  const [loadError, setLoadError] = useState('');
  const [exportName, setExportName] = useState<string>();
  const [exporting, setExporting] = useState(false);
  const scopes = initialState?.user?.scopes || [];
  const canManageRange = (row: Member) => row.dataRange === 0 ? initialState?.user?.dataRange === 0
    : row.dataRange !== 6 || [0, 6].includes(initialState?.user?.dataRange ?? -1);
  const departmentName = (id?: string) =>
    departments.find((item) => item.value === id)?.label || '未设置';

  useEffect(() => {
    {
      void departmentOptions()
        .then(setDepartments)
        .catch(() => {});
      if (access.canGrantRoles)
        void roleOptions()
          .then(setRoles)
          .catch(() => {});
    }
    return () => setDirty(false);
  }, [access.canGrantRoles]);

  async function open(member: Member, kind: 'detail' | 'edit' | 'roles') {
    try {
      const result = await memberDetail(member.id);
      if (kind === 'detail') setDetail(result);
      if (kind === 'edit') {
        const permission = access.canManageMemberDataPermission ? await memberDataPermission(member.id) : undefined;
        if (permission && permission.version !== result.member.version) {
          void message.warning('用户已变更，请重新打开修改');
          return;
        }
        setEditingPermission(permission);
        setEditing(result.member);
      }
      if (kind === 'roles') setGranting(result);
    } catch {
      /* 请求入口已处理错误及过期响应。 */
    }
  }
  const columns: ProColumns<Member>[] = [
    {
      title: '姓名',
      dataIndex: 'displayName',
      render: (_, row) => <a onClick={() => open(row, 'detail')}>{row.displayName}</a>,
    },
    { title: '登录账号', dataIndex: 'loginName', search: false },
    { title: '所属机构', dataIndex: 'scopeId', search: false,
      render: (_, row) => row.scopeId ? scopes.find(scope => scope.id === row.scopeId)?.name || '机构已失效' : '系统账号' },
    { title: '部门', dataIndex: 'orgId', search: false, render: (_, row) => departmentName(row.orgId) },
    {
      title: '数据范围', dataIndex: 'dataRange', search: false,
      renderText: value => dataRanges.find(range => range.value === value)?.label || '未配置',
    },
    {
      title: '状态',
      dataIndex: 'status',
      search: false,
      render: (_, row) => (
        <Tag color={row.status === 1 ? 'success' : 'default'}>
          {row.status === 1 ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      valueType: 'dateTime',
      search: false,
    },
    {
      title: '操作',
      valueType: 'option',
      width: 330,
      render: (_, row) => [
              <a key="detail" onClick={() => open(row, 'detail')}>
                详情
              </a>,
              access.canEditUsers && canManageRange(row) && (
                <a key="edit" onClick={() => open(row, 'edit')}>
                  修改
                </a>
              ),
              access.canGrantRoles && canManageRange(row) && (
                <a key="roles" onClick={() => open(row, 'roles')}>
                  角色
                </a>
              ),
              access.canManageMemberDataPermission && canManageRange(row) && <a key="data" onClick={() => {
                void memberDataPermission(row.id).then(permission => setDataPermission({ member: row, permission })).catch(() => {});
              }}>数据权限</a>,
              access.canResetAccountPasswords && <a key="password" onClick={() => setResetting(row)}>重置密码</a>,
            ].filter(Boolean),
    },
  ];
  return (
    <PageContainer
      title="用户管理"
      subTitle="统一维护用户、角色与数据范围。"
    >
      {loadError && (
        <Alert
          type="error"
          showIcon
          title={loadError}
          style={{ marginBottom: 20 }}
        />
      )}
      <ProTable<Member>
        actionRef={action}
        rowKey="id"
        headerTitle="用户列表"
        columns={columns}
        form={{ name: 'member-search' }}
        search={{ labelWidth: 'auto' }}
        options={{
          density: true,
          fullScreen: false,
          setting: false,
          reload: true,
        }}
        pagination={{
          defaultPageSize: 20,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50, 100, 200],
        }}
        request={async (params) => {
          const query = ++querySequence.current;
          setExportName(undefined);
          setLoadError('');
          try {
            const result = await listMembers(
              params.current || 1,
              params.pageSize || 20,
              params.displayName,
            );
            if (query !== querySequence.current) throw new StaleRequest();
            setExportName(params.displayName || '');
            return { data: result.records, total: result.total, success: true };
          } catch (error) {
            if (query === querySequence.current && !(error instanceof StaleRequest))
              setLoadError(
                error instanceof ApiError
                  ? error.message
                  : '用户列表加载失败，请重试',
              );
            return { data: [], total: 0, success: false };
          }
        }}
        toolBarRender={() => [
          access.canExportUsers && <Button key="export" loading={exporting} disabled={exportName === undefined}
            onClick={async () => {
              if (exportName === undefined) return;
              setExporting(true); setLoadError('');
              try { await saveDownload(() => exportMembers(exportName), '单位用户.xlsx'); }
              catch (error) {
                if (!(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '导出失败，请重试');
              } finally { setExporting(false); }
            }}>导出当前筛选</Button>,
          access.canCreateUsers && <CreateForm
                  key="create"
                  departments={departments}
                  reload={async () => {
                    await action.current?.reload();
                  }}
                />,
        ].filter(Boolean)}
      />

      <UpdateForm
        key={editing?.id || 'edit'}
        values={editing}
        permission={editingPermission}
        departments={departments}
        onClose={() => { setEditing(undefined); setEditingPermission(undefined); }}
        reload={async () => {
          await action.current?.reload();
        }}
      />
      {dataPermission && <DataPermissionForm {...dataPermission} onClose={() => setDataPermission(undefined)} onSaved={() => { void action.current?.reload(); }} />}
      {resetting && <ResetPasswordForm account={resetting} onClose={() => setResetting(undefined)} />}

      <ModalForm<{ roleIds: string[] }> {...compactFormProps}
        name="member-roles"
        key={granting?.member.id || 'roles'}
        title="授予角色"
        open={!!granting}
        initialValues={{ roleIds: granting?.roleIds }}
        onOpenChange={(value) => {
          if (!value) {
            setGranting(undefined);
            setDirty(false);
          }
        }}
        onValuesChange={() => setDirty(true)}
        modalProps={{ destroyOnHidden: true }}
        onFinish={async (values) => {
          if (!granting) return false;
          try {
            await grantRoles(granting.member.id, values.roleIds || []);
            void message.success('角色已更新');
            return true;
          } catch {
            return false;
          }
        }}
      >
        <p>
          {granting?.member.displayName} · {granting?.member.loginName}
        </p>
        <ProFormSelect formItemProps={verticalFormItemProps}
          name="roleIds"
          label="单位角色"
          mode="multiple"
          options={roles}
          placeholder="选择角色，留空表示移除角色"
        />
      </ModalForm>

      <Drawer
        title="用户详情"
        open={!!detail}
        onClose={() => setDetail(undefined)}
        size={480}
        destroyOnHidden
      >
        <ProDescriptions<Member>
          column={1}
          dataSource={detail?.member}
          columns={[
            { title: '姓名', dataIndex: 'displayName' },
            { title: '登录账号', dataIndex: 'loginName' },
            { title: '部门', dataIndex: 'orgId', renderText: departmentName },
            {
              title: '状态',
              dataIndex: 'status',
              valueEnum: { 1: '启用', 0: '停用' },
            },
          ]}
        />
      </Drawer>
    </PageContainer>
  );
}
