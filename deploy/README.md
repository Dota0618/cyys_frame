# Linux / Docker / Nginx 部署准备

用户于 2026-09-20 确认该部署方式。本目录提供部署输入与操作顺序；当前没有连接生产主机、构建容器、创建数据库或执行上线。正式使用前必须完成最终验收、备份恢复演练和维护窗口确认。

## 构建和验收

在已配置固定工具链、现有受管 MySQL 测试 schema、Redis、本机 Chrome 的 Windows 构建机运行：

```powershell
./scripts/verify-release.ps1 -InstallDependencies
```

这是最终验收入口，当前开发阶段不自动执行。它要求已提交且干净的源码，按顺序运行后端完整 verify、前端类型检查/Ant lint/构建、真实后端浏览器验收、产物结构检查，生成 `backend/logs/<prefix>_release.json`。失败停止后续阶段，并记录失败阶段；浏览器和后端数据库套件不并行。多个该入口使用文件句柄互斥，直接运行其他验证脚本时仍须避免同库并发。

报告保存提交号、步骤结果、JAR/前端文件摘要。结构检查覆盖唯一可执行 JAR、普通 common/admin 库、正式迁移内容匹配、排除部分明确的测试/旧运行库和凭据文件；它不是完整秘密扫描或 T01–T40 覆盖证明。此入口可供配置好环境的 CI runner 调用，目前尚未绑定外部 CI 服务。

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
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml run --rm --no-deps web nginx -t
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

出现迁移或边界异常时先保持停写和外部访问关闭，保存日志及迁移失败点。只在确认数据库前向兼容时才允许单独切回已验证的上一组 api/web 镜像；否则在停写窗口按演练恢复配套数据库、文件元数据/对象及外部配置，再启动匹配版本。不得只回滚 JAR，让旧无单位隔离代码读取新多单位库；不得执行 Flyway clean 或删除卷试图回退。

发布记录必须包含新旧源码提交与镜像 digest、验收报告、迁移前后版本/行数核对、备份位置和恢复证据、文件 profile、单位模式、密码/会话/缓存转换方案、开始结束时间和实际 smoke 结果。框架派生项目按提交/changelog 选择并验证补丁，不会因复制目录自动获得后续修复。

技术依据：[Docker Compose secrets](https://docs.docker.com/compose/how-tos/use-secrets/)、[Spring 外部配置与 configtree](https://docs.spring.io/spring-boot/reference/features/external-config.html)、[Nginx 路由与 try_files](https://nginx.org/en/docs/http/ngx_http_core_module.html#try_files)。
