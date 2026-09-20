import { ModalForm, PageContainer, ProFormSelect, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Popconfirm, Select, Space } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import FileUpload from '@/components/FileUpload';
import ScopeSwitch from '@/components/ScopeSwitch';
import { cancelUpload, cleanupUpload, downloadFile, listFiles, renameFile, trashFile, type ManagedFile } from '@/services/system/files';
import { createDirectory, deleteDirectory, listDirectories, moveDirectoryOptions, moveFiles, renameDirectory, trashFiles, type FileDirectory } from '@/services/system/files';
import { ApiError, StaleRequest } from '@/utils/request';
import { hasUnsavedChanges, setDirty } from '@/utils/session';
import { saveDownload } from '@/utils/download';

export default function FilesPage() {
  const access = useAccess();
  const { modal } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [editing, setEditing] = useState<ManagedFile>();
  const [resuming, setResuming] = useState<string>();
  const [error, setError] = useState('');
  const [directories, setDirectories] = useState<FileDirectory[]>([]);
  const [directoryId, setDirectoryId] = useState('__all__');
  const [directoryForm, setDirectoryForm] = useState<{ record?: FileDirectory }>();
  const [selected, setSelected] = useState<ManagedFile[]>([]);
  const [moving, setMoving] = useState<ManagedFile[]>();
  const [moveOptions, setMoveOptions] = useState<FileDirectory[]>([]);
  const currentDirectory = directories.find((item) => item.id === directoryId);
  const reloadDirectories = useCallback(async () => {
    try { setDirectories(await listDirectories()); }
    catch (failure) { if (!(failure instanceof StaleRequest)) setError('目录加载失败，请刷新重试'); }
  }, []);
  useEffect(() => { void reloadDirectories(); }, [reloadDirectories]);
  useEffect(() => () => setDirty(false), []);
  const changed = () => { setSelected([]); void action.current?.reload(); };
  const selectDirectory = (value: string) => {
    const apply = () => { setDirectoryId(value); setResuming(undefined); setEditing(undefined); setDirectoryForm(undefined); setMoving(undefined); setSelected([]); setDirty(false); };
    if (hasUnsavedChanges()) modal.confirm({ title: '切换目录？', content: '当前编辑会清除，上传会暂停；已上传分片可以稍后继续。', onOk: apply });
    else apply();
  };
  const beginMove = async (files: ManagedFile[]) => {
    try { setMoveOptions(await moveDirectoryOptions()); setMoving(files); }
    catch (failure) { if (!(failure instanceof StaleRequest)) setError('可用目录加载失败，请重试'); }
  };
  const run = async (work: () => Promise<unknown>) => {
    setError('');
    try { await work(); changed(); }
    catch (failure) { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试'); }
  };
  const columns: ProColumns<ManagedFile>[] = [
    { title: '文件名', dataIndex: 'name' },
    { title: '大小', dataIndex: 'byteSize', search: false, renderText: (bytes) => bytes === undefined ? '—' : `${bytes} 字节` },
    { title: '状态', dataIndex: 'state', search: false, valueEnum: { READY: { text: '可用', status: 'Success' }, UPLOADING: { text: '待完成', status: 'Processing' }, FAILED: { text: '已取消或失败', status: 'Error' } } },
    { title: '上传时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      access.canDownloadFiles && row.state === 'READY' && <Button key="download" type="link" onClick={() => { void run(async () => {
        await saveDownload(() => downloadFile(row.id), row.name);
      }); }}>下载</Button>,
      access.canEditFiles && <Button key="rename" type="link" onClick={() => setEditing(row)}>重命名</Button>,
      access.canMoveFiles && row.ownUpload && <Button key="move" type="link" onClick={() => { void beginMove([row]); }}>移动</Button>,
      access.canUploadFiles && row.resumable && row.ownUpload && row.state === 'UPLOADING' && <Button key="resume" type="link" onClick={() => setResuming(row.id)}>继续上传</Button>,
      access.canUploadFiles && row.resumable && row.ownUpload && row.state === 'UPLOADING' && <Popconfirm key="cancel" title="取消此上传任务？" onConfirm={() => run(() => cancelUpload(row.id))}>
        <Button type="link">取消上传</Button></Popconfirm>,
      access.canUploadFiles && row.resumable && row.ownUpload && row.state !== 'UPLOADING' && <Button key="cleanup" type="link" onClick={() => { void run(() => cleanupUpload(row.id)); }}>清理临时分片</Button>,
      access.canDeleteFiles && row.state !== 'UPLOADING' && <Popconfirm key="delete" title="删除文件记录？" description="删除后不能下载，文件内容暂时保留。" onConfirm={() => run(() => trashFile(row))}>
        <Button type="link" danger>删除</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="文件管理" subTitle="大文件支持暂停和续传；恢复时请选择同一个原文件。">
    <ScopeSwitch />
    {error && <Alert type="error" title={error} showIcon />}
    <Space wrap style={{ margin: '16px 0' }}>
      <span>我的目录</span>
      <Select aria-label="文件目录" style={{ width: 240 }} value={directoryId} onChange={selectDirectory}
        options={[{ value: '__all__', label: '全部可见文件' }, { value: '__root__', label: '默认目录' }, ...directories.map((item) => ({ value: item.id, label: item.name }))]} />
      {access.canCreateFileDirectories && <Button onClick={() => setDirectoryForm({})}>新增目录</Button>}
      {access.canEditFileDirectories && currentDirectory && <Button onClick={() => setDirectoryForm({ record: currentDirectory })}>重命名目录</Button>}
      {access.canDeleteFileDirectories && currentDirectory && <Popconfirm title="删除目录？" description="目录内文件移回默认目录，文件内容不会删除。" onConfirm={() => run(async () => {
        await deleteDirectory(currentDirectory); setDirectoryId('__root__'); setResuming(undefined); await reloadDirectories();
      })}><Button danger>删除目录</Button></Popconfirm>}
    </Space>
    {access.canUploadFiles && <FileUpload key={`${directoryId}-${resuming || 'new'}`} resumeId={resuming} directoryId={currentDirectory?.id} onChanged={changed} />}
    {resuming && <Button onClick={() => setResuming(undefined)}>关闭续传</Button>}
    <ProTable<ManagedFile> form={{ name: 'file-search' }} actionRef={action} rowKey="id" columns={columns}
      params={{ directoryId }} rowSelection={{ selectedRowKeys: selected.map((file) => file.id), onChange: (_, rows) => setSelected(rows) }}
      tableAlertOptionRender={() => <Space>
        {access.canMoveFiles && <Button disabled={!selected.length || selected.some((file) => !file.ownUpload)} onClick={() => { void beginMove(selected); }}>批量移动</Button>}
        {access.canDeleteFiles && <Popconfirm title="删除所选文件？" onConfirm={() => run(() => trashFiles(selected))}>
          <Button danger disabled={!selected.length || selected.some((file) => file.state === 'UPLOADING')}>批量删除</Button></Popconfirm>}
      </Space>}
      request={async (params) => {
      try { const page = await listFiles(params.current || 1, params.pageSize || 20, params.name, directoryId === '__all__' ? undefined : directoryId); return { data: page.records, total: page.total, success: true }; }
      catch (failure) { if (!(failure instanceof StaleRequest)) setError('文件列表加载失败，请重试'); return { data: [], success: false }; }
    }} />
    <ModalForm<{ name: string }> name="file-rename" key={editing?.id || 'closed'} title="重命名文件" open={!!editing} initialValues={{ name: editing?.name }}
      onValuesChange={() => setDirty(true)}
      modalProps={{ destroyOnHidden: true, onCancel: () => { setEditing(undefined); setDirty(false); } }}
      onFinish={async (values) => {
        if (!editing) return false;
        try { await renameFile(editing.id, values.name, editing.version); setEditing(undefined); setDirty(false); changed(); return true; }
        catch { return false; }
      }}>
      <ProFormText name="name" label="文件名" rules={[{ required: true, whitespace: true }, { max: 200 }, { pattern: /^[^/\\\x00-\x1f\x7f]+$/, message: '文件名不能包含路径或控制字符' }]} />
    </ModalForm>
    <ModalForm<{ name: string }> name="file-directory-editor" key={directoryForm?.record?.id || String(!!directoryForm)}
      title={directoryForm?.record ? '重命名目录' : '新增目录'} open={!!directoryForm} initialValues={{ name: directoryForm?.record?.name }}
      onValuesChange={() => setDirty(true)} modalProps={{ destroyOnHidden: true }}
      onOpenChange={(open) => { if (!open) { setDirectoryForm(undefined); setDirty(false); } }}
      onFinish={async ({ name }) => {
        try {
          const value = directoryForm?.record ? await renameDirectory(directoryForm.record, name) : await createDirectory(name);
          setDirectoryForm(undefined); setDirectoryId(value.id); setDirty(false); await reloadDirectories(); changed(); return true;
        } catch { return false; }
      }}>
      <ProFormText name="name" label="目录名称" rules={[{ required: true, whitespace: true, message: '请输入目录名称' }, { max: 100 }]} />
    </ModalForm>
    <ModalForm<{ directory: string }> name="file-move" title="移动文件" open={!!moving} initialValues={{ directory: '__root__' }}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setMoving(undefined); setDirty(false); } }}
      onFinish={async ({ directory }) => {
        if (!moving) return false;
        try { await moveFiles(moving, directory === '__root__' ? undefined : directory); setMoving(undefined); setDirty(false); changed(); return true; }
        catch { return false; }
      }}>
      <ProFormSelect name="directory" label="目标目录" rules={[{ required: true }]}
        options={[{ value: '__root__', label: '默认目录' }, ...moveOptions.map((item) => ({ value: item.id, label: item.name }))]} />
    </ModalForm>
  </PageContainer>;
}
