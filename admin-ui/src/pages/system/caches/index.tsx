import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Card, Descriptions, Empty, Input, Modal, Popconfirm, Space, Spin, Tag, Tree, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { compactFormProps, ManagementSplit } from '@/components/ManagementLayout';
import {
  createCacheRefreshItem, deleteCacheRefreshItem, listCacheActions, listCacheRefreshItems,
  refreshCache, refreshConfiguredCache, updateCacheRefreshItem,
} from '@/services/system/caches';
import type { CacheAction, CacheRefreshItem, CreateCacheRefreshItem, UpdateCacheRefreshItem } from '@/services/system/caches';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

interface CacheEntry {
  id: string;
  name: string;
  scope: string;
  description: string;
}

export default function CachesPage() {
  const access = useAccess();
  const [actions, setActions] = useState<CacheAction[]>([]);
  const [refreshItems, setRefreshItems] = useState<CacheRefreshItem[]>([]);
  const [selectedId, setSelectedId] = useState<string>();
  const [groupQuery, setGroupQuery] = useState('');
  const [cacheQuery, setCacheQuery] = useState('');
  const [pending, setPending] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; title: string }>();
  const [revision, setRevision] = useState(0);
  const [viewing, setViewing] = useState<{ group: CacheAction; cache: CacheEntry }>();
  const [editingRefreshItem, setEditingRefreshItem] = useState<CacheRefreshItem>();

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFeedback(undefined);
    void Promise.all([listCacheActions(), listCacheRefreshItems()]).then(([result, items]) => {
      if (!active) return;
      setActions(result);
      setRefreshItems(items);
      setSelectedId(current => result.some(action => action.id === current) ? current : result[0]?.id);
    }).catch(error => {
      if (active && !(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存配置加载失败，请重试',
      });
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [revision]);
  useEffect(() => () => setDirty(false), []);

  const filteredActions = useMemo(() => {
    const keyword = groupQuery.trim().toLowerCase();
    return keyword ? actions.filter(action => `${action.name} ${action.id} ${action.scope}`.toLowerCase().includes(keyword)) : actions;
  }, [actions, groupQuery]);
  const selected = actions.find(action => action.id === selectedId);
  const cacheRows = useMemo<CacheEntry[]>(() => selected?.contents.map((content, index) => ({
    id: `${selected.id}-${index}`,
    name: content,
    scope: selected.scope,
    description: selected.description,
  })) || [], [selected]);
  const filteredCacheRows = useMemo(() => {
    const keyword = cacheQuery.trim().toLowerCase();
    return keyword ? cacheRows.filter(cache => `${cache.name} ${cache.description}`.toLowerCase().includes(keyword)) : cacheRows;
  }, [cacheQuery, cacheRows]);

  const refresh = async (action: CacheAction) => {
    setPending(action.id);
    setFeedback(undefined);
    try {
      await refreshCache(action.id);
      setFeedback({ type: 'success', title: `${action.name}已刷新，下次读取时重新加载` });
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存刷新失败，请重试',
      });
    } finally { setPending(undefined); }
  };

  const refreshConfigured = async (item: CacheRefreshItem) => {
    setPending(item.id);
    setFeedback(undefined);
    try {
      await refreshConfiguredCache(item.id);
      setFeedback({ type: 'success', title: `${item.name}已刷新` });
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存刷新失败，请重试',
      });
    } finally { setPending(undefined); }
  };

  const removeRefreshItem = async (item: CacheRefreshItem) => {
    setPending(item.id);
    setFeedback(undefined);
    try {
      await deleteCacheRefreshItem(item.id, item.version);
      setFeedback({ type: 'success', title: `${item.name}已删除` });
      setRevision(value => value + 1);
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '删除刷新配置失败，请重试',
      });
    } finally { setPending(undefined); }
  };

  const columns: ProColumns<CacheEntry>[] = [
    { title: '序号', valueType: 'index', width: 72, align: 'center', search: false },
    { title: '缓存内容', dataIndex: 'name', ellipsis: true, search: false },
    { title: '作用范围', dataIndex: 'scope', width: 120, search: false,
      render: (_, row) => <Tag color={row.scope === '当前单位' ? 'blue' : 'default'}>{row.scope}</Tag> },
    { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
    { title: '操作', valueType: 'option', width: 80, render: (_, row) => [
      <Button key="view" type="link" size="small" onClick={() => selected && setViewing({ group: selected, cache: row })}>查看</Button>,
    ] },
  ];

  const refreshItemColumns: ProColumns<CacheRefreshItem>[] = [
    { title: '刷新名称', dataIndex: 'name', ellipsis: true },
    { title: '作用范围', width: 120, search: false, render: () => <Tag>系统共享</Tag> },
    { title: '说明', search: false, render: () => '按名称清理对应的平台缓存区域' },
    { title: '操作', valueType: 'option', width: 230, render: (_, row) => [
      <Button key="refresh" type="link" size="small" icon={<ReloadOutlined />} loading={pending === row.id}
        disabled={!!pending && pending !== row.id} onClick={() => { void refreshConfigured(row); }}>刷新</Button>,
      <Button key="edit" type="link" size="small" icon={<EditOutlined />} disabled={!!pending}
        onClick={() => setEditingRefreshItem(row)}>修改</Button>,
      <Popconfirm key="delete" title={`确认删除“${row.name}”吗？`} okText="删除" cancelText="取消"
        onConfirm={() => { void removeRefreshItem(row); }}>
        <Button type="link" danger size="small" icon={<DeleteOutlined />} disabled={!!pending}>删除</Button>
      </Popconfirm>,
    ] },
  ];

  return <PageContainer title="缓存管理" subTitle="业务缓存按代码登记；独立刷新配置只维护可手动刷新的名称，不编辑缓存数据。">
    {feedback && <Alert {...feedback} showIcon style={{ marginBottom: 16 }} />}
    <ManagementSplit navigation={<Card title="缓存分组" size="small" extra={<Button type="link" size="small" icon={<ReloadOutlined />} loading={loading} onClick={() => setRevision(value => value + 1)}>刷新</Button>}>
      <Input.Search aria-label="搜索缓存分组" placeholder="搜索分组名称或编码" value={groupQuery} allowClear maxLength={50}
        onChange={event => setGroupQuery(event.target.value)} style={{ marginBottom: 12 }} />
      <Spin spinning={loading}>
        <div style={{ maxHeight: 'calc(100dvh - 340px)', minHeight: 220, overflow: 'auto' }}>
          {filteredActions.length ? <Tree aria-label="缓存分组" blockNode selectedKeys={selected ? [selected.id] : []}
            treeData={filteredActions.map(action => ({ key: action.id, title: <Space size={6}><span>{action.name}</span><Tag>{action.contents.length} 项</Tag></Space>, isLeaf: true }))}
            onSelect={keys => { const id = String(keys[0] || ''); if (id) { setSelectedId(id); setCacheQuery(''); } }} />
            : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={feedback ? '分组加载失败' : '暂无匹配分组'} />}
        </div>
      </Spin>
      <Typography.Text type="secondary">共 {actions.length} 个分组</Typography.Text>
    </Card>}>
      {selected ? <>
        <Card size="small" title={<Space>{selected.name}<Tag color={selected.scope === '当前单位' ? 'blue' : 'default'}>{selected.scope}</Tag></Space>}
          extra={access.canManageCaches && <Button size="small" icon={<ReloadOutlined />} loading={pending === selected.id}
            disabled={!!pending && pending !== selected.id} onClick={() => { void refresh(selected); }}>刷新分组</Button>} style={{ marginBottom: 16 }}>
          <Descriptions size="small" column={{ xs: 1, sm: 2 }}>
            <Descriptions.Item label="分组编码">{selected.id}</Descriptions.Item>
            <Descriptions.Item label="缓存数量">{selected.contents.length} 项</Descriptions.Item>
          </Descriptions>
          <Typography.Paragraph type="secondary" ellipsis={{ rows: 2, expandable: true }} style={{ margin: '12px 0 0' }}>
            {selected.description}
          </Typography.Paragraph>
        </Card>
        <Card size="small" title="缓存列表" extra={<Input.Search aria-label="查询分组缓存" placeholder="查询缓存内容" value={cacheQuery} allowClear maxLength={100}
          onChange={event => setCacheQuery(event.target.value)} style={{ width: 240 }} />}>
          <ProTable<CacheEntry> rowKey="id" columns={columns} dataSource={filteredCacheRows} search={false}
            options={{ fullScreen: false, setting: false, density: false, reload: false }} pagination={false} />
        </Card>
      </> : <Card><Empty description={loading ? '正在加载缓存分组' : '请在左侧选择缓存分组'} /></Card>}
    </ManagementSplit>
    <Card size="small" title="独立刷新配置" extra={access.canManageCaches && <ModalForm<CreateCacheRefreshItem> {...compactFormProps}
      name="cache-refresh-create" title="新增刷新配置" trigger={<Button type="primary" size="small" icon={<PlusOutlined />}>新增刷新配置</Button>}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) setDirty(false); }}
      onFinish={async ({ name }) => {
        try {
          await createCacheRefreshItem({ name });
          setRevision(value => value + 1);
          return true;
        } catch { return false; }
      }}>
      <ProFormText name="name" label="刷新名称" extra="名称同时作为独立平台缓存的刷新标识。"
        fieldProps={{ maxLength: 50 }} rules={[{ required: true, whitespace: true, max: 50, message: '请输入 1–50 个字符的刷新名称' }]} />
    </ModalForm>}>
      <ProTable<CacheRefreshItem> rowKey="id" columns={refreshItemColumns} dataSource={refreshItems} search={false}
        loading={loading} options={{ fullScreen: false, setting: false, density: false, reload: false }} pagination={false} />
      <Typography.Text type="secondary">共 {refreshItems.length} 个独立刷新配置</Typography.Text>
    </Card>
    <ModalForm<Omit<UpdateCacheRefreshItem, 'version'>> {...compactFormProps}
      key={editingRefreshItem?.id || 'cache-refresh-edit'} name="cache-refresh-edit"
      title={editingRefreshItem ? `修改刷新配置：${editingRefreshItem.name}` : '修改刷新配置'}
      open={!!editingRefreshItem} initialValues={editingRefreshItem && { name: editingRefreshItem.name }}
      modalProps={{ destroyOnHidden: true }} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) { setEditingRefreshItem(undefined); setDirty(false); } }}
      onFinish={async ({ name }) => {
        if (!editingRefreshItem) return false;
        try {
          await updateCacheRefreshItem(editingRefreshItem.id, { name, version: editingRefreshItem.version });
          setRevision(value => value + 1);
          return true;
        } catch { return false; }
      }}>
      <ProFormText name="name" label="刷新名称" extra="修改名称后，刷新会按新名称定位缓存区域。"
        fieldProps={{ maxLength: 50 }} rules={[{ required: true, whitespace: true, max: 50, message: '请输入 1–50 个字符的刷新名称' }]} />
    </ModalForm>
    <Modal title={viewing ? `${viewing.group.name} · 缓存详情` : '缓存详情'} open={!!viewing} footer={null} destroyOnHidden onCancel={() => setViewing(undefined)}>
      {viewing && <Descriptions size="small" column={1}>
        <Descriptions.Item label="缓存内容">{viewing.cache.name}</Descriptions.Item>
        <Descriptions.Item label="所属分组">{viewing.group.name}</Descriptions.Item>
        <Descriptions.Item label="分组编码">{viewing.group.id}</Descriptions.Item>
        <Descriptions.Item label="作用范围">{viewing.cache.scope}</Descriptions.Item>
        <Descriptions.Item label="说明">{viewing.cache.description}</Descriptions.Item>
      </Descriptions>}
    </Modal>
  </PageContainer>;
}
