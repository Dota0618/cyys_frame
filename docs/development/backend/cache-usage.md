# 缓存使用

## 规则

- 关联展示字段用 DTO 的 `@CacheValue`，批量读取缓存及回源；主记录字段直接返回。
- 缓存保存最小 DTO，缺失值返回 null；授权、筛选和保存校验使用实时数据。
- 接收字段使用可空类型，展示缓存不回退历史值。
- 写事务提交后失效缓存；自动失效和手动刷新使用同一分组。
- 业务复用缓存组件，不直接拼接、扫描 Redis 键或清理登录会话。

## 已缓存清单

| cache | 范围 | 来源 |
|---|---|---|
| `user` | 系统共享，含停用用户 | `UserCacheSource` |
| `scope` | 系统共享，含停用单位 | `ScopeCacheSource` |
| `department` | 实际所属单位 | `DepartmentCacheSource` |
| `dictionary-item`、`dictionary-options` | 系统共享 | `DictionaryItemCacheSource/DictionaryOptionsCacheSource` |
| `configuration-values` | 系统共享 | `ConfigurationLookupService` |

字段、调用处和失效入口以来源类及 `BusinessCacheGroups` 为准。

## 用法

- `cache/key/field` 分别指定来源、匹配键和展示字段，省略 `field` 时取记录或列表项。
- 列表省略 `key` 返回完整列表；`variant` 指定字典名称，`scopeKey` 指定实际机构。
- HTTP 成功响应自动回填，其他调用用 `CachedResponseValues.enrich`。
- 组及记录失效分别用 `invalidateAfterCommit`、`invalidateEntriesAfterCommit`。

## 新功能接入

- 在 `BusinessCacheGroups` 登记区域及分组，来源实现 `CacheValueSource` 并复用 `RecordCache` 或 `ScopedHashCache`。
- 来源 `name()` 与注解名称一致；手动刷新登记对应定义。

## 缓存管理

- 管理页维护定义和刷新组，字段与已登记区域一致；同组使用相同范围并整体刷新。
- 定义维护及刷新使用 `cache:user:refresh`，配置限已登记区域。
