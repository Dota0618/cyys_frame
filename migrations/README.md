# 数据库初始化

## 新库入口

[system/V1__init_system_schema.sql](system/V1__init_system_schema.sql) 创建系统结构及内置数据。数据库使用 MySQL 8、InnoDB、utf8mb4。

初始化不创建账号、密码或实际机构，接入项目按部署流程准备管理员及总单位。

## 执行方式

- 按[运行配置](../README.md#运行配置)提供数据库和 Redis，应用启动时由 Flyway 执行迁移。
- 新库执行 V1，已有受管库沿用迁移历史及增量脚本；已有未纳管库先适配结构和历史。
- 仅使用应用内的 Flyway，不重复手工导入 SQL 或设置另一套初始化容器。

## 同步到其他项目

- 保留接入项目的业务迁移，system/business 共用版本序列及历史表。
- 已执行迁移保持不变，结构和内置数据变化新增迁移。
- 更新打包资源时清除旧 SQL 副本，保留构建目录内的本机配置和文件。

## 当前表与关联职责

结构以初始化 SQL 和后续迁移为准；业务接入见[数据库规则](../docs/development/backend/database-query.md)、[身份规则](../docs/development/backend/identity-context.md)及[权限规则](../docs/development/backend/data-permission.md)。

## 验证边界

数据库使用约定统一见[验证与保存](../docs/development/verification.md)。
