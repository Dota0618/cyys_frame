# cyys-framework 项目规则

## 工作区

- 项目目录：`F:\git\frame\cyys-framework`。
- `F:\git\frame\cyys_monomer` 和 `F:\git\frame\docs` 只读。
- 工具链遵循[工作区规则](../AGENTS.md)。
- 未经确认不新增项目顶层目录。
- 不向项目根、`backend` 根或 `backend/.m2` 写临时文件。

## 目录与构建

- `backend` 根只放 `pom.xml`；`.m2` 只放 `settings.xml` 和 `repo`。
- 后端模块使用 `cyys-common`、`cyys-admin`、`cyys-business`。
- SQL 迁移放 `migrations`，脚本放 `scripts`，部署文件放 `deploy`，前端放 `admin-ui`。
- 构建和运行日志只放 `backend/logs`。
- Maven 使用 `-s F:\git\frame\cyys-framework\backend\.m2\settings.xml`。
- 保留 Maven `target` 的正常构建布局。

```powershell
mvn -s F:\git\frame\cyys-framework\backend\.m2\settings.xml clean install
mvn -s F:\git\frame\cyys-framework\backend\.m2\settings.xml -pl cyys-business spring-boot:run
```

## 开发边界

- admin 包使用 `com.cyys.admin.<功能>.<职责>`。
- 业务包使用 `com.cyys.modules.<功能>.<职责>`。
- 按需使用 controller、model、dto、service、mapper，不增加空层。
- common 只提供技术能力，不查询 admin 业务表。
- Controller、Service、DTO 和事务遵循[API 规则](docs/development/backend/api-command-validation.md)。
- 功能权限和数据范围遵循[权限规则](docs/development/backend/data-permission.md)。
- 身份、机构和账号操作遵循[身份规则](docs/development/backend/identity-context.md)。
- 缓存接入遵循[缓存规则与清单](docs/development/backend/cache-usage.md)。
- 前端开发先读[前端入口](admin-ui/AGENTS.md)。
- 其他任务按需查[规则索引](docs/development/README.md)，不预加载全部规则。
- 新增规则或改变已批准范围前先确认。

## 当前范围

- 保留一个管理体系及真实总单位，不恢复独立租户模式。
- 保留现有机构和业务数据。
- 代码生成器、引导页暂停；隐藏代码生成器入口。
- 保留单位通知；系统通知统一维护，不增加阅读回执。
- 暂不接入节假日。
- 机构选择和页面状态遵循[请求规则](docs/development/frontend/request-context.md)。
- 表单布局遵循[模板规则](docs/development/frontend/template-reuse.md)。

## 验证与保存

- 开发期按改动编译、类型检查和定向验证，不反复运行全量测试。
- 页面操作和全量回归留到集中验收。
- 新资源通过实际操作和隔离验证后再开放。
- 保留现有 MySQL 测试库数据，不为通过空库检查清库。
- 使用同一数据库测试前停止本地 Docker API。
- 未经再次明确授权，不部署或更新运行中的 Docker 服务。
- 提交前检查差异，将验证结果和剩余工作写入[项目记录](docs/migration/final-acceptance.md)。
- 使用本地 Git 提交保存，不另建源码快照。
- 未经要求不推送，已忽略文档、配置、日志和构建产物继续排除。
