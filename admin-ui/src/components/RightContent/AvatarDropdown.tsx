import { LockOutlined, LogoutOutlined, SkinOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { App, Spin } from 'antd';
import type { PropsWithChildren } from 'react';
import { useEffect, useState } from 'react';
import { logout } from '@/services/auth';
import { clearSession } from '@/utils/session';
import HeaderDropdown from '../HeaderDropdown';
import ChangePasswordForm from './ChangePasswordForm';

export const AvatarDropdown = ({ children }: PropsWithChildren) => {
  const { initialState, setInitialState } = useModel('@@initialState');
  const [changingPassword, setChangingPassword] = useState(false);
  useEffect(() => setChangingPassword(false), [initialState?.revision]);
  const onMenuClick: MenuProps['onClick'] = ({ key }) => {
    if (key === 'password') setChangingPassword(true);
    if (key === 'theme') {
      void setInitialState((s) => ({ ...s, settingDrawerOpen: true }));
    }
    if (key === 'logout') {
      void logout()
        .catch(() => {})
        .finally(clearSession);
    }
  };
  if (!initialState?.user) return <Spin size="small" />;
  return (
    <>
    <HeaderDropdown
      placement="bottomRight"
      arrow
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: [
          { key: 'password', icon: <LockOutlined />, label: '修改密码' },
          { key: 'theme', icon: <SkinOutlined />, label: '主题设置' },
          { type: 'divider' },
          { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
        ],
      }}
    >
      {children}
    </HeaderDropdown>
    {changingPassword && <App><ChangePasswordForm onClose={() => setChangingPassword(false)} /></App>}
    </>
  );
};
