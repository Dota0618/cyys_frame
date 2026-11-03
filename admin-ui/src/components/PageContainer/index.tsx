import { PageContainer as ProPageContainer } from '@ant-design/pro-components';
import type { MenuDataItem, PageContainerProps } from '@ant-design/pro-components';
import { createContext, useContext } from 'react';
import type { NavigationMenu } from '@/types/api';
import { navigationTree } from '@/utils/menus';
import styles from './index.less';

export const PageTitleContext = createContext<string | undefined>(undefined);

/** 页面标题和页签共用已授权菜单，分别显示完整路径与页面名称。 */
export function pageTitles(navigation: NavigationMenu[], routes: MenuDataItem[], access: Record<string, boolean>) {
  const titles = new Map<string, { name: string; path: string }>();
  for (const route of routes) {
    if (route.path && route.name && route.component && (!route.access || access[String(route.access)]))
      titles.set(route.path, { name: route.name, path: route.name });
  }
  const visit = (items: MenuDataItem[], parents: string[] = []) => items.forEach(item => {
    const names = item.name ? [...parents, item.name] : parents;
    if (item.path && item.name) titles.set(item.path, { name: item.name, path: names.join(' > ') });
    if (item.children) visit(item.children, names);
  });
  visit(navigationTree(navigation, routes, access));
  return titles;
}

/** 统一路径标题、字号和说明显隐，保留官方容器的内容与操作配置。 */
export default function PageContainer(props: PageContainerProps) {
  const title = useContext(PageTitleContext) || props.title;
  return <ProPageContainer {...props}
    className={[styles.container, props.className].filter(Boolean).join(' ')}
    title={title} subTitle={undefined} header={{ ...props.header, subTitle: undefined }} />;
}
