import { ProFormText } from '@ant-design/pro-components';
import { useModel } from '@umijs/max';

/** 密码不做 trim；调用方可覆盖系统组合强度。 */
export default function NewPasswordFields({ name = 'newPassword', label = '新密码', confirm = true, strength: strengthOverride }: {
  name?: string; label?: string; confirm?: boolean; strength?: number;
}) {
  const { initialState } = useModel('@@initialState');
  const configuredStrength = Number(initialState?.publicConfigurations?.PWD_STRENGTH || 0);
  const strength = strengthOverride === undefined
    ? (Number.isFinite(configuredStrength) ? Math.min(2, Math.max(0, Math.trunc(configuredStrength))) : 0)
    : Math.min(2, Math.max(0, Math.trunc(strengthOverride)));
  const strengthMessage = strength >= 2
    ? '至少 8 位，且必须包含大写字母、小写字母和数字。'
    : strength >= 1
      ? '至少 8 位，且必须包含字母和数字。'
      : '至少 8 位。';
  return <>
    <ProFormText.Password name={name} label={label} fieldProps={{ autoComplete: 'new-password', maxLength: 72 }}
      extra={`${strengthMessage} 中文等多字节字符合计不能超过 72 字节。`}
      rules={[
        { required: true, whitespace: true, message: '请输入密码' },
        { min: 8, message: '密码至少 8 位' },
        { validator: async (_, value?: string) => {
          if (!value) return;
          if (strength >= 1 && !/[\p{L}]/u.test(value)) throw new Error('密码必须包含字母');
          if (strength >= 1 && !/\p{Nd}/u.test(value)) throw new Error('密码必须包含数字');
          if (strength >= 2 && !/\p{Lu}/u.test(value)) throw new Error('密码必须包含大写字母');
          if (strength >= 2 && !/\p{Ll}/u.test(value)) throw new Error('密码必须包含小写字母');
        } },
        { validator: async (_, value?: string) => {
          if (value && new TextEncoder().encode(value).length > 72) throw new Error('密码不能超过 72 个 UTF-8 字节');
        } },
      ]} />
    {confirm && <ProFormText.Password name="confirmPassword" label="确认新密码" dependencies={[name]}
      fieldProps={{ autoComplete: 'new-password', maxLength: 72 }} rules={[
        { required: true, message: '请再次输入新密码' },
        ({ getFieldValue }) => ({ validator: async (_, value?: string) => {
          if (value && value !== getFieldValue(name)) throw new Error('两次输入的密码不一致');
        } }),
      ]} />}
  </>;
}
