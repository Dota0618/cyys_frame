import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, ProFormDigit, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Popconfirm } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { CompactFields, compactModalProps } from '@/components/ManagementLayout';
import {
  createDictionaryItem, deleteDictionaryItem, dictionaryItemDetail, listDictionaryItems, updateDictionaryItem,
} from '@/services/system/dictionaries';
import type { CreateDictionaryItem, Dictionary, DictionaryItem, UpdateDictionaryItem } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

function ItemFields() {
  return <>
    <ProFormText name="paramValue" label="字典值" fieldProps={{ maxLength: 20 }}
      rules={[{ required: true, whitespace: true, max: 20, message: '请输入 1–20 个字符的字典值' }]} />
    <ProFormText name="desp" label="说明" fieldProps={{ maxLength: 200 }}
      rules={[{ max: 200, message: '说明最多 200 个字符' }]} />
    <ProFormDigit name="sort" label="排序" min={-2147483648} max={2147483647}
      fieldProps={{ precision: 0 }} rules={[{ type: 'integer', message: '排序请输入整数' }]} />
  </>;
}

export default function DictionaryItems({ dictionary }: { dictionary: Dictionary }) {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [editing, setEditing] = useState<DictionaryItem>();
  const [loadError, setLoadError] = useState('');
  useEffect(() => () => setDirty(false), []);

  const columns: ProColumns<DictionaryItem>[] = [
    { title: '编码', dataIndex: 'paramKey', search: false },
    { title: '字典值', dataIndex: 'paramValue', fieldProps: { maxLength: 20 } },
    { title: '说明', dataIndex: 'desp', search: false, ellipsis: true },
    { title: '排序', dataIndex: 'sort', search: false, width: 80 },
    { title: '操作', valueType: 'option', width: 150, hideInTable: dictionary.systemBuiltin, render: (_, row) => [
      !dictionary.systemBuiltin && access.canEditDictionaries && <a key="edit" onClick={() => {
        void dictionaryItemDetail(dictionary.id, row.id).then(setEditing).catch(() => {});
      }}>修改</a>,
      !dictionary.systemBuiltin && access.canDeleteDictionaries && <Popconfirm key="delete" title="删除该字典项？"
        description="编码会保留，删除不会改写已保存的业务记录。" okText="删除" cancelText="取消"
        onConfirm={async () => {
          try {
            await deleteDictionaryItem(dictionary.id, row.id, row.version);
            void action.current?.reload();
          } catch { /* 统一请求层已提示；保留当前字典项。 */ }
        }}><a>删除</a></Popconfirm>,
    ].filter(Boolean) },
  ];

  return <>
    {loadError && <Alert type="error" title={loadError} showIcon />}
    <ProTable<DictionaryItem> actionRef={action} rowKey="id" columns={columns}
      size="small" scroll={{ x: 600 }}
      form={{ name: 'dictionary-item-search' }}
      headerTitle={`${dictionary.name}（${dictionary.id}）· 字典项`}
      options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, paramValue }) => {
        setLoadError('');
        try {
          const result = await listDictionaryItems(dictionary.id, current, pageSize, paramValue);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest))
            setLoadError(error instanceof ApiError ? error.message : '字典项加载失败，请重试');
          return { data: [], total: 0, success: false };
        }
      }}
      toolBarRender={() => !dictionary.systemBuiltin && access.canCreateDictionaries ? [
        <ModalForm<CreateDictionaryItem> key="create" name="dictionary-item-create" title="新增字典项" width={720}
          trigger={<Button type="primary" icon={<PlusOutlined />}>新增字典项</Button>}
          initialValues={{ sort: 0 }} modalProps={compactModalProps}
          onValuesChange={() => setDirty(true)}
          onOpenChange={(open) => { if (!open) setDirty(false); }}
          onFinish={async ({ paramKey, paramValue, desp, sort }) => {
            try {
              await createDictionaryItem(dictionary.id, { paramKey, paramValue, desp, sort });
              void action.current?.reload();
              return true;
            } catch { return false; }
          }}>
          <CompactFields><ProFormText name="paramKey" label="编码" extra="同一字典内唯一；创建后保持不变。"
            fieldProps={{ maxLength: 20 }}
            rules={[{ required: true, whitespace: true, max: 20, message: '请输入 1–20 个字符的编码' }]} />
          <ItemFields /></CompactFields>
        </ModalForm>,
      ] : []}
    />
    <ModalForm<Omit<UpdateDictionaryItem, 'version'>> key={editing?.id || 'edit'}
      name="dictionary-item-edit" title={editing ? `修改字典项：${editing.paramKey}` : '修改字典项'} width={720}
      open={!!editing && !dictionary.systemBuiltin} initialValues={editing && {
        paramValue: editing.paramValue, desp: editing.desp, sort: editing.sort,
      }}
      modalProps={compactModalProps}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditing(undefined); setDirty(false); } }}
      onFinish={async ({ paramValue, desp, sort }) => {
        if (!editing) return false;
        try {
          await updateDictionaryItem(dictionary.id, editing.id, {
            version: editing.version, paramValue, desp, sort,
          });
          void action.current?.reload();
          return true;
        } catch { return false; }
      }}>
      <CompactFields><ItemFields /></CompactFields>
    </ModalForm>
  </>;
}
