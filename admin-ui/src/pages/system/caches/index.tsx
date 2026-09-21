import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Button } from 'antd';
import { useState } from 'react';
import { listCacheActions, refreshCache } from '@/services/system/caches';
import type { CacheAction } from '@/services/system/caches';
import { ApiError, StaleRequest } from '@/utils/request';

export default function CachesPage() {
  const [pending, setPending] = useState<string>();
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; title: string }>();
  const refresh = async (action: CacheAction) => {
    setPending(action.id);
    setFeedback(undefined);
    try {
      await refreshCache(action.id);
      setFeedback({ type: 'success', title: `${action.name}已刷新，下次读取时重新加载` });
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存更新失败，请重试',
      });
    } finally { setPending(undefined); }
  };
  const columns: ProColumns<CacheAction>[] = [
    { title: '业务组', dataIndex: 'name', width: 150 },
    { title: '作用范围', dataIndex: 'scope', width: 120 },
    { title: '包含缓存', dataIndex: 'contents', width: 240, render: (_, action) => action.contents.join('、') },
    { title: '说明', dataIndex: 'description' },
    { title: '操作', valueType: 'option', width: 140, render: (_, action) => [
      <Button key="refresh" type="link" loading={pending === action.id}
        disabled={!!pending && pending !== action.id} onClick={() => { void refresh(action); }}>刷新缓存</Button>,
    ] },
  ];
  return <PageContainer title="缓存管理" subTitle="业务数据保存成功后自动更新，日常操作无需手动刷新。">
    {feedback && <Alert {...feedback} showIcon />}
    <ProTable<CacheAction> rowKey="id" columns={columns} search={false} pagination={false}
      options={{ fullScreen: false, setting: false, density: false }}
      request={async () => {
        setFeedback(undefined);
        try {
          const actions = await listCacheActions();
          return { data: actions, total: actions.length, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest)) setFeedback({
            type: 'error', title: error instanceof ApiError ? error.message : '缓存列表加载失败，请重试',
          });
          return { data: [], success: false };
        }
      }} />
  </PageContainer>;
}
