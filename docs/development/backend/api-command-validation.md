# API 命令与输入校验

包与分层按[项目结构](../project-structure.md)组织。

- Controller 调用主要 Service；跨业务事务由该 Service 协调。
- DTO 通过 `@Valid` 校验非空、长度和格式，Controller 校验路径及查询参数。
- Service 校验跨字段、归属、状态和版本，Model 不重复请求校验。
- DTO 按操作定义，单独放所属功能的 `dto` 包；请求只接收可修改字段，路径 ID 不在请求体重复。
- 字段使用明确类型，默认值由服务端赋值；返回只读 DTO，排除密码及内部异常详情。
- 业务异常统一处理，保留既有响应编号。
- 新增和编辑共用选项查询；权限和范围按[权限规则](data-permission.md)执行。
- 文件清理和密码错误计数保留分段事务。

## 公共能力

- 文件名校验复用 `@SafeFileName`。
- 表格导出和下载复用 `BoundedXlsx`、`DownloadResponse`。
- 出站 HTTP 复用 Boot `RestClient.Builder`。
