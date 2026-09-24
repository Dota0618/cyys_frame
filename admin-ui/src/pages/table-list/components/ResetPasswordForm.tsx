import { compactFormProps } from '@/components/ManagementLayout';
import { ModalForm } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';
import { App } from 'antd';
import { useEffect, useId } from 'react';
import NewPasswordFields from '@/components/NewPasswordFields';
import { resetAccountPassword } from '@/services/auth';
import type { Member } from '@/types/api';
import { clearSession, setDirty } from '@/utils/session';

export default function ResetPasswordForm({ account, onClose }: { account: Member; onClose: () => void }) {
  const { initialState } = useModel('@@initialState');
  const { message } = App.useApp();
  const owner = useId();
  useEffect(() => () => setDirty(false, owner), [owner]);
  return <ModalForm<{ newPassword: string; confirmPassword: string }> {...compactFormProps} name="reset-account-password"
    title={`重置密码：${account.loginName}`} open width={480} modalProps={{ destroyOnHidden: true }}
    onValuesChange={() => setDirty(true, owner)} onOpenChange={(open) => { if (!open) onClose(); }}
    onFinish={async ({ newPassword }) => {
      try {
        await resetAccountPassword(account.userId, { newPassword });
        setDirty(false, owner); onClose();
        void message.success('密码已重置，该账号需要重新登录');
        if (account.userId === initialState?.user?.id) clearSession();
        return true;
      } catch { return false; }
    }}>
    <p>这会重置账号的登录密码，并使其原有登录失效。请通过合适的方式将新密码交给账号本人。</p>
    <NewPasswordFields strength={0} />
  </ModalForm>;
}
