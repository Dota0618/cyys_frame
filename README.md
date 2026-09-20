# CYYS Framework

I often build Java applications for standalone projects, and I see many that are over-engineered. Actually, your system design doesn't need to be complex. If your project has few users, serves as an internal government or corporate system, or just needs to be delivered fast — I believe this is what you need.


基于 JDK 25 / Spring Boot 4.1.1 的框架改造工程。A 方案目录、身份基础及 Phase 3 / G1、Phase 4.1 已完成；Phase 4.2 已完成部门管理、组织树与负责人选择、单位角色、共享菜单、GLOBAL 平台角色、全局配置/字典、两类审计、受控缓存及本地私有文件/断点续传、个人目录、文件回收、后台临时清理及组织通知/首次阅读/附件引用切片。最新完整后端回归为 127 项通过（含统一出站 HTTP/TLS 和 Jackson 转换链隔离），正式迁移执行至 V14；当前 35 组真实浏览器用例分轮覆盖通过（全套首轮 31 项通过，修正测试定位与字典样本计数后，通知专项 3 项和全局配置/字典专项 10 项通过）。系统通知、指南、版本等 Phase 4.2 其余能力及 G2 仍待办，实际范围见 [改造进度](docs/migration/progress.md)。

## 开发入口

- 地区参数 V17 的管理接口和页面已开发，采用逐级分页及旧字段；迁移、真实数据与功能验收待最终集中执行。
- 旧“系统编号”经源码核对为微信 AppID/AppSecret 配置，随微信接入暂缓；独立业务流水号尚无实际消费者，保留待接入事项，不用数据库主键生成器替代。正式迁移目前执行至 V15，V16/V17 待最终集中验证。

- 密码管理源码已补齐：头像菜单个人改密、用户列表平台管理员重置；管理员无需再次输入自己的密码。复用现有 BCrypt/凭据指纹机制，功能及页面验收待最后集中执行。

- 最新补充：引导页也暂停；版本更新 V16 的接口、页面和共享附件源码已完成，迁移及功能验收待最终集中执行，不视为已通过 G2。

- 2026-09-20 最新范围：节假日不做；代码生成器、单位通知暂停。系统通知 V15 的平台管理接口和页面已实现，无阅读状态/回执；后端专项 3 项及前端类型检查/构建通过，页面操作验收留到最后。上述 127 项是上一节点的完整回归，本轮没有重新运行全量。

- 开发节点使用本地 Git 提交保存，不再另存源码快照。现有 `docs/` 忽略设置保留；未跟踪文档不包含在提交中。

- 执行本次改造：先读 [改造期规则](docs/migration/execution-rules.md) 和 [实际进度](docs/migration/progress.md)。
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
  cyys-business/     唯一运行应用，application 装配和未来的 modules 业务
migrations/          system/business 版本脚本，dev-data 为显式本地开发数据
admin-ui/            唯一前端，Umi Max / Ant Design Pro，独立构建
deploy/              部署材料
docs/                改造记录、按需开发规则
```

依赖方向保持 `business -> admin -> common`，business 也可直接使用 common。Mapper 扫描在启动装配中按 `@Mapper` 限定。部门功能包为 department，已有 SysOrg 实体和 sys_org 表保留名称。

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

数据库测试直接连接现有 MySQL，不使用 H2，也不自动创建数据库。本机测试连接配置在 `backend/cyys-business/src/test/resources/application-test.xml`；也可执行 `scripts/verify.ps1 -Clean` 统一构建并记录日志。必须选择用于验证的空 schema，或此前由本套测试初始化且已清理的 schema；启动预检发现未知表或已有业务数据会立即拒绝，且发生在 Flyway 和测试写入之前。该测试配置只用于当前本地验收，正式运行仍使用外部参数。

测试通过 HTTP 调用随机端口的实际应用、真实 Mapper 和 Flyway 脚本；每例结束只清理该组记录和缓存键，测试类结束清理测试单位。正式结构及已执行的 V1/V3/V4/V5/V6/V7/V8 历史保留。V7 全局配置与字典已通过真实后端测试及两个新页面的浏览器验收，详见 [验收记录](docs/migration/phase4-2-global-acceptance.md)。旧结构演练仅创建并清理专属测试表；测试材料不进入应用 JAR。完整旧业务库的升级需单独核对。实际测试和清理结果见 [改造进度](docs/migration/progress.md)。

出站 HTTP 统一注入 Boot `RestClient.Builder`，默认连接/读取期限为 3 秒/15 秒，可用 `CYYS_HTTP_CONNECT_TIMEOUT`、`CYYS_HTTP_READ_TIMEOUT` 调整（1 毫秒至 2 分钟）。`CYYS_HTTP_MAX_RESPONSE_BYTES` 默认 1048576，最大 16 MiB；禁止自动重定向和 Cookie 共享。正常信任库/主机名校验保持启用，私有 CA 使用外部 SSL Bundle。对应规则见 [API 与 HTTP 边界](docs/development/backend/api-command-validation.md)，不代表供应商 API 已接入。

## 前端开发与浏览器验证

固定 Node.js 24.0.1，使用唯一 `admin-ui/package-lock.json`：

```powershell
. "F:\git\frame\scripts\use-local-toolchain.ps1"
Set-Location "F:\git\frame\cyys-framework\admin-ui"
npm ci
npm run dev
```

开发页面默认在 8000 端口，`/api` 转发到 `http://127.0.0.1:8080`；可用 `CYYS_API_TARGET` 指定实际后端。前端用 `npm run typecheck`、`npm run build` 独立检查和构建，输出为 `admin-ui/dist`。后端需先注入运行参数及受控的初始身份，当前空库没有可用于日常登录的管理员。

从工程目录运行 `scripts/verify-ui.ps1`：完成类型检查、构建，再用本机 Chrome 无界面浏览器访问真实测试后端。测试服务只绑定回环地址，前端 18041 端口必须空闲；Spring 使用随机端口。`-SkipBuild` 仅适用于前端产物已与源码一致时。浏览器用例不会使用运行中的业务后端或模拟 API；日志在 `backend/logs`，截图在 `admin-ui/test-results`。验收用静态转发服务不是部署方案。

单个功能复验可传 `-Spec caches.spec.ts -LogPrefix phase4_cache`，默认不传 Spec 则运行全套；用独立日志前缀保留既有验收证据。

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

准备数据库及初始化身份前，先读 [迁移说明](migrations/README.md)。未被 Flyway 纳管的旧库会被拒绝，应用不会自动 baseline 或删表重建。

配置就绪并完成构建后：

```powershell
. "F:\git\frame\scripts\use-local-toolchain.ps1"
java -jar "F:\git\frame\cyys-framework\backend\cyys-business\target\cyys-business.jar"
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
| GET/POST/PUT/DELETE /api/admin/platform/menus | GLOBAL 平台控制的共享菜单和操作权限定义；编码、类型不可变，组件字段不动态执行 |
| GET/POST/PUT/DELETE /api/admin/platform/roles | GLOBAL 平台角色、权限树和账号平台授权；账号授权只替换 GLOBAL 关系 |
| GET/POST /api/admin/platform/configurations，GET/PUT/DELETE /{id} | 全局通用配置分页及管理；保留空值，修改/删除带版本，后端运行测试及真实浏览器验收通过 |
| GET/POST /api/admin/platform/dictionaries，GET/PUT/DELETE /{id} | 全局字典分类分页及管理；编号与内部名称保持稳定，后端运行测试及真实浏览器验收通过 |
| GET/POST /api/admin/platform/dictionaries/{basicId}/items，GET/PUT/DELETE /{id} | 分类下的字典项管理；编码唯一、版本冲突及分类删除依赖保护，后端运行测试及真实浏览器验收通过 |
| GET /api/admin/audits | 当前单位操作审计分页，独立 audit:read 权限与过滤参数校验 |
| GET /api/admin/login-audits | 当前单位登录记录分页，独立 login-audit:read 权限；未知/未确认单位记录不混入单位查询 |
| GET /api/admin/caches，POST /api/admin/caches/{action}/refresh | 登记的业务缓存动作，当前仅 user-display；按 cache:user:refresh 授权，只刷新当前单位，失败返回错误 |

平台菜单和平台角色入口允许不选择业务单位，但每次操作仍实时验证 `platform:scope:access` 及对应 GLOBAL 操作权限。停用关键权限、停用或改权平台角色、替换账号 GLOBAL 授权时使用同一数据库锁串行检查，拒绝移除最后一名可用平台管理员。

Token 仅从 `Authorization: Bearer <token>` 请求头读取，不读 Cookie 或查询/表单参数。默认有效期 2 小时、空闲超时 30 分钟，允许并发登录且各 Token 不共享。当前使用 Sa-Token 进程内会话，重启会失效；多实例会话存储尚未接入，不能直接按多实例部署验收。

single 固定到配置的真实单位主键；multi 的每次单位业务请求都携带 `X-Cyys-Scope-Id`，该值只表示选择。/me 在 multi 未传单位时返回“未选择”的身份，不从登录会话补单位；缺少单位不能进入单位业务。平台单位访问来自显式 `platform:scope:access` 授权，不依赖某个账号主键或角色名称，也不等于跨单位写入、导出或角色授予权。

业务成功与失败均使用 R 响应；预期错误同时设置对应 HTTP 状态（400/401/403/409/423 等）。健康检查保留独立健康响应。ID 为字符串；时间字段使用 ISO 本地日期时间字符串，部署实例应采用一致时区。分页返回 records/total/current/size，size 上限 200。现有成员编辑不修改全局姓名、账号和密码；已有账号加入其他单位及独立账号管理仍待实现。

旧账号密码必须在迁移时重置/转换为有效 BCrypt，旧 Token 失效，不引入第二条兼容鉴权链。密码散列或重置时间改变后，旧会话的后续请求会被拒绝。更详细的迁移和模式变更限制见迁移说明。
