# CYYS Framework

I'm L.C.I live in China.

基于 JDK 25、Spring Boot 4.1.1 和 Ant Design Pro 的单租户框架，采用一个可执行后端和一个独立前端。

当前功能包括机构与部门、账号与角色、菜单权限、数据范围、全局参数、字典、地区、操作与登录审计、缓存、私有文件与断点续传、数据回收站、通知、版本发布、密码管理、XLSX 导出和流水号。

## 使用入口

| 任务 | 说明 |
|---|---|
| 开发约定 | [项目入口](AGENTS.md)、[规则索引](docs/development/README.md) |
| 功能范围与限制 | [当前开发范围](docs/development/current-scope.md) |
| 环境准备与构建 | [目录与构建](docs/development/project-build.md) |
| 数据库准备 | [单文件初始化](migrations/README.md) |
| 更新本机开发环境 | [开发部署](deploy/development.md) |
| 发布与恢复 | [部署说明](deploy/README.md) |
| 文件存储配置 | [存储规则](docs/development/backend/storage.md)、[本地配置示例](deploy/storage-local.yml.example) |
| 检查与保存 | [验证与保存](docs/development/verification.md) |
| 版本与任务资料 | [版本目录](docs/versions/README.md) |

## 目录

```text
backend/
  cyys-common/       通用技术能力
  cyys-admin/        系统管理功能
  cyys-application/  启动、配置和模块装配，生成唯一可执行 JAR
admin-ui/            前端
migrations/          数据库初始化与版本迁移
scripts/             构建、验证和部署命令
deploy/              部署配置与说明
docs/development/    当前开发规则与模板清单
docs/versions/       按版本或独立任务归集的方案、变更与验证资料
```

业务模块与 `cyys-admin` 平级，由 `cyys-application` 装配；依赖关系和新增模块方式见目录与构建说明。

## 运行配置

连接信息通过进程环境变量或外部配置提供，凭据不提交到仓库。

| 参数 | 含义 |
|---|---|
| `CYYS_DB_URL` | MySQL JDBC URL，指向明确选择的数据库 |
| `CYYS_DB_USERNAME` / `CYYS_DB_PASSWORD` | 数据库连接身份 |
| `CYYS_REDIS_HOST` / `CYYS_REDIS_PORT` | Redis 地址，默认 `127.0.0.1:6379` |
| `CYYS_REDIS_DATABASE` / `CYYS_REDIS_PASSWORD` | Redis 数据库序号（默认 0）及密码 |
| `CYYS_ENV` | 稳定的缓存环境命名空间，默认 `local` |
| `CYYS_PORT` | API 端口，默认 `8080` |
| `CYYS_API_DOCS_ENABLED` | 是否开放 API 文档，默认 `false` |

数据库使用 MySQL 8。空库由应用 Flyway 自动执行唯一 V1，包含表结构、菜单及必要基础数据；管理员、角色授权和实际总单位需受控准备，没有默认账号密码。已有数据库的接入方式见初始化说明。

当前机构模型为一个实际总单位及其下级机构，功能权限与数据范围分别校验。会话保存在单个 API 进程中，服务重启后失效，部署使用单 API 实例。

## 开发与验证

前端在 `admin-ui` 目录执行 `npm ci`、`npm run dev`，开发端口为 `8000`；`/api` 默认转发到 `http://127.0.0.1:8080`，可用 `CYYS_API_TARGET` 指定后端。类型检查和构建分别使用 `npm run typecheck`、`npm run build`。

后端构建与运行按目录与构建说明执行。构建和运行日志位于 `backend/logs`；前端产物位于 `admin-ui/dist`。

检查范围按验证与保存规则选择。数据库测试使用独立 MySQL 测试库；测试守卫拒绝未知表、业务数据和残留测试记录，不清空已有数据库。
