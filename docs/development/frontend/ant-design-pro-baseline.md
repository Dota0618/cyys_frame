# Ant Design Pro 模板清单

基线：[官方预览](https://preview.pro.ant.design/)使用的 Ant Design Pro 6.0.1，[源码提交 2db826c](https://github.com/ant-design/ant-design-pro/tree/2db826cb350e46206252197f416a0a22f2b9bbfe)。模板源码位于 `admin-ui/`，依赖版本由 package.json 和 package-lock.json 锁定。

## 已接入入口

| 入口 / 能力 | 官方源码位置（相对 admin-ui） | CYYS 接入内容 |
|---|---|---|
| 布局、主题与头像菜单 | `src/app.tsx`、`config/defaultSettings.ts`、`src/components/RightContent/` | 保留 ProLayout、SettingDrawer、语言切换和菜单结构；接入真实身份、退出与单位选择 |
| 登录 | `src/pages/user/Login/` | 复用官方 LoginForm、背景和排版，接入账号密码登录 |
| 工作台 | `src/pages/dashboard/workplace/` | 复用页头、卡片和 EditableLinkGroup；显示当前身份与有权限的业务入口 |
| 用户管理 | `src/pages/table-list/` | 复用查询表格、CreateForm、详情抽屉；接入单位成员 API。编辑使用官方 basic-list 的 ModalForm 模式 |
| 异常及基础组件 | `src/pages/exception/`、`src/components/` | 使用官方 404、ErrorBoundary、OfflineBanner、HeaderDropdown 和 Footer |

实际路由只登记在 `config/routes.ts`。原始完整路由目录保留在 `config/routes.templates.ts`，供查找模板，不参与运行。

Ant lint 基线由 `scripts/antd-template-baseline.json` 维护，按文件摘要和具体告警登记允许的模板告警。`check-antd-findings.ps1` 拒绝新增告警、模板内容变化、路由变更或不完整扫描。启用示例页前须修正对应用法并重新评估基线，不自动接受扫描结果。

## 可按需复用的模板

| 开发需求 | 源码位置（相对 admin-ui） |
|---|---|
| 查询、标准、卡片和搜索列表 | `src/pages/table-list/`、`src/pages/list/` |
| 基础、分步和高级表单 | `src/pages/form/` |
| 基础和高级详情 | `src/pages/profile/` |
| 分析页、监控页、工作台 | `src/pages/dashboard/` |
| 成功、失败及异常页面 | `src/pages/result/`、`src/pages/exception/` |
| 个人中心和设置 | `src/pages/account/` |
| 头像组、标签选择、标准表单行 | `src/components/AvatarList/`、`TagSelect/`、`StandardFormRow/` |

保留的示例 service、模拟数据和演示交互仅供接入时参考，没有注册为 CYYS 业务入口。运行配置关闭 mock 和官方演示接口，不包含演示站统计配置。CYYS 接口从 `src/utils/request.ts` 调用。

当前使用固定 Node 24.0.1、Umi 4.6.51 及其 Webpack 构建；官方预览的 Utoo 构建器未启用。模板中的页面和组件仍保留，构建器差异不改变页面基线。

## 组件知识入口

- `antd` 基础组件：项目 Ant Design CLI 6.6.4，`npm run antd -- info Table --format json` 等命令按实际 antd 版本查询。官方 Skill 位于 `admin-ui/.agents/skills/antd/SKILL.md`，仅在相关任务中读取；CLI 作为开发依赖锁定，不参与网页运行。
- ProTable、ProForm、ProLayout 等：[Pro Components](https://pro-components.antdigital.dev/)及本工程对应模板源码。
- 页面选型先看上表；组件使用约定见 [前端复用规则](template-reuse.md)。
