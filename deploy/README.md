# Linux / Docker / Nginx 部署准备

用户于 2026-09-20 确认该部署方式，并指定本机 Docker 演练。已在 Docker Desktop Linux 引擎构建并启动 api/web、连接现有 MySQL/Redis，通过 HTTPS/静态路由/未登录拒绝/413 JSON 和 API 停机重建检查；仅绑定本机地址，没有连接生产主机、新建数据库或执行上线。专属 A/B 单位样本与附件的停写备份恢复、多/单单位来回切换已通过；这不等于真实旧库生产切换。用户本轮选择的基础数据已导入现有测试库并核对，见 [迁移记录](../docs/migration/legacy-basic-data.md)；生产切换另需真实环境和维护窗口。

2026-09-21：代码生成器和单位通知页面更新已部署到同一本机 Compose 项目 `cyys-local-acceptance`，API/web 镜像分别为 `cyys-framework-api:generator-20260921`、`cyys-framework-web:generator-20260921`。生成器入口 `https://localhost/system/generator`。沿用登录/平台权限，不创建账号、不绕过登录；本轮无新迁移，启动校验现有 17 个脚本、版本 V18。旧 `local-acceptance` 镜像保留，忽略目录 `backend/cyys-business/target/docker-local/` 内保留旧环境文件 `.env.before-generator-20260921`；当前 `.env` 指向新镜像。

本次验证为 6 项无数据库写入的生成器专项、普通/树形生成源码实际 Java 编译和 TypeScript 检查、后台类型检查/构建、改动页面与服务 Ant lint、产物结构及本机部署 HTTP 检查；未重跑整套数据库或页面套件，也未创建登录身份做已登录页面验收。生成业务包未合入运行应用，须按包内接入说明完成具体业务操作和单位隔离验证。证据在 `backend/logs/generator_*`；基础数据逐字段核对和其余 22 张表摘要不变见 `legacy-basic-verify-20260921-084946.log`。

## 构建和验收

在已配置固定工具链、现有受管 MySQL 测试 schema、Redis、本机 Chrome 的 Windows 构建机运行：

```powershell
./scripts/verify-release.ps1 -InstallDependencies
```

这是最终验收入口。它要求已提交且干净的源码，按顺序运行后端完整 verify、前端类型检查/Ant lint/构建、真实后端浏览器验收、产物结构检查，生成 `backend/logs/<prefix>_release.json`。失败停止后续阶段，并记录失败阶段；浏览器和后端数据库套件不并行。多个该入口使用文件句柄互斥，直接运行其他验证脚本时仍须避免同库并发；连接同一测试库的容器也应先停止。已有分阶段验收记录保留，不因聚合脚本尚未执行而重复全部检查。

报告保存提交号、步骤结果、JAR/前端文件摘要。结构检查覆盖唯一可执行 JAR、普通 common/admin 库、正式迁移内容匹配、排除部分明确的测试/旧运行库和凭据文件；它不是完整秘密扫描或 T01–T40 覆盖证明。此入口可供配置好环境的 CI runner 调用，目前尚未绑定外部 CI 服务。

Ant 检查仍扫描全部 `src`。保留但未启用的官方示例页当前有 35 条告警（30 条弃用、5 条用法），明确登记在 `scripts/antd-template-baseline.json`；这不代表全源码零告警。检查器仅接受这些文件内容未变且位置、规则、消息完全匹配的 warning，拒绝新增告警、不完整扫描和文件变化；实际路由变化也要求重新核对模板是否启用。不要自动重建基线来消除失败；启用参考页时应解决对应告警。

验收通过后，只使用该报告对应的 `cyys-business.jar` 和 `admin-ui/dist`。镜像构建不会重新编译源码；两个 Dockerfile 的专用忽略文件仅允许相应产物进入构建上下文，不发送 `.git`、本地配置、日志或源码快照。核对本轮所选 JDK 25 和 Nginx 基础镜像的真实 digest，写入外部环境文件；示例中的 digest 是必须替换的占位值，不能直接构建。API 镜像在构建时检查 Java 主版本为 25。

## 外部输入

将 `.env.example` 复制到仓库之外，例如 `/etc/cyys/release.env`，权限限部署账号，填写已确认的数据库地址、单位模式、文件根目录和镜像版本。不会自动创建 MySQL 或 Redis。数据库需要支持当前正式迁移；Flyway 仍是应用启动时唯一的迁移执行者，禁止另设一套手工重复跑 SQL 的初始化容器。

- 数据库/Redis 密码是独立文件；TLS 提供实际域名的完整证书链和私钥。文件通过 Compose secrets 挂载，Spring 使用 `configtree:/run/secrets/` 加载密码。密码不写进环境文件、镜像参数或前端。
- 普通 Compose 对文件型 secrets 使用宿主机 bind mount，不依赖 `uid/gid/mode` 字段改变宿主机权限。容器运行 UID/GID 均为 `10001:10001`；密码/私钥文件须由该 UID 可读且对无关用户不可读（例如相应所有者及 `0400`），目录和证书按实际主机权限安排。不要为了启动将密钥改成全局可读。
- `CYYS_FILE_ROOT` 必须是事先建立的绝对、非链接私有目录，UID 10001 可写；Compose 拒绝自动创建缺失目录。该目录只挂载给 api，Nginx 容器不能看到它。迁移旧附件必须先核对历史 profile 名及存储对象，不能将旧 profile 悄悄指向新目录。
- `single` 必须填写真实 `CYYS_SINGLE_SCOPE_ID`；切换 `multi` 不改写任何历史归属。`CYYS_ENV` 是稳定环境命名空间，不随每次发布变化；改变它需要独立缓存切换记录。
- 当前 Sa-Token 会话保存在进程内，因此只运行一个 api 实例；重启后旧会话失效。未完成分布式会话验收前不能扩容多个 api。Redis 仍只承担已接入的展示缓存。

Compose 的 api 不发布宿主机端口，只允许 Nginx 从容器网络访问。不要给未信任容器加入同一网络或另行发布 api 端口；外部身份头由 Nginx 覆盖，运行配置才开启代理头解析。Nginx 的 80 端口跳转 HTTPS，443 提供静态页面和 API；证书更新后需要重新加载或重建 web 容器。

## 维护窗口内发布

以下命令在 Linux 的项目目录执行，依赖已经复制且摘要一致的验收产物、实际外部环境文件及 Docker Compose。它们不是本次开发中已运行的记录。

```sh
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml config --quiet
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml build --pull
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml run --rm --no-deps web -t
```

先停止旧系统写入及任务，停止当前 web/api，完成同一停写点的数据库、私有文件、外部配置和当前镜像版本备份，并在独立环境证明可以恢复。不得把旧服务接到已经改造的共享数据库继续写入。确认备份和演练记录后：

```sh
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml up -d --no-build --force-recreate
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml ps
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml logs --tail=100 api web
```

启动会执行未执行的正式 Flyway 迁移。`depends_on` 仅控制启动顺序，不证明应用或数据库就绪；`/api/health` 只证明应用可响应，不能代替登录、权限、数据库及文件检查。开放访问前核对迁移历史、普通成员/单位管理员/平台管理员、单/多单位限制、旧附件读取和当前导出；保存实际结果。

静态路由对业务路径刷新回到 `index.html`，缺失 JS/CSS 返回 404；`/api/` 始终走后端，不回退成前端 HTML。Nginx 自身的代理故障和请求过大返回 JSON；后端错误保留原状态和响应。文件仍由后端授权传输，本版本不启用公开 alias 或 X-Accel-Redirect。需要实测 HTTPS、深层路径刷新、404/401/403/413/502、分片与下载、API 重建后的 DNS 解析；未完成这些检查前不得标记部署验收通过。

## 回滚与升级记录

本机可复现演练入口为 `scripts/rehearse-local-deployment.ps1`。它只接受已准备的 loopback Docker 配置及现有本机 `cyys_frame` 空测试库；`LocalDeploymentFixture` 位于测试源码，拒绝未知表或非本例数据，不进入发布 JAR。过程为专属 A/B 样本、真实 HTTPS 上传/下载、多→单→多及会话失效、停止 API、MySQL 全备与数据备份、配套文件/配置/镜像标识备份、定向清理本例记录和对象、恢复数据与附件、逐表摘要/文件 SHA-256 对比、重启后授权访问。成功后定向清理样本并保持 API 停止，避免与后续本地测试同库并发。

2026-09-20 演练 `local-recovery-20260920` 通过，26 张表（含迁移历史）的值摘要一致，附件恢复前后及实际下载字节一致。此演练恢复到未变化的 V18 结构，不删除/重建现有数据库；完整 SQL 备份保留但没有在另一台空服务器导入。配套输入保存在忽略的 `backend/cyys-business/target/docker-local/<run>`，含敏感配置，只作本机受控恢复材料，不能提交或当作发布包；不要在保留演练证据前执行 clean。失败时保留现场，不能删除其他数据来满足空库检查。

出现迁移或边界异常时先保持停写和外部访问关闭，保存日志及迁移失败点。只在确认数据库前向兼容时才允许单独切回已验证的上一组 api/web 镜像；否则在停写窗口按演练恢复配套数据库、文件元数据/对象及外部配置，再启动匹配版本。不得只回滚 JAR，让旧无单位隔离代码读取新多单位库；不得执行 Flyway clean 或删除卷试图回退。

发布记录必须包含新旧源码提交与镜像 digest、验收报告、迁移前后版本/行数核对、备份位置和恢复证据、文件 profile、单位模式、密码/会话/缓存转换方案、开始结束时间和实际 smoke 结果。框架派生项目按提交/changelog 选择并验证补丁，不会因复制目录自动获得后续修复。

技术依据：[Docker Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/)、[Spring 外部配置与 configtree](https://docs.spring.io/spring-boot/reference/features/external-config.html)、[Nginx 路由与 try_files](https://nginx.org/en/docs/http/ngx_http_core_module.html#try_files)。

2026-09-20 17:02：本机 API 在基础数据导入后重新启动，健康 200、Flyway 校验 V18。现在测试库有保留数据，以上要求空库的全量测试及样本恢复脚本会拒绝；不要为执行它们删除导入数据。普通日常登录需要另行受控初始化身份，交付不含默认管理员。

2026-09-21：本机 `cyys-local-acceptance` 已更新为 `cyys-framework-api:menu-tree-20260921` / `cyys-framework-web:menu-tree-20260921-2`，地址 `https://localhost/`。使用用户另行授权建立的本机账号，镜像和源码不内置账号密码。正式迁移推进到 V19；中文菜单目录通过 `migrations/dev-data/menu-tree.sql` 单事务初始化，已有权限主键及用户授权保留。单位表没有预置假单位，首次进入“组织管理 → 单位管理”按实际需要创建。升级前数据库备份、上一版 `.env` 和旧镜像保留在本机受控位置；本次未新增源码快照。Nginx 对外禁止 actuator，应用启动与真实登录/API 请求用于本次可用性核对。

2026-09-21 后续界面调整：API/Web 当前均为 `split-layout-20260921` 标签，增加导航图标返回、菜单和字典左右管理布局及屏幕内紧凑编辑。没有新迁移，数据库仍为 V19。原 `.env` 保存为本机忽略文件 `.env.before-split-20260921`，原镜像保留；后端打包、前端类型/生产构建、Ant 基线和本机定向布局检查通过。布局检查覆盖 1366×768 和 1280×600，保存了系统管理目录的官方设置图标；未更改字典内容或账号授权。

2026-09-21 字典保护及文件管理调整：API/Web 更新为 `dictionary-files-20260921`，Flyway 已执行 V20。13 类固定编码字典只读，4 类业务字典可维护；原有 17 类/41 项的内容逐项核对不变。文件菜单入口按权限展示，未选具体单位时只显示单位选择提示、不调用文件接口；选定单位后使用左侧目录树、右侧文件管理布局，上传和续传能力沿用。master 的原 12 项文件权限完整保留，本次未授予新权限。升级前停 API 并备份数据库，备份位于忽略的 `target/docker-local/before-dictionary-files-20260921.sql`，原配置为 `.env.before-dictionary-files-20260921`；旧镜像保留但旧 API 不识别系统字典保护，不能直接单独回退旧 API。验证记录见 `backend/logs/dictionary-files-verification.json`。接口核对用的业务字典及字典项已按正常 API 软删除，保留审计；未改动用户现有单位、目录或文件。

2026-09-21 人员数据范围及紧凑表单：按用户确认，角色仅授予菜单和操作，每个单位成员统一配置本人/本部门/本部门及下级/自定义部门/本单位全部，不按文件、通知等业务分别配置。V21 删除旧角色范围及关系，不兼容或转换旧范围；成员默认本人，独立配置权限为 user:data-permission。平台授权管理复用 UnitAdministrationService，master 按实际平台权限可管理单位角色及人员范围，没有用户名绕过。新增/修改用户使用左侧基本信息、右侧范围和自定义部门的紧凑表单，复用 CompactFields、compactModalProps 与 DataPermissionFields；数据权限独立入口保留。可选 dataPermission 与基本资料在同一事务保存，独立授权、版本、单位和部门校验失败全部回滚；普通资料修改保留既有范围。自定义部门只包含明确勾选项，不自动扩展父子。

本机 API/Web 当前标签 member-form-20260921，https://localhost/，Flyway V21。升级前备份在忽略的 backend/cyys-business/target/docker-local/before-member-data-20260921.sql，历史配置及镜像保留；旧 API 依赖已删除的角色范围列，不能单独回退。后端打包和 8 项权限单元测试、前端类型及生产构建通过，用户页面 Ant lint 无告警。集中验证五类范围在成员/通知查询一致，拒绝越权、跨单位部门、旧版本和旧角色范围字段；角色修改不改变成员范围。补充验证新增/修改原子保存、失败回滚、无独立授权拒绝、资料单独修改保留范围、自定义部门回显，1280×600 两列表单及提交按钮在屏幕内。用户已反馈当前可以修改。临时单位、账号、角色及所属样本已停 API 后按唯一清单定向清理，用户原数据和审计保留，API 已重新启动。证据 backend/logs/member-data-api-verification.json、member-data-ui-verification.json、member-form-verification.json 及对应构建日志。未运行需要空库的全量回归，不删除现有基础数据来满足测试守卫。代码仅作本地 Git 提交，不推送、不建立源码快照。
