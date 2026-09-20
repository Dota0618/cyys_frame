import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Button } from 'antd';
import { useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
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
      setFeedback({ type: 'success', title: `${action.name}缓存已更新` });
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存更新失败，请重试',
      });
    } finally { setPending(undefined); }
  };
  const columns: ProColumns<CacheAction>[] = [
    { title: '缓存', dataIndex: 'name', width: 220 },
    { title: '说明', dataIndex: 'description' },
    { title: '操作', valueType: 'option', width: 140, render: (_, action) => [
      <Button key="refresh" type="link" loading={pending === action.id}
        disabled={!!pending && pending !== action.id} onClick={() => { void refresh(action); }}>更新缓存</Button>,
    ] },
  ];
  return <PageContainer title="缓存管理" subTitle="管理当前单位已接入的业务缓存。">
    <div style={{ marginBottom: 24 }}><ScopeSwitch /></div>
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
