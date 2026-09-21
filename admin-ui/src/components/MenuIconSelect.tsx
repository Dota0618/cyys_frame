import { Select } from 'antd';
import { menuIconOptions } from '@/utils/menuIcons';

export default function MenuIconSelect({ value, onChange, id }: { value?: string; onChange?: (value?: string) => void; id?: string }) {
  const historical = value && !menuIconOptions.some(option => option.value === value)
    ? [{ value, search: value, label: <span>{value}（已有图标）</span> }] : [];
  return <Select id={id} aria-label="菜单图标" value={value || undefined} onChange={onChange}
    allowClear showSearch={{ optionFilterProp: 'search' }} placeholder="选择图标，可搜索中文或名称"
    options={[...historical, ...menuIconOptions]} style={{ width: '100%' }} />;
}
