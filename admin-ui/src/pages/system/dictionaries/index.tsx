import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Popconfirm } from 'antd';
import { useEffect, useRef, useState } from 'react';
import ScopeSwitch from '@/components/ScopeSwitch';
import {
  createDictionary, deleteDictionary, dictionaryDetail, listDictionaries, updateDictionary,
} from '@/services/system/dictionaries';
import type { CreateDictionary, Dictionary, UpdateDictionary } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';
import DictionaryItems from './DictionaryItems';

function DictionaryFields() {
  return <>
    <div style={{ marginBottom: 24 }}><ScopeSwitch /></div>
    <ProFormText name="name" label="字典名称" fieldProps={{ maxLength: 20 }}
      rules={[{ required: true, whitespace: true, max: 20, message: '请输入 1–20 个字符的字典名称' }]} />
    <ProFormDigit name="sort" label="排序" min={-2147483648} max={2147483647}
      fieldProps={{ precision: 0 }} rules={[{ type: 'integer', message: '排序请输入整数' }]} />
  </>;
}

export default function DictionariesPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const [selected, setSelected] = useState<Dictionary>();
  const [editing, setEditing] = useState<Dictionary>();
  const [loadError, setLoadError] = useState('');
  useEffect(() => () => setDirty(false), []);

  const columns: ProColumns<Dictionary>[] = [
    { title: '编号', dataIndex: 'id', search: false },
    { title: '字典名称', dataIndex: 'name', fieldProps: { maxLength: 20 } },
    { title: '内部名称', dataIndex: 'systemName', search: false, ellipsis: true },
    { title: '排序', dataIndex: 'sort', search: false, width: 80 },
    { title: '操作', valueType: 'option', width: 220, render: (_, row) => [
      <a key="items" onClick={() => setSelected(row)}>字典项</a>,
      access.canEditDictionaries && <a key="edit" onClick={() => {
        void dictionaryDetail(row.id).then(setEditing).catch(() => {});
      }}>修改</a>,
      access.canDeleteDictionaries && <Popconfirm key="delete" title="删除该字典？"
        description="存在字典项时不能删除。" okText="删除" cancelText="取消"
        onConfirm={async () => {
          try {
            await deleteDictionary(row.id, row.version);
            setSelected((current) => current?.id === row.id ? undefined : current);
            void action.current?.reload();
          } catch { /* 统一请求层已提示；保留分类和字典项。 */ }
        }}><a>删除</a></Popconfirm>,
    ].filter(Boolean) },
  ];

  return <PageContainer title="数据字典" subTitle="维护全局字典分类及分类下的编码、字典值。">
    {loadError && <Alert type="error" title={loadError} showIcon />}
    <ProTable<Dictionary> actionRef={action} rowKey="id" columns={columns} headerTitle="字典分类"
      form={{ name: 'dictionary-search' }}
      options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      request={async ({ current = 1, pageSize = 20, name }) => {
        setLoadError('');
        try {
          const result = await listDictionaries(current, pageSize, name);
          return { data: result.records, total: result.total, success: true };
        } catch (error) {
          if (!(error instanceof StaleRequest))
            setLoadError(error instanceof ApiError ? error.message : '字典加载失败，请重试');
          return { data: [], total: 0, success: false };
        }
      }}
      toolBarRender={() => access.canCreateDictionaries ? [
        <ModalForm<CreateDictionary> key="create" name="dictionary-create" title="新增字典"
          trigger={<Button type="primary" icon={<PlusOutlined />}>新增字典</Button>}
          initialValues={{ sort: 0 }} modalProps={{ destroyOnHidden: true }}
          onValuesChange={() => setDirty(true)}
          onOpenChange={(open) => { if (!open) setDirty(false); }}
          onFinish={async ({ id, name, systemName, sort }) => {
            try {
              await createDictionary({ id, name, systemName, sort });
              void action.current?.reload();
              return true;
            } catch { return false; }
          }}>
          <ProFormText name="id" label="字典编号" extra="保留原工程的分类编号；创建后保持不变。"
            fieldProps={{ maxLength: 32 }}
            rules={[{ required: true, pattern: /^[0-9]{1,32}$/, message: '字典编号应为 1–32 位数字' }]} />
          <ProFormText name="systemName" label="内部名称" extra="全局唯一，供业务识别该字典；创建后保持不变。"
            fieldProps={{ maxLength: 255 }}
            rules={[{ required: true, whitespace: true, max: 255, message: '请输入 1–255 个字符的内部名称' }]} />
          <DictionaryFields />
        </ModalForm>,
      ] : []}
    />
    {selected ? <DictionaryItems key={`items-${selected.id}`} dictionary={selected} /> :
      <Alert type="info" title="点击分类行的“字典项”，管理该分类下的内容。" showIcon />}
    <ModalForm<Omit<UpdateDictionary, 'version'>> key={`edit-${editing?.id || 'closed'}`}
      name="dictionary-edit" title={editing ? `修改字典：${editing.systemName}` : '修改字典'}
      open={!!editing} initialValues={editing && { name: editing.name, sort: editing.sort }}
      modalProps={{ destroyOnHidden: true }}
      onValuesChange={() => setDirty(true)}
      onOpenChange={(open) => { if (!open) { setEditing(undefined); setDirty(false); } }}
      onFinish={async ({ name, sort }) => {
        if (!editing) return false;
        try {
          const updated = await updateDictionary(editing.id, { version: editing.version, name, sort });
          setSelected((current) => current?.id === updated.id ? updated : current);
          void action.current?.reload();
          return true;
        } catch { return false; }
      }}>
      <DictionaryFields />
    </ModalForm>
  </PageContainer>;
}
