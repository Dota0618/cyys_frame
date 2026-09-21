# 开发环境部署

在项目根目录的 PowerShell 7.4+ 中执行：

```powershell
./scripts/deploy-dev.ps1
```

命令复用本机 Docker Desktop Linux 环境，完成后端打包、前端类型检查与构建、镜像构建、停服务备份、容器更新和 HTTP 检查。成功后访问 https://localhost/，重新登录。

默认更新现有 `cyys-local-acceptance` 项目。优先使用 `deploy/.env.dev`；未创建时沿用已有的 `backend/cyys-business/target/docker-local/.env`。日常运行不重新配置环境；前端依赖只在缺失或锁文件变化时安装。

## 首次使用

1. 准备 PowerShell、项目要求的 Java/Node.js、Maven、Git、Docker Desktop Linux 引擎及 MySQL 客户端。
2. 按[构建配置](../docs/development/project-build.md#环境准备)准备 `backend/.m2/settings.xml`。
3. 将[开发配置示例](.env.dev.example)复制为 `deploy/.env.dev`，填写已有本机 MySQL、Redis、基础镜像 digest、证书和私有文件目录；密码使用独立文件。
4. 现有工具链脚本默认位于工作区 `scripts/use-local-toolchain.ps1`；其他位置用 `-ToolchainScript` 指定。MySQL 客户端从 PATH 或唯一的标准安装目录查找，也可用 `-MySqlDump` 指定。

已有本机配置可直接运行，不必重复上述步骤。新准备的私有目录、密码和证书须允许容器 UID 10001 访问；命令不创建数据库、Redis、账号或证书。

## 排查与恢复

仅检查配置：`./scripts/deploy-dev.ps1 -Mode Check`。仅构建产物：`./scripts/deploy-dev.ps1 -Mode Build`。这两个模式均不停止或更新服务。

结果和日志在 `backend/logs/dev-*/`，停服务后的数据库、附件、部署配置和凭据备份在 `deploy/.local/dev-*/`，均不提交。部署期间不要启动其他连接同库的写入进程。

构建失败保留旧服务；备份失败尝试恢复原先运行的服务。新服务启动失败时保留现场，不自动回滚数据库。`previous.env` 保存旧镜像配置，`next.env` 保存本轮镜像配置；数据库迁移后的恢复须配套恢复数据库、附件、配置及对应镜像。

命令会跳过后端测试，不运行空库、历史恢复或全量页面验收。正式 Flyway 迁移随应用启动执行；HTTP 检查通过不代表业务回归通过。

沿用旧 `target/docker-local` 配置时不要执行 Maven `clean`。首次配置建议把私有文件、凭据和证书放在构建目录外。备份包含敏感信息，应限制本机目录访问权限。
