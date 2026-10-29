# 官方模板与组件复用

- 以官方 Ant Design Pro 为基线，先查[模板清单](ant-design-pro-baseline.md)。
- 优先复用 Pro 组件和项目已有组件。
- antd 用法通过项目 CLI 按锁定版本查询，Pro 组件查官方文档。
- 仅在现有能力缺失且业务需要时自建组件。
- 替换官方实现或改变整体布局前说明并确认。
- 示例页面接入真实接口和权限后才能作为业务入口。
- 普通表单字段复用 `compactFormProps`，采用左标签、右输入布局。
- 富文本、多行、多选、树形勾选和附件复用 `verticalFormItemProps`。
- 布局只覆盖对应表单项，不用全局 CSS 强制所有字段同排。
- 代码生成器沿用相同表单配置。

## 已复用组件

- `DictionarySelect`：字典选择和失效历史值回显。
- `AttachmentField`：有序附件字段。
- `FilePicker`：调用方提供操作专用候选接口。
- `FileUpload`：文件上传。
- `NewPasswordFields`：新密码和确认字段。
