# 全局参数规则

## 规则

- 全局参数统一存储在 `sys_configure`。
- 全局参数值通过 Redis 缓存读取，Redis 不可用时回源数据库。
- 参数修改提交后失效对应缓存，参数读取不得依赖前端固定值。
- `SYSTEM_NAME` 用于登录页、主页面左上角和浏览器标签页显示。
- `SYSTEM_PROVINCE_CODE` 设置后，地区树和地区选择仅显示该编码地区及其下级。
- `PWD_STRENGTH` 只允许 0、1、2；仅约束个人修改密码，用户新增、用户修改和管理员重置密码不受字母/数字组合要求；所有明文新密码至少 8 个字符。
- `PWD_LOCK_NUM` 设置连续密码错误锁定次数；0 表示不锁定。
- `PWD_LOCK_HOUR` 设置密码锁定时长，单位为小时。
- `COPYRIGHT_TEXT` 设置页脚版权文本；为空时使用系统名称和当前年份。
- `MAINTENANCE_MODE` 只允许 0、1；1 时显示维护提示并限制普通账号登录。
- `PASSWORD_EXPIRE_DAYS` 设置密码有效期，单位为天；0 表示永不过期。
- `FILE_MAX_SIZE_MB` 设置单个文件最大大小，单位为 MB。
- `EXPORT_MAX_ROWS` 设置单次同步导出最大行数，最大为 5000。
- `AUDIT_RETENTION_DAYS` 设置审计记录保留天数；0 表示不自动清理，当前暂不执行后台清理任务。
- `CONTACT_PHONE` 设置页脚显示的系统联系电话；为空时不显示。
- 全局参数记录只允许修改，不允许删除；前端不显示删除入口，后端拒绝删除请求。

## 已实现

| 参数 | 默认值 | 用途 |
|---|---:|---|
| `SYSTEM_NAME` | `CYYS` | 系统展示名称 |
| `SYSTEM_PROVINCE_CODE` | 空 | 限定地区树和地区选择的根节点 |
| `PWD_STRENGTH` | `0` | 密码组成要求 |
| `PWD_LOCK_NUM` | `5` | 连续错误锁定次数 |
| `PWD_LOCK_HOUR` | `1` | 锁定时长 |
| `COPYRIGHT_TEXT` | 空 | 页脚版权文本 |
| `MAINTENANCE_MODE` | `0` | 维护模式 |
| `PASSWORD_EXPIRE_DAYS` | `0` | 密码有效期 |
| `FILE_MAX_SIZE_MB` | `20` | 单文件大小上限 |
| `EXPORT_MAX_ROWS` | `5000` | 单次导出行数上限 |
| `AUDIT_RETENTION_DAYS` | `365` | 审计保留天数；当前暂不自动清理 |
| `CONTACT_PHONE` | 空 | 页脚联系电话 |
