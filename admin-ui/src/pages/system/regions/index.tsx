import { compactFormProps } from '@/components/ManagementLayout';
import { ModalForm, PageContainer, ProFormDigit, ProFormText, ProTable } from '@ant-design/pro-components';
import type { ActionType, ProColumns } from '@ant-design/pro-components';
import { useAccess } from '@umijs/max';
import { Alert, Button, Card, Col, Popconfirm, Row, Space } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { createRegion, deleteRegion, deleteRegions, exportRegions, listRegions, regionDetail, updateRegion } from '@/services/system/regions';
import type { Region, RegionFields } from '@/services/system/regions';
import { ApiError, StaleRequest } from '@/utils/request';
import { setDirty } from '@/utils/session';
import { saveDownload } from '@/utils/download';
import RegionTree from './RegionTree';

export default function RegionsPage() {
  const access = useAccess();
  const action = useRef<ActionType | null>(null);
  const querySequence = useRef(0);
  const [trail, setTrail] = useState<Region[]>([]);
  const [editor, setEditor] = useState<{ detail?: Region }>();
  const [selected, setSelected] = useState<Region[]>([]);
  const [error, setError] = useState('');
  const [exporting, setExporting] = useState(false);
  const [treeVersion, setTreeVersion] = useState(0);
  const [exportFilter, setExportFilter] = useState<{ parentId?: string; name: string }>();
  const parent = trail.at(-1);
  useEffect(() => () => setDirty(false), []);
  const failed = (failure: unknown) => {
    if (!(failure instanceof StaleRequest)) setError(failure instanceof ApiError ? failure.message : '操作失败，请重试');
  };
  const navigate = (next: Region[]) => { querySequence.current++; setTrail(next); setSelected([]); setExportFilter(undefined); setError(''); };
  const changed = () => { setSelected([]); setTreeVersion(version => version + 1); void action.current?.reload(); };
  const columns: ProColumns<Region>[] = [
    { title: '地区名称', dataIndex: 'name', fieldProps: { maxLength: 20 } },
    { title: '地区编码', dataIndex: 'cityCode', search: false },
    { title: '简称', dataIndex: 'shortName', search: false },
    { title: '邮编', dataIndex: 'zipCode', search: false },
    { title: '经度', dataIndex: 'longitude', search: false },
    { title: '纬度', dataIndex: 'latitude', search: false },
    { title: '排序', dataIndex: 'sort', search: false },
    { title: '操作', valueType: 'option', render: (_, row) => [
      <Button key="children" type="link" onClick={() => navigate([...trail, row])}>下级地区</Button>,
      access.canEditRegions && <Button key="edit" type="link" onClick={() => {
        void regionDetail(row.id).then((detail) => setEditor({ detail })).catch(failed);
      }}>编辑</Button>,
      access.canDeleteRegions && <Popconfirm key="delete" title="删除此地区？" description="存在下级时不能删除。" okText="删除" cancelText="取消"
        onConfirm={async () => { try { await deleteRegion(row); changed(); } catch (failure) { failed(failure); } }}>
        <Button type="link" danger disabled={row.hasChildren}>删除</Button>
      </Popconfirm>,
    ].filter(Boolean) },
  ];
  return <PageContainer title="地区管理" subTitle="左侧选择地区，右侧管理其下级；导出当前层级筛选结果，不含更深层级。">
    <Space wrap style={{ marginBottom: 16 }}>
      <Button type="link" onClick={() => navigate([])}>全部顶级地区</Button>
      {trail.map((region, index) => <Button key={region.id} type="link" onClick={() => navigate(trail.slice(0, index + 1))}>{region.name}</Button>)}
    </Space>
    {error && <Alert type="error" showIcon title={error} />}
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={7}><Card title="地区树" extra={<Button type="link" onClick={() => setTreeVersion(version => version + 1)}>刷新</Button>}>
        <div style={{ maxHeight: '70vh', overflow: 'auto' }}><RegionTree key={treeVersion} trail={trail} onSelect={navigate} onError={failed} /></div>
      </Card></Col>
      <Col xs={24} lg={17}>
    <ProTable<Region> key={parent?.id || 'root'} rowKey="id" actionRef={action} columns={columns}
      scroll={{ x: 900 }}
      headerTitle={parent ? `${parent.name}的下级地区` : '顶级地区列表'} form={{ name: 'region-search' }}
      options={{ fullScreen: false, setting: false }}
      pagination={{ defaultPageSize: 20, showSizeChanger: true, pageSizeOptions: [20, 50, 100, 200] }}
      rowSelection={access.canDeleteRegions ? { selectedRowKeys: selected.map((row) => row.id), onChange: (_, rows) => setSelected(rows),
        getCheckboxProps: (row) => ({ disabled: row.hasChildren }) } : false}
      request={async ({ current = 1, pageSize = 20, name }) => {
        const query = ++querySequence.current;
        setError('');
        setExportFilter(undefined);
        try {
          const data = await listRegions(current, pageSize, parent?.id, name);
          if (query !== querySequence.current) throw new StaleRequest();
          setExportFilter({ parentId: parent?.id, name: name || '' });
          return { data: data.records, total: data.total, success: true };
        }
        catch (failure) { if (query === querySequence.current) failed(failure); return { data: [], total: 0, success: false }; }
      }}
      toolBarRender={() => [
        access.canExportRegions && <Button key="export" loading={exporting}
          disabled={!exportFilter || exportFilter.parentId !== parent?.id} onClick={async () => {
            if (!exportFilter) return;
            setExporting(true); setError('');
            try { await saveDownload(() => exportRegions(exportFilter), '地区.xlsx'); }
            catch (failure) { failed(failure); }
            finally { setExporting(false); }
          }}>导出当前筛选</Button>,
        access.canCreateRegions && <Button key="create" type="primary" onClick={() => setEditor({})}>新增地区</Button>,
        access.canDeleteRegions && <Popconfirm key="batch" title={`删除选中的 ${selected.length} 个地区？`} okText="删除" cancelText="取消"
          disabled={!selected.length || selected.length > 100} onConfirm={async () => {
            try { await deleteRegions(selected); changed(); } catch (failure) { failed(failure); }
          }}><Button danger disabled={!selected.length || selected.length > 100}>批量删除</Button></Popconfirm>,
      ].filter(Boolean)} />
      </Col>
    </Row>
    {editor && <ModalForm<RegionFields> {...compactFormProps} name="region-editor" title={editor.detail ? '编辑地区' : '新增地区'} open
      omitNil={false} initialValues={editor.detail || { sort: 0 }} modalProps={{ destroyOnHidden: true }}
      onValuesChange={() => setDirty(true)} onOpenChange={(open) => { if (!open) { setEditor(undefined); setDirty(false); } }}
      onFinish={async ({ cityCode, name, shortName, zipCode, longitude, latitude, sort }) => {
        try {
          const command = { cityCode, name, shortName: shortName ?? null, zipCode: zipCode ?? null, longitude: longitude ?? null, latitude: latitude ?? null, sort };
          if (editor.detail) await updateRegion(editor.detail.id, { ...command, version: editor.detail.version });
          else await createRegion({ ...command, parentId: parent?.id });
          changed(); return true;
        } catch (failure) { failed(failure); return false; }
      }}>
      <p>上级地区：{parent?.name || '无（顶级地区）'}。创建后保持不变。</p>
      <ProFormText name="name" label="地区名称" fieldProps={{ maxLength: 20 }} rules={[{ required: true, whitespace: true, max: 20, message: '请输入 1–20 个字符的地区名称' }]} />
      <ProFormText name="cityCode" label="地区编码" fieldProps={{ maxLength: 20 }} rules={[{ required: true, whitespace: true, max: 20, message: '请输入 1–20 个字符的地区编码' }]} />
      <ProFormText name="shortName" label="地区简称" fieldProps={{ maxLength: 20 }} rules={[{ max: 20, message: '简称最多 20 个字符' }]} />
      <ProFormText name="zipCode" label="邮编" fieldProps={{ maxLength: 6 }} rules={[{ max: 6, message: '邮编最多 6 个字符' }]} />
      <ProFormDigit name="longitude" label="经度" min={-180} max={180} rules={[{ type: 'number', min: -180, max: 180, message: '经度应在 -180 至 180 之间' }]} />
      <ProFormDigit name="latitude" label="纬度" min={-90} max={90} rules={[{ type: 'number', min: -90, max: 90, message: '纬度应在 -90 至 90 之间' }]} />
      <ProFormDigit name="sort" label="排序" min={-2147483648} max={2147483647} fieldProps={{ precision: 0 }} rules={[{ type: 'integer', message: '排序请输入整数' }]} />
    </ModalForm>}
  </PageContainer>;
}
