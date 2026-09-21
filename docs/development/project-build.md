# 目录与构建

## 目录

- 未经确认不新增项目顶层目录。
- `backend` 根只放 `pom.xml`，`.m2` 只放本机 `settings.xml` 和 `repo`。
- 后端模块使用 `cyys-common`、`cyys-admin`、`cyys-business`。
- SQL 迁移放 `migrations`，脚本放 `scripts`，部署文件放 `deploy`，前端放 `admin-ui`。
- 构建和运行日志只放 `backend/logs`。
- 不向项目根、`backend` 根或 `backend/.m2` 写临时文件。
- 保留 Maven `target` 的正常构建布局。

## 环境准备

- Java 和 Node.js 版本以 `backend/pom.xml` 和 `admin-ui/package.json` 为准。
- 工具启动方式遵循当前执行环境的约定。
- 首次准备 Maven 配置使用[配置示例](maven-settings.example.xml)，不覆盖已有文件。
- 本机仓库、镜像和凭据仅配置在实际 `settings.xml` 中，不提交凭据。
- Maven 使用项目的 `backend/.m2/settings.xml`，不使用 `-Dmaven.repo.local`。

在项目根目录执行：

```powershell
New-Item -ItemType Directory -Path backend/.m2 -Force | Out-Null
if (!(Test-Path -LiteralPath backend/.m2/settings.xml)) {
    Copy-Item -LiteralPath docs/development/maven-settings.example.xml -Destination backend/.m2/settings.xml
}
```

示例使用 Maven 默认本地仓库，不包含私有镜像或认证信息。实际配置继续由 Git 忽略。

## 后端命令

在 `backend` 目录执行；日志重定向遵循本机工具规则。

```text
mvn -s .m2/settings.xml clean install
mvn -s .m2/settings.xml -pl cyys-business spring-boot:run
```

集成测试和运行服务前，按[部署说明](../../deploy/README.md)准备数据库、Redis 等环境。
