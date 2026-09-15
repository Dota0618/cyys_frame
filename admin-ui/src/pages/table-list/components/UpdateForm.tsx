import {
  type ActionType,
  ModalForm,
  ProFormText,
  ProFormSelect,
  ProFormTreeSelect,
} from '@ant-design/pro-components';
import { App, Descriptions } from 'antd';
import ScopeSwitch from '@/components/ScopeSwitch';
import { editMember } from '@/services/system/users';
import type { DepartmentOption, EditMember, Member } from '@/types/api';
import { departmentTreeData } from '@/utils/departments';
import { setDirty } from '@/utils/session';

export default function UpdateForm({
  values,
  departments,
  onClose,
  reload,
}: {
  values?: Member;
  departments: DepartmentOption[];
  onClose: () => void;
  reload?: ActionType['reload'];
}) {
  const { message } = App.useApp();
  if (!values) return null;
  return (
    <ModalForm<EditMember>
      name="member-edit"
      title="修改成员"
      width={640}
      open
      initialValues={values}
      modalProps={{ destroyOnHidden: true }}
      onOpenChange={(open) => {
        if (!open) {
          onClose();
          setDirty(false);
        }
      }}
      onValuesChange={() => setDirty(true)}
      onFinish={async (form) => {
        try {
          await editMember(values.id, {
            displayName: form.displayName,
            orgId: form.orgId,
            status: form.status,
            version: values.version,
          });
          void message.success('成员资料已更新');
          void reload?.();
          return true;
        } catch {
          return false;
        }
      }}
    >
      <div style={{ marginBottom: 24 }}>
        <ScopeSwitch />
      </div>
      <Descriptions
        size="small"
        column={1}
        style={{ marginBottom: 20 }}
        items={[
          { key: 'account', label: '登录账号', children: values.loginName },
        ]}
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
        fieldProps={{
          allowClear: true,
          showSearch: true,
          treeData: departmentTreeData(departments),
          treeDefaultExpandAll: true,
          treeNodeFilterProp: 'title',
        }}
      />
      <ProFormSelect
        name="status"
        label="成员状态"
        options={[
          { value: 1, label: '启用' },
          { value: 0, label: '停用' },
        ]}
        rules={[{ required: true }]}
      />
    </ModalForm>
  );
}
