# 当前迁移设计与操作边界

本文记录数据库结构、关联校验及升级边界。Phase 3 的实现和 G1 证据见 [验收记录](../docs/migration/phase3-acceptance.md)。

## 唯一执行入口与资源

由业务应用中的 Flyway 执行，默认加载 `classpath:db/migration`。构建只将本目录 system/business 中的版本 SQL 打进 JAR；不包含 dev-data。使用一个 schema history，不按功能另建迁移引擎。

- V1：已执行的空库系统结构，历史内容和校验值保持不变。
- V2：保留给本地身份初始化，文件在 dev-data，默认不加载。需要时由同一个 Flyway 显式添加该文件系统位置。
- V3：移除 V1 的全部数据库外键；增加部门版本和唯一键、成员部门字段、规范化角色部门范围及数据操作审计。最终结构没有外键。
- V4：成员关系增加本单位 display_name、version 和创建时间索引；初次从全局昵称/姓名/登录名回填，此后的单位编辑不改全局资料。
- 后续正式迁移从 V5 递增，system/business 共用版本序列。
- 测试单位由测试配置在正式迁移之后准备、测试类结束时清理，不占用版本号、不留下测试迁移历史。

`baseline-on-migrate=false`、`clean-disabled=true`、`validate-on-migrate=true` 防止把未核对的旧库自动当成新基线。V1 在本次调整前尚未接入版本迁移权威；改造前内容已保存到本地源码快照。不要拿新 V1 覆盖某个已有迁移历史的已部署库。

## 首次本地运行

先核对现有 MySQL 中明确选择的数据库及其迁移历史，再注入连接配置。空 schema 执行 V1、V3、V4；含旧表或业务数据的 schema 需要先制定对应升级脚本，不能删表重建或自动 baseline。使用 MySQL 8、InnoDB、utf8mb4，排序规则按部署约定统一。没有内置默认账号或密码。

需要本地初始身份时，在首次空库迁移前，于被版本控制忽略的外部 application-local.yml 中显式增加：

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration
      - filesystem:F:/git/frame/cyys-framework/migrations/dev-data
    placeholders:
      bootstrapScopeId: unit-default
      bootstrapAccountId: local-admin
      bootstrapLoginName: local-admin
      bootstrapPasswordHash: "<自行生成的有效 BCrypt 散列>"
cyys:
  data-isolation:
    mode: single
    single-scope-id: unit-default
```

该文件还需提供数据库连接参数；显式激活 local profile 或指定外部配置位置。四个占位符缺少任一个，Flyway 都应停止；不要把示例占位文字直接作为密码散列使用，也不要把这份本地配置提交到版本库。

V2 仅创建一个实际成员关系和一项显式平台单位访问权限，不创建通用管理 CRUD 权限。生产环境不得加载 dev-data，生产初始管理员应由部署流程提供受控的身份数据并独立验收。身份初始化后，single 启动校验要求其单位主键存在且启用。

V2 在本地首次初始化时按 V1 → V2 → V3 顺序执行。已经到 V3 的库不会自动补执行较小版本 V2；不得为补种子开启全局乱序迁移。此时应另行准备受控的身份初始化事务。当前 cyys_frame 没有实际使用账号，不能把测试账号当作本地管理员。

## 当前表归属

| 表 | 分类与边界 |
|---|---|
| sys_user | 全局账号，身份查询受控；不会直接作为接口输入输出 |
| sys_scope | 受保护的平台单位目录 |
| sys_menu | 受保护的共享菜单/权限定义；type=1/2/3，平台专用权限不能由单位角色生效 |
| sys_user_scope | 单位成员关系；读取同时检查账号、单位、成员状态及有效期 |
| sys_role | GLOBAL 平台角色或具体单位角色；GLOBAL 不是业务数据拥有者 |
| sys_user_role | 授权关系与被授予角色必须同 scope_id；由 RoleGrantService 写入校验和读取条件共同保证 |
| sys_role_menu | 通过角色决定授权所属范围，不伪造独立业务单位 |
| sys_org | 单位内部门，归属不等于独立单位 |
| sys_role_org | 自定义部门范围；role_id、org_id 对应角色和部门，scope_id 必须同时匹配两者，读取拒绝错配 |
| sys_data_audit | 成功写入事件；真实 actor_id、scope_id 和 resource/record_id 由业务服务填写，与业务一起提交或回滚 |

关联关系只在文档中声明：成员关联 user/scope；授权关联 user/role/scope；角色菜单关联 role/menu；部门关联 scope 及同单位父部门；成员 org_id 和角色部门范围关联同单位部门。部门删除检查子部门、成员及角色范围引用。单位、账号、角色和菜单的管理删除入口尚未开放，后续启用前必须按这些关系定义删除/停用影响，不能依赖数据库级联。

登录名全局唯一。角色编码在 scope 内唯一。成员和授权关联保留业务唯一键：软删除后需要恢复原关系，不通过重新插入相同关系绕开冲突。多个有效默认单位会被身份解析拒绝，后续成员写服务仍需保证最多一个有效默认值。

成员管理使用 sys_user_scope.id 作为页面记录 ID，user_id 由服务端解析。新增流程在同一事务创建全局账号和当前单位成员；已有账号加入另一单位尚无公开入口。编辑只改 display_name、org_id、status，带 version 处理并发；单位归属、账号、密码、默认单位和成员身份不由编辑表单修改。角色操作只替换当前单位的授予记录，保留其他单位和 GLOBAL 授予。

## 旧数据与模式切换

现有 cyys_frame 已执行正式 V1、V3、V4；没有向只读旧框架数据库写入。旧组织样本的显式归属转换已在同库专属测试表演练。完整旧库的登录名冲突、旧密码、单位归属、主子关系、附件/回收数据仍需逐项盘点；不明确的归属隔离待处理，不能默认写入某个单位。

旧库升级需要独立的版本脚本和核对报告，包含数据量、重复键、错配角色、孤立关联与回滚路径。本次不通过删除旧数据证明新结构可用，也不自动修复旧库数据。

V3 在移除外键前检查旧自定义部门串和范围值。非空 custom_orgs 需要独立升级方案，保存并核对明确的 role/scope/org 映射；本版不自动转换非空旧串，不能清空原字段以跳过检查。MySQL DDL 不保证整个版本原子回滚，正式升级前备份并预检；失败后按已执行语句核对，不直接 repair 掩盖失败。

模式变更采取停机调整：先停止实例、保留原归属数据、统一部署配置，再启动并重新登录。当前会话为进程内存，重启后失效；无运行任务传播机制，也没有热切换功能。single/multi 切换不会移动或合并数据。

## 验证范围

自动测试只使用 MySQL，通过 `CYYS_TEST_DB_URL/USERNAME/PASSWORD` 指向明确选定的现有测试 schema，不自动创建数据库。使用正式 V1/V3、实际 Mapper、HTTP 和 Redis 验证。MySqlTestDatabaseGuard 在应用刷新及 Flyway 之前检查目标：只接受空 schema 或本套测试已初始化且不含业务数据的 schema。不会清空用户数据库；不明残留拒绝自动处理。

结构和旧样本演练在同一库的专属 `g1_mig_` 表执行：仅给正式 SQL 的对象名加测试前缀，专属历史从 0 起步，覆盖空结构 V1/V3、显式 V1/V2/V3、代表性组织样本和失败反例。开始前拒绝覆盖已有同名前缀表，结束后只移除本例创建的表；不修改正式迁移历史，不调用 Flyway.clean。实际结果见 [改造进度](../docs/migration/progress.md)。

接入机制核对依据：[Spring Boot 数据库初始化说明](https://docs.spring.io/spring-boot/how-to/data-initialization.html)、[Sa-Token 配置说明](https://github.com/dromara/Sa-Token/blob/dev/sa-token-doc/use/config.md)。这些资料说明框架机制，不代替本项目的行为验收。
