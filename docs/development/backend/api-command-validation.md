# API 命令与输入校验

- admin 包使用 `com.cyys.admin.<功能>.<职责>`。
- 业务包使用 `com.cyys.modules.<功能>.<职责>`。
- 按需使用 controller、model、dto、service、mapper，不增加空层。
- common 只提供技术能力，不查询 admin 业务表。
- Controller 只调用一个主要 Service。
- 请求体基础约束写在 DTO，由 Controller 的 `@Valid` 触发。
- 路径和查询参数在 Controller 校验。
- Service 校验跨字段、归属、业务状态和版本冲突。
- Service 不重复非空、长度、格式等基础校验。
- 持久化 Model 不重复请求校验，数据库保留完整性约束。
- 业务异常在 Service 抛出，由全局处理器统一响应。
- 保留现有响应编号，不增加异常分类方法。
- DTO 按操作定义，只保留允许提交的字段。
- 独立请求和响应类型单独放在功能的 `dto` 包。
- 路径中的 ID 不在请求体重复传入。
- 字段使用明确类型，稳定默认值由服务端赋值。
- 返回只读 DTO，不暴露持久化 Model、密码或内部异常详情。
- 跨业务操作由主要 Service 协调事务。
- 文件清理和密码错误计数保留分段事务。
- 新增和编辑共用的选项保留一个查询方法。
- 不默认增加 interface/Impl、Manager、Facade、Converter 层。
- 权限和数据范围遵循[权限规则](data-permission.md)。

## 已实现

- 文件名校验：`@SafeFileName`。
- 表格导出与下载：`BoundedXlsx`、`DownloadResponse`。
- 出站 HTTP：Boot `RestClient.Builder`。
