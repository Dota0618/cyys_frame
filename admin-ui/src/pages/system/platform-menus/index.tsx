import { PlusOutlined } from '@ant-design/icons';
import {
  ModalForm,
  PageContainer,
  ProFormDigit,
  ProForm,
  ProFormDependency,
  ProFormRadio,
  ProFormSelect,
  ProFormText,
  ProFormTreeSelect,
  ProTable,
} from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, App, Button, Checkbox, Popconfirm, Tag } from 'antd';
import { useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import {
  createPlatformMenu,
  deletePlatformMenu,
  editPlatformMenu,
  listPlatformMenus,
  platformMenuDetail,
} from '@/services/system/platformMenus';
import type { CreatePlatformMenu, EditPlatformMenu, PlatformMenu } from '@/types/api';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';
import { menuRows } from '@/utils/menus';
import type { MenuRow } from '@/utils/menus';

const menuTypes = [
  { value: 1, label: '目录' },
  { value: 2, label: '菜单' },
  { value: 3, label: '按钮权限' },
];
const typeNames = new Map(menuTypes.map((item) => [item.value, item.label]));
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
  return (
    <>
      <ProFormDependency name={['type']}>
        {({ type }) => <ProFormTreeSelect name="parentId" label="上级菜单" rules={[{ required: true }, {
          validator: (_, value) => type === 3 && !menus.some(menu => menu.id === value && menu.type === 2)
            ? Promise.reject(new Error('按钮权限请选择所属菜单')) : Promise.resolve(),
        }]}
          fieldProps={{ treeDefaultExpandAll: true, treeData: [{ value: '0', title: '根目录', selectable: type !== 3, children: asOptions(parents, type) }] }} />}
      </ProFormDependency>
      {editing ? (
        <ProFormText name="code" label="菜单编码" disabled />
      ) : (
        <ProFormText name="code" label="菜单编码" fieldProps={{ maxLength: 100 }}
          rules={[{ required: true, whitespace: true }]} />
      )}
      <ProFormText name="name" label="菜单名称" fieldProps={{ maxLength: 50 }}
        rules={[{ required: true, whitespace: true }]} />
      {editing ? (
        <ProFormSelect name="type" label="类型" options={menuTypes} disabled />
      ) : (
        <ProFormSelect name="type" label="类型" options={menuTypes} rules={[{ required: true }]} />
      )}
      <ProFormDependency name={['type']}>
        {({ type }) => <>
          {type !== 3 && <><ProFormText name="path" label="路由路径" fieldProps={{ maxLength: 255 }} />
          <ProFormText name="icon" label="图标" fieldProps={{ maxLength: 100 }} /></>}
          <ProForm.Item name="visible" hidden={type === 3} getValueProps={(value: number) => ({ checked: value === 1 })} getValueFromEvent={(event: ChangeEvent<HTMLInputElement>) => event.target.checked ? 1 : 0}>
            <Checkbox>在左侧菜单中显示</Checkbox>
          </ProForm.Item>
        </>}
      </ProFormDependency>
      <ProFormDigit name="sort" label="排序" fieldProps={{ precision: 0 }} rules={[{ required: true }]} />
      <ProFormRadio.Group name="status" label="状态" options={enabled} rules={[{ required: true }]} />
      <ProFormRadio.Group name="platformOnly" label="仅平台角色" options={yesNo} rules={[{ required: true }]} />
      <ProFormRadio.Group name="keepAlive" label="页面缓存提示" options={yesNo} rules={[{ required: true }]} />
      <ProFormRadio.Group name="alwaysShow" label="始终展开" options={yesNo} rules={[{ required: true }]} />
    </>
  );
}

export default function PlatformMenusPage() {
  const access = useAccess();
  const { message } = App.useApp();
  const action = useRef<ActionType | null>(null);
  const [menus, setMenus] = useState<PlatformMenu[]>([]);
  const [editing, setEditing] = useState<PlatformMenu>();
  const [loadError, setLoadError] = useState('');
  const [expanded, setExpanded] = useState<React.Key[]>([]);

  const columns: ProColumns<MenuRow>[] = [
    { title: '菜单名称', dataIndex: 'name' },
    { title: '菜单编码', dataIndex: 'code', search: false },
    { title: '类型', dataIndex: 'type', search: false, renderText: (value) => typeNames.get(value) || '未知' },
    { title: '路径', dataIndex: 'path', search: false, ellipsis: true },
    {
      title: '范围', dataIndex: 'platformOnly', search: false,
      render: (_, row) => <Tag color={row.platformOnly === 1 ? 'blue' : 'default'}>
        {row.platformOnly === 1 ? '仅平台' : '共享'}
      </Tag>,
    },
    {
      title: '状态', dataIndex: 'status', search: false,
      render: (_, row) => <Tag color={row.status === 1 ? 'success' : 'default'}>
        {row.status === 1 ? '启用' : '停用'}
      </Tag>,
    },
    { title: '排序', dataIndex: 'sort', search: false, width: 70 },
    {
      title: '操作', valueType: 'option', width: 140,
      render: (_, row) => [
        access.canEditPlatformMenus && <a key="edit" onClick={() => {
          void platformMenuDetail(row.id).then(setEditing).catch(() => {});
        }}>修改</a>,
        access.canDeletePlatformMenus && (
          <Popconfirm key="delete" title="删除菜单" description="存在子菜单或角色引用时不能删除。"
            okText="删除" cancelText="取消" onConfirm={async () => {
              await deletePlatformMenu(row.id, row.version);
              void message.success('菜单已删除');
              await action.current?.reload();
            }}><a>删除</a></Popconfirm>
        ),
      ].filter(Boolean),
    },
  ];

  return (
    <PageContainer title="菜单管理" subTitle="目录下放菜单，菜单下放按钮权限；角色通过树形复选框选择可用菜单和操作。">
      {loadError && <Alert type="error" showIcon title={loadError} style={{ marginBottom: 20 }} />}
      <ProTable<MenuRow>
        actionRef={action}
        rowKey="id"
        headerTitle="菜单定义"
        columns={columns}
        search={false}
        options={{ density: true, fullScreen: false, setting: false, reload: true }}
        pagination={false}
        expandable={{ expandedRowKeys: expanded, onExpandedRowsChange: (keys) => setExpanded([...keys]) }}
        request={async () => {
          setLoadError('');
          try {
            const result = await listPlatformMenus();
            setMenus(result);
            setExpanded(result.filter(menu => menu.type === 1).map(menu => menu.id));
            return { data: menuRows(result), total: result.length, success: true };
          } catch (error) {
            if (!(error instanceof StaleRequest)) setLoadError(error instanceof ApiError ? error.message : '菜单加载失败，请重试');
            return { data: [], total: 0, success: false };
          }
        }}
        toolBarRender={() => [
          <Button key="expand" onClick={() => setExpanded(menus.filter(menu => menu.type !== 3).map(menu => menu.id))}>展开全部</Button>,
          <Button key="collapse" onClick={() => setExpanded([])}>收起全部</Button>,
          access.canCreatePlatformMenus &&
          <ModalForm<CreatePlatformMenu>
            key="create" name="platform-menu-create" title="新增菜单" width={680}
            trigger={<Button type="primary" icon={<PlusOutlined />}>新增菜单</Button>}
            initialValues={{ parentId: '0', type: 2, sort: 0, visible: 1, status: 1,
              platformOnly: 0, keepAlive: 0, alwaysShow: 0 }}
            modalProps={{ destroyOnHidden: true }}
            onValuesChange={() => setDirty(true)}
            onOpenChange={(open) => { if (!open) setDirty(false); }}
            onFinish={async (values) => {
              try {
                await createPlatformMenu(values);
                void message.success('菜单已新增');
                await action.current?.reload();
                return true;
              } catch { return false; }
            }}
          ><MenuFields menus={menus} /></ModalForm>,
        ].filter(Boolean)}
      />

      <ModalForm<EditPlatformMenu & { code: string; type: number }>
        key={editing?.id || 'edit'} name="platform-menu-edit" title="修改菜单" width={680}
        open={!!editing} initialValues={editing}
        modalProps={{ destroyOnHidden: true }}
        onValuesChange={() => setDirty(true)}
        onOpenChange={(open) => { if (!open) { setEditing(undefined); setDirty(false); } }}
        onFinish={async ({ code: _code, type: _type, ...values }) => {
          if (!editing) return false;
          try {
            await editPlatformMenu({ ...values, id: editing.id, version: editing.version });
            void message.success('菜单已更新');
            await action.current?.reload();
            return true;
          } catch { return false; }
        }}
      ><MenuFields menus={menus} editing={editing} /></ModalForm>
    </PageContainer>
  );
}
