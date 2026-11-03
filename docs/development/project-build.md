# 构建与运行

目录和模块约定见[项目结构](project-structure.md)，部署配置见[开发部署](../../deploy/development.md)。

## 环境准备

- Java、Node.js 版本以 `backend/pom.xml`、`admin-ui/package.json` 为准；工具按工作区约定加载。
- Maven 使用 `backend/.m2/settings.xml`；首次从[配置模板](maven-settings.example.xml)复制，不覆盖已有配置。
- 本机仓库、镜像和凭据只在实际 `settings.xml` 中维护，不提交凭据。

## 后端命令

在 `backend` 目录执行：

```text
mvn -s .m2/settings.xml -DskipTests install
mvn -s .m2/settings.xml -pl cyys-application spring-boot:run
```

## 前端命令

在 `admin-ui` 目录执行：

```text
npm ci
npm run dev
npm run build
```

依赖缺失或锁文件变化时执行 `npm ci`。开发端口为 `8000`，后端默认地址为 `http://127.0.0.1:8080`，可通过 `CYYS_API_TARGET` 修改。

## 产物与文件

- 后端产物为 `backend/cyys-application/target/cyys-application.jar`，前端产物为 `admin-ui/dist`。
- 构建和运行日志放在 `backend/logs`；临时文件不写入项目根、`backend` 根或 `backend/.m2`。
- 保留 Maven `target` 布局；私有文件、凭据、证书和运行配置放在构建目录外。
- 持久数据仍在构建目录时，按[持久文件保护](../../deploy/development.md#排查与恢复)处理，不执行 Maven `clean`。
