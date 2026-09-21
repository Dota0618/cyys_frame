import { ModalForm } from '@ant-design/pro-components';
import DataPermissionFields from './DataPermissionFields';
import { compactModalProps, compactFormProps } from '@/components/ManagementLayout';
import { updateMemberDataPermission } from '@/services/system/users';
import type { Member, MemberDataPermission, UpdateMemberDataPermission } from '@/types/api';
import { setDirty, refreshIdentity } from '@/utils/session';

export default function DataPermissionForm({ member, permission, onClose, onSaved }: {
  member: Member; permission: MemberDataPermission; onClose: () => void; onSaved: () => void;
}) {
  return <ModalForm<UpdateMemberDataPermission> {...compactFormProps} name="member-data-permission" title={`数据权限：${member.displayName}`} open
    width={680} modalProps={compactModalProps} initialValues={permission}
    onValuesChange={() => setDirty(true)}
    onOpenChange={open => { if (!open) { setDirty(false); onClose(); } }}
    onFinish={async values => {
      try {
        await updateMemberDataPermission(member.id, {
          version: permission.version, dataRange: values.dataRange,
          orgIds: values.dataRange === 2 ? values.orgIds || [] : [],
        });
        setDirty(false); onClose(); onSaved(); refreshIdentity(); return true;
      } catch { return false; }
    }}>
    <DataPermissionFields departments={permission.departments} systemAccount={!member.scopeId} />
  </ModalForm>;
}
