import { ModalForm, PageContainer, ProForm, ProFormText, ProFormTextArea, ProFormTreeSelect, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Modal, Popconfirm } from 'antd';
import { useEffect, useRef, useState } from 'react';
import AttachmentField from '@/components/AttachmentField';
import DictionarySelect from '@/components/DictionarySelect';
import ScopeSwitch from '@/components/ScopeSwitch';
import { departmentTreeData } from '@/utils/departments';
import { ApiError, StaleRequest } from '@/utils/request';
import { session, setDirty } from '@/utils/session';
import { createNotice, listNotices, noticeAttachment, noticeDetail, noticeEdit, noticeFiles, noticeOptions, noticeReaders, readNotice, removeNotice, updateNotice } from '@/services/system/orgNotices';
import type { Attachment, NoticeDetail, NoticeOptions, NoticeReader, OrgNotice } from '@/services/system/orgNotices';

interface FormValue { departmentId?: string; title: string; type?: string | null; content: string; attachments: Attachment[] }
export default function OrgNoticesPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [editor, setEditor] = useState<{ detail?: NoticeDetail; options: NoticeOptions }>();
  const [viewing, setViewing] = useState<NoticeDetail>();
  const [readers, setReaders] = useState<OrgNotice>();
  const [error, setError] = useState('');
  useEffect(() => () => setDirty(false), []);
  const failed = (failure: unknown) => { if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试'); };
  const edit = async (id?: string) => {
    setError('');
    try {
      const [options, detail] = await Promise.all([noticeOptions(id), id ? noticeEdit(id) : Promise.resolve(undefined)]);
      setEditor({ options, detail });
    } catch (failure) { failed(failure); }
  };
  const read = async (row: OrgNotice) => {
    const context = { revision: session().revision };
    try { const detail = await noticeDetail(row.id, context); await readNotice(row.id, context); setViewing(detail); void action.current?.reload(); }
    catch (failure) { failed(failure); }
  };
  const download = async (attachment: Attachment) => {
    if (!viewing) return;
    try {
      const blob = await noticeAttachment(viewing.notice.id, attachment.id); const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a'); anchor.href = url; anchor.download = attachment.name; anchor.click(); URL.revokeObjectURL(url);
    } catch (failure) { failed(failure); }
  };
  const columns: ProColumns<OrgNotice>[] = [
    { title: '通知标题', dataIndex: 'title' },
    { title: '所属部门', dataIndex: 'departmentName', search: false },
    { title: '通知类型', dataIndex: 'typeLabel', search: false, render: (_, row) => row.type ? `${row.typeLabel || row.type}${row.typeDisabled ? '（已失效）' : ''}` : '—' },
    { title: '阅读人数', dataIndex: 'readCount', search: false },
    { title: '我的阅读', dataIndex: 'read', search: false, renderText: (read) => read ? '已读' : '未读' },
    { title: '创建时间', dataIndex: 'createdAt', valueType: 'dateTime', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <Button key="view" type="link" onClick={() => { void read(row); }}>查看</Button>,
      access.canEditOrgNotices && <Button key="edit" type="link" onClick={() => { void edit(row.id); }}>编辑</Button>,
      access.canReadNoticeReceipts && <Button key="readers" type="link" onClick={() => setReaders(row)}>阅读记录</Button>,
      access.canDeleteOrgNotices && <Popconfirm key="delete" title="删除此通知？" description="同时解除附件关联，文件内容保留。" onConfirm={async () => {
        try { await removeNotice(row); void action.current?.reload(); } catch (failure) { failed(failure); }
      }}><Button type="link" danger>删除</Button></Popconfirm>,
    ].filter(Boolean) },
  ];
  const detail = editor?.detail;
  return <PageContainer title="组织通知" subTitle="保存后即可在授权范围内查看；记录首次阅读，编辑不会清空已有阅读记录。">
    <ScopeSwitch />
    {error && <Alert type="error" title={error} showIcon />}
    <ProTable<OrgNotice> actionRef={action} rowKey="id" columns={columns} form={{ name: 'org-notice-search' }}
      toolBarRender={() => access.canCreateOrgNotices ? [<Button key="create" type="primary" onClick={() => { void edit(); }}>新增通知</Button>] : []}
      request={async (params) => {
        try { const result = await listNotices(params.current || 1, params.pageSize || 20, params.title); return { data: result.records, total: result.total, success: true }; }
        catch (failure) { failed(failure); return { data: [], success: false }; }
      }} />
    {editor && <ModalForm<FormValue> name="org-notice-editor" title={detail ? '编辑通知' : '新增通知'} open omitNil={false}
      initialValues={{ title: detail?.notice.title, content: detail?.content, type: detail?.notice.type ?? null, attachments: detail?.attachments || [] }}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async (values) => {
        const command = { title: values.title, content: values.content, type: values.type ?? null, attachments: values.attachments.map((file) => file.id) };
        try {
          if (detail) await updateNotice(detail.notice.id, { ...command, version: detail.notice.version });
          else await createNotice({ ...command, departmentId: values.departmentId! });
          setEditor(undefined); setDirty(false); void action.current?.reload(); return true;
        } catch { return false; }
      }}>
      <ScopeSwitch />
      {detail ? <p>所属部门：{detail.notice.departmentName}</p> : <ProFormTreeSelect name="departmentId" label="所属部门"
        rules={[{ required: true, message: '请选择所属部门' }]} fieldProps={{ treeData: departmentTreeData(editor.options.departments), treeDefaultExpandAll: true }} />}
      <ProFormText name="title" label="通知标题" rules={[{ required: true, whitespace: true, message: '请输入通知标题' }, { max: 100 }]} />
      <ProForm.Item name="type" label="通知类型"><DictionarySelect ariaLabel="通知类型" options={editor.options.types}
        historical={detail?.notice.type ? { value: detail.notice.type, label: detail.notice.typeLabel || detail.notice.type, disabled: detail.notice.typeDisabled } : undefined} /></ProForm.Item>
      <ProFormTextArea name="content" label="通知内容" fieldProps={{ rows: 5 }} rules={[{ required: true, whitespace: true, message: '请输入通知内容' }, { max: 2000 }]} />
      <ProForm.Item name="attachments" label="附件" rules={[{ type: 'array', max: 10 }]}>
        <AttachmentField canUpload={access.canUploadFiles} load={(page, size, search) => noticeFiles(detail?.notice.id, page, size, search)} />
      </ProForm.Item>
    </ModalForm>}
    <Modal title={viewing?.notice.title || '通知详情'} open={!!viewing} destroyOnHidden footer={null} onCancel={() => setViewing(undefined)}>
      {viewing && <><p style={{ whiteSpace: 'pre-wrap' }}>{viewing.content}</p><ul>{viewing.attachments.map((file) => <li key={file.id}>
        <Button type="link" disabled={file.available === false} onClick={() => { void download(file); }}>{file.name}</Button>
      </li>)}</ul></>}
    </Modal>
    <Modal title="阅读记录" open={!!readers} destroyOnHidden footer={null} width={720} onCancel={() => setReaders(undefined)}>
      {readers && <ProTable<NoticeReader> key={readers.id} rowKey="userId" search={false} options={false}
        columns={[{ title: '阅读人', dataIndex: 'userName' }, { title: '首次阅读时间', dataIndex: 'firstReadAt', valueType: 'dateTime' }]}
        request={async (params) => {
          try { const result = await noticeReaders(readers.id, params.current || 1, params.pageSize || 20); return { data: result.records, total: result.total, success: true }; }
          catch (failure) { failed(failure); return { data: [], success: false }; }
        }} />}
    </Modal>
  </PageContainer>;
}
