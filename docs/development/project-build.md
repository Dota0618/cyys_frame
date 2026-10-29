# 目录与构建

## 目录

- 未经确认不新增项目顶层目录。
- `backend` 根只放 `pom.xml`，`.m2` 只放本机 `settings.xml` 和 `repo`。
- `cyys-common` 提供通用技术能力，`cyys-admin` 提供系统管理功能。
- `cyys-application` 只负责启动、配置和模块装配，生成唯一可执行 JAR。
- 新业务按完整业务域新建 `backend/cyys-<业务>`，与已有模块平级。
- 同一业务域的功能放在同一模块，不按每张表或页面拆模块。
- 业务模块依赖 `cyys-common`，需要系统能力时再依赖 `cyys-admin`。
- `common`、`admin` 不依赖具体业务模块，业务模块不依赖启动模块。
- 模块之间不得循环依赖。
- 新模块在 `backend/pom.xml` 登记，并由 `cyys-application/pom.xml` 添加装配依赖。
- 新业务的单元测试放在所属模块的 `src/test/java`，跨模块集成测试放在启动模块的 `src/test/java`。
- 不为尚未开展的业务创建空模块。
- SQL 迁移放 `migrations`，新库使用[单文件初始化](../../migrations/README.md)；脚本放 `scripts`，部署文件放 `deploy`，前端放 `admin-ui`。
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
mvn -s .m2/settings.xml -DskipTests install
mvn -s .m2/settings.xml -pl cyys-application spring-boot:run
```

测试范围按[验证与保存](verification.md)选择。持久文件、配置或备份位于构建目录时，遵循[开发部署说明](../../deploy/development.md#排查与恢复)保护这些文件，不执行 Maven `clean`。

集成测试和运行服务前，按[部署说明](../../deploy/README.md)准备数据库、Redis 等环境。

## 新业务模块最小示例

以新增 `cyys-orders` 为例，源码放在 `backend/cyys-orders/src/main/java/com/cyys/orders/`。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.cyys</groupId>
        <artifactId>cyys-framework</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>cyys-orders</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.cyys</groupId>
            <artifactId>cyys-common</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</project>
```

父工程添加 `<module>cyys-orders</module>`；启动模块添加对应 `com.cyys:cyys-orders:${project.version}` 依赖。业务模块不配置 Spring Boot 打包插件。
