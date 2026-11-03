# 页面设置

## 页面结构

- 根节点使用 `PageContainer`，内容区沿用公共布局，不重复显示标题和路径。
- 页面间距、字号和卡片密度由公共主题、全局样式及 `ManagementLayout` 维护，页面不重复覆盖。
- 页面按内容自然撑开；分类树只设置滚动区域上限，不用固定高度或最小高度填满页面。
- 页面复用现有布局、表格、表单及弹窗，接口见[请求规则](request-context.md)。

## 页面状态

使用组件 `loading` 或 `Spin`，状态随页面生命周期清理。

## 权限与操作

- 路由和按钮使用 `useAccess`，服务端负责最终授权。
- 删除、不可逆或影响较大的批量操作需要确认；版本冲突提示刷新。

## 表格与设置项

- `ProTable` 使用稳定 `rowKey`，查询字段与服务端同名，工具栏按页面需要配置。
- 列表复用 `PageTable` 并展开 `managementTableProps`，搜索复用 `tableSearchProps`，独立筛选复用 `ListFilters/FilterField`；宽表在表格内部横向滚动。
- 多页签的手动查询和导航状态复用 `usePageState`，不缓存业务响应及表单草稿。
- 页面设置使用组件状态或已有会话工具，业务状态不放 `localStorage`。

## 表单与反馈

字段和校验见[表单规则](form-validation.md)，每次操作只显示一次结果反馈。
