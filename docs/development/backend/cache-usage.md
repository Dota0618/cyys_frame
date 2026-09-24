# 缓存使用

## 规则

- 关联展示字段使用返回 DTO 的 `@CacheValue`，复用已登记缓存。
- 主记录自身字段直接返回，不额外缓存回填。
- 整页批量读缓存，未命中再批量查库，不保留重复展示查询。
- 授权、筛选、排序、候选和保存校验不依赖展示缓存。
- 用户展示缓存包含停用和跨机构用户，不重复查其机构或权限。
- 缓存只保存最小展示 DTO，不保存敏感实体。
- 取值时校验声明类型，缺记录或字段返回 null。
- 接收字段使用可空类型，不做启动预检查或历史值回退。
- 写事务提交后失效相关缓存，回滚不失效。
- 自动失效和手动刷新共用分组，不清空 Redis 或登录会话。
- 修改缓存时更新下方来源、字段、使用处和失效入口。

## 已缓存清单

| cache | 形态 / 类型与字段 | 匹配键 | 范围 | 来源 |
|---|---|---|---|---|
| `user` | 记录 `UserLabel`：id、name 为 String，active 为 boolean | 用户 ID | 系统共享，含停用用户 | `UserCacheSource` |
| `scope` | 记录 `ScopeLabel`：id、name、shortName 为 String | 单位 ID | 系统共享，含停用单位 | `ScopeCacheSource` |
| `department` | 记录 `DepartmentLabel`：id、name 为 String | 部门 ID | 实际所属单位 | `DepartmentCacheSource` |
| `dictionary-item` | 记录 `DictionaryOption`：value、label 为 String，disabled 为 boolean | 字典内部名称＋value | 系统共享 | `DictionaryItemCacheSource` |
| `dictionary-options` | 列表 `List<DictionaryOption>`，仅有效候选 | 字典内部名称；列表项按 value 匹配 | 系统共享 | `DictionaryOptionsCacheSource` |
| `configuration-values` | Hash 字符串值 | 参数名称 | 系统共享 | `ConfigurationLookupService` |

| 缓存组 | 已使用处 | 提交后失效入口 |
|---|---|---|
| 用户 | `OrgNoticeReadView.userName`、`DepartmentView.leaderName/leaderActive`、`UserDisplayService.names`、`RecycleRecord.deletedByName` | `MemberService.create/edit`，仅对应用户 ID |
| 单位 | `ScopeInfo.name`：登录资料、当前用户资料、右上角单位列表、工作台；`MemberView.scopeName`：用户列表、详情；`RecycleRecord.scopeName` | `ScopeManagementService.create/update`，仅对应单位 ID；当前没有单位删除入口 |
| 部门 | `MemberView.orgName`：用户列表、详情、导出；`RecycleRecord.departmentName`，按记录实际单位取值 | `DepartmentService` 新增、修改、删除，失效对应单位部门组 |
| 字典 | `OrgNoticeView`、`SystemNoticeView`、`ReleaseView` 的类型名称/状态；`OrgNoticeOptions.types`、`AuditView` 的操作/结果名称、`AuditOptions` 候选列表；系统通知/版本候选列表 | `DictionaryService` 分类和字典项增删改，同时失效记录与列表缓存 |
| 全局参数 | 配置管理列表/详情、公共配置接口、地区根节点和密码策略 | `ConfigurationService` 新增、修改后按参数名称失效；支持手动刷新全局参数缓存 |

## 用法

- `cache` 指定来源，`key` 指定当前 DTO 的匹配字段。
- `field` 指定展示字段；省略时取整条记录或列表项。
- 列表缓存省略 `key` 时返回完整列表。
- `variant` 指定字典内部名称。
- `scopeKey` 指定实际机构字段；按机构缓存省略时使用当前机构。
- 成功 HTTP 响应自动回填，其他调用使用 `CachedResponseValues.enrich`。
- JSON 省略 null，前端接收字段声明为可选。
- 新来源实现 `CacheValueSource`，分组登记在 `BusinessCacheGroups`。
- 组失效使用 `invalidateAfterCommit`，按 ID 失效使用 `invalidateEntriesAfterCommit`。
- 不通过数据库配置类名或方法名执行反射。

## 缓存管理

- 缓存管理维护缓存定义，不保存或编辑缓存值。
- 每个定义登记展示名称、实际缓存区域标识、展示分组、刷新组和作用范围。
- 同一刷新组中的多个缓存区域必须使用同一作用范围，手动操作只能刷新整个刷新组。
- 自动失效和手动刷新共用 `BusinessCacheGroups` 的真实缓存区域，不通过数据库配置类名、方法名或 Redis 键执行反射。
- 缓存定义的新增、修改、删除和刷新均使用 `cache:user:refresh` 权限。

```java
public record ReaderView(String userId,
    @CacheValue(cache="user", key="userId", field="name") String userName) {}
```
