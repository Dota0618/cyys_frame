import { Select } from 'antd';

export interface DictionaryChoice { value: string; label: string; disabled?: boolean }
/** 字典保存真实编码；已有失效值可回显和清除，不能作为新候选选择。 */
export default function DictionarySelect({ value, onChange, options, historical, ariaLabel }: {
  value?: string | null; onChange?: (value: string | null) => void;
  options: DictionaryChoice[]; historical?: DictionaryChoice; ariaLabel: string;
}) {
  const choices = historical && !options.some((option) => option.value === historical.value)
    ? [...options, { ...historical, disabled: true }] : options;
  return <Select aria-label={ariaLabel} allowClear value={value ?? undefined} onChange={(next) => onChange?.(next ?? null)}
    showSearch={{ optionFilterProp: 'label' }} options={choices.map((option) => ({
      ...option, label: option.disabled ? `${option.label}（已失效）` : option.label,
    }))} />;
}
