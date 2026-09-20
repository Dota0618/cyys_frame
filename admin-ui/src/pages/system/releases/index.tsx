import { ModalForm, PageContainer, ProForm, ProFormDatePicker, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Modal, Popconfirm } from 'antd';
import { useEffect, useRef, useState } from 'react';
import AttachmentField from '@/components/AttachmentField';
import DictionarySelect from '@/components/DictionarySelect';
import type { DictionaryChoice } from '@/components/DictionarySelect';
import type { FileChoice } from '@/components/FilePicker';
import ScopeSwitch from '@/components/ScopeSwitch';
import { createRelease, deleteRelease, listReleases, releaseAttachment, releaseDetail, releaseFiles, releaseOptions, updateRelease } from '@/services/system/releases';
import type { Release, ReleaseDetail } from '@/services/system/releases';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

interface FormValue { releaseNumber: string; dateUpdate: string; type: string; content: string; attachments: FileChoice[] }
export default function ReleasesPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [editor, setEditor] = useState<{ detail?: ReleaseDetail; options: DictionaryChoice[] }>();
  const [viewing, setViewing] = useState<ReleaseDetail>();
  const [error, setError] = useState('');
  useEffect(() => () => setDirty(false), []);
  const failed = (failure: unknown) => {
    if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试');
  };
  const edit = async (id?: string) => {
    setError('');
    try {
      const [options, detail] = await Promise.all([releaseOptions(id), id ? releaseDetail(id) : Promise.resolve(undefined)]);
      setEditor({ options, detail });
    } catch (failure) { failed(failure); }
  };
  const download = async (file: FileChoice) => {
    if (!viewing) return;
    try {
      const blob = await releaseAttachment(viewing.release.id, file.id);
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a'); anchor.href = url; anchor.download = file.name; anchor.click(); URL.revokeObjectURL(url);
    } catch (failure) { failed(failure); }
  };
  const columns: ProColumns<Release>[] = [
    { title: '版本号', dataIndex: 'releaseNumber', fieldProps: { maxLength: 10 } },
    { title: '更新日期', dataIndex: 'dateUpdate', valueType: 'date', search: false },
    { title: '版本类型', dataIndex: 'typeLabel', search: false,
      render: (_, row) => `${row.typeLabel || row.type}${row.typeDisabled ? '（已失效）' : ''}` },
    { title: '更新说明', dataIndex: 'content', search: false, ellipsis: true },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <Button key="detail" type="link" onClick={() => { void releaseDetail(row.id).then(setViewing).catch(failed); }}>详情</Button>,
      access.canEditReleases && <Button key="edit" type="link" onClick={() => { void edit(row.id); }}>编辑</Button>,
      access.canDeleteReleases && <Popconfirm key="delete" title="删除此版本记录？" description="删除记录将解除附件关联，源文件保留。" okText="删除" cancelText="取消"
        onConfirm={async () => { try { await deleteRelease(row.id, row.version); void action.current?.reload(); } catch (failure) { failed(failure); } }}>
        <Button type="link" danger>删除</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  const detail = editor?.detail;
  const row = detail?.release;
  return <PageContainer title="版本更新" subTitle="平台统一维护，各单位共享。">
    {error && <Alert type="error" title={error} showIcon />}
    <ProTable<Release> rowKey="id" actionRef={action} columns={columns} headerTitle="版本列表"
      form={{ name: 'release-search' }} options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, releaseNumber }) => {
        setError('');
        try { const data = await listReleases(current, pageSize, releaseNumber); return { data: data.records, total: data.total, success: true }; }
        catch (failure) { failed(failure); return { data: [], total: 0, success: false }; }
      }}
      toolBarRender={() => access.canCreateReleases ? [<Button key="create" type="primary" onClick={() => { void edit(); }}>新增版本</Button>] : []} />
    {editor && <ModalForm<FormValue> name="release-editor" title={row ? '编辑版本' : '新增版本'} open
      initialValues={{ releaseNumber: row?.releaseNumber, dateUpdate: row?.dateUpdate, type: row?.type, content: row?.content, attachments: detail?.attachments || [] }}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async ({ releaseNumber, dateUpdate, type, content, attachments }) => {
        try {
          const command = { releaseNumber, dateUpdate, type, content, attachments: attachments.map((file) => file.id) };
          if (row) await updateRelease(row.id, { ...command, version: row.version }); else await createRelease(command);
          void action.current?.reload(); return true;
        } catch (failure) { failed(failure); return false; }
      }}>
      <ProFormText name="releaseNumber" label="版本号" fieldProps={{ maxLength: 10 }}
        rules={[{ required: true, whitespace: true, max: 10, message: '请输入 1–10 个字符的版本号' }]} />
      <ProFormDatePicker name="dateUpdate" label="更新日期" fieldProps={{ format: 'YYYY-MM-DD' }}
        rules={[{ required: true, message: '请选择更新日期' }]} />
      <ProForm.Item name="type" label="版本类型" rules={[{ required: true, message: '请选择版本类型' }]}
        extra={!editor.options.length ? '暂无可用类型，请先在数据字典维护版本类型。' : undefined}>
        <DictionarySelect ariaLabel="版本类型" options={editor.options}
          historical={row ? { value: row.type, label: row.typeLabel || row.type } : undefined} />
      </ProForm.Item>
      <ProFormTextArea name="content" label="更新说明" fieldProps={{ maxLength: 1000, showCount: true, rows: 6 }}
        rules={[{ required: true, whitespace: true, max: 1000, message: '请输入 1–1000 个字符的更新说明' }]} />
      <ScopeSwitch />
      <p>新增附件前请选择来源单位，并确认附件可随版本共享给各单位。已有附件可以保留或移除。</p>
      <ProForm.Item name="attachments" label="版本附件">
        <AttachmentField canSelect={access.canSelectReleaseFiles} canUpload={access.canSelectReleaseFiles && access.canUploadFiles}
          load={(page, size, search) => releaseFiles(row?.id, page, size, search)} />
      </ProForm.Item>
    </ModalForm>}
    <Modal title={viewing ? `版本 ${viewing.release.releaseNumber}` : '版本详情'} open={!!viewing} footer={null}
      destroyOnHidden onCancel={() => setViewing(undefined)}>
      <p>{viewing?.release.dateUpdate} · {viewing?.release.typeLabel}</p>
      <div style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{viewing?.release.content}</div>
      <ul>{viewing?.attachments.map((file) => <li key={file.id}>
        <Button type="link" disabled={file.available === false} onClick={() => { void download(file); }}>{file.name}</Button>
      </li>)}</ul>
    </Modal>
  </PageContainer>;
}
