# 项目结构

## 目录与职责

| 位置 | 职责 |
|---|---|
| `backend/cyys-common` | 通用技术能力 |
| `backend/cyys-admin` | 系统管理功能 |
| `backend/cyys-application` | 启动、配置和模块装配，生成唯一可执行 JAR |
| `admin-ui` | 前端源码与依赖 |
| `migrations` | [数据库初始化与版本迁移](../../migrations/README.md) |
| `scripts`、`deploy` | 命令脚本、部署配置与说明 |
| `docs/development`、`docs/versions` | 开发约定、[版本与任务资料](../versions/README.md) |

- `backend` 根只放 `pom.xml`；`backend/.m2` 只放本机 `settings.xml` 和 `repo`。
- 产物和日志位置见[构建与运行](project-build.md#产物与文件)。
- admin 包使用 `com.cyys.admin.<功能>.<职责>`，独立业务用 `com.cyys.<业务>`，启动包为 `com.cyys.application`。
- 按需划分 controller、model、dto、service、mapper；不预建空层或默认增加 interface/Impl、Manager、Facade、Converter。
- common 提供技术能力，不查询 admin 业务表；请求和响应见[API 规则](backend/api-command-validation.md)。

## 新增业务模块

- 按完整业务域创建 `backend/cyys-<业务>`，与 `cyys-admin` 平级；不按表拆模块，不预建空模块。
- 业务模块依赖 `cyys-common`，按需依赖 `cyys-admin`；两者不反向依赖业务模块，模块间不得循环依赖。
- 在 `backend/pom.xml` 登记模块，在 `cyys-application/pom.xml` 登记对应依赖；版本与父工程一致。
- 业务模块不配置 Spring Boot 打包插件，由 `cyys-application` 统一装配。
