# CYYS Framework
I'm L.C.I live in China.
I often build Java applications for standalone projects, and I see many that are over-engineered. Actually, your system design doesn't need to be complex. If your project has few users, serves as an internal government or corporate system, or just needs to be delivered fast — I believe this is what you need.


基于 JDK 25 / Spring Boot 4.1.1 的框架改造工程。A 方案目录、身份基础及 Phase 3 / G1、Phase 4.1 已完成；Phase 4.2 已开始部门管理和组织树选择切片。最新完整回归为 61 项后端测试及 7 组真实浏览器验证。实际范围见 [改造进度](docs/migration/progress.md)。

## 开发入口

- 执行本次改造：先读 [改造期规则](docs/migration/execution-rules.md) 和 [实际进度](docs/migration/progress.md)。
- 后续功能开发：按任务查询 [开发规则索引](docs/development/README.md)，单项指南在能力稳定并获用户批准后建立。
- 阶段来源为仓库外层的 `F:\git\frame\docs\task.md`，该文件保留为输入计划；本工程的实际完成依据在进度记录中。

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

测试通过 HTTP 调用随机端口的实际应用、真实 Mapper 和 Flyway 脚本；每例结束只清理该组记录和缓存键，测试类结束清理测试单位。正式结构及 V1/V3/V4 历史保留。旧结构演练仅创建并清理专属测试表；测试材料不进入应用 JAR。完整旧业务库的升级需单独核对。实际测试和清理结果见 [改造进度](docs/migration/progress.md)。

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

Token 仅从 `Authorization: Bearer <token>` 请求头读取，不读 Cookie 或查询/表单参数。默认有效期 2 小时、空闲超时 30 分钟，允许并发登录且各 Token 不共享。当前使用 Sa-Token 进程内会话，重启会失效；多实例会话存储尚未接入，不能直接按多实例部署验收。

single 固定到配置的真实单位主键；multi 的每次单位业务请求都携带 `X-Cyys-Scope-Id`，该值只表示选择。/me 在 multi 未传单位时返回“未选择”的身份，不从登录会话补单位；缺少单位不能进入单位业务。平台单位访问来自显式 `platform:scope:access` 授权，不依赖某个账号主键或角色名称，也不等于跨单位写入、导出或角色授予权。

业务成功与失败均使用 R 响应；预期错误同时设置对应 HTTP 状态（400/401/403/409/423 等）。健康检查保留独立健康响应。ID 为字符串；时间字段使用 ISO 本地日期时间字符串，部署实例应采用一致时区。分页返回 records/total/current/size，size 上限 200。现有成员编辑不修改全局姓名、账号和密码；已有账号加入其他单位及独立账号管理仍待实现。

旧账号密码必须在迁移时重置/转换为有效 BCrypt，旧 Token 失效，不引入第二条兼容鉴权链。密码散列或重置时间改变后，旧会话的后续请求会被拒绝。更详细的迁移和模式变更限制见迁移说明。
