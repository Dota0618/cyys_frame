import { Button, Space } from 'antd';
import { useRef, useState } from 'react';
import FilePicker from '@/components/FilePicker';
import type { FileChoice } from '@/components/FilePicker';
import FileUpload from '@/components/FileUpload';
import type { Page } from '@/types/api';

/** 表单只维护有序文件对象；移除仅改表单值，物理删除由文件管理负责。 */
export default function AttachmentField({ value = [], onChange, load, canUpload = false, limit = 10 }: {
  value?: FileChoice[]; onChange?: (value: FileChoice[]) => void; canUpload?: boolean; limit?: number;
  load: (page: number, size: number, search?: string) => Promise<Page<FileChoice>>;
}) {
  const [selecting, setSelecting] = useState(false);
  const current = useRef(value);
  current.current = value;
  const move = (index: number, offset: number) => {
    const ordered = [...value]; [ordered[index], ordered[index + offset]] = [ordered[index + offset], ordered[index]]; onChange?.(ordered);
  };
  return <div>
    <Button onClick={() => setSelecting(true)}>选择已有附件</Button>
    {canUpload && value.length < limit && <FileUpload onChanged={() => {}} onUploaded={(file) => {
      const selected = current.current;
      if (selected.length < limit && !selected.some((item) => item.id === file.id)) {
        onChange?.([...selected, { id: file.id, name: file.name, byteSize: file.byteSize }]);
      }
    }} />}
    <ol>{value.map((file, index) => <li key={file.id}>
      <Space wrap><span>{file.name}{file.available === false ? '（暂不可用）' : ''}</span>
        <Button size="small" disabled={index === 0} onClick={() => move(index, -1)}>上移</Button>
        <Button size="small" disabled={index === value.length - 1} onClick={() => move(index, 1)}>下移</Button>
        <Button size="small" onClick={() => onChange?.(value.filter((item) => item.id !== file.id))}>移除关联</Button>
      </Space>
    </li>)}</ol>
    <p>移除关联不会删除服务器文件。</p>
    {selecting && <FilePicker value={value} limit={limit} load={load} onCancel={() => setSelecting(false)}
      onConfirm={(files) => { onChange?.(files); setSelecting(false); }} />}
  </div>;
}
