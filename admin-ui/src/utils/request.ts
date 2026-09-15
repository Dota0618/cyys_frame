import { clearSession, refreshIdentity, session } from './session';

export class StaleRequest extends Error {}
export class ApiError extends Error {
  constructor(public status: number, message: string) { super(message); }
}
interface Options { method?: string; body?: unknown; quiet?: boolean; anonymous?: boolean }

/** 唯一请求入口：R 在此解包，调用者只取得 data。上下文变化后不交付旧响应。 */
export async function api<T>(path: string, options: Options = {}): Promise<T> {
  const captured = session();
  const headers: Record<string, string> = {};
  if (!options.anonymous && captured.token) headers.Authorization = `Bearer ${captured.token}`;
  if (!options.anonymous && captured.scopeId && !captured.allUnits) headers['X-Cyys-Scope-Id'] = captured.scopeId;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';
  try {
    const response = await fetch(path, {
      method: options.method || 'GET', headers, credentials: 'omit', cache: 'no-store',
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
    });
    const envelope = await response.json();
    if (captured.revision !== session().revision) throw new StaleRequest();
    if (!response.ok || envelope.code !== 200) {
      if (!options.anonymous && response.status === 401) clearSession();
      if (!options.quiet && response.status === 403) refreshIdentity();
      throw new ApiError(response.status, envelope.msg || '请求未完成');
    }
    return envelope.data as T;
  } catch (error) {
    if (error instanceof StaleRequest) throw error;
    if (!(error instanceof ApiError) && captured.revision !== session().revision) throw new StaleRequest();
    if (!options.quiet) window.dispatchEvent(new CustomEvent('cyys:error', {
      detail: error instanceof ApiError ? error.message : '暂时无法连接服务，请稍后重试',
    }));
    throw error;
  }
}
