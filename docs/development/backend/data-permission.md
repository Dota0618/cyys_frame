# 功能权限与数据范围

## 功能权限

- 普通操作权限在 Controller 使用 `@SaCheckPermission`。
- 普通 Service 增删改查不重复检查入口权限。
- 全类权限相同时放类上，不同动作放方法上。
- 新增/编辑共用选项使用 `SaMode.OR` 接受任一权限。
- 切换机构不切换角色和操作权限。
- `SUPER_ADMIN` 角色的授予上限为全部启用的操作权限，不按登录名判断。
- `SUPER_ADMIN` 的数据范围、最后管理员保护和机构授权校验仍然生效。

## 数据范围

- 每个账号在 `sys_user_scope` 保存一份所属机构、部门和统一数据范围。
- 范围取值：0 全系统、1 本机构、2 自定义部门、3 本部门、4 本部门及下级、5 本人、6 本机构及下级。
- 新账号默认本人范围。
- 自定义部门只包含明确勾选项。
- 机构树和部门树分别维护。
- 所选机构必须位于授权范围内，并进一步缩小查询结果。
- 新记录归属当前选定机构。
- 通用范围由 MyBatis-Plus `DataPermissionInterceptor` 处理。
- 数据范围上下文只保存视图，不绑定功能权限码。
- SQL 类型以 MyBatis `MappedStatement` 为准。
- 只读事务和跨机构只读视图禁止写入。
- 缺少上下文、未登记表、解析失败或子查询未被插件覆盖时拒绝执行。
- 不得通过忽略拦截器、原生 Statement 或直接 JDBC 绕过范围。

## 业务接入

- 模型使用 `@DataResourceDefinition`，只配置与默认值不同的字段。
- 默认名称为表名，默认字段为 `scope_id`、`org_id`、`created_by`、`version`。
- 业务 Mapper 使用 `@ProtectedMapper(模型类型.class)`。
- 已注册受保护表的关联查询按各表独立过滤。
- 身份、授权、审计、系统共享目录及手工限定范围的 Mapper 使用 `@ManualDataScope`。
- 每个 Mapper 只能选择一种数据范围声明。
- 普通业务不新增权限 SQL 或专用策略类。

```java
@DataResourceDefinition(immutableColumns = "org_id")
@TableName("biz_example")
public class Example { /* 业务字段 */ }

@ProtectedMapper(Example.class)
public interface ExampleMapper extends BaseMapper<Example> {}
```

## 特殊写入

- 新增归属、关联候选和业务状态由 Service 校验。
- 修改用户数据范围必须具有 `user:data-permission`。
- 用户资料和提交的数据范围在同一事务保存。
- 未提交数据范围时保留原范围。
- 用户授权管理要求所选机构的完整数据范围。
- 不得管理或授予超出本人权限及数据范围的用户、角色。
- 角色、菜单、用户状态和授权写入保留最后可用管理员保护。
- 授权写入按保护锁、用户/角色锁顺序执行，并在锁后核验授权上限。
- 共享附件、密码重置、流水号预留保留各自的条件性授权检查。
