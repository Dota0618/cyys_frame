import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Modal, Select } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { changeRecycled, listRecycled, recycleResources } from '@/services/system/recycle';
import type { RecycleRecord, RecycleResource } from '@/services/system/recycle';
import { ApiError, StaleRequest } from '@/utils/request';
import { session } from '@/utils/session';

export default function RecyclePage() {
  const access = useAccess();
  return access.canReadRecycle ? <RecycleContent /> : <PageContainer title="数据回收站" />;
}

function RecycleContent() {
  const [resources, setResources] = useState<RecycleResource[]>([]);
  const [code, setCode] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  const selectedScope = !!session().scopeId;
  useEffect(() => {
    let active = true;
    setLoading(true); setError(false);
    recycleResources().then((result) => {
      if (!active) return;
      setResources(result);
      setCode(result.find((item) => selectedScope || !item.scopeRequired)?.code ?? result[0]?.code);
    }).catch((failure) => {
      if (active && !(failure instanceof StaleRequest)) setError(true);
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [retry, selectedScope]);
  const resource = resources.find((item) => item.code === code);
  return <PageContainer title="数据回收站" subTitle="恢复误删数据，或彻底删除不再需要的记录。">
    {error ? <Alert type="error" title="资源加载失败" showIcon action={<Button onClick={() => setRetry((value) => value + 1)}>重试</Button>} /> : <>
      <Select aria-label="回收站资源" style={{ width: 240, marginBottom: 16 }} loading={loading}
        placeholder="选择数据类型" value={code} onChange={setCode}
        options={resources.map((item) => ({ value: item.code, label: item.name }))} />
      {!loading && resources.length === 0 && <Alert type="info" title="暂无可访问的数据回收站" showIcon />}
      {resource && (selectedScope || !resource.scopeRequired) && <RecycleTable key={resource.code} resource={resource} />}
    </>}
  </PageContainer>;
}

function RecycleTable({ resource }: { resource: RecycleResource }) {
  const actionRef = useRef<ActionType | null>(null);
  const active = useRef(true);
  const submitting = useRef(false);
  const [selected, setSelected] = useState<RecycleRecord[]>([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState<{ action: 'restore' | 'purge'; records: RecycleRecord[] }>();
  useEffect(() => { active.current = true; return () => { active.current = false; }; }, []);
  const submit = async () => {
    if (!pending || submitting.current) return;
    submitting.current = true; setBusy(true); setError('');
    try {
      await changeRecycled(resource.code, pending.action, pending.records);
      if (!active.current) return;
      setPending(undefined); setSelected([]); await actionRef.current?.reload();
    } catch (failure) {
      if (active.current && !(failure instanceof StaleRequest)) {
        setPending(undefined); setSelected([]); await actionRef.current?.reload();
        if (active.current) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试');
      }
    } finally {
      submitting.current = false;
      if (active.current) setBusy(false);
    }
  };
  const columns: ProColumns<RecycleRecord>[] = [
    { title: resource.searchLabel, dataIndex: 'title', ellipsis: true, fieldProps: { maxLength: 100 } },
    ...(resource.scopeRequired ? [
      { title: '所属机构', dataIndex: 'scopeName', search: false },
      { title: '所属部门', dataIndex: 'departmentName', search: false },
    ] : []),
    { title: '删除人', dataIndex: 'deletedByName', search: false },
    { title: '删除时间', dataIndex: 'deletedAt', valueType: 'dateTime', search: false },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      resource.canRestore && <Button key="restore" type="link" disabled={busy} onClick={() => setPending({ action: 'restore', records: [row] })}>恢复</Button>,
      resource.canPurge && <Button key="purge" type="link" danger disabled={busy} onClick={() => setPending({ action: 'purge', records: [row] })}>彻底删除</Button>,
    ].filter(Boolean) },
  ];
  return <>
    <Alert type="info" title={resource.description} showIcon style={{ marginBottom: 16 }} />
    {error && <Alert type="error" title={error} showIcon style={{ marginBottom: 16 }} />}
    <ProTable<RecycleRecord> actionRef={actionRef} rowKey="id" columns={columns}
      headerTitle={`${resource.name}回收站`} form={{ name: `recycle-${resource.code}-search` }}
      pagination={{ defaultPageSize: 20, pageSizeOptions: [10, 20, 50, 100] }}
      rowSelection={resource.canRestore || resource.canPurge ? {
        selectedRowKeys: selected.map((row) => row.id), onChange: (_, rows) => setSelected(rows),
        getCheckboxProps: () => ({ disabled: busy }),
      } : false}
      tableAlertOptionRender={() => [
        resource.canRestore && <Button key="restore" disabled={busy || !selected.length} onClick={() => setPending({ action: 'restore', records: selected })}>批量恢复</Button>,
        resource.canPurge && <Button key="purge" danger disabled={busy || !selected.length} onClick={() => setPending({ action: 'purge', records: selected })}>批量彻底删除</Button>,
      ].filter(Boolean)}
      request={async (params) => {
        try {
          const page = await listRecycled(resource.code, params.current || 1, params.pageSize || 20, params.title);
          if (active.current) { setError(''); setSelected([]); }
          return { data: page.records, total: page.total, success: true };
        } catch (failure) {
          if (active.current && !(failure instanceof StaleRequest)) setError('数据加载失败，请使用表格刷新按钮重试');
          return { data: [], total: 0, success: false };
        }
      }} />
    <Modal open={!!pending} title={pending?.action === 'purge' ? '确认彻底删除' : '确认恢复'}
      okText={pending?.action === 'purge' ? '彻底删除' : '恢复'} cancelText="取消"
      okButtonProps={{ danger: pending?.action === 'purge' }} confirmLoading={busy}
      cancelButtonProps={{ disabled: busy }} closable={!busy} keyboard={!busy}
      mask={{ closable: !busy }} onCancel={() => setPending(undefined)} onOk={() => void submit()}>
      <p>已选择 {pending?.records.length ?? 0} 条{resource.name}记录。</p>
      <p>{pending?.records.slice(0, 5).map((row) => row.title).join('、')}{(pending?.records.length ?? 0) > 5 ? '等' : ''}</p>
      <p>{pending?.action === 'purge' ? '彻底删除后无法恢复。关联附件仅解除引用，文件本身保留。' : resource.description}</p>
      <p>任一记录无法处理时，本批次均不生效。</p>
    </Modal>
  </>;
}
