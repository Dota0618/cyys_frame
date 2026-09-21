import { compactFormProps, verticalFormItemProps } from '@/components/ManagementLayout';
import { ModalForm, PageContainer, ProForm, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Modal, Popconfirm, Switch } from 'antd';
import { useEffect, useRef, useState } from 'react';
import DictionarySelect from '@/components/DictionarySelect';
import type { DictionaryChoice } from '@/components/DictionarySelect';
import {
  createSystemNotice, deleteSystemNotice, listSystemNotices, systemNoticeDetail, systemNoticeOptions, updateSystemNotice,
} from '@/services/system/systemNotices';
import type { SystemNotice, SystemNoticeCommand } from '@/services/system/systemNotices';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

export default function SystemNoticesPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [editor, setEditor] = useState<{ detail?: SystemNotice; options: DictionaryChoice[] }>();
  const [viewing, setViewing] = useState<SystemNotice>();
  const [error, setError] = useState('');
  useEffect(() => () => setDirty(false), []);
  const failed = (failure: unknown) => {
    if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试');
  };
  const edit = async (id?: string) => {
    setError('');
    try {
      const [options, detail] = await Promise.all([systemNoticeOptions(id), id ? systemNoticeDetail(id) : Promise.resolve(undefined)]);
      setEditor({ options, detail });
    } catch (failure) { failed(failure); }
  };
  const columns: ProColumns<SystemNotice>[] = [
    { title: '通知标题', dataIndex: 'title', fieldProps: { maxLength: 100 } },
    { title: '通知类型', dataIndex: 'typeLabel', search: false,
      render: (_, row) => `${row.typeLabel || row.type}${row.typeDisabled ? '（已失效）' : ''}` },
    { title: '是否显示', dataIndex: 'visible', search: false, renderText: (visible) => visible ? '显示' : '隐藏' },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <Button key="detail" type="link" onClick={() => { void systemNoticeDetail(row.id).then(setViewing).catch(failed); }}>详情</Button>,
      access.canEditSystemNotices && <Button key="edit" type="link" onClick={() => { void edit(row.id); }}>编辑</Button>,
      access.canDeleteSystemNotices && <Popconfirm key="delete" title="删除此系统通知？" okText="删除" cancelText="取消"
        onConfirm={async () => {
          try { await deleteSystemNotice(row.id, row.version); void action.current?.reload(); } catch (failure) { failed(failure); }
        }}><Button type="link" danger>删除</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  const detail = editor?.detail;
  return <PageContainer title="系统通知" subTitle="平台统一维护，各单位共享。">
    {error && <Alert type="error" showIcon title={error} />}
    <ProTable<SystemNotice> rowKey="id" actionRef={action} columns={columns} headerTitle="系统通知列表"
      form={{ name: 'system-notice-search' }} options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, title }) => {
        setError('');
        try {
          const result = await listSystemNotices(current, pageSize, title);
          return { data: result.records, total: result.total, success: true };
        } catch (failure) { failed(failure); return { data: [], total: 0, success: false }; }
      }}
      toolBarRender={() => access.canCreateSystemNotices ? [
        <Button key="create" type="primary" onClick={() => { void edit(); }}>新增系统通知</Button>,
      ] : []} />
    {editor && <ModalForm<SystemNoticeCommand> {...compactFormProps} name="system-notice-editor" title={detail ? '编辑系统通知' : '新增系统通知'}
      open initialValues={detail || { visible: false }} modalProps={{ destroyOnHidden: true }}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async ({ title, type, content, visible }) => {
        try {
          const command = { title, type, content, visible };
          if (detail) await updateSystemNotice(detail.id, { ...command, version: detail.version });
          else await createSystemNotice(command);
          void action.current?.reload();
          return true;
        } catch (failure) { failed(failure); return false; }
      }}>
      <ProFormText name="title" label="通知标题" fieldProps={{ maxLength: 100 }}
        rules={[{ required: true, whitespace: true, max: 100, message: '请输入 1–100 个字符的通知标题' }]} />
      <ProForm.Item name="type" label="通知类型" rules={[{ required: true, message: '请选择通知类型' }]}
        extra={!editor.options.length ? '暂无可用类型，请先在数据字典维护系统通知类型。' : undefined}>
        <DictionarySelect ariaLabel="通知类型" options={editor.options}
          historical={detail ? { value: detail.type, label: detail.typeLabel || detail.type } : undefined} />
      </ProForm.Item>
      <ProFormTextArea formItemProps={verticalFormItemProps} name="content" label="通知正文" fieldProps={{ maxLength: 2000, showCount: true, rows: 8 }}
        rules={[{ required: true, whitespace: true, max: 2000, message: '请输入 1–2000 个字符的通知正文' }]} />
      <ProForm.Item name="visible" label="是否显示" valuePropName="checked">
        <Switch aria-label="是否显示" checkedChildren="显示" unCheckedChildren="隐藏" />
      </ProForm.Item>
    </ModalForm>}
    <Modal title={viewing?.title} open={!!viewing} footer={null} destroyOnHidden onCancel={() => setViewing(undefined)}>
      <p>{viewing?.typeLabel} · {viewing?.visible ? '显示' : '隐藏'}</p>
      <div style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{viewing?.content}</div>
    </Modal>
  </PageContainer>;
}
