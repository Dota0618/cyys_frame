import { Alert, Button, Upload } from 'antd';
import { useEffect, useId, useRef, useState } from 'react';
import type { ManagedFile } from '@/services/system/files';
import { completeUpload, cleanupUpload, startUpload, uploadFile, uploadLimits, uploadPart, uploadProgress } from '@/services/system/files';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty } from '@/utils/session';

/** 通用上传入口：选择文件、校验、暂停、服务端续传；成功时只通知消费页面刷新。 */
export default function FileUpload({ resumeId, directoryId, onChanged, onUploaded }: { resumeId?: string; directoryId?: string; onChanged: () => void; onUploaded?: (file: ManagedFile) => void }) {
  const dirtyOwner = useId();
  const running = useRef<AbortController | null>(null);
  const mounted = useRef(true);
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'info' | 'success' | 'error'; title: string }>();
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; running.current?.abort(); setDirty(false, dirtyOwner); };
  }, []);

  const begin = async (file: File) => {
    if (running.current) return;
    const controller = new AbortController(); running.current = controller;
    const options = { signal: controller.signal, revision: session().revision, quiet: true };
    const check = () => {
      if (!mounted.current || session().revision !== options.revision) throw new StaleRequest();
      controller.signal.throwIfAborted();
    };
    setBusy(true); setDirty(true, dirtyOwner); setFeedback({ type: 'info', title: '正在检查文件…' });
    let taskId = resumeId;
    try {
      const limits = await uploadLimits(options); check();
      if (file.size > limits.resumableBytes) throw new Error(`文件不能超过 ${Math.floor(limits.resumableBytes / 1024 / 1024)} MB`);
      let uploaded: ManagedFile;
      if (!resumeId && file.size <= limits.partBytes) {
        uploaded = await uploadFile(file, options, directoryId); check();
      } else {
        let task = resumeId ? await uploadProgress(resumeId, options) : undefined; check();
        if (task && task.state !== 'UPLOADING') throw new Error('上传任务已结束，请刷新列表');
        if (task && task.bytes !== file.size) throw new Error('请选择原文件，文件大小不一致');
        const partBytes = task?.partBytes || limits.partBytes;
        const hashes: string[] = [];
        for (let offset = 0; offset < file.size; offset += partBytes) {
          const digest = await crypto.subtle.digest('SHA-256', await file.slice(offset, offset + partBytes).arrayBuffer()); check();
          hashes.push(Array.from(new Uint8Array(digest), (value) => value.toString(16).padStart(2, '0')).join(''));
          setFeedback({ type: 'info', title: `正在校验文件 ${Math.min(100, Math.round((offset + partBytes) / file.size * 100))}%` });
        }
        const expectedHashes = task?.hashes;
        if (expectedHashes && hashes.some((hash, index) => hash !== expectedHashes[index])) throw new Error('文件内容不一致，请选择原文件');
        if (!task) { task = await startUpload(file.name, file.size, hashes, options, directoryId); check(); taskId = task.id; }
        const completed = new Set(task.completed);
        for (let index = 0; index < hashes.length; index++) {
          check();
          if (!completed.has(index)) {
            await uploadPart(task.id, index, file.slice(index * partBytes, (index + 1) * partBytes), options); check();
            completed.add(index);
          }
          setFeedback({ type: 'info', title: `已上传 ${completed.size}/${hashes.length} 片，可暂停后继续` });
        }
        setFeedback({ type: 'info', title: '正在合并并校验文件…' });
        uploaded = await completeUpload(task.id, options); check();
        await cleanupUpload(task.id, options); check();
      }
      onUploaded?.(uploaded);
      setFeedback({ type: 'success', title: '文件上传完成' });
    } catch (error) {
      if (!(error instanceof StaleRequest) && mounted.current && options.revision === session().revision) {
        setFeedback({ type: controller.signal.aborted ? 'info' : 'error', title: controller.signal.aborted
          ? (taskId ? '已暂停，可在列表中选择原文件继续上传' : '已停止，请重新选择文件')
          : error instanceof ApiError || error instanceof Error ? error.message : '上传失败，可稍后继续' });
      }
    } finally {
      running.current = null;
      if (mounted.current && options.revision === session().revision) { setBusy(false); setDirty(false, dirtyOwner); onChanged(); }
    }
  };

  return <div>
    <Upload showUploadList={false} disabled={busy} beforeUpload={(file) => { void begin(file); return Upload.LIST_IGNORE; }}>
      <Button disabled={busy}>{resumeId ? '选择原文件继续' : '上传文件'}</Button>
    </Upload>
    {busy && <Button onClick={() => running.current?.abort()}>暂停上传</Button>}
    {feedback && <Alert {...feedback} showIcon style={{ marginTop: 8 }} />}
  </div>;
}
