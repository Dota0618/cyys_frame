import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProFormText } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Card, Empty, Input, Pagination, Popconfirm, Space, Spin, Tag, Tree, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { CompactFields, compactModalProps, ManagementSplit, compactFormProps } from '@/components/ManagementLayout';
import { createDictionary, deleteDictionary, dictionaryDetail, listDictionaries, updateDictionary } from '@/services/system/dictionaries';
import type { CreateDictionary, Dictionary } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';
import DictionaryItems from './DictionaryItems';

export default function DictionariesPage() {
  const access = useAccess();
  const [categories, setCategories] = useState<Dictionary[]>([]);
  const [selected, setSelected] = useState<Dictionary>();
  const [editor, setEditor] = useState<{ dictionary?: Dictionary }>();
  const [loadError, setLoadError] = useState('');
  const [loading, setLoading] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [revision, setRevision] = useState(0);
  const reload = () => setRevision(value => value + 1);
  useEffect(() => () => setDirty(false), []);
  useEffect(() => {
    let active = true;
    setLoading(true); setLoadError('');
    void listDictionaries(page, 20, query).then(result => {
      if (!active) return;
      if (page > 1 && !result.records.length && result.total > 0) { setPage(page - 1); return; }
      setCategories(result.records); setTotal(result.total);
      setSelected(current => result.records.find(row => row.id === current?.id) || result.records[0]);
    }).catch(error => {
      if (active && !(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '字典加载失败，请重试');
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [page, query, revision]);

  return <PageContainer title="数据字典" subTitle="左侧选择字典分类，右侧维护对应的编码、字典值和说明。">
    {loadError && <Alert type="error" title={loadError} showIcon style={{ marginBottom: 16 }} action={<Button onClick={reload}>重试</Button>} />}
    <ManagementSplit navigation={<Card title="字典分类" size="small" extra={access.canCreateDictionaries && <Button type="link" size="small" icon={<PlusOutlined />} onClick={() => setEditor({})}>新增字典</Button>}>
      <Input.Search aria-label="搜索字典分类" placeholder="搜索字典名称" value={searchText} allowClear maxLength={20}
        onChange={event => setSearchText(event.target.value)} onSearch={value => { setQuery(value); setPage(1); reload(); }} style={{ marginBottom: 12 }} />
      <Spin spinning={loading}>
        <div style={{ maxHeight: 'calc(100dvh - 340px)', minHeight: 220, overflow: 'auto' }}>
          {categories.length ? <Tree aria-label="字典分类" blockNode selectedKeys={selected ? [selected.id] : []}
            treeData={categories.map(row => ({ key: row.id, title: <Space size={4}>{row.name}<Tag color={row.systemBuiltin ? 'blue' : undefined}>{row.systemBuiltin ? '系统' : '业务'}</Tag></Space>, isLeaf: true }))}
            onSelect={keys => { const row = categories.find(item => item.id === keys[0]); if (row) setSelected(row); }} />
            : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={loadError ? '分类加载失败' : '暂无匹配字典'} />}
        </div>
      </Spin>
      <Pagination size="small" simple current={page} pageSize={20} total={total} showSizeChanger={false} onChange={setPage} style={{ marginTop: 12 }} />
      <Typography.Text type="secondary">共 {total} 个分类</Typography.Text>
    </Card>}>
      {selected ? <>
        <Card size="small" title={<Space>{selected.name}<Tag color={selected.systemBuiltin ? 'blue' : undefined}>{selected.systemBuiltin ? '系统字典 · 只读' : '业务字典'}</Tag></Space>} style={{ marginBottom: 16 }} extra={<Space>
          {!selected.systemBuiltin && access.canEditDictionaries && <Button size="small" onClick={() => { void dictionaryDetail(selected.id).then(dictionary => { if (!dictionary.systemBuiltin) setEditor({ dictionary }); else reload(); }).catch(() => {}); }}>修改分类</Button>}
          {!selected.systemBuiltin && access.canDeleteDictionaries && <Popconfirm title="删除该字典？" description="存在字典项时不能删除。" okText="删除" cancelText="取消"
            onConfirm={async () => { try { await deleteDictionary(selected.id, selected.version); setSelected(undefined); reload(); } catch { /* 保留当前分类。 */ } }}>
            <Button size="small" danger>删除分类</Button>
          </Popconfirm>}
        </Space>}>
          <Typography.Text type="secondary">编号：{selected.id}　内部名称：{selected.systemName}</Typography.Text>
          {selected.systemBuiltin && <Alert type="info" showIcon title="系统字典及其字典项只读，由系统版本统一维护。" style={{ marginTop: 12 }} />}
        </Card>
        <DictionaryItems key={selected.id} dictionary={selected} />
      </> : <Card><Empty description="请在左侧选择或新增字典分类" /></Card>}
    </ManagementSplit>
    {editor && <ModalForm<CreateDictionary> {...compactFormProps} name="dictionary-editor" title={editor.dictionary ? '修改字典' : '新增字典'} width={720} open modalProps={compactModalProps}
      initialValues={editor.dictionary || { sort: 0 }} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async ({ id, name, systemName, sort }) => {
        try {
          if (editor.dictionary) {
            const updated = await updateDictionary(editor.dictionary.id, { version: editor.dictionary.version, name, sort });
            setSelected(current => current?.id === updated.id ? updated : current);
          } else { await createDictionary({ id, name, systemName, sort }); setSearchText(name); setQuery(name); setPage(1); }
          setDirty(false); setEditor(undefined); reload(); return true;
        } catch { return false; }
      }}>
      {!editor.dictionary && <Typography.Paragraph type="secondary">新增分类为业务字典，可按权限维护分类及字典项。</Typography.Paragraph>}
      <CompactFields>
        <ProFormText name="id" label="字典编号" disabled={!!editor.dictionary} fieldProps={{ maxLength: 32 }} rules={[{ required: true, pattern: /^[0-9]{1,32}$/, message: '字典编号应为 1–32 位数字' }]} />
        <ProFormText name="systemName" label="内部名称" disabled={!!editor.dictionary} fieldProps={{ maxLength: 255 }} rules={[{ required: true, whitespace: true, max: 255 }]} />
        <ProFormText name="name" label="字典名称" fieldProps={{ maxLength: 20 }} rules={[{ required: true, whitespace: true, max: 20 }]} />
        <ProFormDigit name="sort" label="排序" min={-2147483648} max={2147483647} fieldProps={{ precision: 0 }} rules={[{ type: 'integer', message: '排序请输入整数' }]} />
      </CompactFields>
    </ModalForm>}
  </PageContainer>;
}
