import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Popconfirm } from 'antd';
import { useRef, useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
import { listRecycledFiles, purgeFile, restoreFiles, type ManagedFile } from '@/services/system/files';
import { ApiError, StaleRequest } from '@/utils/request';

export default function FileRecyclePage() {
  const access = useAccess();
  if (!access.canReadFileRecycle) return <PageContainer title="文件回收站">
    <ScopeSwitch />
    <Alert type="info" showIcon title="请选择一个具体工作单位后查看文件回收站。" style={{ marginTop: 16 }} />
  </PageContainer>;
  return <FileRecycleContent />;
}

function FileRecycleContent() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [selected, setSelected] = useState<ManagedFile[]>([]);
  const [error, setError] = useState('');
  const run = async (work: () => Promise<void>) => {
    setError('');
    try { await work(); setSelected([]); await action.current?.reload(); }
    catch (failure) {
      if (!(failure instanceof StaleRequest)) {
        setError(failure instanceof ApiError ? failure.message : '操作失败，请重试');
        setSelected([]); await action.current?.reload();
      }
    }
  };
  const columns: ProColumns<ManagedFile>[] = [
    { title: '文件名', dataIndex: 'name' },
    { title: '大小', dataIndex: 'byteSize', search: false, renderText: (value) => value === undefined ? '—' : `${value} 字节` },
    { title: '状态', dataIndex: 'state', search: false, valueEnum: { READY: { text: '已删除', status: 'Default' }, FAILED: { text: '失败记录', status: 'Error' }, PURGING: { text: '待清理', status: 'Warning' } } },
    { title: '操作', valueType: 'option', render: (_, row) => [
      access.canRestoreFiles && row.state === 'READY' && <Button key="restore" type="link" onClick={() => { void run(() => restoreFiles([row])); }}>恢复到默认目录</Button>,
      access.canPurgeFiles && <Popconfirm key="purge" title="永久删除此文件？" description="文件内容将被清除，无法恢复。" okText="永久删除" cancelText="取消"
        onConfirm={() => run(() => purgeFile(row))}><Button type="link" danger>{row.state === 'PURGING' ? '重试清理' : '永久删除'}</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="文件回收站" subTitle="恢复到默认目录；永久删除后无法找回文件。">
    <ScopeSwitch />
    {error && <Alert type="error" title={error} showIcon />}
    <ProTable<ManagedFile> actionRef={action} rowKey="id" columns={columns} form={{ name: 'file-recycle-search' }}
      rowSelection={access.canRestoreFiles ? { selectedRowKeys: selected.map((file) => file.id), onChange: (_, rows) => setSelected(rows), getCheckboxProps: (row) => ({ disabled: row.state !== 'READY' }) } : false}
      tableAlertOptionRender={() => <Button disabled={!selected.length} onClick={() => { void run(() => restoreFiles(selected)); }}>批量恢复到默认目录</Button>}
      request={async (params) => {
        try { const page = await listRecycledFiles(params.current || 1, params.pageSize || 20, params.name); return { data: page.records, total: page.total, success: true }; }
        catch (failure) { if (!(failure instanceof StaleRequest)) setError('回收站加载失败，请重试'); return { data: [], success: false }; }
      }} />
  </PageContainer>;
}
