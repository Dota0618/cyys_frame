import { DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormSelect, ProFormText, ProFormTextArea, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Card, Empty, Input, Popconfirm, Space, Spin, Tag, Tree, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { compactFormProps, compactModalProps, ManagementSplit, verticalFormItemProps } from '@/components/ManagementLayout';
import {
  createCacheDefinition, deleteCacheDefinition, listCacheDefinitions, refreshCacheGroup, updateCacheDefinition,
} from '@/services/system/caches';
import type { CacheDefinition, CacheScope, CreateCacheDefinition, UpdateCacheDefinition } from '@/services/system/caches';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';

const scopeOptions = [
  { value: 'SYSTEM', label: '系统共享' },
  { value: 'UNIT', label: '当前单位' },
];

function DefinitionFields() {
  return <>
    <ProFormText name="name" label="缓存名称" fieldProps={{ maxLength: 100 }}
      rules={[{ required: true, whitespace: true, max: 100, message: '请输入 1–100 个字符的缓存名称' }]} />
    <ProFormText name="cacheKey" label="缓存键" extra="填写服务端登记的实际缓存区域标识，不是 Redis 键。"
      fieldProps={{ maxLength: 100 }}
      rules={[{ required: true, whitespace: true, max: 100, message: '请输入 1–100 个字符的缓存键' }]} />
    <ProFormText name="groupName" label="展示分组" fieldProps={{ maxLength: 50 }}
      rules={[{ required: true, whitespace: true, max: 50, message: '请输入 1–50 个字符的展示分组' }]} />
    <ProFormText name="refreshGroup" label="刷新组" extra="同一刷新组中的缓存会一次性刷新。"
      fieldProps={{ maxLength: 50 }}
      rules={[{ required: true, whitespace: true, max: 50, message: '请输入 1–50 个字符的刷新组' }]} />
    <ProFormSelect name="scope" label="作用范围" options={scopeOptions}
      rules={[{ required: true, message: '请选择作用范围' }]} />
    <ProFormTextArea name="description" label="说明" formItemProps={verticalFormItemProps}
      fieldProps={{ maxLength: 255, showCount: true, rows: 3 }}
      rules={[{ max: 255, message: '说明最多 255 个字符' }]} />
  </>;
}

export default function CachesPage() {
  const access = useAccess();
  const [definitions, setDefinitions] = useState<CacheDefinition[]>([]);
  const [selectedGroupName, setSelectedGroupName] = useState<string>();
  const [groupQuery, setGroupQuery] = useState('');
  const [cacheQuery, setCacheQuery] = useState('');
  const [pending, setPending] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; title: string }>();
  const [revision, setRevision] = useState(0);
  const [editing, setEditing] = useState<CacheDefinition>();

  useEffect(() => {
    let active = true;
    setLoading(true);
    setFeedback(undefined);
    void listCacheDefinitions().then(result => {
      if (!active) return;
      setDefinitions(result);
      const names = [...new Set(result.map(row => row.groupName))];
      setSelectedGroupName(current => current && names.includes(current) ? current : names[0]);
    }).catch(error => {
      if (active && !(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存定义加载失败，请重试',
      });
    }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [revision]);
  useEffect(() => () => setDirty(false), []);

  const groupNames = useMemo(() => [...new Set(definitions.map(row => row.groupName))], [definitions]);
  const filteredGroups = useMemo(() => {
    const keyword = groupQuery.trim().toLowerCase();
    return keyword ? groupNames.filter(name => name.toLowerCase().includes(keyword)) : groupNames;
  }, [groupNames, groupQuery]);
  const selectedRows = useMemo(() => definitions.filter(row => row.groupName === selectedGroupName), [definitions, selectedGroupName]);
  const filteredRows = useMemo(() => {
    const keyword = cacheQuery.trim().toLowerCase();
    return keyword ? selectedRows.filter(row => `${row.name} ${row.cacheKey} ${row.refreshGroup} ${row.description || ''}`.toLowerCase().includes(keyword)) : selectedRows;
  }, [cacheQuery, selectedRows]);
  const refreshGroups = useMemo(() => {
    const map = new Map<string, { name: string; scope: CacheScope }>();
    selectedRows.forEach(row => map.set(`${row.scope}:${row.refreshGroup}`, { name: row.refreshGroup, scope: row.scope }));
    return [...map.values()];
  }, [selectedRows]);

  const reload = () => setRevision(value => value + 1);
  const refresh = async (name: string, scope: CacheScope) => {
    const key = `${scope}:${name}`;
    setPending(key);
    setFeedback(undefined);
    try {
      await refreshCacheGroup({ refreshGroup: name, scope });
      setFeedback({ type: 'success', title: `刷新组“${name}”已刷新` });
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '缓存刷新失败，请重试',
      });
    } finally { setPending(undefined); }
  };

  const remove = async (row: CacheDefinition) => {
    setPending(`delete:${row.id}`);
    setFeedback(undefined);
    try {
      await deleteCacheDefinition(row.id, row.version);
      setFeedback({ type: 'success', title: `缓存“${row.name}”已删除` });
      reload();
    } catch (error) {
      if (!(error instanceof StaleRequest)) setFeedback({
        type: 'error', title: error instanceof ApiError ? error.message : '删除缓存定义失败，请重试',
      });
    } finally { setPending(undefined); }
  };

  const columns: ProColumns<CacheDefinition>[] = [
    { title: '缓存名称', dataIndex: 'name', ellipsis: true, search: false },
    { title: '缓存键', dataIndex: 'cacheKey', ellipsis: true, search: false },
    { title: '刷新组', dataIndex: 'refreshGroup', ellipsis: true, search: false },
    { title: '作用范围', dataIndex: 'scope', width: 110, search: false,
      render: (_, row) => row.scope === 'UNIT' ? <Tag color="blue">当前单位</Tag> : <Tag>系统共享</Tag> },
    { title: '说明', dataIndex: 'description', ellipsis: true, search: false },
    { title: '操作', valueType: 'option', width: 150, render: (_, row) => access.canManageCaches ? [
      <Button key="edit" type="link" size="small" icon={<EditOutlined />} disabled={!!pending}
        onClick={() => setEditing(row)}>修改</Button>,
      <Popconfirm key="delete" title={`确认删除“${row.name}”吗？`} okText="删除" cancelText="取消"
        onConfirm={() => { void remove(row); }}>
        <Button type="link" danger size="small" icon={<DeleteOutlined />} disabled={!!pending}>删除</Button>
      </Popconfirm>,
    ] : [] },
  ];

  return <PageContainer title="缓存管理" subTitle="维护缓存定义、展示分组和刷新组；手动操作只刷新整个刷新组，不编辑缓存数据。">
    {feedback && <Alert {...feedback} showIcon style={{ marginBottom: 16 }} />}
    <ManagementSplit navigation={<Card title="缓存分组" size="small" extra={<Button type="link" size="small" icon={<ReloadOutlined />} loading={loading} onClick={reload}>刷新</Button>}>
      <Input.Search aria-label="搜索缓存分组" placeholder="搜索分组名称" value={groupQuery} allowClear maxLength={50}
        onChange={event => setGroupQuery(event.target.value)} style={{ marginBottom: 12 }} />
      <Spin spinning={loading}>
        <div style={{ maxHeight: 'calc(100dvh - 340px)', minHeight: 220, overflow: 'auto' }}>
          {filteredGroups.length ? <Tree aria-label="缓存分组" blockNode selectedKeys={selectedGroupName ? [selectedGroupName] : []}
            treeData={filteredGroups.map(name => ({
              key: name,
              title: <Space size={6}><span>{name}</span><Tag>{definitions.filter(row => row.groupName === name).length} 项</Tag></Space>,
              isLeaf: true,
            }))}
            onSelect={keys => { const name = String(keys[0] || ''); if (name) { setSelectedGroupName(name); setCacheQuery(''); } }} />
            : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={feedback ? '分组加载失败' : '暂无缓存分组'} />}
        </div>
      </Spin>
      <Typography.Text type="secondary">共 {groupNames.length} 个分组</Typography.Text>
    </Card>}>
      {selectedGroupName ? <>
        <Card size="small" title={<Space>{selectedGroupName}<Tag>{selectedRows.length} 项</Tag></Space>}
          extra={access.canManageCaches && <Space wrap>
            {refreshGroups.map(group => {
              const key = `${group.scope}:${group.name}`;
              return <Button key={key} size="small" type="primary" icon={<ReloadOutlined />} loading={pending === key}
                disabled={!!pending && pending !== key} onClick={() => { void refresh(group.name, group.scope); }}>
                刷新组：{group.name}
              </Button>;
            })}
          </Space>} style={{ marginBottom: 16 }}>
          <Typography.Text type="secondary">刷新组内的所有缓存区域会一起失效；不会刷新单个缓存。</Typography.Text>
        </Card>
        <Card size="small" title="缓存定义列表"
          extra={<Space><Input.Search aria-label="查询缓存定义" placeholder="查询缓存名称、缓存键或刷新组" value={cacheQuery} allowClear maxLength={100}
            onChange={event => setCacheQuery(event.target.value)} style={{ width: 280 }} />
            {access.canManageCaches && <ModalForm<CreateCacheDefinition> {...compactFormProps} name="cache-definition-create" title="新增缓存定义" width={680}
              trigger={<Button type="primary" size="small" icon={<PlusOutlined />}>新增缓存</Button>}
              initialValues={{ scope: 'SYSTEM' }} modalProps={compactModalProps} onValuesChange={() => setDirty(true)}
              onOpenChange={open => { if (!open) setDirty(false); }}
              onFinish={async values => {
                try { await createCacheDefinition(values); reload(); return true; } catch { return false; }
              }}>
              <DefinitionFields />
            </ModalForm>}
          </Space>}>
          <ProTable<CacheDefinition> rowKey="id" columns={columns} dataSource={filteredRows} search={false}
            loading={loading} options={{ fullScreen: false, setting: false, density: false, reload: false }} pagination={false} />
        </Card>
      </> : <Card><Empty description={loading ? '正在加载缓存分组' : '暂无缓存定义'} /></Card>}
    </ManagementSplit>
    <ModalForm<Omit<UpdateCacheDefinition, 'version'>> {...compactFormProps} key={editing?.id || 'cache-definition-edit'}
      name="cache-definition-edit" title={editing ? `修改缓存：${editing.name}` : '修改缓存'} open={!!editing} width={680}
      initialValues={editing && { name: editing.name, cacheKey: editing.cacheKey, groupName: editing.groupName,
        refreshGroup: editing.refreshGroup, scope: editing.scope, description: editing.description }}
      modalProps={compactModalProps} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) { setEditing(undefined); setDirty(false); } }}
      onFinish={async values => {
        if (!editing) return false;
        try { await updateCacheDefinition(editing.id, { ...values, version: editing.version }); setEditing(undefined); reload(); return true; } catch { return false; }
      }}>
      <DefinitionFields />
    </ModalForm>
  </PageContainer>;
}
