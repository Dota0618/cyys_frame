import {
  BookOutlined,
  CheckOutlined,
  ForkOutlined,
  GlobalOutlined,
} from '@ant-design/icons';
import { getAllLocales, getLocale, history, Link, setLocale, useAccess, useLocation, useModel } from '@umijs/max';
import type { MenuProps } from 'antd';
import { Breadcrumb, Button, Tooltip } from 'antd';
import { createStyles } from 'antd-style';
import React, { useMemo } from 'react';
import HeaderDropdown from '../HeaderDropdown';
import { pageTitles } from '../PageContainer';
import routes from '../../../config/routes';

export const localeLabelMap: Record<string, { emoji: string; label: string }> =
  {
    'zh-CN': { emoji: '🇨🇳', label: '简体中文' },
    'zh-TW': { emoji: '🇭🇰', label: '繁體中文' },
    'en-US': { emoji: '🇺🇸', label: 'English' },
    'ja-JP': { emoji: '🇯🇵', label: '日本語' },
    'pt-BR': { emoji: '🇧🇷', label: 'Português' },
    'id-ID': { emoji: '🇮🇩', label: 'Bahasa Indonesia' },
    'fa-IR': { emoji: '🇮🇷', label: 'فارسی' },
    'bn-BD': { emoji: '🇧🇩', label: 'বাংলা' },
  };

const useStyles = createStyles(({ token, css }) => ({
  action: css`
    display: inline-flex !important;
    align-items: center !important;
    justify-content: center !important;
    height: 32px !important;
    min-width: 32px;
    padding-inline: 8px !important;
    padding-block: 0 !important;
    border-radius: ${token.borderRadius}px !important;
  `,
  breadcrumb: css`
    min-width: 0;
    padding-inline: 12px;
    overflow: hidden;
    .ant-breadcrumb ol { flex-wrap: nowrap; }
    .ant-breadcrumb li:last-child { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    @media (max-width: 767px) { display: none; }
  `,
}));

export function HeaderBreadcrumb() {
  const { styles } = useStyles();
  const { initialState } = useModel('@@initialState');
  const access = useAccess();
  const location = useLocation();
  const title = pageTitles(initialState?.user?.navigation || [], routes, access).get(location.pathname)?.path;
  if (!title) return null;
  const items = title === '工作台' ? [{ title }] : [
    { title: <Link to="/workspace">工作台</Link> },
    ...title.split(' > ').map(name => ({ title: name })),
  ];
  return <nav className={styles.breadcrumb} aria-label="当前位置"><Breadcrumb items={items} /></nav>;
}
export const DocLink: React.FC = () => {
  const { styles } = useStyles();
  return (
    <Tooltip title="使用文档">
      <Button
        type="text"
        className={styles.action}
        icon={<BookOutlined />}
        aria-label="使用文档"
        onClick={() => {
          history.push('/welcome');
        }}
      />
    </Tooltip>
  );
};

const versionItems: MenuProps['items'] = [
  { key: 'https://v5.pro.ant.design', label: 'v5' },
  { key: 'https://v4.pro.ant.design', label: 'v4' },
  { key: 'https://v2.pro.ant.design', label: 'v2' },
  { key: 'https://v1.pro.ant.design', label: 'v1' },
];

const onVersionClick: MenuProps['onClick'] = ({ key }) => {
  window.open(key, '_blank', 'noopener,noreferrer');
};

export const VersionDropdown: React.FC = () => {
  const { styles } = useStyles();
  return (
    <HeaderDropdown
      placement="bottomRight"
      arrow
      menu={{
        selectedKeys: [],
        onClick: onVersionClick,
        items: versionItems,
        style: { minWidth: 100 },
      }}
    >
      <Button type="text" className={styles.action} aria-label="历史版本">
        <ForkOutlined />
      </Button>
    </HeaderDropdown>
  );
};

export const LangDropdown: React.FC = () => {
  const { styles } = useStyles();
  const allLocales = useMemo(() => getAllLocales(), []);
  const currentLocale = getLocale();
  const supportLocales = allLocales.filter((l) => l in localeLabelMap);

  if (supportLocales.length <= 1) {
    return null;
  }

  const langItems: MenuProps['items'] = supportLocales.map((locale) => ({
    key: `lang-${locale}`,
    icon:
      locale === currentLocale ? (
        <CheckOutlined style={{ color: '#52c41a' }} />
      ) : (
        <span style={{ display: 'inline-block', width: 14 }} />
      ),
    label: `${localeLabelMap[locale]?.emoji ?? ''} ${localeLabelMap[locale]?.label ?? locale}`,
  }));

  const onLangClick: MenuProps['onClick'] = ({ key }) => {
    if (key.startsWith('lang-')) {
      setLocale(key.replace('lang-', ''), false);
    }
  };

  return (
    <HeaderDropdown
      placement="bottomRight"
      arrow
      menu={{
        selectedKeys: [`lang-${currentLocale}`],
        onClick: onLangClick,
        items: langItems,
        style: { minWidth: 180 },
      }}
    >
      <Button type="text" className={styles.action} aria-label="语言切换">
        <GlobalOutlined />
      </Button>
    </HeaderDropdown>
  );
};
