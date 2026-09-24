import { PageContainer, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert } from 'antd';
import { useEffect, useState } from 'react';
import { listLoginAudits, loginAuditOptions } from '@/services/system/loginAudits';
import type { DictionaryOption, LoginAuditRecord } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';

export default function LoginAuditsPage() {
  const [loadError, setLoadError] = useState('');
  const [outcomeOptions, setOutcomeOptions] = useState<DictionaryOption[]>([]);
  useEffect(() => {
    let active = true;
    void loginAuditOptions().then((result) => {
      if (active) setOutcomeOptions(result.outcomes);
    }).catch((error) => {
      if (active && !(error instanceof StaleRequest))
        setLoadError(error instanceof ApiError ? error.message : '登录结果字典加载失败，请重试');
    });
    return () => { active = false; };
  }, []);
  const outcomeValueEnum = Object.fromEntries(outcomeOptions.map((item) => [item.value, item.label]));
  const columns: ProColumns<LoginAuditRecord>[] = [
    { title: '时间', dataIndex: 'occurredAt', search: false, valueType: 'dateTime' },
    { title: '登录名', dataIndex: 'loginName' },
    { title: '真实姓名', dataIndex: 'realName', search: false, renderText: (value) => value || '—' },
    { title: 'IP', dataIndex: 'clientIp', search: false },
    { title: '结果', dataIndex: 'outcome', valueType: 'select', valueEnum: outcomeValueEnum,
      render: (_, row) => row.outcomeLabel || row.outcome },
    { title: '原因', dataIndex: 'reason', search: false },
  ];

  return <PageContainer title="登录记录" subTitle="当前单位内的登录结果记录。">
    {loadError && <Alert type="error" showIcon title={loadError} />}
    <ProTable<LoginAuditRecord> rowKey="id" columns={columns} options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, loginName, outcome }) => {
        setLoadError('');
        try {
          const result = await listLoginAudits(current, pageSize, loginName, outcome);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest))
            setLoadError(error instanceof ApiError ? error.message : '登录记录加载失败，请重试');
          return { data: [], total: 0, success: false };
        }
      }}
      search={{ labelWidth: 'auto' }}
    />
  </PageContainer>;
}
