import {
  type ActionType,
  ModalForm,
  ProFormText,
  ProFormSelect,
  ProFormTreeSelect,
} from '@ant-design/pro-components';
import { App, Descriptions } from 'antd';
import { CompactFields, compactModalProps, compactFormProps } from '@/components/ManagementLayout';
import DataPermissionFields from './DataPermissionFields';
import { editMember } from '@/services/system/users';
import type { DepartmentOption, EditMember, Member, MemberDataPermission, MemberDataPermissionInput } from '@/types/api';
import { departmentTreeData } from '@/utils/departments';
import { setDirty, refreshIdentity } from '@/utils/session';

export default function UpdateForm({
  values,
  departments,
  permission,
  onClose,
  reload,
}: {
  values?: Member;
  departments: DepartmentOption[];
  permission?: MemberDataPermission;
  onClose: () => void;
  reload?: ActionType['reload'];
}) {
  const { message } = App.useApp();
  if (!values) return null;
  return (
    <ModalForm<EditMember & MemberDataPermissionInput> {...compactFormProps}
      name="member-edit"
      title="修改成员"
      width={920}
      open
      initialValues={{ ...values, orgIds: permission?.orgIds || [] }}
      modalProps={compactModalProps}
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
            ...(permission ? { dataPermission: {
              dataRange: form.dataRange, orgIds: form.dataRange === 2 ? form.orgIds || [] : [],
            } } : {}),
          });
          void message.success('成员资料已更新');
          void reload?.();
          if (permission) refreshIdentity();
          return true;
        } catch {
          return false;
        }
      }}
    >
      <CompactFields><div>
      <Descriptions
        size="small"
        column={1}
        style={{ marginBottom: 14 }}
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
      </div><div><DataPermissionFields departments={permission?.departments || departments}
        editable={!!permission} currentRange={values.dataRange} /></div></CompactFields>
    </ModalForm>
  );
}
