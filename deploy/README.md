# 部署

日常更新本机开发环境使用[开发部署命令](development.md)。本页用于 Linux、Docker Compose 和 Nginx 的发布与恢复。

## 发布产物

- 后端使用 `backend/cyys-application/target/cyys-application.jar`，前端使用 `admin-ui/dist`。
- 构建方式见[目录与构建](../docs/development/project-build.md)，检查范围见[验证与保存](../docs/development/verification.md)。产物结构使用 `scripts/check-release-artifacts.ps1` 核对。
- Dockerfile 使用已有构建产物，不重新编译源码。构建上下文仅包含对应产物，不包含本机配置、日志或凭据。
- JDK 25 和 Nginx 基础镜像须指定实际 digest；环境示例中的占位值必须替换。

## 外部配置

将 [.env.example](.env.example) 复制到仓库之外，例如 `/etc/cyys/release.env`，填写数据库地址、文件根目录和镜像版本。MySQL 和 Redis 需提前准备。

- 数据库初始化和已有库接入遵循[初始化说明](../migrations/README.md)。Flyway 是唯一 SQL 执行入口，不另设初始化容器。
- 数据库与 Redis 密码使用独立文件；TLS 使用实际域名的完整证书链和私钥。Compose secrets 挂载这些文件，Spring 通过 `configtree:/run/secrets/` 加载密码。
- 容器运行 UID/GID 为 `10001:10001`。密码和私钥文件须由该 UID 可读，并限制无关用户访问；Compose 文件型 secrets 的权限由宿主机管理。
- `CYYS_FILE_ROOT` 必须是提前创建的绝对、非链接私有目录，并允许 UID 10001 写入。目录只挂载给 API，Nginx 无权读取。
- `CYYS_ENV` 使用稳定环境命名空间。会话保存在 API 进程中，只部署一个 API 实例；服务重启后需要重新登录。

API 不发布宿主机端口，仅允许 Nginx 从容器网络访问。Nginx 的 80 端口跳转 HTTPS，443 提供静态页面和 API；客户端身份头由 Nginx 覆盖。证书更新后重新加载或重建 Web 容器。

## 发布

以下命令在 Linux 项目根目录执行。先核对构建产物及其摘要，准备实际外部环境文件：

```sh
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml config --quiet
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml build --pull
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml run --rm --no-deps web -t
```

进入维护窗口，停止写入与任务，停止当前 Web/API。在同一停写点备份数据库、私有文件、外部配置和镜像标识，并在独立环境验证可恢复，再启动服务：

```sh
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml up -d --no-build --force-recreate
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml ps
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml logs --tail=100 api web
```

`depends_on` 只控制启动顺序。`/api/health` 用于检查应用可响应；开放访问前还需核对数据库版本、登录、权限与机构数据范围、附件读取、上传下载和导出。

静态业务路径刷新应返回 `index.html`，缺失 JS/CSS 返回 404；`/api/` 始终转发后端，错误保留原状态。核对 HTTPS、404/401/403/413/502 和 API 容器重建后的代理解析。文件下载由后端授权，不公开映射私有目录。

## 恢复

发生迁移或权限异常时保持停写，保存日志和失败位置。数据库兼容时可切回配套 API/Web 镜像；不兼容时须同时恢复数据库、文件元数据与对象、外部配置和对应镜像，再启动服务。不要使用 Flyway clean 或删除卷进行恢复。

保留部署所用提交号、镜像 digest、检查结果及备份位置，用于定位和恢复。备份包含敏感信息，应限制访问权限。
