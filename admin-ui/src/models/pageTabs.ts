import { useCallback, useEffect, useState } from 'react';
import { AUTHORIZATION_CHANGED, CONTEXT_CHANGED } from '@/utils/session';
import { PageStateCache, visitPage, workspaceTab } from '@/utils/page-tabs';
import type { PageTab } from '@/utils/page-tabs';

export default function usePageTabs() {
  const [enabled, updateEnabled] = useState(true);
  const [tabs, setTabs] = useState<PageTab[]>(() => [workspaceTab()]);
  const [cache] = useState(() => new PageStateCache());

  useEffect(() => {
    const reset = () => { cache.clear(); setTabs([workspaceTab()]); };
    window.addEventListener(CONTEXT_CHANGED, reset);
    window.addEventListener(AUTHORIZATION_CHANGED, reset);
    return () => {
      window.removeEventListener(CONTEXT_CHANGED, reset);
      window.removeEventListener(AUTHORIZATION_CHANGED, reset);
      cache.clear();
    };
  }, [cache]);

  const visit = useCallback((tab: PageTab) => {
    cache.touch(tab.path);
    setTabs(previous => visitPage(previous, tab, enabled));
  }, [cache, enabled]);

  const close = useCallback((path: string) => {
    if (path === workspaceTab().path) return;
    cache.forget(path);
    setTabs(previous => previous.filter(tab => tab.path !== path));
  }, [cache]);

  const closeOthers = useCallback((path: string) => {
    cache.keep([path]);
    setTabs(previous => previous.filter(tab => tab.path === path || tab.path === workspaceTab().path));
  }, [cache]);

  const setEnabled = useCallback((value: boolean, current: PageTab) => {
    cache.keep([current.path]);
    setTabs(visitPage([workspaceTab()], current, value));
    updateEnabled(value);
  }, [cache]);

  return { enabled, tabs, cache, visit, close, closeOthers, setEnabled };
}
