import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProFormText,
  ProFormTreeSelect,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Popconfirm } from 'antd';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
import {
  createDepartment,
  deleteDepartment,
  departmentParentOptions,
  departmentTreeOptions,
  editDepartment,
  listDepartments,
} from '@/services/system/departments';
import type {
  CreateDepartment,
  Department,
  DepartmentOption,
  EditDepartment,
} from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { departmentTreeData } from '@/utils/departments';
import { setDirty } from '@/utils/session';

export default function DepartmentsPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [treeOptions, setTreeOptions] = useState<DepartmentOption[]>([]);
  const [parentOptions, setParentOptions] = useState<DepartmentOption[]>([]);
  const [editing, setEditing] = useState<Department>();
  const [loadError, setLoadError] = useState('');

  const reloadOptions = useCallback(async () => {
    const tree = await departmentTreeOptions();
    setTreeOptions(tree);
    if (access.canCreateDepartments) {
      setParentOptions(await departmentParentOptions());
    } else {
      setParentOptions([]);
    }
  }, [access.canCreateDepartments]);

  useEffect(() => {
    void reloadOptions().catch(() => {});
    return () => setDirty(false);
  }, [reloadOptions]);

  const names = useMemo(
    () => new Map(treeOptions.map((item) => [item.value, item.label])),
    [treeOptions],
  );

  async function reload() {
    await Promise.all([action.current?.reload(), reloadOptions()]);
  }

  const columns: ProColumns<Department>[] = [
    {
      title: '部门名称',
      dataIndex: 'name',
    },
    {
      title: '部门编码',
      dataIndex: 'code',
      search: false,
      renderText: (value) => value || '—',
    },
    {
      title: '上级部门',
      dataIndex: 'parentId',
      search: false,
      renderText: (value) => (value ? names.get(value) || '历史部门' : '—'),
    },
    {
      title: '操作',
      valueType: 'option',
      width: 150,
      render: (_, row) =>
        [
          access.canEditDepartments && (
            <a key="edit" onClick={() => setEditing(row)}>
              修改
            </a>
          ),
          access.canDeleteDepartments && (
            <Popconfirm
              key="delete"
              title="删除部门"
              description="存在子部门、成员或角色范围引用时，服务端会拒绝删除。"
              okText="删除"
              cancelText="取消"
              onConfirm={async () => {
                await deleteDepartment(row.id, row.version);
                void message.success('部门已删除');
                await reload();
              }}
            >
              <a>删除</a>
            </Popconfirm>
          ),
        ].filter(Boolean),
    },
  ];

  return (
    <PageContainer
      title="部门管理"
      subTitle="维护当前单位的组织结构；父部门候选和保存操作均按服务端权限范围校验。"
    >
      {loadError && (
        <Alert
          type="error"
          showIcon
          title={loadError}
          style={{ marginBottom: 20 }}
        />
      )}
      <ProTable<Department>
        actionRef={action}
        rowKey="id"
        headerTitle="部门列表"
        columns={columns}
        form={{ name: 'department-search' }}
        search={{ labelWidth: 'auto' }}
        options={{ density: true, fullScreen: false, setting: false, reload: true }}
        pagination={{
          defaultPageSize: 20,
          showSizeChanger: true,
          pageSizeOptions: [10, 20, 50, 100, 200],
        }}
        request={async (params) => {
          setLoadError('');
          try {
            const result = await listDepartments(
              params.current || 1,
              params.pageSize || 20,
              params.name,
            );
            return { data: result.records, total: result.total, success: true };
          } catch (error) {
            if (!(error instanceof StaleRequest)) {
              setLoadError(
                error instanceof ApiError
                  ? error.message
                  : '部门列表加载失败，请重试',
              );
            }
            return { data: [], total: 0, success: false };
          }
        }}
        toolBarRender={() =>
          access.canCreateDepartments
            ? [
                <ModalForm<CreateDepartment>
                  key="create"
                  name="department-create"
                  title="新增部门"
                  width={520}
                  trigger={
                    <Button type="primary" icon={<PlusOutlined />}>
                      新增部门
                    </Button>
                  }
                  modalProps={{ destroyOnHidden: true }}
                  onValuesChange={() => setDirty(true)}
                  onOpenChange={(open) => {
                    if (!open) setDirty(false);
                  }}
                  onFinish={async (values) => {
                    try {
                      await createDepartment(values);
                      void message.success('部门已新增');
                      await reload();
                      return true;
                    } catch {
                      return false;
                    }
                  }}
                >
                  <div style={{ marginBottom: 24 }}>
                    <ScopeSwitch />
                  </div>
                  <ProFormText
                    name="name"
                    label="部门名称"
                    fieldProps={{ maxLength: 100 }}
                    rules={[{ required: true, whitespace: true }]}
                  />
                  <ProFormText
                    name="code"
                    label="部门编码"
                    fieldProps={{ maxLength: 64 }}
                  />
                  <ProFormTreeSelect
                    name="parentId"
                    label="上级部门"
                    placeholder="不选择表示建立根部门"
                    fieldProps={{
                      allowClear: true,
                      showSearch: true,
                      treeData: departmentTreeData(parentOptions),
                      treeDefaultExpandAll: true,
                      treeNodeFilterProp: 'title',
                    }}
                  />
                </ModalForm>,
              ]
            : []
        }
      />

      <ModalForm<EditDepartment>
        key={editing?.id || 'edit'}
        name="department-edit"
        title="修改部门"
        width={480}
        open={!!editing}
        initialValues={editing}
        modalProps={{ destroyOnHidden: true }}
        onValuesChange={() => setDirty(true)}
        onOpenChange={(open) => {
          if (!open) {
            setEditing(undefined);
            setDirty(false);
          }
        }}
        onFinish={async (values) => {
          if (!editing) return false;
          try {
            await editDepartment({
              id: editing.id,
              version: editing.version,
              name: values.name,
            });
            void message.success('部门已更新');
            await reload();
            return true;
          } catch {
            return false;
          }
        }}
      >
        <div style={{ marginBottom: 24 }}>
          <ScopeSwitch />
        </div>
        <ProFormText
          name="name"
          label="部门名称"
          fieldProps={{ maxLength: 100 }}
          rules={[{ required: true, whitespace: true }]}
        />
      </ModalForm>
    </PageContainer>
  );
}
