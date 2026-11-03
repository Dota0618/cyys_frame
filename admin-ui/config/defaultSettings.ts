import type { ProLayoutProps } from '@ant-design/pro-components';

const Settings = {
  navTheme: 'light',
  colorPrimary: '#1890ff',
  layout: 'mix',
  contentWidth: 'Fluid',
  fixedHeader: false,
  fixSiderbar: true,
  siderWidth: 224,
  colorWeak: false,
  logo: '/logo.svg',
  iconfontUrl: '',
  token: {
    bgLayout: 'var(--ant-color-bg-container)',
    header: {
      colorBgHeader: 'var(--ant-color-bg-container)',
      colorBgScrollHeader: 'var(--ant-color-bg-container)',
    },
    sider: {
      colorMenuBackground: 'var(--ant-color-bg-container)',
      colorBgMenuItemHover: 'var(--ant-color-primary-bg-hover)',
      colorBgMenuItemSelected: 'var(--ant-color-primary-bg)',
      colorTextMenuSelected: 'var(--ant-color-primary-text-active)',
      colorTextMenuActive: 'var(--ant-color-primary-text-active)',
      colorTextMenuItemHover: 'var(--ant-color-primary-text)',
      colorTextMenu: 'var(--ant-color-text-secondary)',
      colorTextMenuSecondary: 'var(--ant-color-text-tertiary)',
      colorTextSubMenuSelected: 'var(--ant-color-primary-text-active)',
      colorTextMenuTitle: 'var(--ant-color-text)',
      colorBgMenuItemCollapsedElevated: 'var(--ant-color-bg-elevated)',
      paddingInlineLayoutMenu: 8,
      paddingBlockLayoutMenu: 8,
    },
  },
} satisfies ProLayoutProps;

export default Settings;
