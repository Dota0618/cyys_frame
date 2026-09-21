import { compactFormProps, verticalFormItemProps } from '@/components/ManagementLayout';
import { PageContainer, ProForm, ProFormDigit, ProFormList, ProFormSelect, ProFormSwitch, ProFormText } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Card, Form, Select, Space, Tabs, Typography, Upload } from 'antd';
import { useEffect, useRef, useState } from 'react';
import * as service from '@/services/system/generator';
import type { GeneratorCommand, GeneratorField, GeneratorFieldType, GeneratorPreview, GeneratorTable } from '@/services/system/generator';
import { saveDownload } from '@/utils/download';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty } from '@/utils/session';

const fieldTypes: { value: GeneratorFieldType; label: string }[] = [
  { value: 'TEXT', label: '短文本' }, { value: 'TEXTAREA', label: '多行文本' }, { value: 'INTEGER', label: '整数' },
  { value: 'DECIMAL', label: '金额 / 小数' }, { value: 'BOOLEAN', label: '是 / 否' }, { value: 'DATE', label: '日期' },
  { value: 'DATETIME', label: '日期时间' }, { value: 'DICTIONARY', label: '字典选择' },
];
const initial: GeneratorCommand = {
  table: 'biz_example', feature: 'example', className: 'Example', title: '示例业务', layout: 'LIST', existingTable: false,
  fields: [{ column: 'name', label: '名称', type: 'TEXT', length: 100, required: true, inList: true, searchable: true }],
};
const reserved = new Set(['id', 'scope_id', 'org_id', 'parent_id', 'version', 'deleted', 'created_by', 'created_at', 'updated_by', 'updated_at']);
export default function GeneratorPage() {
  const access = useAccess();
  const [form] = Form.useForm<GeneratorCommand>();
  const [tables, setTables] = useState<GeneratorTable[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState<{ command: GeneratorCommand; preview: GeneratorPreview }>();
  const [file, setFile] = useState('');
  const revision = useRef(0);
  const existing = Form.useWatch('existingTable', form);
  const fields = Form.useWatch('fields', form);
  const changed = () => { revision.current++; setResult(undefined); setDirty(true); };
  const failed = (failure: unknown) => { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError || failure instanceof Error ? failure.message : '操作失败'); };
  useEffect(() => { void service.tables().then(setTables).catch(failed); return () => { revision.current++; setDirty(false); }; }, []);
  const loadTable = async (table: string) => {
    changed(); const version = revision.current; setBusy(true); setError('');
    try {
      const columns = await service.columns(table);
      const next: GeneratorField[] = columns.filter(column => !reserved.has(column.name)).map(column => {
        let type: GeneratorFieldType;
        if (column.dataType === 'varchar') type = (column.length || 0) > 255 ? 'TEXTAREA' : 'TEXT';
        else if (column.dataType === 'int') type = 'INTEGER';
        else if (column.columnType === 'tinyint(1)') type = 'BOOLEAN';
        else if (column.columnType === 'decimal(18,2)') type = 'DECIMAL';
        else if (column.dataType === 'date') type = 'DATE';
        else if (column.dataType === 'datetime') type = 'DATETIME';
        else throw new Error(`字段 ${column.name} 的类型暂不支持，请先调整业务设计`);
        return { column: column.name, label: column.comment || column.name, type, length: column.length || undefined,
          required: !column.nullable, inList: true, searchable: type === 'TEXT' };
      });
      if (revision.current !== version) return;
      form.setFieldsValue({ table, fields: next, layout: columns.some(c => c.name === 'parent_id') ? 'TREE' : 'LIST' });
    } catch (failure) { failed(failure); } finally { setBusy(false); }
  };
  const show = async (command: GeneratorCommand) => {
    const version = revision.current; setBusy(true); setError('');
    try {
      const preview = await service.preview(command);
      if (version !== revision.current) return false;
      setResult({ command: structuredClone(command), preview }); setFile(preview.files[0]?.path || ''); return true;
    } catch (failure) { failed(failure); return false; } finally { setBusy(false); }
  };
  return <PageContainer title="代码生成器" subTitle="配置业务字段，预览后下载后端、前端和建表脚本。">
    <Space orientation="vertical" size="middle" style={{ width: '100%' }}>
      <Alert type="info" showIcon title="沿用当前框架的单位隔离和权限规则" description="支持普通列表与树形列表。单位、部门、审计和版本字段自动生成；业务代码可在下载后修改。" />
      {error && <Alert type="error" title={error} showIcon />}
      <Card title="生成配置" extra={<Upload accept=".json" showUploadList={false} disabled={busy} beforeUpload={async (upload) => {
        const context = session().revision, version = revision.current;
        try {
          if (upload.size > 50_000) throw new Error('配置文件不能超过 50 KB');
          const command = JSON.parse(await upload.text()) as GeneratorCommand;
          if (context !== session().revision || version !== revision.current) return Upload.LIST_IGNORE;
          if (await show(command)) { form.resetFields(); form.setFieldsValue(command); setDirty(true); }
        } catch (failure) { failed(failure); }
        return Upload.LIST_IGNORE;
      }}><Button disabled={busy}>导入生成配置</Button></Upload>}>
        <ProForm<GeneratorCommand> {...compactFormProps} form={form} name="generator-config" initialValues={initial} disabled={busy}
          onValuesChange={changed} onFinish={show} submitter={{ searchConfig: { submitText: '预览代码' }, resetButtonProps: false }}>
          <ProFormSwitch formItemProps={verticalFormItemProps} name="existingTable" label="从已有业务表读取字段" />
          {existing && <Form.Item label="选择业务表"><Select aria-label="选择业务表" showSearch={{ optionFilterProp: 'label' }}
            options={tables.map(table => ({ value: table.name, label: table.comment ? `${table.name} · ${table.comment}` : table.name }))}
            notFoundContent="当前没有 biz_ 业务表，可关闭此选项设计新表" onChange={(table) => { void loadTable(table); }} /></Form.Item>}
          <ProForm.Group>
            <ProFormText name="table" label="业务表名" disabled={!!existing} rules={[{ required: true }, { pattern: /^biz_[a-z][a-z0-9_]{0,43}$/, message: '以 biz_ 开头，使用小写字母、数字和下划线' }]} />
            <ProFormText name="feature" label="模块标识" rules={[{ required: true }, { pattern: /^[a-z][a-z0-9]{1,23}$/, message: '2–24 位小写字母或数字' }]} />
            <ProFormText name="className" label="Java 类名" rules={[{ required: true }, { pattern: /^[A-Z][A-Za-z0-9]{1,39}$/, message: '大写字母开头，2–40 位字母或数字' }]} />
            <ProFormText name="title" label="页面名称" rules={[{ required: true, whitespace: true }, { max: 40 }]} />
            <ProFormSelect name="layout" label="页面类型" rules={[{ required: true }]} options={[{ value: 'LIST', label: '普通列表' }, { value: 'TREE', label: '树形列表' }]} />
          </ProForm.Group>
          <Typography.Paragraph type="secondary">第一个字段作为记录名称，须为必填短文本。树形记录创建后不移动父级。</Typography.Paragraph>
          <ProFormList {...verticalFormItemProps} name="fields" label="业务字段" min={1} max={30} creatorRecord={{ column: '', label: '', type: 'TEXT', length: 100, required: false, inList: true, searchable: false }}
            copyIconProps={false} creatorButtonProps={{ creatorButtonText: '添加字段' }}>
            {(field) => <ProForm.Group>
              <ProFormText name="column" label="字段名" width="sm" rules={[{ required: true }, { pattern: /^[a-z][a-z0-9]*(?:_[a-z0-9]+)*$/, message: '使用小写字段名' }]} />
              <ProFormText name="label" label="显示名称" width="sm" rules={[{ required: true, whitespace: true }, { max: 40 }]} />
              <ProFormSelect name="type" label="字段类型" width="sm" rules={[{ required: true }]} options={fieldTypes} />
              {['TEXT', 'TEXTAREA'].includes(fields?.[field.name]?.type) && <ProFormDigit name="length" label="最大长度" min={1} max={2000} width="xs" rules={[{ required: true }]} />}
              {fields?.[field.name]?.type === 'DICTIONARY' && <ProFormText name="dictionary" label="字典内部名称" width="sm" rules={[{ required: true }, { pattern: /^[A-Z][A-Z0-9_]{0,63}$/ }]} />}
              <ProFormSwitch name="required" label="必填" /><ProFormSwitch name="inList" label="列表显示" /><ProFormSwitch name="searchable" label="查询条件" />
            </ProForm.Group>}
          </ProFormList>
        </ProForm>
      </Card>
      {result && <Card title={`代码预览 · ${result.preview.files.length} 个文件`} extra={<Button type="primary" disabled={!access.canGenerateCode} loading={busy} onClick={async () => {
        setBusy(true);
        try { await saveDownload(() => service.download(result.command), `${result.command.feature}-generated.zip`); setDirty(false); }
        catch (failure) { failed(failure); } finally { setBusy(false); }
      }}>下载代码包</Button>}>
        <Typography.Paragraph type="secondary">配置文件也包含在包内，后续可导入继续生成。合入前请阅读 INTEGRATION.md。</Typography.Paragraph>
        <Tabs activeKey={file} onChange={setFile} tabPlacement="start" items={result.preview.files.map(source => ({ key: source.path, label: source.path.split('/').pop(),
          children: <><Typography.Text code>{source.path}</Typography.Text><pre style={{ overflow: 'auto', maxHeight: 600, whiteSpace: 'pre', padding: 16 }}>{source.content}</pre></> }))} />
      </Card>}
    </Space>
  </PageContainer>;
}
