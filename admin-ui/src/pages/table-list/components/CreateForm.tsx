import { PlusOutlined } from '@ant-design/icons';
import {
  type ActionType,
  ModalForm,
  ProFormText,
  ProFormTreeSelect,
} from '@ant-design/pro-components';
import { useMutation } from '@tanstack/react-query';
import { App, Button } from 'antd';
import ScopeSwitch from '@/components/ScopeSwitch';
import { createMember } from '@/services/system/users';
import type { CreateMember, DepartmentOption } from '@/types/api';
import { departmentTreeData } from '@/utils/departments';
import { setDirty } from '@/utils/session';

export default function CreateForm({
  reload,
  departments,
}: {
  reload?: ActionType['reload'];
  departments: DepartmentOption[];
}) {
  const { message } = App.useApp();
  const { mutateAsync: run, isPending: loading } = useMutation({
    mutationFn: createMember,
    onSuccess: () => {
      void message.success('成员已新增');
      void reload?.();
    },
  });
  return (
    <ModalForm<CreateMember>
      name="member-create"
      title="新增成员"
      width={480}
      trigger={
        <Button type="primary" icon={<PlusOutlined />}>
          新增成员
        </Button>
      }
      modalProps={{ destroyOnHidden: true, okButtonProps: { loading } }}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => {
        if (!open) setDirty(false);
      }}
      onFinish={async (values) => {
        try {
          await run(values);
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
        name="loginName"
        label="登录账号"
        placeholder="3–64 位字母、数字或 . _ -"
        rules={[
          { required: true },
          {
            pattern: /^[A-Za-z0-9][A-Za-z0-9_.-]{2,63}$/,
            message: '账号格式不正确',
          },
        ]}
      />
      <ProFormText.Password
        name="password"
        label="初始密码"
        fieldProps={{ autoComplete: 'new-password', maxLength: 72 }}
        rules={[{ required: true }, { min: 12, message: '密码至少 12 位' }]}
      />
      <ProFormText
        name="displayName"
        label="本单位姓名"
        fieldProps={{ maxLength: 100 }}
        rules={[{ required: true, whitespace: true }]}
      />
      <ProFormTreeSelect
        name="orgId"
        label="部门"
        placeholder="选择所属部门，可不设置"
        fieldProps={{
          allowClear: true,
          showSearch: true,
          treeData: departmentTreeData(departments),
          treeDefaultExpandAll: true,
          treeNodeFilterProp: 'title',
        }}
      />
    </ModalForm>
  );
}
