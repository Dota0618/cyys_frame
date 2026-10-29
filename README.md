# CYYS Framework

I often build Java applications for standalone projects, and I see many that are over-engineered. Actually, your system design doesn't need to be complex. If your project has few users, serves as an internal government or corporate system, or just needs to be delivered fast — I believe this is what you need.


基于 JDK 25 / Spring Boot 4.1.1 的框架改造工程，采用一个可执行后端与一个 Ant Design Pro 前端。已实现身份与单位隔离、成员/部门/角色与平台授权、全局配置/字典、审计、受控缓存、本地私有文件与断点续传、文件目录与回收、系统通知、版本附件、地区、密码管理、成员/地区导出及通用流水号。

2026-09-20 验收：完整后端回归 149 项通过，另补充导出途中撤权及两个独立 JVM 取号各 1 项通过；40 项真实页面用例分轮通过，正式迁移已执行至 V18。本机 Docker 的 HTTPS/代理、A/B 样本与附件备份恢复、多→单→多模式切换通过。用户选定的基础数据已导入现有 MySQL：17 类字典、41 项、3720 个地区和 3 个普通配置；本轮框架交付已收口，不包含全部旧业务或生产切换。逐项证据与边界见 [最终验收对照](docs/migration/final-acceptance.md) 和 [改造进度](docs/migration/progress.md)。

## 开发入口

- 日常部署开发环境：运行 `./scripts/deploy-dev.ps1 web` 更新前端，`api` 更新后端，见[使用说明](deploy/development.md)。
- 首次准备环境和构建：见[目录与构建](docs/development/project-build.md)。
- 新库仅由应用自动执行 [V1 初始化文件](migrations/system/V1__init_system_schema.sql)，已包含当前结构、菜单及必要基础数据；执行方式和旧库同步边界见[数据库初始化说明](migrations/README.md)。
- 地区使用平台共享的逐级分页与旧字段。成员/地区同步 XLSX 导出各需独立权限，最多 5000 条、服务端固定字段；成员导出使用当前单位和操作对应的行范围，生成后复核授权及可见性。全部单位只读视图不导出。
- 通用流水号按用户要求已实现，使用数据库事务和唯一约束保证单位/业务/周期计数；服务器配置规则，业务 Service 调用，无任意取号 HTTP 接口。旧“系统编号”实际是微信 AppID/AppSecret 配置，随微信接入暂缓。
- 个人改密需验证原密码；平台管理员重置只输入目标账号的新密码与确认，不再输入管理员自身密码。改密/重置后旧会话失效。
- 系统通知、版本由平台维护、单位共享；系统通知没有阅读状态/回执。单位通知已恢复开发，沿用当前单位内的通知管理、附件和首次阅读记录，页面 `/system/org-notices`。节假日不做，引导页继续暂停。
- 代码生成器已于 2026-09-21 恢复：后台 `/system/generator` 支持读取当前库 `biz_` 业务表或设计新表、配置字段、预览与下载 ZIP；普通列表/树形列表，文本、多行文本、整数、小数、布尔、日期、日期时间和字典字段。生成独立 DTO、业务包下的后端、前端、待编号建表脚本及接入说明，配置 JSON 可再次导入。文件、富文本、流程和业务约束需具体开发，不从字段名猜测。已有表须完整匹配配置及框架归属/审计/版本字段。
- 生成器沿用平台权限：`platform:scope:access` + `generator:read`（元数据/预览），下载另需 `generator:generate` 并记录审计。不创建默认账号，不自动授权，不写入工程或执行 SQL；生成后的业务代码按包内 `INTEGRATION.md` 合入并做实际业务与单位隔离验收。生成配置随下载包保存，无额外配置表或数据库迁移。
- [部署与回滚顺序](deploy/README.md) 提供 Linux + Docker + Nginx 配置、本机恢复脚本与集中验收入口；当前会话为进程内存储，只部署一个 API 实例。两 JVM 流水号验收不代表分布式登录会话已接入。

- 开发节点使用本地 Git 提交保存，不再另存源码快照。开发规则和必要模板清单随仓库保存，其余本机记录继续忽略。

- 查阅本轮交付：见 [最终验收对照](docs/migration/final-acceptance.md) 和 [实际进度](docs/migration/progress.md)；临时改造规则已结束，后续按现有开发指南执行。
- 后续功能开发：按任务查询 [开发规则索引](docs/development/README.md)，单项指南在能力稳定并获用户批准后建立。
- 阶段来源为仓库外层的 `F:\git\frame\docs\task.md`，该文件保留为输入计划；本工程的实际完成依据在进度记录中。
- 旧工具、视图、表和接口去向见 [输入盘点](docs/migration/source-inventory.md)。存储按 [三模式配置切换规则](docs/development/backend/storage.md) 推进，目前不接真实云 API；微信、支付、短信和视频暂不接入。
- 本地存储部署示例：[storage-local.yml.example](deploy/storage-local.yml.example)。文件接口默认不自行创建根目录；部署显式配置后启用。独立请求/响应类型单独成文件，见 [API 命令规则](docs/development/backend/api-command-validation.md)。

## 结构

```text
backend/
  pom.xml
  cyys-common/       普通 JAR，web/satoken/mybatis 等技术能力
  cyys-admin/        普通 JAR，功能内按 controller/model/dto/service/mapper 放置
  cyys-application/  唯一运行应用，负责启动、配置和模块装配
migrations/          system/V1 单文件初始化，后续 system/business 共用版本序列
admin-ui/            唯一前端，Umi Max / Ant Design Pro，独立构建
deploy/              部署材料
docs/                改造记录、按需开发规则
```

依赖方向保持 `application -> admin -> common`，业务模块与 admin 平级并由 application 装配。Mapper 扫描在启动装配中按 `@Mapper` 限定。部门功能包为 department，已有 SysOrg 实体和 sys_org 表保留名称。

## 构建与验证

先激活固定本地工具链；Maven 统一使用本工程 settings，不传 `-Dmaven.repo.local`。Windows 构建日志统一写入 backend/logs。

```powershell
. "F:\git\frame\scripts\use-local-toolchain.ps1"
Start-Process -FilePath "$env:MAVEN_HOME\bin\mvn.cmd" -ArgumentList @(
  "-B", "-s", "F:\git\frame\cyys-framework\backend\.m2\settings.xml", "clean", "verify"
) -WorkingDirectory "F:\git\frame\cyys-framework\backend" -WindowStyle Hidden -Wait -PassThru `
  -RedirectStandardOutput "F:\git\frame\cyys-framework\backend\logs\verify-out.log" `
  -RedirectStandardError "F:\git\frame\cyys-framework\backend\logs\verify-err.log"
```

数据库测试直接连接明确指定的独立 MySQL，不使用 H2，也不自动创建数据库。本机测试连接配置在 `backend/cyys-application/src/test/resources/application-test.xml`；也可执行 `scripts/verify.ps1 -Clean` 统一构建并记录日志。必须选择用于验证的空 schema，或此前由本套测试初始化且已清理的 schema；启动预检允许 V1 的原始内置数据，发现未知表或已有业务数据会立即拒绝，且发生在 Flyway 和测试写入之前。该测试配置只用于本地验收，正式运行仍使用外部参数。

测试通过 HTTP 调用随机端口的实际应用、真实 Mapper 和 Flyway 脚本；每例结束只清理该组记录和缓存键，测试类结束清理测试单位。新库只执行合并后的 V1，旧迁移链及其专属样本测试已移除；测试材料不进入应用 JAR。运行同库测试前先停止本机演练 API，避免并发写库。当前业务库保留已有基础数据，空库集成/浏览器测试会拒绝，不能清空这些数据来重跑。历史测试与迁移结果见 [改造进度](docs/migration/progress.md)。

出站 HTTP 统一注入 Boot `RestClient.Builder`，默认连接/读取期限为 3 秒/15 秒，可用 `CYYS_HTTP_CONNECT_TIMEOUT`、`CYYS_HTTP_READ_TIMEOUT` 调整（1 毫秒至 2 分钟）。`CYYS_HTTP_MAX_RESPONSE_BYTES` 默认 1048576，最大 16 MiB；禁止自动重定向和 Cookie 共享。正常信任库/主机名校验保持启用，私有 CA 使用外部 SSL Bundle。对应规则见 [API 与 HTTP 边界](docs/development/backend/api-command-validation.md)，不代表供应商 API 已接入。

## 前端开发与浏览器验证

固定 Node.js 24.0.1，使用唯一 `admin-ui/package-lock.json`：

```powershell
. "F:\git\frame\scripts\use-local-toolchain.ps1"
Set-Location "F:\git\frame\cyys-framework\admin-ui"
npm ci
npm run dev
```

开发页面默认在 8000 端口，`/api` 转发到 `http://127.0.0.1:8080`；可用 `CYYS_API_TARGET` 指定实际后端。前端用 `npm run typecheck`、`npm run build` 独立检查和构建，输出为 `admin-ui/dist`。后端需先注入运行参数及受控的初始身份，当前测试库保留迁入基础数据，没有可用于日常登录的管理员。

从工程目录运行 `scripts/verify-ui.ps1`：完成类型检查、构建，再用本机 Chrome 无界面浏览器访问真实测试后端。测试服务只绑定回环地址，前端 18041 端口必须空闲；Spring 使用随机端口。`-SkipBuild` 仅适用于前端产物已与源码一致时。浏览器用例不会使用运行中的业务后端或模拟 API；日志在 `backend/logs`，截图在 `admin-ui/test-results`。验收用静态转发服务不是部署方案。

专项复验可传 `-Spec caches.spec.ts -LogPrefix phase4_cache`，或多个文件 `-Spec passwords.spec.ts,releases.spec.ts`；不传 Spec 则运行全套。仅在实际失败或新增改动需要时复验，用独立日志前缀保留证据。

## 运行参数

应用不再内置真实连接密码，不默认激活 dev profile。通过进程环境或外部配置提供以下参数：

| 参数 | 含义 |
|---|---|
| CYYS_DB_URL | MySQL JDBC URL，指向明确选择的数据库 |
| CYYS_DB_USERNAME / CYYS_DB_PASSWORD | 该数据库的连接身份 |
| CYYS_ISOLATION_MODE | single 或 multi，默认 single |
| CYYS_SINGLE_SCOPE_ID | single 时必填，值为已存在且启用的 sys_scope.id，不是 code |
| CYYS_PORT | 端口，默认 8080 |
| CYYS_API_DOCS_ENABLED | 是否开放 API 文档，默认 false |
| CYYS_ENV | 缓存环境命名空间，默认 local |
| CYYS_REDIS_HOST / CYYS_REDIS_PORT | Redis 地址，默认 127.0.0.1:6379 |
| CYYS_REDIS_DATABASE / CYYS_REDIS_PASSWORD | Redis 数据库序号（默认 0）及连接密码 |

准备数据库及初始身份前，先读[数据库初始化说明](migrations/README.md)。只保留新版 V1，旧迁移链全部删除；空库启动应用即可完成初始化，无需另行导入菜单或开发 SQL，没有默认账号和密码。已有库同步前须完成基线切换或增量升级；应用不会自动 baseline 或删表重建。

配置就绪并完成构建后：

```powershell
. "F:\git\frame\scripts\use-local-toolchain.ps1"
java -jar "F:\git\frame\cyys-framework\backend\cyys-application\target\cyys-application.jar"
```

当前公开接口：

| 接口 | 行为 |
|---|---|
| GET /api/health | 健康状态，不要求登录 |
| POST /api/auth/login | loginName/password，可附 scopeId；成功返回 Token 和身份快照 |
| GET /api/auth/me | 返回当前账号、有效单位及当前选择下的权限 |
| POST /api/auth/logout | 撤销当前 Token；成员关系失效后仍可退出 |
| GET /api/admin/users | 当前单位成员分页，page/size/name；创建时间倒序 |
| GET /api/admin/users/all | 平台授权的跨单位只读分页；single 模式仍受固定单位限制 |
| GET /api/admin/users/{id} | 当前单位成员详情，id 是成员关系主键 |
| POST /api/admin/users | 同事务创建全局账号和当前单位成员 |
| PUT /api/admin/users/{id} | 按 version 修改本单位姓名、部门和成员状态 |
| PUT /api/admin/users/{id}/roles | 替换本单位角色，保留其他单位和平台授予 |
| GET /api/admin/users/departments、/roles | 受权限约束的本单位选项 |
| GET/POST/PUT/DELETE /api/admin/departments | 当前单位部门分页、详情及受版本保护的写入；父部门与负责人候选按新增/修改操作约束，保存时重新校验 |
| GET/POST/PUT/DELETE /api/admin/roles | 当前单位角色、菜单权限及数据范围管理；写入受授予上限约束 |
| GET/POST /api/admin/platform/scopes，PUT /{id} | 单位查询、新增、修改和启停；独立平台权限、版本冲突保护，不删除历史单位归属 |
| GET/POST/PUT/DELETE /api/admin/platform/menus | GLOBAL 平台控制的共享菜单和操作权限定义；编码、类型不可变，组件字段不动态执行 |
| GET/POST/PUT/DELETE /api/admin/platform/roles | GLOBAL 平台角色、权限树和账号平台授权；账号授权只替换 GLOBAL 关系 |
| GET/POST /api/admin/platform/configurations，GET/PUT/DELETE /{id} | 全局通用配置分页及管理；保留空值，修改/删除带版本，后端运行测试及真实浏览器验收通过 |
| GET/POST /api/admin/platform/dictionaries，GET/PUT/DELETE /{id} | 全局字典分类分页及管理；编号与内部名称保持稳定，后端运行测试及真实浏览器验收通过 |
| GET/POST /api/admin/platform/dictionaries/{basicId}/items，GET/PUT/DELETE /{id} | 分类下的字典项管理；编码唯一、版本冲突及分类删除依赖保护，后端运行测试及真实浏览器验收通过 |
| GET /api/admin/audits | 当前单位操作审计分页，独立 audit:read 权限与过滤参数校验 |
| GET /api/admin/login-audits | 当前单位登录记录分页，独立 login-audit:read 权限；未知/未确认单位记录不混入单位查询 |
| GET/POST/PUT/DELETE /api/admin/caches | 缓存定义的查询、新增、修改和软删除；按 cache:user:refresh 授权 |
| POST /api/admin/caches/refresh-groups/refresh | 按刷新组和作用范围整体刷新；不提供单缓存刷新 |

平台菜单和平台角色入口允许不选择业务单位，但每次操作仍实时验证 `platform:scope:access` 及对应 GLOBAL 操作权限。停用关键权限、停用或改权平台角色、替换账号 GLOBAL 授权时使用同一数据库锁串行检查，拒绝移除最后一名可用平台管理员。

后台导航按“目录 → 菜单”显示中文树；菜单维护采用左侧目录/菜单树、右侧当前层级列表，菜单下的按钮权限在右侧维护。角色使用“目录 → 菜单 → 按钮权限”的复选框树，按钮可以单独取消，保存后不会因父菜单被选中而重新勾上。菜单图标使用现有 Select 和官方图标库，支持中文/名称搜索、预览、清空；导航只解析本地登记的图标，不执行下发代码。侧栏只展示已授予且可见的菜单，并匹配本地登记的路由及操作权限。初次目录数据已包含在唯一 V1 中，后续通过菜单和角色管理维护。

单位和用户入口位于“组织管理”。未选择单位时，用户管理提示选择或创建单位；有跨单位只读权限时可查看全部单位成员。地区管理使用左侧按需加载的地区树与右侧当前层级列表，新增、编辑和删除后刷新树与列表。

数据字典采用左侧分页/搜索分类、右侧字典项管理。菜单、字典及字典项表单使用双列表单，平台角色为左侧基本信息、右侧权限树；编辑弹窗限制在当前屏幕内，内容区滚动，标题和提交按钮保持可见。

Token 仅从 `Authorization: Bearer <token>` 请求头读取，不读 Cookie 或查询/表单参数。默认有效期 2 小时、空闲超时 30 分钟，允许并发登录且各 Token 不共享。当前使用 Sa-Token 进程内会话，重启会失效；多实例会话存储尚未接入，不能直接按多实例部署验收。

single 固定到配置的真实单位主键；multi 的每次单位业务请求都携带 `X-Cyys-Scope-Id`，该值只表示选择。/me 在 multi 未传单位时返回“未选择”的身份，不从登录会话补单位；缺少单位不能进入单位业务。平台单位访问来自显式 `platform:scope:access` 授权，不依赖某个账号主键或角色名称，也不等于跨单位写入、导出或角色授予权。

业务成功与失败均使用 R 响应；预期错误同时设置对应 HTTP 状态（400/401/403/409/423 等）。健康检查保留独立健康响应。ID 为字符串；时间字段使用 ISO 本地日期时间字符串，部署实例应采用一致时区。分页返回 records/total/current/size，size 上限 200。现有成员编辑不修改全局姓名、账号和密码；已有账号加入其他单位及独立账号管理仍待实现。

旧账号密码必须在迁移时重置/转换为有效 BCrypt，旧 Token 失效，不引入第二条兼容鉴权链。密码散列或重置时间改变后，旧会话的后续请求会被拒绝。更详细的迁移和模式变更限制见迁移说明。

选择性旧基础数据迁移与复核入口： [迁移记录](docs/migration/legacy-basic-data.md)。
