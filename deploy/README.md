# 部署

本页用于 Linux、Docker Compose 和 Nginx 发布，本机更新见[开发部署](development.md)。

## 发布产物

后端使用 `backend/cyys-application/target/cyys-application.jar`，前端使用 `admin-ui/dist`，构建见[构建与运行](../docs/development/project-build.md)。Dockerfile 接收已有产物，基础镜像使用实际 digest。

## 外部配置

将[环境配置](.env.example)复制到仓库外，填写数据库、Redis、文件目录及镜像版本。

- 数据库接入见[初始化说明](../migrations/README.md)，由应用内 Flyway 执行迁移。
- 密码通过 Compose secrets 和 Spring configtree 加载，证书和凭据限制访问。
- 容器 UID/GID 为 `10001:10001`；私有目录预先建立、位于构建目录外并允许该 UID 写入。
- 私有目录只挂载 API，外部入口统一通过 Nginx HTTPS。
- 当前运行单个 API 实例，会话在进程内，重启后重新登录；`CYYS_ENV` 使用稳定命名空间。

## 发布

在 Linux 项目根目录执行，环境文件路径按实际替换。先构建，再于维护窗口停止写入并保存配套数据库、文件及配置：

```sh
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml build --pull
docker compose --env-file /etc/cyys/release.env -f deploy/compose.yml up -d --no-build --force-recreate
```

服务状态和日志分别使用同一 Compose 配置的 `ps`、`logs` 命令。证书更新后重新加载 Web。

## 恢复

数据库兼容时切回配套镜像；不兼容时同时恢复数据库、私有文件、外部配置及对应镜像。保留日志、提交号、镜像 digest 和恢复材料，恢复期间保持停写。
