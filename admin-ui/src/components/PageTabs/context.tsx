import { createContext, useContext, useEffect, useState } from 'react';
import type { PageStateCache } from '@/utils/page-tabs';

export const PageStateContext = createContext<ReturnType<PageStateCache['capture']> | undefined>(undefined);

/** 只保存查询和导航状态；页面卸载后不保留业务组件。 */
export function usePageState<T>(key: string, initial: T | (() => T)) {
  const context = useContext(PageStateContext);
  const [value, setValue] = useState<T>(() => context?.read<T>(key) ?? (typeof initial === 'function' ? (initial as () => T)() : initial));
  useEffect(() => { context?.write(key, value); }, [context, key, value]);
  return [value, setValue] as const;
}
