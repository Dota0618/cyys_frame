import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert } from 'antd';
import { useEffect, useState } from 'react';
import { auditOptions, listAudits } from '@/services/system/audits';
import type { AuditRecord, DictionaryOption } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';

const resources = [
  { label: '用户', value: 'user' },
  { label: '部门', value: 'department' },
  { label: '角色', value: 'role' },
  { label: '缓存', value: 'cache' },
];

export default function AuditsPage() {
  const [loadError, setLoadError] = useState('');
  const [operationOptions, setOperationOptions] = useState<DictionaryOption[]>([]);
  const [outcomeOptions, setOutcomeOptions] = useState<DictionaryOption[]>([]);
  useEffect(() => {
    let active = true;
    void auditOptions().then((result) => {
      if (active) {
        setOperationOptions(result.operations);
        setOutcomeOptions(result.outcomes);
      }
    }).catch((error) => {
      if (active && !(error instanceof StaleRequest))
        setLoadError(error instanceof ApiError ? error.message : '操作审计字典加载失败，请重试');
    });
    return () => { active = false; };
  }, []);
  const operationValueEnum = Object.fromEntries(operationOptions.map((item) => [item.value, item.label]));
  const outcomeValueEnum = Object.fromEntries(outcomeOptions.map((item) => [item.value, item.label]));
  const columns: ProColumns<AuditRecord>[] = [
    { title: '时间', dataIndex: 'occurredAt', search: false, valueType: 'dateTime' },
    { title: '用户编号', dataIndex: 'userId', search: false },
    { title: '登录名', dataIndex: 'loginName', search: false, renderText: (value) => value || '—' },
    { title: '真实姓名', dataIndex: 'realName', search: false, renderText: (value) => value || '—' },
    { title: '资源', dataIndex: 'resource', valueType: 'select', valueEnum: Object.fromEntries(resources.map((item) => [item.value, item.label])) },
    { title: '记录', dataIndex: 'recordId', search: false, ellipsis: true },
    { title: '操作', dataIndex: 'operation', valueType: 'select', valueEnum: operationValueEnum,
      render: (_, row) => row.operationLabel || row.operation },
    { title: '授权视图', dataIndex: 'authorizedView', search: false },
    { title: '结果', dataIndex: 'outcome', valueType: 'select', valueEnum: outcomeValueEnum,
      render: (_, row) => row.outcomeLabel || row.outcome },
  ];

  return <PageContainer title="操作审计" subTitle="当前单位内已成功完成的管理操作。">
    {loadError && <Alert type="error" showIcon title={loadError} />}
    <ProTable<AuditRecord> rowKey="id" columns={columns} options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, resource, operation, outcome }) => {
        setLoadError('');
        try {
          const result = await listAudits(current, pageSize, resource, operation, outcome);
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
