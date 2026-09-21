import { PlusOutlined } from '@ant-design/icons';
import { ModalForm, PageContainer, ProFormDigit, ProForm, ProFormDependency, ProFormRadio, ProFormSelect, ProFormText, ProFormTreeSelect, ProTable } from '@ant-design/pro-components';
import type { ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Card, Checkbox, Popconfirm, Space, Tag, Tree } from 'antd';
import type { TreeDataNode } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { createPlatformMenu, deletePlatformMenu, editPlatformMenu, listPlatformMenus, platformMenuDetail } from '@/services/system/platformMenus';
import type { CreatePlatformMenu, EditPlatformMenu, PlatformMenu } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { refreshIdentity, setDirty } from '@/utils/session';
import { menuRows } from '@/utils/menus';
import type { MenuRow } from '@/utils/menus';
import { menuIcon } from '@/utils/menuIcons';
import MenuIconSelect from '@/components/MenuIconSelect';
import { CompactFields, compactModalProps, ManagementSplit, compactFormProps } from '@/components/ManagementLayout';

const menuTypes = [{ value: 1, label: '目录' }, { value: 2, label: '菜单' }, { value: 3, label: '按钮权限' }];
const typeNames = new Map(menuTypes.map(item => [item.value, item.label]));
const yesNo = [{ value: 1, label: '是' }, { value: 0, label: '否' }];
const enabled = [{ value: 1, label: '启用' }, { value: 0, label: '停用' }];
interface ParentOption { value: string; title: string; selectable: boolean; children?: ParentOption[] }

function MenuFields({ menus, editing }: { menus: PlatformMenu[]; editing?: PlatformMenu }) {
  const excluded = new Set(editing ? [editing.id] : []);
  for (let i = 0; i < menus.length; i++) menus.forEach(menu => { if (excluded.has(menu.parentId)) excluded.add(menu.id); });
  const parents = menuRows(menus.filter(menu => menu.type !== 3 && !excluded.has(menu.id)));
  const asOptions = (rows: MenuRow[], type: number): ParentOption[] => rows.map(row => ({
    value: row.id, title: row.name, selectable: type !== 3 || row.type === 2,
    children: row.children ? asOptions(row.children, type) : undefined,
  }));
  return <CompactFields>
    <ProFormText name="name" label="菜单名称" fieldProps={{ maxLength: 50 }} rules={[{ required: true, whitespace: true }]} />
    <ProFormText name="code" label="菜单编码" disabled={!!editing} fieldProps={{ maxLength: 100 }} rules={[{ required: true, whitespace: true }]} />
    <ProFormSelect name="type" label="类型" options={menuTypes} disabled={!!editing} rules={[{ required: true }]} />
    <ProFormDependency name={['type']}>
      {({ type }) => <ProFormTreeSelect name="parentId" label="上级菜单" rules={[{ required: true }, {
        validator: (_, value) => type === 3 && !menus.some(menu => menu.id === value && menu.type === 2)
          ? Promise.reject(new Error('按钮权限请选择所属菜单')) : Promise.resolve(),
      }]} fieldProps={{ treeDefaultExpandAll: true, treeData: [{ value: '0', title: '根目录', selectable: type !== 3, children: asOptions(parents, type) }] }} />}
    </ProFormDependency>
    <ProFormDependency name={['type']}>
      {({ type }) => type !== 3 && <>
        <ProFormText name="path" label="路由路径" fieldProps={{ maxLength: 255 }} />
        <ProForm.Item name="icon" label="图标"><MenuIconSelect /></ProForm.Item>
      </>}
    </ProFormDependency>
    <ProFormDigit name="sort" label="排序" fieldProps={{ precision: 0 }} rules={[{ required: true }]} />
    <ProFormRadio.Group name="status" label="状态" options={enabled} rules={[{ required: true }]} />
    <ProFormRadio.Group name="keepAlive" label="页面缓存提示" options={yesNo} rules={[{ required: true }]} />
    <ProFormRadio.Group name="alwaysShow" label="始终展开" options={yesNo} rules={[{ required: true }]} />
    <ProFormDependency name={['type']}>
      {({ type }) => <ProForm.Item name="visible" label="导航显示" hidden={type === 3}
        getValueProps={(value: number) => ({ checked: value === 1 })}
        getValueFromEvent={(event: ChangeEvent<HTMLInputElement>) => event.target.checked ? 1 : 0}>
        <Checkbox>在左侧菜单中显示</Checkbox>
      </ProForm.Item>}
    </ProFormDependency>
  </CompactFields>;
}

const treeNodes = (rows: MenuRow[]): TreeDataNode[] => rows.map(row => ({
  key: row.id, title: row.name, icon: menuIcon(row.icon), children: row.children ? treeNodes(row.children) : undefined,
}));

export default function PlatformMenusPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const [menus, setMenus] = useState<PlatformMenu[]>([]);
  const [selectedId, setSelectedId] = useState('0');
  const [editing, setEditing] = useState<PlatformMenu>();
  const [creating, setCreating] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [loading, setLoading] = useState(false);
  const [expanded, setExpanded] = useState<React.Key[]>(['0']);
  const initialized = useRef(false);
  const reload = useCallback(async () => {
    setLoading(true); setLoadError('');
    try {
      const result = await listPlatformMenus(); setMenus(result);
      setSelectedId(id => id === '0' || result.some(menu => menu.id === id) ? id : '0');
      if (!initialized.current) { setExpanded(['0', ...result.filter(menu => menu.type === 1).map(menu => menu.id)]); initialized.current = true; }
    } catch (error) { if (!(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '菜单加载失败，请重试'); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void reload(); return () => setDirty(false); }, [reload]);
  const selected = menus.find(menu => menu.id === selectedId);
  const openEdit = (row: PlatformMenu) => { void platformMenuDetail(row.id).then(setEditing).catch(() => {}); };
  const columns: ProColumns<PlatformMenu>[] = [
    { title: '菜单名称', dataIndex: 'name', render: (_, row) => <Space>{menuIcon(row.icon)}{row.type === 3 ? row.name : <a onClick={() => setSelectedId(row.id)}>{row.name}</a>}</Space> },
    { title: '编码', dataIndex: 'code', ellipsis: true },
    { title: '类型', dataIndex: 'type', width: 90, renderText: value => typeNames.get(value) || '未知' },
    { title: '状态', dataIndex: 'status', width: 80, render: (_, row) => <Tag color={row.status === 1 ? 'success' : 'default'}>{row.status === 1 ? '启用' : '停用'}</Tag> },
    { title: '排序', dataIndex: 'sort', width: 60 },
    { title: '操作', valueType: 'option', width: 120, render: (_, row) => [
      access.canEditPlatformMenus && <a key="edit" onClick={() => openEdit(row)}>修改</a>,
      access.canDeletePlatformMenus && <Popconfirm key="delete" title="删除菜单" description="存在子菜单或角色引用时不能删除。" okText="删除" cancelText="取消"
        onConfirm={async () => { await deletePlatformMenu(row.id, row.version); void message.success('菜单已删除'); await reload(); }}><a>删除</a></Popconfirm>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="菜单管理" subTitle="左侧选择目录或菜单，右侧维护下级菜单和按钮权限。">
    {loadError && <Alert type="error" showIcon title={loadError} style={{ marginBottom: 16 }} action={<Button onClick={() => void reload()}>重试</Button>} />}
    <ManagementSplit navigation={<Card title="菜单树" size="small" extra={<Button type="link" size="small" loading={loading} onClick={() => void reload()}>刷新</Button>}>
      <Space style={{ marginBottom: 12 }}><Button size="small" onClick={() => setExpanded(['0', ...menus.filter(menu => menu.type !== 3).map(menu => menu.id)])}>展开</Button><Button size="small" onClick={() => setExpanded(['0'])}>收起</Button></Space>
      <div style={{ maxHeight: 'calc(100dvh - 300px)', minHeight: 220, overflow: 'auto' }}>
        <Tree aria-label="菜单树" blockNode showIcon selectedKeys={[selectedId]} expandedKeys={expanded} onExpand={keys => setExpanded(keys)}
          onSelect={keys => { if (keys.length) setSelectedId(String(keys[0])); }}
          treeData={[{ key: '0', title: '全部菜单', children: treeNodes(menuRows(menus.filter(menu => menu.type !== 3))) }]} />
      </div>
    </Card>}>
      <ProTable<PlatformMenu> key={selectedId} rowKey="id" columns={columns} dataSource={menus.filter(menu => menu.parentId === selectedId)} loading={loading}
        headerTitle={selected ? `${selected.name} · ${selected.type === 2 ? '下级菜单与按钮' : '下级菜单'}` : '顶级目录与菜单'}
        search={false} options={false} size="small" scroll={{ x: 680 }} pagination={{ defaultPageSize: 20, showSizeChanger: true }}
        toolBarRender={() => [
          selected && access.canEditPlatformMenus && <Button key="edit" onClick={() => openEdit(selected)}>修改当前{selected.type === 1 ? '目录' : '菜单'}</Button>,
          access.canCreatePlatformMenus && <Button key="create" type="primary" icon={<PlusOutlined />} onClick={() => setCreating(true)}>{selected ? '新增下级' : '新增菜单'}</Button>,
        ].filter(Boolean)} />
    </ManagementSplit>
    {creating && <ModalForm<CreatePlatformMenu> {...compactFormProps} name="platform-menu-create" title={selected ? `新增下级 · ${selected.name}` : '新增菜单'} width={800} open
      modalProps={compactModalProps} initialValues={{ parentId: selectedId, type: selected?.type === 2 ? 3 : selected ? 2 : 1, sort: 0, visible: 1, status: 1, keepAlive: 0, alwaysShow: 0 }}
      onValuesChange={() => setDirty(true)} onOpenChange={open => { if (!open) { setCreating(false); setDirty(false); } }}
      onFinish={async values => { try { await createPlatformMenu(values); setDirty(false); setCreating(false); await reload(); void message.success('菜单已新增'); return true; } catch { return false; } }}>
      <MenuFields menus={menus} />
    </ModalForm>}
    <ModalForm<EditPlatformMenu & { code: string; type: number }> {...compactFormProps} key={editing?.id || 'edit'} name="platform-menu-edit" title="修改菜单" width={800}
      open={!!editing} initialValues={editing} modalProps={compactModalProps} onValuesChange={() => setDirty(true)}
      onOpenChange={open => { if (!open) { setEditing(undefined); setDirty(false); } }}
      onFinish={async ({ code: _code, type: _type, ...values }) => {
        if (!editing) return false;
        try { await editPlatformMenu({ ...values, icon: values.icon || '', id: editing.id, version: editing.version }); setEditing(undefined); setDirty(false); void message.success('菜单已更新'); refreshIdentity(); return true; }
        catch { return false; }
      }}><MenuFields menus={menus} editing={editing} /></ModalForm>
  </PageContainer>;
}
