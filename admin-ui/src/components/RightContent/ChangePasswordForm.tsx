import { compactFormProps } from '@/components/ManagementLayout';
import { ModalForm, ProFormText } from '@ant-design/pro-components';
import { App } from 'antd';
import { useEffect, useId } from 'react';
import NewPasswordFields from '@/components/NewPasswordFields';
import { changePassword } from '@/services/auth';
import { clearSession, setDirty } from '@/utils/session';

export default function ChangePasswordForm({ onClose }: { onClose: () => void }) {
  const { message } = App.useApp();
  const owner = useId();
  useEffect(() => () => setDirty(false, owner), [owner]);
  return <ModalForm<{ currentPassword: string; newPassword: string; confirmPassword: string }> {...compactFormProps}
    name="account-password" title="修改密码" open width={480} modalProps={{ destroyOnHidden: true }}
    onValuesChange={() => setDirty(true, owner)} onOpenChange={(open) => { if (!open) onClose(); }}
    onFinish={async ({ currentPassword, newPassword }) => {
      try {
        await changePassword({ currentPassword, newPassword });
        setDirty(false, owner); onClose();
        void message.success('密码已修改，请重新登录');
        clearSession(); return true;
      } catch { return false; }
    }}>
    <p>修改后，此账号在所有单位的原有登录均会失效。</p>
    <ProFormText.Password name="currentPassword" label="原密码" fieldProps={{ autoComplete: 'current-password', maxLength: 72 }}
      rules={[{ required: true, message: '请输入原密码' }]} />
    <NewPasswordFields />
  </ModalForm>;
}
