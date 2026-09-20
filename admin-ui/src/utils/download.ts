import { StaleRequest } from '@/utils/request';
import { session } from '@/utils/session';

/** 下载仍经调用方的授权 API；完成只表示交给浏览器，不表示用户已保存。 */
export async function saveDownload(load: () => Promise<Blob>, name: string): Promise<void> {
  const revision = session().revision;
  const blob = await load();
  if (revision !== session().revision) throw new StaleRequest();
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  try {
    anchor.href = url;
    anchor.download = name.replace(/[\\/\u0000-\u001f\u007f]/g, '_') || '下载';
    anchor.hidden = true;
    document.body.appendChild(anchor);
    anchor.click();
  } finally {
    anchor.remove();
    // 给浏览器处理下载的机会，再释放临时对象；不把 URL 存储为可复用下载地址。
    window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }
}
