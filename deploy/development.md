# 开发环境部署

在项目根目录的 PowerShell 7.4+ 中执行：

```powershell
./scripts/deploy-dev.ps1 web   # 只更新前端
./scripts/deploy-dev.ps1 api   # 只更新后端
./scripts/deploy-dev.ps1 all   # 前后端一起更新
```

命令复用本机 Docker Desktop Linux 环境，只构建和替换指定的服务。前端部署不重启 API、不操作数据库；后端部署先备份数据库和附件，保留 Web 容器。成功后访问 https://localhost/，仅 API 重启后需要重新登录。

必须指定 `web`、`api` 或 `all`，不默认更新全部。前端检查静态页面和 Nginx，后端通过已有 Web 代理检查 API。

默认更新现有 `cyys-local-acceptance` 项目。优先使用 `deploy/.env.dev`；未创建时沿用已有的 `backend/cyys-business/target/docker-local/.env`。日常运行不重新配置环境；前端依赖只在缺失或锁文件变化时安装。

后端启动模块为 `cyys-application`。旧 `backend/cyys-business/target` 仅保留本机运行配置和历史材料，不参与 Maven 构建；已有部署继续读取原配置位置。

## 首次使用

1. 准备 PowerShell、项目要求的 Java/Node.js、Maven、Git、Docker Desktop Linux 引擎及 MySQL 客户端。
2. 按[构建配置](../docs/development/project-build.md#环境准备)准备 `backend/.m2/settings.xml`。
3. 将[开发配置示例](.env.dev.example)复制为 `deploy/.env.dev`，填写已有本机 MySQL、Redis、基础镜像 digest、证书和私有文件目录；密码使用独立文件。
4. 现有工具链脚本默认位于工作区 `scripts/use-local-toolchain.ps1`；其他位置用 `-ToolchainScript` 指定。MySQL 客户端从 PATH 或唯一的标准安装目录查找，也可用 `-MySqlDump` 指定。

已有本机配置可直接运行，不必重复上述步骤。新准备的私有目录、密码和证书须允许容器 UID 10001 访问；命令不创建数据库、Redis、账号或证书。

## 排查与恢复

按目标追加 `-Mode Check` 仅检查配置，追加 `-Mode Build` 仅构建产物；均不停止或更新服务。

结果和日志在 `backend/logs/dev-*/`，备份在 `deploy/.local/dev-*/`，均不提交。前端仅备份部署配置和 TLS 文件；涉及后端时才停服务备份数据库、附件和后端凭据，此期间不要启动其他同库写入进程。

构建失败保留运行中的服务；备份失败尝试恢复运行状态。新服务启动失败时保留现场，不自动回滚数据库。`previous.env` 和 `next.env` 保存切换前后的镜像配置；数据库迁移后的恢复须配套恢复数据库、附件、配置及对应镜像。

命令跳过后端测试，检查服务健康和部署结果。Flyway 随应用启动执行；数据库准备和已有库接入遵循[初始化说明](../migrations/README.md)。HTTP 检查通过不代表业务回归通过。

配置、附件或备份位于 `target/docker-local` 时不要执行 Maven `clean`。首次配置建议把私有文件、凭据和证书放在构建目录外。备份包含敏感信息，应限制本机目录访问权限。
