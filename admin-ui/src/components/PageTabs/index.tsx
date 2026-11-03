import { AppstoreOutlined, EllipsisOutlined, ReloadOutlined } from '@ant-design/icons';
import { ProProvider, RouteContext } from '@ant-design/pro-components';
import { history, useAccess, useLocation, useModel } from '@umijs/max';
import { App, Button, Dropdown, Tabs, Tooltip } from 'antd';
import type { MenuProps } from 'antd';
import { createStyles } from 'antd-style';
import { useCallback, useContext, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import type { PropsWithChildren } from 'react';
import routes from '../../../config/routes';
import { confirmModalProps } from '@/components/ManagementLayout';
import { PageTitleContext, pageTitles } from '@/components/PageContainer';
import { DIRTY_CHANGED, hasUnsavedChanges, session } from '@/utils/session';
import { TOGGLE_PAGE_TABS, WORKSPACE_PATH } from '@/utils/page-tabs';
import type { PageTab } from '@/utils/page-tabs';
import { PageStateContext } from './context';

const useStyles = createStyles(({ css, token }) => ({
  bar: css`
    position: fixed;
    z-index: 99;
    min-width: 0;
    background: ${token.colorBgContainer};
    border-bottom: 1px solid ${token.colorBorderSecondary};
  `,
  tabs: css`
    min-width: 0;
    background: ${token.colorBgContainer};
    && > .ant-tabs-nav {
      min-height: 40px;
      margin: 0;
      padding: 0 12px;
    }
    && > .ant-tabs-nav::before { border: 0; }
    && .ant-tabs-nav-list { column-gap: 0; }
    && > .ant-tabs-nav .ant-tabs-tab {
      margin: 4px 8px 4px 0;
      padding: 4px 12px;
      border: 1px solid ${token.colorBorderSecondary};
      border-radius: 6px;
      background: ${token.colorBgContainer};
      color: ${token.colorTextSecondary};
      font-size: 13px;
      line-height: 22px;
      transition: color ${token.motionDurationMid}, background ${token.motionDurationMid};
    }
    && .ant-tabs-tab:hover { color: ${token.colorPrimary}; }
    && > .ant-tabs-nav .ant-tabs-tab-active {
      background: ${token.colorPrimaryBg};
      border-color: ${token.colorPrimaryBorder};
    }
    && .ant-tabs-tab-active .ant-tabs-tab-btn {
      color: ${token.colorPrimary};
      font-weight: 500;
      text-shadow: none;
    }
    && .ant-tabs-tab-remove {
      margin-inline-start: 8px;
      margin-inline-end: 0;
      padding: 0;
      color: inherit;
      font-size: 12px;
    }
    && .ant-tabs-tab-active .ant-tabs-tab-remove,
    && .ant-tabs-tab-remove:hover { color: ${token.colorPrimary}; }
    .ant-tabs-nav-operations { align-self: stretch; }
    .ant-tabs-content-holder { display: none; }
  `,
  page: css`min-width: 0;`,
  toggle: css`
    height: 32px;
    padding-inline: 8px;
    border-radius: ${token.borderRadius}px;
  `,
  toggleLabel: css`@media (max-width: 767px) { display: none; }`,
}));

export function PageTabsToggle() {
  const enabled = useModel('pageTabs', model => model.enabled);
  const { styles } = useStyles();
  return <Tooltip title={enabled ? '关闭多页签' : '开启多页签'}>
    <Button type="text" className={styles.toggle} icon={<AppstoreOutlined />}
      aria-label={enabled ? '关闭多页签' : '开启多页签'} aria-pressed={enabled}
      color={enabled ? 'primary' : 'default'} variant="text"
      onClick={() => window.dispatchEvent(new Event(TOGGLE_PAGE_TABS))}><span className={styles.toggleLabel}>多页签</span></Button>
  </Tooltip>;
}

export default function PageTabs({ children }: PropsWithChildren) {
  const model = useModel('pageTabs');
  const { initialState } = useModel('@@initialState');
  const access = useAccess();
  const location = useLocation();
  const path = location.pathname;
  const { modal } = App.useApp();
  const { styles } = useStyles();
  const layout = useContext(RouteContext);
  const { token: layoutToken } = useContext(ProProvider);
  const hasFixedHeader = layout.hasHeader && (layout.fixedHeader || layout.layout === 'mix') &&
    (layout.layout !== 'side' || layout.isMobile);
  const headerOffset = hasFixedHeader ? layoutToken.layout?.header?.heightLayoutHeader || 56 : 0;
  const bar = useRef<HTMLDivElement>(null);
  const [barHeight, setBarHeight] = useState(0);

  useLayoutEffect(() => {
    const element = bar.current;
    if (!element) { setBarHeight(0); return; }
    const measure = () => setBarHeight(element.getBoundingClientRect().height);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, [model.enabled]);

  const [refresh, setRefresh] = useState(0);
  const pendingClose = useRef<{ path: string; next: string } | undefined>(undefined);
  const confirmRef = useRef<ReturnType<typeof modal.confirm> | undefined>(undefined);
  const stateRef = useRef({ model, path, location, revision: initialState?.revision });
  stateRef.current = { model, path, location, revision: initialState?.revision };

  const titles = useMemo(() => pageTitles(initialState?.user?.navigation || [], routes, access),
    [access, initialState?.user?.navigation]);

  const current = useMemo<PageTab>(() => ({ path, title: titles.get(path)?.name || '', search: location.search, hash: location.hash, state: location.state }),
    [path, titles, location.search, location.hash, location.state]);

  useLayoutEffect(() => {
    if (current.title && initialState?.revision === session().revision) model.visit(current);
  }, [current, initialState?.revision, model.visit]);

  const confirmAction = useCallback((action: () => void) => {
    if (confirmRef.current) return;
    if (!hasUnsavedChanges()) { action(); return; }
    const revision = session().revision;
    const instance = modal.confirm({
      ...confirmModalProps,
      title: '离开当前页面？',
      content: '未保存的修改将丢失，正在上传的任务会暂停。',
      okText: '继续', cancelText: '继续编辑',
      onOk: () => { if (revision === session().revision) action(); },
      onCancel: () => { pendingClose.current = undefined; },
      afterClose: () => { if (confirmRef.current === instance) confirmRef.current = undefined; },
    });
    confirmRef.current = instance;
  }, [modal]);

  useEffect(() => {
    let unblock: (() => void) | undefined;
    let alive = true;
    const syncGuard = () => {
      unblock?.(); unblock = undefined;
      if (!alive || !hasUnsavedChanges()) return;
      unblock = history.block(transition => {
        const retry = () => { if (!alive) return; unblock?.(); unblock = undefined; transition.retry(); };
        if (!session().token || stateRef.current.revision !== session().revision ||
          transition.location.pathname === stateRef.current.path) retry();
        else confirmAction(retry);
      });
    };
    const unlisten = history.listen(({ location: next }) => {
      const pending = pendingClose.current;
      pendingClose.current = undefined;
      if (pending && next.pathname === pending.next) stateRef.current.model.close(pending.path);
      syncGuard();
    });
    window.addEventListener(DIRTY_CHANGED, syncGuard);
    syncGuard();
    return () => {
      alive = false; unblock?.(); unlisten();
      window.removeEventListener(DIRTY_CHANGED, syncGuard);
      confirmRef.current?.destroy(); confirmRef.current = undefined;
    };
  }, [confirmAction]);

  useEffect(() => {
    const toggle = () => {
      const state = stateRef.current;
      const tab = state.model.tabs.find(item => item.path === state.path);
      if (tab) state.model.setEnabled(!state.model.enabled, tab);
    };
    window.addEventListener(TOGGLE_PAGE_TABS, toggle);
    return () => window.removeEventListener(TOGGLE_PAGE_TABS, toggle);
  }, []);

  const context = useMemo(() => model.cache.capture(path), [model.cache, path, refresh, initialState?.revision]);
  const scroll = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    if (!model.enabled) return;
    const saved = context.read<{ top: number; left: number }>('scroll') || { top: 0, left: 0 };
    let applying = true;
    const restore = () => {
      if (!applying) return;
      window.scrollTo(saved.left, saved.top);
      if (document.documentElement.scrollHeight >= saved.top + window.innerHeight) applying = false;
    };
    restore();
    const observer = new ResizeObserver(restore);
    if (scroll.current) observer.observe(scroll.current);
    const interrupt = () => { applying = false; };
    const remember = () => {
      if (!applying) context.write('scroll', { top: window.scrollY, left: window.scrollX });
    };
    window.addEventListener('scroll', remember, { passive: true });
    window.addEventListener('wheel', interrupt, { passive: true });
    window.addEventListener('touchstart', interrupt, { passive: true });
    window.addEventListener('keydown', interrupt);
    return () => {
      observer.disconnect();
      window.removeEventListener('scroll', remember);
      window.removeEventListener('wheel', interrupt);
      window.removeEventListener('touchstart', interrupt);
      window.removeEventListener('keydown', interrupt);
    };
  }, [context, model.enabled]);

  const navigate = (tab: PageTab, replace = false) => {
    const target = { pathname: tab.path, search: tab.search, hash: tab.hash };
    if (replace) history.replace(target, tab.state);
    else history.push(target, tab.state);
  };
  const close = (target: string) => {
    if (target === WORKSPACE_PATH) return;
    if (target !== path) { model.close(target); return; }
    // 先通过统一路由确认；只有导航成功才移除当前页签。
    const index = model.tabs.findIndex(tab => tab.path === target);
    const next = model.tabs[Math.max(0, index - 1)];
    if (!next) return;
    pendingClose.current = { path: target, next: next.path };
    navigate(next, true);
  };
  const refreshPage = () => confirmAction(() => {
    model.cache.forget(path);
    setRefresh(value => value + 1);
    window.scrollTo(0, 0);
  });
  const menuItems: MenuProps['items'] = [
    { key: 'refresh', label: '刷新当前页', icon: <ReloadOutlined /> },
    { key: 'close-others', label: '关闭其他页签', disabled: model.tabs.every(tab => tab.path === path || tab.path === WORKSPACE_PATH) },
  ];

  return <>
    {model.enabled && <>
      <div aria-hidden="true" style={{ height: barHeight }} />
      <div ref={bar} className={styles.bar} style={{ top: headerOffset, insetInlineStart: layout.siderWidth || 0, insetInlineEnd: 0 }}>
      <Tabs className={styles.tabs} type="editable-card" size="small" hideAdd animated={false}
      activeKey={path} more={{ trigger: 'click' }}
      items={model.tabs.filter(tab => titles.has(tab.path)).map(tab => ({
        key: tab.path, label: titles.get(tab.path)?.name, closable: tab.path !== WORKSPACE_PATH,
      }))}
      onChange={key => { const tab = model.tabs.find(item => item.path === key); if (tab) navigate(tab); }}
      onEdit={(key, action) => { if (action === 'remove' && typeof key === 'string') close(key); }}
      tabBarExtraContent={<Dropdown trigger={['click']} placement="bottomRight" menu={{
        items: menuItems,
        onClick: ({ key }) => { if (key === 'refresh') refreshPage(); if (key === 'close-others') model.closeOthers(path); },
      }}><Button type="text" icon={<EllipsisOutlined />} aria-label="页签操作" /></Dropdown>}
      />
      </div>
    </>}
    <PageTitleContext.Provider value={titles.get(path)?.path}>
      <PageStateContext.Provider value={model.enabled ? context : undefined}>
        <div key={`${path}:${refresh}`} ref={scroll} className={styles.page}>{children}</div>
      </PageStateContext.Provider>
    </PageTitleContext.Provider>
  </>;
}
