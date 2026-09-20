import { ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { Alert, Modal } from 'antd';
import { useState } from 'react';
import type { Page } from '@/types/api';
import { StaleRequest } from '@/utils/request';

export interface FileChoice { id: string; name: string; byteSize?: number; available?: boolean }
export default function FilePicker({ value, limit, load, onConfirm, onCancel }: {
  value: FileChoice[]; limit: number; load: (page: number, size: number, search?: string) => Promise<Page<FileChoice>>;
  onConfirm: (files: FileChoice[]) => void; onCancel: () => void;
}) {
  const [chosen, setChosen] = useState(value);
  const [error, setError] = useState('');
  const columns: ProColumns<FileChoice>[] = [
    { title: '文件名', dataIndex: 'name' },
    { title: '大小', dataIndex: 'byteSize', search: false, renderText: (bytes) => `${bytes} 字节` },
  ];
  return <Modal title="选择已有附件" open width={800} destroyOnHidden okText="确定选择" onCancel={onCancel}
    okButtonProps={{ disabled: chosen.length > limit }} onOk={() => onConfirm(chosen)}>
    <p>最多选择 {limit} 个文件，保存时会重新检查权限和文件状态。</p>
    {error && <Alert type="error" title={error} showIcon />}
    <ProTable<FileChoice> rowKey="id" columns={columns} form={{ name: 'attachment-picker-search' }} options={false}
      rowSelection={{ preserveSelectedRowKeys: true, selectedRowKeys: chosen.map((file) => file.id),
        getCheckboxProps: (file) => ({ disabled: !chosen.some((item) => item.id === file.id) && chosen.length >= limit }),
        onChange: (keys, rows) => {
          const known = new Map([...chosen, ...rows].filter(Boolean).map((file) => [file.id, file]));
          setChosen(keys.map((key) => known.get(String(key))).filter((file): file is FileChoice => !!file));
        } }}
      request={async (params) => {
        try { const page = await load(params.current || 1, params.pageSize || 20, params.name); setError(''); return { data: page.records, total: page.total, success: true }; }
        catch (failure) { if (!(failure instanceof StaleRequest)) setError('附件加载失败，请重试'); return { data: [], success: false }; }
      }} />
  </Modal>;
}
