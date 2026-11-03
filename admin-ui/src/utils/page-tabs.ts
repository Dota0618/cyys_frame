export const WORKSPACE_PATH = '/workspace';
export const TOGGLE_PAGE_TABS = 'cyys:toggle-page-tabs';
export const PAGE_STATE_LIMIT = 5;

export interface PageTab {
  path: string;
  title: string;
  search: string;
  hash: string;
  state?: unknown;
}

export const workspaceTab = (): PageTab => ({ path: WORKSPACE_PATH, title: '工作台', search: '', hash: '' });

export function visitPage(tabs: PageTab[], tab: PageTab, enabled: boolean): PageTab[] {
  const index = tabs.findIndex(item => item.path === tab.path);
  const previous = tabs[index];
  const unchanged = previous && previous.title === tab.title && previous.search === tab.search &&
    previous.hash === tab.hash && previous.state === tab.state;
  if (unchanged && (enabled || tabs.length === (tab.path === WORKSPACE_PATH ? 1 : 2))) return tabs;
  if (!enabled) return tab.path === WORKSPACE_PATH ? [tab] : [workspaceTab(), tab];
  return index < 0 ? [...tabs, tab] : tabs.map((item, position) => position === index ? tab : item);
}

/** 页签元数据与查询状态分开保存；不保留页面、表单草稿或列表响应。 */
export class PageStateCache {
  private pages = new Map<string, Map<string, unknown>>();
  private versions = new Map<string, number>();
  private generation = 0;

  capture(path: string) {
    const generation = this.generation;
    const version = this.versions.get(path) || 0;
    const valid = () => generation === this.generation && version === (this.versions.get(path) || 0);
    return {
      read: <T>(key: string): T | undefined => valid() ? this.pages.get(path)?.get(key) as T | undefined : undefined,
      write: <T>(key: string, value: T) => {
        if (!valid()) return;
        const page = this.pages.get(path) || new Map<string, unknown>();
        page.delete(key);
        page.set(key, value);
        while (page.size > 32) page.delete(page.keys().next().value!);
        this.pages.delete(path);
        this.pages.set(path, page);
        while (this.pages.size > PAGE_STATE_LIMIT) this.pages.delete(this.pages.keys().next().value!);
      },
    };
  }

  touch(path: string) {
    const page = this.pages.get(path);
    if (page) { this.pages.delete(path); this.pages.set(path, page); }
  }

  forget(path: string) {
    this.pages.delete(path);
    this.versions.set(path, (this.versions.get(path) || 0) + 1);
  }

  keep(paths: string[]) {
    for (const path of this.pages.keys()) if (!paths.includes(path)) this.forget(path);
  }

  clear() {
    ++this.generation;
    this.pages.clear();
    this.versions.clear();
  }
}
