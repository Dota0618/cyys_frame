import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Form, Input, InputNumber, Modal, Popconfirm, Select, Space, Switch, Table, Tag } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import {
  createRecycleConfiguration, deleteRecycleConfiguration, listRecycleConfigurations, recycleTableOptions, updateRecycleConfiguration,
} from '@/services/system/recycleConfig';
import type { RecycleConfiguration, RecycleConfigurationInput, RecycleTableOption } from '@/services/system/recycleConfig';
import { ApiError, StaleRequest } from '@/utils/request';
import { CompactFields, compactFormProps, compactModalProps } from '@/components/ManagementLayout';
import { setDirty } from '@/utils/session';

type FormValues = RecycleConfigurationInput;
const columnRule = [{ required: true, message: '请选择字段' }];

function RecycleConfigForm({ tables, editing, onDone, onCancel }: { tables: RecycleTableOption[]; editing?: RecycleConfiguration; onDone: () => void; onCancel: () => void }) {
  const [form] = Form.useForm<FormValues>();
  const tableName = Form.useWatch('tableName', form);
  const selected = tables.find((item) => item.tableName === tableName);
  const textOptions = useMemo(() => selected?.columns.filter((column) => ['char', 'varchar', 'text', 'tinytext', 'mediumtext', 'longtext'].includes(column.type)).map((column) => ({ value: column.name, label: `${column.name}（${column.type}）` })) ?? [], [selected]);
  useEffect(() => {
    if (!editing) return;
    form.setFieldsValue({ ...editing, scopeRequired: editing.scopeRequired, enabled: editing.enabled });
  }, [editing, form]);
  const submit = async (values: FormValues) => {
    try {
      if (editing) await updateRecycleConfiguration(editing.id, { ...values, version: editing.version });
      else await createRecycleConfiguration(values);
      onDone();
      setDirty(false);
      return true;
    } catch (error) {
      if (error instanceof StaleRequest) return false;
      if (error instanceof ApiError) return false;
      return false;
    }
  };
  return <ModalForm<FormValues> {...compactFormProps} form={form} key={editing?.id || 'create'} name="recycle-config-editor"
    title={editing ? `修改回收站资源：${editing.name}` : '登记回收站资源'} width={900} open
    modalProps={compactModalProps} initialValues={editing ?? { scopeRequired: false, enabled: true, sort: 0 }}
    onValuesChange={() => setDirty(true)} onOpenChange={(open) => { if (!open) { setDirty(false); onCancel(); } }}
    onFinish={submit}>
    <Alert type="info" showIcon message="系统字段（版本、删除标记、创建/更新时间等）按统一约定自动识别；这里仅登记业务表、展示字段和数据范围。" style={{ marginBottom: 16 }} />
    <CompactFields><div>
      <Form.Item name="name" label="资源名称" rules={[{ required: true, max: 50 }]}><Input maxLength={50} placeholder="例如订单" /></Form.Item>
      <Form.Item name="tableName" label="业务表" rules={[{ required: true }]}><Select showSearch disabled={!!editing} options={tables.map((item) => ({ value: item.tableName, label: item.tableName }))} onChange={() => form.resetFields(['titleColumn', 'searchColumn'])} /></Form.Item>
      <Form.Item name="titleColumn" label="标题字段" rules={columnRule}><Select showSearch options={textOptions} /></Form.Item>
      <Form.Item name="searchColumn" label="搜索字段" rules={columnRule}><Select showSearch options={textOptions} /></Form.Item>
      <Form.Item name="scopeRequired" label="按当前机构隔离" valuePropName="checked"><Switch /></Form.Item>
    </div><div>
      <Alert type="info" showIcon message="机构、部门、创建人、版本、删除标记及审计时间字段均按系统约定自动使用。" />
      <Form.Item name="sort" label="排序"><InputNumber precision={0} /></Form.Item>
      {editing && <Form.Item name="enabled" label="启用" valuePropName="checked"><Switch /></Form.Item>}
    </div></CompactFields>
  </ModalForm>;
}

export default function RecycleConfigPage() {
  const access = useAccess();
  const [rows, setRows] = useState<RecycleConfiguration[]>([]);
  const [tables, setTables] = useState<RecycleTableOption[]>([]);
  const [editing, setEditing] = useState<RecycleConfiguration>();
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState('');
  const load = async () => {
    try { setError(''); const [items, metadata] = await Promise.all([listRecycleConfigurations(), recycleTableOptions()]); setRows(items); setTables(metadata); }
    catch (failure) { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '回收站配置加载失败'); }
  };
  useEffect(() => { void load(); }, []);
  if (!access.canReadRecycleConfig) return <PageContainer title="回收站配置" />;
  return <PageContainer title="回收站配置" subTitle="选择业务表和软删除字段后即可接入数据回收站。">
    {error && <Alert type="error" showIcon title={error} style={{ marginBottom: 16 }} />}
    {access.canCreateRecycleConfig && <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreating(true)} style={{ marginBottom: 16 }}>登记资源</Button>}
    <Table rowKey="id" dataSource={rows} pagination={false} columns={[
      { title: '资源名称', dataIndex: 'name' }, { title: '业务表', dataIndex: 'tableName' },
      { title: '范围', render: (_: unknown, row: RecycleConfiguration) => row.scopeRequired ? <Tag color="blue">按机构</Tag> : <Tag>全局</Tag> },
      { title: '状态', render: (_: unknown, row: RecycleConfiguration) => row.enabled ? <Tag color="green">启用</Tag> : <Tag>停用</Tag> },
      { title: '操作', render: (_: unknown, row: RecycleConfiguration) => <Space>{access.canEditRecycleConfig && <a onClick={() => setEditing(row)}>修改</a>}{access.canDeleteRecycleConfig && <Popconfirm title="删除该回收站登记？" onConfirm={async () => { try { await deleteRecycleConfiguration(row.id, row.version); void load(); } catch { /* 请求层已提示失败原因。 */ } }}><a>删除</a></Popconfirm>}</Space> },
    ]} />
    {(creating || editing) && <RecycleConfigForm tables={tables} editing={editing} onDone={() => { setCreating(false); setEditing(undefined); void load(); }} onCancel={() => { setCreating(false); setEditing(undefined); }} />}
  </PageContainer>;
}
