# 数据库初始化

## 新库入口

单租户框架只维护一份初始化文件：[system/V1__init_system_schema.sql](system/V1__init_system_schema.sql)。它合并原 V1、V3–V42 的最终结构与原菜单初始化数据，直接创建当前结构，不再逐个执行历史改表脚本。

初始化内容：

- 27 张系统表及其字段注释、索引和检查约束，统一使用 MySQL 8、InnoDB、utf8mb4、utf8mb4_unicode_ci，无数据库外键。
- 中文菜单、操作权限、数据回收站目录与资源登记；已废弃的租户权限和旧平台角色入口不再初始化，代码生成器保持隐藏。
- 登录结果、操作审计操作、操作审计结果三类系统字典及字典项。
- 系统名称、密码策略、文件类型等 15 项全局参数，以及 6 项缓存定义。

不创建账号、密码、角色、授权关系、机构、部门、地区样本或业务数据。初始管理员和实际总单位由接入项目按部署流程受控准备，不能再加载已删除的 `dev-data`。

## 执行方式

1. 在 MySQL 中准备明确选定的空数据库，设置字符集和排序规则。
2. 按[运行参数](../README.md#运行参数)提供数据库及 Redis 连接配置。
3. 启动应用，由唯一的 Flyway 引擎执行 V1，并写入 `flyway_schema_history`。再次启动只校验历史，不重复初始化。

应用默认加载 `classpath:db/migration`，构建将 `system/V*.sql` 和后续 `business/V*.sql` 打入唯一可执行 JAR。保留 `validate-on-migrate=true`、`baseline-on-migrate=false`、`clean-disabled=true`；非空且未纳管的数据库会被拒绝。

不要先手工导入 SQL 再启动应用，也不要配置另一套 SQL 初始化容器。菜单、系统字典、全局参数、回收站资源和缓存定义已包含在 V1 中，无需额外导入文件。初始化后通过现有管理页面维护可编辑内容。

## 同步到其他项目

框架只保留新版 V1，原 V3–V42、开发身份和菜单 SQL 及对应旧迁移测试全部删除，不再保留或分发旧迁移链。新版 V1 重新定义了内容和校验值，仅用于新库初始化。

- 新项目同步新版 V1，并移除原 V3–V42 和原 `dev-data` 两个 SQL 文件；应用的 Flyway 位置和构建资源配置保持现状。
- 若项目目录已有编译产物，重建前只删除 `backend/cyys-application/target/classes/db/migration` 中复制的旧 SQL，防止旧版本残留打包。保留其他 `target` 内容；其中可能保存本机配置、附件和备份。
- 已初始化项目同步前，先备份并核对现有结构、数据及历史，单独完成基线切换或增量升级；新版 V1 不能直接用于升级已有库。本次文件清理不删除数据库中的业务数据和迁移历史。
- 已有独立业务模块或业务迁移的项目，保留自身业务脚本及版本序列，不能套用框架的空库重建流程。
- 新基线之后需要变更结构或内置数据时，继续使用新的版本迁移；已执行的 V1 不再改写。`system` 与 `business` 共用同一版本序列和历史表。

## 当前表与关联职责

- `sys_user` 保存统一登录账号；`sys_user_scope` 为每账号唯一用户资料，保存所属机构、部门、姓名、状态、版本和数据范围；自定义部门范围保存在 `sys_user_scope_org`。
- `sys_scope` 是机构树，只允许一个启用的实际总单位；`sys_org` 是机构内部门树。业务表的 `scope_id` 保存真实归属，筛选或移动机构不改写历史数据归属。
- `sys_role`、`sys_user_role`、`sys_role_menu`、`sys_menu` 构成统一角色和菜单授权体系；角色编码系统唯一，账号与角色关系唯一。
- 字典、地区、全局参数、系统通知和版本是系统共享资源，仍需对应操作权限；单位通知保留独立阅读记录，文件引用通过 `sys_file_binding` 维护。
- `sys_file_entry`、`sys_file_part`、`sys_file_dir` 记录文件、分片和目录；文件元数据与对象需配套备份，永久清理沿用文件状态机。
- `sys_recycle_resource` 保存受控的表和字段映射，不接受任意 SQL；恢复和彻底删除仍校验版本、数据范围、依赖和独立权限。
- `sys_serial_rule`、`sys_serial_counter` 使用数据库唯一约束与事务锁保证取号一致，不导入旧计数、不预置业务规则。

表间关系、查询与写入校验见[数据库与 SQL 规则](../docs/development/backend/database-query.md)；身份和数据范围见[身份规则](../docs/development/backend/identity-context.md)及[权限规则](../docs/development/backend/data-permission.md)；文件配置与清理见[文件存储规则](../docs/development/backend/storage.md)。

## 验证边界

数据库验证使用独立 MySQL 空库，核对一次初始化、再次启动不重复执行、最终表结构及内置数据。测试守卫允许当前 V1 的原始初始化数据，拒绝未知表、业务数据和残留测试记录；不会自动清空已有数据库。

原 `LegacyMigrationTest` 及其旧组织 SQL 样本随历史迁移删除，不再演练已移除的 V1→V2→V3 等旧路径。历史验收记录中的版本号只表示当时结果，不能作为新版初始化步骤。
