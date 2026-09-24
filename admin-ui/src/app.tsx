import { UserOutlined } from '@ant-design/icons';
import type { Settings as LayoutSettings } from '@ant-design/pro-components';
import { SettingDrawer } from '@ant-design/pro-components';
import type { RunTimeLayoutConfig } from '@umijs/max';
import { Helmet, history, Link } from '@umijs/max';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import React from 'react';

// Initialize dayjs plugins globally
dayjs.extend(relativeTime);

import {
  AvatarDropdown,
  ErrorBoundary,
  Footer,
  LangDropdown,
  OfflineBanner,
} from '@/components';
import { currentUser } from '@/services/auth';
import { publicConfigurations } from '@/services/system/configurations';
import IdentityBoundary from '@/components/IdentityBoundary';
import ScopeSwitch from '@/components/ScopeSwitch';
import type { PublicConfiguration, UserInfo } from '@/types/api';
import { clearSession, session } from '@/utils/session';
import defaultSettings from '../config/defaultSettings';
import createAccess from '@/access';
import { navigationTree } from '@/utils/menus';

const loginPath = '/user/login';

export async function getInitialState(): Promise<{
  settings?: Partial<LayoutSettings>;
  user?: UserInfo;
  publicConfigurations?: PublicConfiguration;
  revision?: number;
  switching?: boolean;
  settingDrawerOpen?: boolean;
}> {
  let publicConfig: PublicConfiguration = {};
  try {
    publicConfig = await publicConfigurations();
  } catch {
    // 公共配置不可用时继续使用构建时默认值，不阻断登录页。
  }
  let user: UserInfo | undefined;
  if (session().token) {
    try {
      user = await currentUser();
    } catch {
      clearSession();
    }
  }
  return {
    user,
    revision: session().revision,
    publicConfigurations: publicConfig,
    settings: { ...defaultSettings, title: publicConfig.SYSTEM_NAME || defaultSettings.title },
    settingDrawerOpen: false,
  };
}

// ProLayout 支持的api https://procomponents.ant.design/components/layout
export const layout: RunTimeLayoutConfig = ({
  initialState,
  setInitialState,
}) => {
  const systemName = initialState?.settings?.title || defaultSettings.title;
  return {
    menuDataRender: (items) => navigationTree(initialState?.user?.navigation || [], items, createAccess(initialState)),
    menuItemRender: (item, dom) => {
      if (item.path) {
        return (
          <Link to={item.path} prefetch>
            {dom}
          </Link>
        );
      }
      return dom;
    },
    actionsRender: () => [
      <ScopeSwitch key="scope" />,
      <LangDropdown key="lang" />,
    ],
    avatarProps: {
      icon: <UserOutlined />,
      title: initialState?.user?.displayName,
      render: (_, avatarChildren) => (
        <AvatarDropdown>{avatarChildren}</AvatarDropdown>
      ),
    },
    // waterMarkProps: {
    //   content: initialState?.currentUser?.name,
    // },
    footerRender: () => <Footer />,
    onPageChange: () => {
      const { location } = history;
      // 如果没有登录，重定向到 login
      if (
        !session().token &&
        ![loginPath, '/login'].includes(location.pathname)
      ) {
        history.replace(
          `${loginPath}?redirect=${encodeURIComponent(location.pathname + location.search + location.hash)}`,
        );
      }
    },
    bgLayoutImgList: [
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/D2LWSqNny4sAAAAAAAAAAAAAFl94AQBr',
        left: 85,
        bottom: 100,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/C2TWRpJpiC0AAAAAAAAAAAAAFl94AQBr',
        bottom: -68,
        right: -45,
        height: '303px',
      },
      {
        src: 'https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/F6vSTbj8KpYAAAAAAAAAAAAAFl94AQBr',
        bottom: 0,
        left: 0,
        width: '331px',
      },
    ],
    // Replace ProLayout's default ErrorBoundary with our offline-aware version,
    // so chunk load errors show friendly messages instead of "Something went wrong."
    ErrorBoundary,
    menuHeaderRender: undefined,
    // 自定义 403 页面
    // unAccessible: <div>unAccessible</div>,
    // 增加一个 loading 的状态
    childrenRender: (children) => {
      // if (initialState?.loading) return <PageLoading />;
      return (
        <>
          <Helmet>
            <title>{systemName}</title>
          </Helmet>
          <IdentityBoundary>{children}</IdentityBoundary>
          <SettingDrawer
            disableUrlParams
            enableDarkTheme
            collapse={initialState?.settingDrawerOpen}
            onCollapseChange={(open) => {
              setInitialState((s) => ({
                ...s,
                settingDrawerOpen: open,
              }));
            }}
            settings={initialState?.settings}
            onSettingChange={(settings) => {
              setInitialState((s) => ({
                ...s,
                settings,
              }));
            }}
          />
        </>
      );
    },
    ...initialState?.settings,
  };
};

export function rootContainer(container: React.ReactNode) {
  return (
    <>
      <OfflineBanner />
      <ErrorBoundary>{container}</ErrorBoundary>
    </>
  );
}
