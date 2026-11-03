# 开发环境部署

在项目根目录的 PowerShell 7.4+ 中执行：

```powershell
./scripts/deploy-dev.ps1 web
```

目标必须选择 `web/api/all`。前端更新保留 API，后端更新由脚本备份数据库和附件。成功后访问 https://localhost/，API 重启后重新登录。

默认复用现有 `cyys-local-acceptance` 项目，优先读取 `deploy/.env.dev`，缺少时沿用 `backend/cyys-business/target/docker-local/.env`。后端启动模块为 `cyys-application`，旧 business 目录仅保留本机材料。

## 首次使用

1. 准备 Java、Node、Maven、Git、Docker、MySQL 客户端及 PowerShell 7.4+。
2. 按[构建配置](../docs/development/project-build.md#环境准备)准备 Maven。
3. 将[环境配置](.env.dev.example)复制为 `deploy/.env.dev`，填写已有数据库、Redis、证书及文件目录。
4. 默认工具脚本为工作区 `scripts/use-local-toolchain.ps1`，其他位置使用 `-ToolchainScript`；MySQL 客户端可用 `-MySqlDump` 指定。

已有配置直接复用；目录需允许容器 UID 10001 访问，脚本不创建外部数据库及账号。

## 排查与恢复

`-Mode Check` 仅查看配置，`-Mode Build` 仅构建。日志放 `backend/logs/dev-*/`，备份放 `deploy/.local/dev-*/`。

构建失败保留原服务；启动失败后通过 `previous.env/next.env` 核对镜像。数据库迁移后的恢复需配套数据库、附件、配置及镜像，接入说明见[数据库初始化](../migrations/README.md)。

持久文件、凭据或备份仍在 `target` 时禁止构建清理，首次配置放在构建目录外。
