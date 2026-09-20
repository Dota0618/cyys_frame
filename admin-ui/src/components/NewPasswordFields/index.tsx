import { ProFormText } from '@ant-design/pro-components';

/** 新建账号、个人改密、管理员重置使用同一密码输入规则；密码不做 trim。 */
export default function NewPasswordFields({ name = 'newPassword', label = '新密码', confirm = true }: {
  name?: string; label?: string; confirm?: boolean;
}) {
  return <>
    <ProFormText.Password name={name} label={label} fieldProps={{ autoComplete: 'new-password', maxLength: 72 }}
      extra="至少 12 位；中文等多字节字符合计不能超过 72 字节。"
      rules={[
        { required: true, whitespace: true, message: '请输入密码' },
        { min: 12, message: '密码至少 12 位' },
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
