import { LogoutOutlined, SkinOutlined } from '@ant-design/icons';
import { useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { Spin } from 'antd';
import type { PropsWithChildren } from 'react';
import { logout } from '@/services/auth';
import { clearSession } from '@/utils/session';
import HeaderDropdown from '../HeaderDropdown';

export const AvatarDropdown = ({ children }: PropsWithChildren) => {
  const { initialState, setInitialState } = useModel('@@initialState');
  const onMenuClick: MenuProps['onClick'] = ({ key }) => {
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
    <HeaderDropdown
      placement="bottomRight"
      arrow
      menu={{
        selectedKeys: [],
        onClick: onMenuClick,
        items: [
          { key: 'theme', icon: <SkinOutlined />, label: '主题设置' },
          { type: 'divider' },
          { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
        ],
      }}
    >
      {children}
    </HeaderDropdown>
  );
};
