import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert } from 'antd';
import { useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
import { listAudits } from '@/services/system/audits';
import type { AuditRecord } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';

const resources = [
  { label: '用户', value: 'user' },
  { label: '部门', value: 'department' },
  { label: '角色', value: 'role' },
  { label: '缓存', value: 'cache' },
];

const operations = ['CREATE', 'UPDATE', 'DELETE', 'REPLACE', 'REFRESH'].map((value) => ({ value, label: value }));

export default function AuditsPage() {
  const [loadError, setLoadError] = useState('');
  const columns: ProColumns<AuditRecord>[] = [
    { title: '时间', dataIndex: 'occurredAt', search: false, valueType: 'dateTime' },
    { title: '操作者', dataIndex: 'actorId', search: false },
    { title: '资源', dataIndex: 'resource', valueType: 'select', valueEnum: Object.fromEntries(resources.map((item) => [item.value, item.label])) },
    { title: '记录', dataIndex: 'recordId', search: false, ellipsis: true },
    { title: '操作', dataIndex: 'operation', valueType: 'select', valueEnum: Object.fromEntries(operations.map((item) => [item.value, item.label])) },
    { title: '授权视图', dataIndex: 'authorizedView', search: false },
    { title: '结果', dataIndex: 'outcome', search: false },
  ];

  return <PageContainer title="操作审计" subTitle="当前单位内已成功完成的管理操作。">
    <div style={{ marginBottom: 24 }}><ScopeSwitch /></div>
    {loadError && <Alert type="error" showIcon title={loadError} />}
    <ProTable<AuditRecord> rowKey="id" columns={columns} options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, resource, operation }) => {
        setLoadError('');
        try {
          const result = await listAudits(current, pageSize, resource, operation);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest))
            setLoadError(error instanceof ApiError ? error.message : '审计加载失败，请重试');
          return { data: [], total: 0, success: false };
        }
      }}
      search={{ labelWidth: 'auto' }}
    />
  </PageContainer>;
}
