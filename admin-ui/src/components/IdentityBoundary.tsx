import { history, useAccess, useModel } from '@umijs/max';
import { App, Spin } from 'antd';
import { useEffect, useRef } from 'react';
import type { PropsWithChildren } from 'react';
import type { UserInfo } from '@/types/api';
import { currentUser } from '@/services/auth';
import { ApiError, StaleRequest } from '@/utils/request';
import {
  advanceRevision,
  clearSession,
  CONTEXT_CHANGED,
  session,
} from '@/utils/session';

const authorizationKey = (user?: UserInfo) =>
  user &&
  JSON.stringify({
    id: user.id,
    scope: user.currentScopeId,
    scopes: user.scopes,
    homeScopeId: user.homeScopeId,
    orgId: user.orgId,
    dataRange: user.dataRange,
    roles: [...user.roles].sort(),
    permissions: [...user.permissions].sort(),
    navigation: user.navigation,
  });

function IdentityContent({ children }: PropsWithChildren) {
  const { initialState, setInitialState } = useModel('@@initialState');
  const access = useAccess();
  const stateRef = useRef(initialState);
  stateRef.current = initialState;
  const { message } = App.useApp();

  useEffect(() => {
    const change = async () => {
      const captured = session();
      await setInitialState((s) => ({
        ...s,
        user: undefined,
        revision: captured.revision,
        switching: !!captured.token,
      }));
      if (!captured.token) {
        history.replace('/login');
        return;
      }
      try {
        const user = await currentUser();
        if (captured.revision === session().revision)
          await setInitialState((s) => ({
            ...s,
            user,
            revision: captured.revision,
            switching: false,
          }));
      } catch (error) {
        if (error instanceof StaleRequest) return;
        clearSession();
      }
    };
    const recheck = async () => {
      if (!session().token || stateRef.current?.switching) return;
      try {
        const user = await currentUser();
        if (
          authorizationKey(user) !== authorizationKey(stateRef.current?.user)
        ) {
          await setInitialState((s) => ({
            ...s,
            user,
            revision: advanceRevision(),
          }));
        }
      } catch (error) {
        if (error instanceof ApiError && [401, 403].includes(error.status))
          clearSession();
      }
    };
    const notify = (event: Event) => {
      void message.error((event as CustomEvent<string>).detail);
    };
    window.addEventListener(CONTEXT_CHANGED, change);
    window.addEventListener('focus', recheck);
    window.addEventListener('cyys:error', notify);
    const timer = window.setInterval(recheck, 30_000);
    return () => {
      window.removeEventListener(CONTEXT_CHANGED, change);
      window.removeEventListener('focus', recheck);
      window.removeEventListener('cyys:error', notify);
      window.clearInterval(timer);
    };
  }, [setInitialState, message]);

  const routeAccess: Record<string, boolean> = {
    '/system/recycle': access.canReadRecycle,
    '/system/recycle-config': access.canReadRecycleConfig,
    '/system/scopes': access.canReadScopes,
    '/system/users': access.canReadUsers,
    '/system/departments': access.canReadDepartments,
    '/system/files': access.canOpenFiles,
    '/system/file-recycle': access.canOpenFileRecycle,
    '/system/org-notices': access.canReadOrgNotices,
    '/system/system-notices': access.canReadSystemNotices,
    '/system/releases': access.canReadReleases,
    '/system/regions': access.canReadRegions,
    '/system/generator': access.canReadGenerator,
    '/system/roles': access.canReadRoles,
    '/system/platform-menus': access.canReadPlatformMenus,
    '/system/configurations': access.canReadConfigurations,
    '/system/dictionaries': access.canReadDictionaries,
    '/system/audits': access.canReadAudits,
    '/system/login-audits': access.canReadLoginAudits,
    '/system/caches': access.canManageCaches,
  };
  const inaccessibleRoute =
    !!initialState?.user &&
    !initialState.switching &&
    routeAccess[history.location.pathname] === false;

  useEffect(() => {
    if (inaccessibleRoute) history.replace('/workspace');
  }, [inaccessibleRoute]);

  if (initialState?.switching)
    return (
      <div className="page-loading">
        <Spin description="正在更新机构筛选" size="large">
          <div />
        </Spin>
      </div>
    );
  // Umi's layout memoizes the matched route by pathname. Suppress protected
  // content until the redirect completes when permissions change in-place.
  if (inaccessibleRoute) return null;
  return <section key={initialState?.revision}>{children}</section>;
}

export default function IdentityBoundary(props: PropsWithChildren) {
  return (
    <App>
      <IdentityContent {...props} />
    </App>
  );
}
