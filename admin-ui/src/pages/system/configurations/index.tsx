import { compactFormProps } from '@/components/ManagementLayout';
import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm, PageContainer, ProFormDigit, ProFormSelect, ProFormText, ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button } from 'antd';
import { useEffect, useRef, useState } from 'react';
import {
  configurationDetail, createConfiguration, listConfigurations, updateConfiguration,
} from '@/services/system/configurations';
import type { Configuration, CreateConfiguration, UpdateConfiguration } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

const types = [
  { value: 'SYSTEM', label: '系统参数' },
  { value: 'THIRD_PARTY', label: '第三方接口参数' },
];

function ConfigurationFields() {
  return <>
    <ProFormText name="value" label="配置值" fieldProps={{ maxLength: 100 }}
      rules={[{ max: 100, message: '配置值最多 100 个字符' }]} extra="允许清空；0 和 false 按原值保存。" />
    <ProFormSelect name="type" label="类别" options={types}
      rules={[{ required: true, message: '请选择类别' }]} />
    <ProFormText name="desp" label="说明" fieldProps={{ maxLength: 255 }}
      rules={[{ max: 255, message: '说明最多 255 个字符' }]} />
    <ProFormDigit name="sort" label="排序" min={-2147483648} max={2147483647}
      fieldProps={{ precision: 0 }} rules={[{ type: 'integer', message: '排序请输入整数' }]} />
  </>;
}

export default function ConfigurationsPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [editing, setEditing] = useState<Configuration>();
  const [loadError, setLoadError] = useState('');
  useEffect(() => () => setDirty(false), []);

  const columns: ProColumns<Configuration>[] = [
    { title: '配置名称', dataIndex: 'name', fieldProps: { maxLength: 50 } },
    { title: '配置值', dataIndex: 'value', search: false, ellipsis: true,
      renderText: (value) => value === null ? '未设置' : value === '' ? '空字符串' : value },
    { title: '类别', dataIndex: 'type', search: false,
      valueEnum: { SYSTEM: '系统参数', THIRD_PARTY: '第三方接口参数' } },
    { title: '说明', dataIndex: 'desp', search: false, ellipsis: true },
    { title: '排序', dataIndex: 'sort', search: false, width: 80 },
    { title: '操作', valueType: 'option', width: 150, render: (_, row) => [
      access.canEditConfigurations && <a key="edit" onClick={() => {
        void configurationDetail(row.id).then(setEditing).catch(() => {});
      }}>修改</a>,
    ].filter(Boolean) },
  ];

  return <PageContainer title="通用配置" subTitle="全局共享的业务参数；密码、密钥和部署设置由外部配置管理。">
    {loadError && <Alert type="error" showIcon title={loadError} />}
    <ProTable<Configuration> rowKey="id" actionRef={action} columns={columns} headerTitle="配置列表"
      form={{ name: 'configuration-search' }}
      options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, name }) => {
        setLoadError('');
        try {
          const result = await listConfigurations(current, pageSize, name);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest))
            setLoadError(error instanceof ApiError ? error.message : '配置加载失败，请重试');
          return { data: [], total: 0, success: false };
        }
      }}
      toolBarRender={() => access.canCreateConfigurations ? [
        <ModalForm<CreateConfiguration> {...compactFormProps} key="create" name="configuration-create" title="新增配置"
          omitNil={false}
          trigger={<Button type="primary" icon={<PlusOutlined />}>新增配置</Button>}
          initialValues={{ type: 'SYSTEM', sort: 0, value: '' }}
          modalProps={{ destroyOnHidden: true }}
          onValuesChange={() => setDirty(true)}
          onOpenChange={(open) => { if (!open) setDirty(false); }}
          onFinish={async ({ name, value, type, desp, sort }) => {
            try {
              await createConfiguration({ name, value: value === undefined ? null : value, type, desp, sort });
              void action.current?.reload();
              return true;
            } catch { return false; }
          }}>
          <ProFormText name="name" label="配置名称" extra="创建后保持不变，供业务按名称读取。"
            fieldProps={{ maxLength: 50 }}
            rules={[{ required: true, whitespace: true, max: 50, message: '请输入 1–50 个字符的配置名称' }]} />
          <ConfigurationFields />
        </ModalForm>,
      ] : []}
    />
    <ModalForm<Omit<UpdateConfiguration, 'version'>> {...compactFormProps} key={editing?.id || 'edit'}
      omitNil={false}
      name="configuration-edit" title={editing ? `修改配置：${editing.name}` : '修改配置'}
      open={!!editing} initialValues={editing && {
        value: editing.value, type: editing.type, desp: editing.desp, sort: editing.sort,
      }}
      modalProps={{ destroyOnHidden: true }}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditing(undefined); setDirty(false); } }}
      onFinish={async ({ value, type, desp, sort }) => {
        if (!editing) return false;
        try {
          await updateConfiguration(editing.id, {
            version: editing.version, value: value === undefined ? null : value, type, desp, sort,
          });
          void action.current?.reload();
          return true;
        } catch { return false; }
      }}>
      <ConfigurationFields />
    </ModalForm>
  </PageContainer>;
}
