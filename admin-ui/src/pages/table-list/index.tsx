import { useAccess, useModel } from '@umijs/max';
import {
  PageContainer,
  ProTable,
  ProDescriptions,
  ModalForm,
  ProFormSelect,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { Alert, App, Drawer, Tag } from 'antd';
import { useEffect, useRef, useState } from 'react';
import type { DepartmentOption, Member, MemberDetail, Option } from '@/types/api';
import {
  departmentOptions,
  grantRoles,
  listMembers,
  memberDetail,
  roleOptions,
} from '@/services/system/users';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty, updateSession } from '@/utils/session';
import ScopeSwitch from '@/components/ScopeSwitch';
import CreateForm from './components/CreateForm';
import UpdateForm from './components/UpdateForm';

export default function TableList() {
  const access = useAccess();
  const { initialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const all = !!session().allUnits;
  const [departments, setDepartments] = useState<DepartmentOption[]>([]);
  const [roles, setRoles] = useState<Option[]>([]);
  const [editing, setEditing] = useState<Member>();
  const [detail, setDetail] = useState<MemberDetail>();
  const [granting, setGranting] = useState<MemberDetail>();
  const [loadError, setLoadError] = useState('');
  const scopes = initialState?.user?.scopes || [];
  const departmentName = (id?: string) =>
    departments.find((item) => item.value === id)?.label || '未设置';

  useEffect(() => {
    if (!all) {
      void departmentOptions()
        .then(setDepartments)
        .catch(() => {});
      if (access.canGrantRoles)
        void roleOptions()
          .then(setRoles)
          .catch(() => {});
    }
    return () => setDirty(false);
  }, [all, access.canGrantRoles]);

  async function open(member: Member, kind: 'detail' | 'edit' | 'roles') {
    try {
      const result = await memberDetail(member.id);
      if (kind === 'detail') setDetail(result);
      if (kind === 'edit') setEditing(result.member);
      if (kind === 'roles') setGranting(result);
    } catch {
      /* 请求入口已处理错误及过期响应。 */
    }
  }
  const columns: ProColumns<Member>[] = [
    {
      title: '成员姓名',
      dataIndex: 'displayName',
      search: all ? false : undefined,
      render: (_, row) =>
        all ? (
          row.displayName
        ) : (
          <a onClick={() => open(row, 'detail')}>{row.displayName}</a>
        ),
    },
    { title: '登录账号', dataIndex: 'loginName', search: false },
    ...(all
      ? ([
          {
            title: '所属单位',
            dataIndex: 'scopeId',
            search: false,
            render: (_: unknown, row: Member) =>
              scopes.find((scope) => scope.id === row.scopeId)?.name ||
              '未命名单位',
          },
        ] as ProColumns<Member>[])
      : ([
          {
            title: '部门',
            dataIndex: 'orgId',
            search: false,
            render: (_: unknown, row: Member) => departmentName(row.orgId),
          },
        ] as ProColumns<Member>[])),
    {
      title: '成员状态',
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
      width: all ? 130 : 180,
      render: (_, row) =>
        all
          ? [
              <a
                key="scope"
                onClick={() =>
                  updateSession({
                    token: session().token,
                    scopeId: row.scopeId,
                  })
                }
              >
                进入该单位
              </a>,
            ]
          : [
              <a key="detail" onClick={() => open(row, 'detail')}>
                详情
              </a>,
              access.canEditUsers && (
                <a key="edit" onClick={() => open(row, 'edit')}>
                  修改
                </a>
              ),
              access.canGrantRoles && (
                <a key="roles" onClick={() => open(row, 'roles')}>
                  角色
                </a>
              ),
            ].filter(Boolean),
    },
  ];
  return (
    <PageContainer
      title="用户管理"
      subTitle={
        all
          ? '跨单位查看成员；进入具体单位后进行管理。'
          : '管理当前单位成员，账号资料由独立入口维护。'
      }
    >
      {all && (
        <Alert
          type="info"
          showIcon
          title="全部单位只读视图"
          style={{ marginBottom: 20 }}
        />
      )}
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
        headerTitle="成员列表"
        columns={columns}
        form={{ name: 'member-search' }}
        search={all ? false : { labelWidth: 'auto' }}
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
          setLoadError('');
          try {
            const result = await listMembers(
              params.current || 1,
              params.pageSize || 20,
              params.displayName,
            );
            return { data: result.records, total: result.total, success: true };
          } catch (error) {
            if (!(error instanceof StaleRequest))
              setLoadError(
                error instanceof ApiError
                  ? error.message
                  : '成员列表加载失败，请重试',
              );
            return { data: [], total: 0, success: false };
          }
        }}
        toolBarRender={() =>
          access.canCreateUsers
            ? [
                <CreateForm
                  key="create"
                  departments={departments}
                  reload={async () => {
                    await action.current?.reload();
                  }}
                />,
              ]
            : []
        }
      />

      <UpdateForm
        key={editing?.id || 'edit'}
        values={editing}
        departments={departments}
        onClose={() => setEditing(undefined)}
        reload={async () => {
          await action.current?.reload();
        }}
      />

      <ModalForm<{ roleIds: string[] }>
        name="member-roles"
        key={granting?.member.id || 'roles'}
        title="授予本单位角色"
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
        <div style={{ marginBottom: 24 }}>
          <ScopeSwitch />
        </div>
        <p>
          {granting?.member.displayName} · {granting?.member.loginName}
        </p>
        <ProFormSelect
          name="roleIds"
          label="单位角色"
          mode="multiple"
          options={roles}
          placeholder="选择角色，留空表示移除本单位角色"
        />
      </ModalForm>

      <Drawer
        title="成员详情"
        open={!!detail}
        onClose={() => setDetail(undefined)}
        size={480}
        destroyOnHidden
      >
        <ProDescriptions<Member>
          column={1}
          dataSource={detail?.member}
          columns={[
            { title: '本单位姓名', dataIndex: 'displayName' },
            { title: '登录账号', dataIndex: 'loginName' },
            { title: '部门', dataIndex: 'orgId', renderText: departmentName },
            {
              title: '成员状态',
              dataIndex: 'status',
              valueEnum: { 1: '启用', 0: '停用' },
            },
          ]}
        />
      </Drawer>
    </PageContainer>
  );
}
